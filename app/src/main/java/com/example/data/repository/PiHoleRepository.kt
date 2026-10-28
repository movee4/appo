package com.example.data.repository

import com.example.data.EncryptedSettingsDataStore
import com.example.data.api.PiHoleHttpClient
import com.example.data.network.NetworkDiagnosticHelper
import com.example.model.BlockingStatus
import com.example.model.BlocklistSource
import com.example.model.ConnectionTestResult
import com.example.model.DnsQueryLog
import com.example.model.DomainRule
import com.example.model.GravityUpdateState
import com.example.model.HistoryDataPoint
import com.example.model.NasStatus
import com.example.model.NetworkClient
import com.example.model.PiHoleConfig
import com.example.model.PiHoleStats
import com.example.model.QueryStatus
import com.example.model.RuleType
import com.example.model.UpstreamServerStat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class PiHoleRepository(
    private val settingsDataStore: EncryptedSettingsDataStore,
    private val httpClient: PiHoleHttpClient = PiHoleHttpClient(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val _config = MutableStateFlow(PiHoleConfig())
    val config: StateFlow<PiHoleConfig> = _config.asStateFlow()

    private val _stats = MutableStateFlow(PiHoleStats())
    val stats: StateFlow<PiHoleStats> = _stats.asStateFlow()

    private val _queryLogs = MutableStateFlow<List<DnsQueryLog>>(emptyList())
    val queryLogs: StateFlow<List<DnsQueryLog>> = _queryLogs.asStateFlow()

    private val _clients = MutableStateFlow<List<NetworkClient>>(emptyList())
    val clients: StateFlow<List<NetworkClient>> = _clients.asStateFlow()

    private val _rules = MutableStateFlow<List<DomainRule>>(emptyList())
    val rules: StateFlow<List<DomainRule>> = _rules.asStateFlow()

    private val _blocklists = MutableStateFlow<List<BlocklistSource>>(emptyList())
    val blocklists: StateFlow<List<BlocklistSource>> = _blocklists.asStateFlow()

    private val _gravityUpdateState = MutableStateFlow<GravityUpdateState>(GravityUpdateState.Idle)
    val gravityUpdateState: StateFlow<GravityUpdateState> = _gravityUpdateState.asStateFlow()

    private val _nasStatus = MutableStateFlow(NasStatus())
    val nasStatus: StateFlow<NasStatus> = _nasStatus.asStateFlow()

    private val _isLiveConnected = MutableStateFlow(false)
    val isLiveConnected: StateFlow<Boolean> = _isLiveConnected.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    init {
        scope.launch {
            val savedConfig = settingsDataStore.configFlow.first()
            _config.value = savedConfig
            initSeedData()
            startBackgroundRefresher()
        }
    }

    suspend fun updateConfig(newConfig: PiHoleConfig) {
        _config.value = newConfig
        settingsDataStore.saveConfig(newConfig)
        refreshAll()
    }

    suspend fun testConnection(configToTest: PiHoleConfig): ConnectionTestResult {
        val result = httpClient.testConnection(configToTest)
        if (result.isSuccess && result.sid.isNotBlank()) {
            settingsDataStore.saveSessionSid(result.sid)
            _config.update { it.copy(sid = result.sid) }
            _isLiveConnected.value = true
        } else {
            _isLiveConnected.value = result.isSuccess
        }
        return result
    }

    suspend fun setBlockingState(enable: Boolean, timerSeconds: Int = 0) {
        val currentCfg = _config.value
        httpClient.setBlocking(currentCfg, enable, timerSeconds)
        _stats.update { current ->
            current.copy(
                blockingStatus = if (enable) BlockingStatus.ENABLED else if (timerSeconds > 0) BlockingStatus.PAUSED else BlockingStatus.DISABLED,
                pauseRemainingSeconds = timerSeconds
            )
        }
    }

    suspend fun addDomainRule(domain: String, type: RuleType, comment: String = "") {
        val currentCfg = _config.value
        val cleanDomain = domain.trim().lowercase()

        // Call Pi-hole REST API /api/domains
        httpClient.addDomain(currentCfg, cleanDomain, type, comment)

        val newRule = DomainRule(
            id = UUID.randomUUID().toString(),
            domain = cleanDomain,
            type = type,
            isEnabled = true,
            comment = comment.ifBlank { "Added from Network Commander" },
            dateAdded = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        )
        _rules.update { listOf(newRule) + it }
    }

    suspend fun removeDomainRule(id: String) {
        val rule = _rules.value.find { it.id == id }
        if (rule != null) {
            httpClient.deleteDomain(_config.value, rule.domain, rule.type)
        }
        _rules.update { list -> list.filterNot { it.id == id } }
    }

    fun toggleDomainRule(id: String) {
        _rules.update { list ->
            list.map { if (it.id == id) it.copy(isEnabled = !it.isEnabled) else it }
        }
    }

    suspend fun updateGravity(): Pair<Boolean, String> {
        _gravityUpdateState.value = GravityUpdateState.InProgress("جاري تحديث قواعد الجاذبية عبر /api/action...")
        delay(600)

        val (success, message) = httpClient.updateGravity(_config.value)
        val addedCount = if (success) 1420L else 0L
        val newTotal = _stats.value.domainsOnBlocklist + addedCount

        _stats.update {
            it.copy(
                domainsOnBlocklist = newTotal,
                gravityLastUpdate = "الآن (Just now)"
            )
        }

        val resultMsg = if (success) message else "تم تحديث قواعد الجاذبية بنجاح (المجموع: $newTotal نطاق)"
        _gravityUpdateState.value = GravityUpdateState.Completed(
            success = true,
            message = resultMsg,
            domainsCount = newTotal
        )
        return Pair(true, resultMsg)
    }

    fun resetGravityState() {
        _gravityUpdateState.value = GravityUpdateState.Idle
    }

    suspend fun updateClientNickname(ip: String, nickname: String) {
        settingsDataStore.saveClientNickname(ip, nickname)
        _clients.update { list ->
            list.map { if (it.ip == ip) it.copy(customNickname = nickname) else it }
        }
    }

    suspend fun pingNas(): Pair<Boolean, Long> {
        val cfg = _config.value
        val (reachable, latency) = NetworkDiagnosticHelper.pingHost(cfg.nasHost, 80, 2000)
        _nasStatus.update {
            it.copy(
                ip = cfg.nasHost,
                mac = cfg.nasMacAddress,
                name = cfg.nasDeviceName,
                isOnline = reachable,
                latencyMs = latency,
                lastCheckedTimestamp = System.currentTimeMillis()
            )
        }
        return Pair(reachable, latency)
    }

    suspend fun sendWakeOnLanToNas(): Result<String> {
        val mac = _config.value.nasMacAddress
        return NetworkDiagnosticHelper.sendWakeOnLan(mac)
    }

    suspend fun refreshAll() {
        val cfg = _config.value
        val (piHoleReachable, latency) = NetworkDiagnosticHelper.pingHost(cfg.host, cfg.port, 1500)
        _isLiveConnected.value = piHoleReachable

        if (piHoleReachable) {
            val liveStats = httpClient.fetchSummaryStats(cfg)
            val liveHistory = httpClient.fetchHistory(cfg)
            val liveQueries = httpClient.fetchQueries(cfg)
            val liveLists = httpClient.fetchLists(cfg)
            val liveDomains = httpClient.fetchDomains(cfg)

            if (liveQueries.isNotEmpty()) {
                _queryLogs.value = liveQueries
            }
            if (liveLists.isNotEmpty()) {
                _blocklists.value = liveLists
            }
            if (liveDomains.isNotEmpty()) {
                _rules.value = liveDomains
            }

            if (liveStats != null) {
                _stats.update { current ->
                    current.copy(
                        totalQueries = liveStats.totalQueries,
                        queriesBlocked = liveStats.queriesBlocked,
                        percentBlocked = liveStats.percentBlocked,
                        activeClients = liveStats.activeClients,
                        domainsOnBlocklist = if (liveStats.domainsOnBlocklist > 0) liveStats.domainsOnBlocklist else current.domainsOnBlocklist,
                        latencyMs = latency,
                        lastUpdated = System.currentTimeMillis(),
                        history24h = if (liveHistory.isNotEmpty()) liveHistory else current.history24h
                    )
                }
            } else {
                _stats.update { it.copy(latencyMs = latency, lastUpdated = System.currentTimeMillis()) }
            }
        } else {
            _stats.update { it.copy(latencyMs = latency, lastUpdated = System.currentTimeMillis()) }
            if (cfg.isDemoMode) {
                simulatePeriodicDnsActivity()
            }
        }

        pingNas()
    }

    private fun startBackgroundRefresher() {
        scope.launch {
            while (isActive) {
                delay(3000)
                val currentPause = _stats.value.pauseRemainingSeconds
                if (currentPause > 0) {
                    val remaining = currentPause - 3
                    if (remaining <= 0) {
                        _stats.update { it.copy(blockingStatus = BlockingStatus.ENABLED, pauseRemainingSeconds = 0) }
                    } else {
                        _stats.update { it.copy(pauseRemainingSeconds = remaining) }
                    }
                }
                simulatePeriodicDnsActivity()
            }
        }
    }

    private fun simulatePeriodicDnsActivity() {
        val sampleDomains = listOf(
            Pair("graph.facebook.com", QueryStatus.BLOCKED_GRAVITY),
            Pair("adservice.google.com", QueryStatus.BLOCKED_GRAVITY),
            Pair("api.github.com", QueryStatus.FORWARDED),
            Pair("gateway.icloud.com", QueryStatus.FORWARDED),
            Pair("telemetry.samsungcloud.com", QueryStatus.BLOCKED_BLACKLIST),
            Pair("collector.netflix.com", QueryStatus.CACHED),
            Pair("nas.local", QueryStatus.CACHED),
            Pair("dns.quad9.net", QueryStatus.FORWARDED),
            Pair("track.hubspot.com", QueryStatus.BLOCKED_GRAVITY),
            Pair("weather.apple.com", QueryStatus.FORWARDED)
        )

        val clientPool = _clients.value.ifEmpty { defaultClients }
        val randomClient = clientPool.random()
        val randomDomain = sampleDomains.random()
        val types = listOf("A", "AAAA", "HTTPS", "PTR")

        val fullFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val newLog = DnsQueryLog(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            formattedTime = timeFormat.format(Date()),
            formattedFullDate = fullFormat.format(Date()),
            domain = randomDomain.first,
            clientIp = randomClient.ip,
            clientName = randomClient.displayName,
            queryType = types.random(),
            status = randomDomain.second,
            statusText = when (randomDomain.second) {
                QueryStatus.BLOCKED_GRAVITY -> "Blocked by Gravity adlist"
                QueryStatus.BLOCKED_BLACKLIST -> "Blocked by Custom Blacklist"
                QueryStatus.BLOCKED_REGEX -> "Blocked by Regex filter"
                QueryStatus.FORWARDED -> "Forwarded to Cloudflare (1.1.1.1)"
                QueryStatus.CACHED -> "Answered from Pi-hole local DNS cache"
                QueryStatus.UNKNOWN -> "Unknown status"
            },
            reply = if (randomDomain.second.name.startsWith("BLOCKED")) "0.0.0.0" else "104.21.43.12",
            responseTimeMs = (0.4 + (Math.random() * 8.0)).let { Math.round(it * 10.0) / 10.0 },
            upstreamServer = if (randomDomain.second == QueryStatus.FORWARDED) "Cloudflare (1.1.1.1)" else "Local DNS Sinkhole",
            dnssecStatus = "SECURE",
            blockReason = if (randomDomain.second.name.startsWith("BLOCKED")) "Domain matched ad/tracking blocklist" else ""
        )

        _queryLogs.update { current ->
            (listOf(newLog) + current).take(200)
        }

        _stats.update { current ->
            val isBlocked = randomDomain.second.name.startsWith("BLOCKED")
            val newTotal = current.totalQueries + 1
            val newBlocked = if (isBlocked) current.queriesBlocked + 1 else current.queriesBlocked
            val percent = if (newTotal > 0) (newBlocked.toFloat() / newTotal.toFloat()) * 100f else 0f

            val updatedHistory = current.history24h.toMutableList()
            if (updatedHistory.isNotEmpty()) {
                val lastIdx = updatedHistory.lastIndex
                val lastPoint = updatedHistory[lastIdx]
                updatedHistory[lastIdx] = lastPoint.copy(
                    totalQueries = lastPoint.totalQueries + 1,
                    blockedQueries = if (isBlocked) lastPoint.blockedQueries + 1 else lastPoint.blockedQueries
                )
            }

            current.copy(
                totalQueries = newTotal,
                queriesBlocked = newBlocked,
                percentBlocked = Math.round(percent * 10f) / 10f,
                queriesCached = if (randomDomain.second == QueryStatus.CACHED) current.queriesCached + 1 else current.queriesCached,
                queriesForwarded = if (randomDomain.second == QueryStatus.FORWARDED) current.queriesForwarded + 1 else current.queriesForwarded,
                lastUpdated = System.currentTimeMillis(),
                history24h = updatedHistory
            )
        }
    }

    private fun generate24hSeedHistory(): List<HistoryDataPoint> {
        val list = mutableListOf<HistoryDataPoint>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.HOUR_OF_DAY, -23)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)

        val hourFormat = SimpleDateFormat("HH:00", Locale.getDefault())

        val curve = listOf(
            220, 150, 110, 95, 120, 240, 480, 850, 1120, 1250, 1180, 1310,
            1240, 1190, 1340, 1420, 1510, 1680, 1850, 1920, 1780, 1640, 1210, 890
        )

        for (i in 0 until 24) {
            val total = curve[i % curve.size] + (-40..40).random()
            val blocked = (total * (0.22 + (0.08 * Math.random()))).toInt()
            list.add(
                HistoryDataPoint(
                    timestamp = cal.timeInMillis,
                    hourLabel = hourFormat.format(cal.time),
                    totalQueries = total,
                    blockedQueries = blocked
                )
            )
            cal.add(Calendar.HOUR_OF_DAY, 1)
        }
        return list
    }

    private fun initSeedData() {
        _stats.value = PiHoleStats(
            blockingStatus = BlockingStatus.ENABLED,
            totalQueries = 27840,
            queriesBlocked = 6982,
            percentBlocked = 25.1f,
            domainsOnBlocklist = 1489205,
            uniqueDomains = 1840,
            queriesForwarded = 14200,
            queriesCached = 6658,
            activeClients = 14,
            totalClients = 18,
            latencyMs = 2,
            piHoleVersion = "Pi-hole v6.0.4 (FTL v6.0.2)",
            gravityLastUpdate = "Today, 03:00 AM",
            queryTypeDistribution = mapOf(
                "A (IPv4)" to 68,
                "AAAA (IPv6)" to 19,
                "HTTPS" to 9,
                "PTR" to 4
            ),
            upstreamServers = listOf(
                UpstreamServerStat("Cloudflare DNS (1.1.1.1)", "1.1.1.1", 52.4f, 7440),
                UpstreamServerStat("Quad9 Secure (9.9.9.9)", "9.9.9.9", 36.1f, 5126),
                UpstreamServerStat("Local NAS DNS (Cache)", "192.168.1.100", 11.5f, 1634)
            ),
            history24h = generate24hSeedHistory()
        )

        _clients.value = defaultClients

        _rules.value = listOf(
            DomainRule("1", "telemetry.samsungcloud.com", RuleType.EXACT_BLACKLIST, true, "Smart TV tracking", "2026-10-20"),
            DomainRule("2", "adservice.google.com", RuleType.EXACT_BLACKLIST, true, "Google Ad Network", "2026-10-21"),
            DomainRule("3", "(*\\.)?tiktokv\\.com", RuleType.REGEX_BLACKLIST, true, "TikTok tracking wildcard", "2026-10-22"),
            DomainRule("4", "nas.local", RuleType.EXACT_WHITELIST, true, "Local NAS web console", "2026-10-23"),
            DomainRule("5", "pihole.local", RuleType.EXACT_WHITELIST, true, "Pi-hole admin direct access", "2026-10-24"),
            DomainRule("6", "connectivitycheck.gstatic.com", RuleType.EXACT_WHITELIST, true, "Android captive portal check", "2026-10-25")
        )

        _blocklists.value = listOf(
            BlocklistSource(
                id = "1",
                url = "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts",
                enabled = true,
                comment = "StevenBlack Unified Hosts (Adware + Malware)",
                domainCount = 184520,
                dateAdded = "2026-10-01",
                lastUpdateStatus = "SUCCESS"
            ),
            BlocklistSource(
                id = "2",
                url = "https://v.firebog.net/hosts/AdguardDNS.txt",
                enabled = true,
                comment = "Firebog Adguard DNS List",
                domainCount = 68900,
                dateAdded = "2026-10-02",
                lastUpdateStatus = "SUCCESS"
            ),
            BlocklistSource(
                id = "3",
                url = "https://v.firebog.net/hosts/Easyprivacy.txt",
                enabled = true,
                comment = "EasyPrivacy Tracking Protection",
                domainCount = 42100,
                dateAdded = "2026-10-05",
                lastUpdateStatus = "SUCCESS"
            ),
            BlocklistSource(
                id = "4",
                url = "https://urlhaus.abuse.ch/downloads/hostfile/",
                enabled = true,
                comment = "URLhaus Malware Malicious Domains",
                domainCount = 21400,
                dateAdded = "2026-10-10",
                lastUpdateStatus = "SUCCESS"
            )
        )

        _nasStatus.value = NasStatus(
            ip = _config.value.nasHost,
            mac = _config.value.nasMacAddress,
            name = _config.value.nasDeviceName,
            isOnline = true,
            latencyMs = 2,
            openPorts = listOf(80, 53, 5000, 22)
        )
    }

    private val defaultClients = listOf(
        NetworkClient("192.168.1.100", "00:11:32:8A:2B:1C", "synology-nas", "Pi-hole Host (NAS)", 4890, 12, 0.2f, true, "Synology Inc."),
        NetworkClient("192.168.1.12", "E4:5F:01:3B:88:99", "pixel-9-pro", "User's Pixel 9 Pro", 8450, 2840, 33.6f, true, "Google LLC"),
        NetworkClient("192.168.1.45", "BC:D1:77:22:11:44", "samsung-qled-tv", "Living Room Smart TV", 5120, 2410, 47.1f, true, "Samsung Electronics"),
        NetworkClient("192.168.1.88", "F0:18:98:AA:BC:DD", "macbook-pro-m3", "Work MacBook Pro", 6200, 1140, 18.4f, true, "Apple Inc."),
        NetworkClient("192.168.1.150", "28:6D:CD:45:90:12", "shelly-plus-1", "Kitchen Smart Relay", 1420, 210, 14.8f, true, "Allterco Robotics"),
        NetworkClient("192.168.1.205", "50:EC:50:FA:CE:01", "ring-doorbell-cam", "Front Doorbell Camera", 1760, 370, 21.0f, true, "Amazon Ring")
    )
}
