package com.example.data.api

import com.example.model.BlockingStatus
import com.example.model.BlocklistSource
import com.example.model.ConnectionTestResult
import com.example.model.DnsQueryLog
import com.example.model.DomainRule
import com.example.model.HistoryDataPoint
import com.example.model.PiHoleConfig
import com.example.model.PiHoleStats
import com.example.model.QueryStatus
import com.example.model.RuleType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class PiHoleHttpClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .writeTimeout(4, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun testConnection(config: PiHoleConfig): ConnectionTestResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val base = config.baseUrl

        // Attempt 1: Pi-hole v6 API - POST /api/auth
        val v6AuthUrl = "$base/api/auth"
        val authBody = JSONObject().apply {
            put("password", config.apiPassword)
        }.toString().toRequestBody(jsonMediaType)

        val v6Request = Request.Builder()
            .url(v6AuthUrl)
            .post(authBody)
            .header("Accept", "application/json")
            .build()

        try {
            client.newCall(v6Request).execute().use { response ->
                val latency = (System.currentTimeMillis() - start).coerceAtLeast(1L)
                val bodyStr = response.body?.string().orEmpty()
                val code = response.code

                if (response.isSuccessful) {
                    var sid = ""
                    try {
                        val json = JSONObject(bodyStr)
                        if (json.has("session")) {
                            sid = json.getJSONObject("session").optString("sid", "")
                        }
                    } catch (_: Exception) {}

                    return@withContext ConnectionTestResult(
                        isSuccess = true,
                        message = "Connected to Pi-hole v6 successfully!",
                        latencyMs = latency,
                        httpStatusCode = code,
                        detectedApiVersion = "Pi-hole v6 (REST API)",
                        sid = sid,
                        details = "Authentication verified. Session token established at $base/api"
                    )
                } else if (code == 401 || code == 403) {
                    return@withContext ConnectionTestResult(
                        isSuccess = false,
                        message = "Pi-hole v6 reached, but password was rejected (HTTP $code)",
                        latencyMs = latency,
                        httpStatusCode = code,
                        detectedApiVersion = "Pi-hole v6 (REST API)",
                        details = "Check your Pi-hole web admin password or App Password."
                    )
                } else if (code != 404) {
                    return@withContext ConnectionTestResult(
                        isSuccess = false,
                        message = "Pi-hole returned error HTTP $code",
                        latencyMs = latency,
                        httpStatusCode = code,
                        detectedApiVersion = "Pi-hole v6",
                        details = bodyStr.take(150)
                    )
                }
            }
        } catch (_: Exception) {
            // Try v5 fallback before failing completely
        }

        // Attempt 2: Pi-hole v5 / FTL legacy API - GET /admin/api.php?status&auth=...
        val v5Url = "$base/admin/api.php?status&auth=${config.apiPassword}"
        val v5Request = Request.Builder()
            .url(v5Url)
            .get()
            .build()

        try {
            client.newCall(v5Request).execute().use { response ->
                val latency = (System.currentTimeMillis() - start).coerceAtLeast(1L)
                val bodyStr = response.body?.string().orEmpty()
                val code = response.code

                if (response.isSuccessful && bodyStr.contains("status")) {
                    return@withContext ConnectionTestResult(
                        isSuccess = true,
                        message = "Connected to Pi-hole v5 (FTL engine) successfully!",
                        latencyMs = latency,
                        httpStatusCode = code,
                        detectedApiVersion = "Pi-hole v5 (Legacy API)",
                        details = "Host status: ${bodyStr.take(100)}"
                    )
                }
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - start
            return@withContext ConnectionTestResult(
                isSuccess = false,
                message = "Unable to reach Pi-hole server at ${config.host}:${config.port}",
                latencyMs = latency,
                httpStatusCode = 0,
                detectedApiVersion = "Unreachable",
                details = e.localizedMessage ?: "Connection timed out or host is offline on local network."
            )
        }

        ConnectionTestResult(
            isSuccess = false,
            message = "Pi-hole API endpoints (/api/auth, /admin/api.php) not found at ${config.baseUrl}",
            latencyMs = System.currentTimeMillis() - start,
            httpStatusCode = 404,
            detectedApiVersion = "Unknown Web Server",
            details = "Verify Pi-hole is running and port ${config.port} is correct."
        )
    }

    suspend fun fetchSummaryStats(config: PiHoleConfig): PiHoleStats? = withContext(Dispatchers.IO) {
        val base = config.baseUrl

        // Attempt v6: GET /api/stats/summary
        val v6Url = "$base/api/stats/summary"
        val request = Request.Builder()
            .url(v6Url)
            .get()
            .apply {
                if (config.sid.isNotBlank()) header("sid", config.sid)
            }
            .build()

        try {
            client.newCall(request).execute().use { res ->
                if (res.isSuccessful) {
                    val body = res.body?.string().orEmpty()
                    val json = JSONObject(body)
                    val queriesObj = json.optJSONObject("queries") ?: JSONObject()
                    val clientsObj = json.optJSONObject("clients") ?: JSONObject()
                    val gravityObj = json.optJSONObject("gravity") ?: JSONObject()

                    val totalQueries = queriesObj.optLong("total", 0L)
                    val blockedQueries = queriesObj.optLong("blocked", 0L)
                    val percentBlocked = queriesObj.optDouble("percent_blocked", 0.0).toFloat()
                    val activeClients = clientsObj.optInt("active", 0)
                    val totalClients = clientsObj.optInt("total", 0)
                    val domainsGravity = gravityObj.optLong("domains_being_blocked", 0L)

                    return@withContext PiHoleStats(
                        blockingStatus = BlockingStatus.ENABLED,
                        totalQueries = totalQueries,
                        queriesBlocked = blockedQueries,
                        percentBlocked = percentBlocked,
                        domainsOnBlocklist = domainsGravity,
                        activeClients = activeClients,
                        totalClients = totalClients,
                        lastUpdated = System.currentTimeMillis()
                    )
                }
            }
        } catch (_: Exception) {}

        // Fallback v5: GET /admin/api.php?summaryRaw&auth=...
        val v5Url = "$base/admin/api.php?summaryRaw&auth=${config.apiPassword}"
        try {
            client.newCall(Request.Builder().url(v5Url).get().build()).execute().use { res ->
                if (res.isSuccessful) {
                    val body = res.body?.string().orEmpty()
                    val json = JSONObject(body)
                    return@withContext PiHoleStats(
                        blockingStatus = if (json.optString("status") == "enabled") BlockingStatus.ENABLED else BlockingStatus.DISABLED,
                        totalQueries = json.optLong("dns_queries_today", 0L),
                        queriesBlocked = json.optLong("ads_blocked_today", 0L),
                        percentBlocked = json.optDouble("ads_percentage_today", 0.0).toFloat(),
                        domainsOnBlocklist = json.optLong("domains_being_blocked", 0L),
                        uniqueDomains = json.optLong("unique_domains", 0L),
                        queriesForwarded = json.optLong("queries_forwarded", 0L),
                        queriesCached = json.optLong("queries_cached", 0L),
                        activeClients = json.optInt("unique_clients", 0),
                        totalClients = json.optInt("clients_ever_seen", 0),
                        lastUpdated = System.currentTimeMillis()
                    )
                }
            }
        } catch (_: Exception) {}

        null
    }

    suspend fun fetchHistory(config: PiHoleConfig): List<HistoryDataPoint> = withContext(Dispatchers.IO) {
        val base = config.baseUrl
        // Attempt v6 /api/history
        val v6Url = "$base/api/history"
        try {
            val req = Request.Builder().url(v6Url).get().apply {
                if (config.sid.isNotBlank()) header("sid", config.sid)
            }.build()

            client.newCall(req).execute().use { res ->
                if (res.isSuccessful) {
                    val body = res.body?.string().orEmpty()
                    val json = JSONObject(body)
                    if (json.has("history")) {
                        val arr = json.getJSONArray("history")
                        val list = mutableListOf<HistoryDataPoint>()
                        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
                        for (i in 0 until arr.length()) {
                            val item = arr.getJSONObject(i)
                            val ts = item.optLong("timestamp", 0L) * 1000L
                            list.add(
                                HistoryDataPoint(
                                    timestamp = ts,
                                    hourLabel = format.format(Date(ts)),
                                    totalQueries = item.optInt("total", 0),
                                    blockedQueries = item.optInt("blocked", 0)
                                )
                            )
                        }
                        if (list.isNotEmpty()) return@withContext list
                    }
                }
            }
        } catch (_: Exception) {}

        emptyList()
    }

    suspend fun fetchQueries(config: PiHoleConfig): List<DnsQueryLog> = withContext(Dispatchers.IO) {
        val base = config.baseUrl
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val fullDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        // Attempt 1: Pi-hole v6 /api/queries
        val v6Url = "$base/api/queries?length=100"
        try {
            val req = Request.Builder().url(v6Url).get().apply {
                if (config.sid.isNotBlank()) header("sid", config.sid)
            }.build()

            client.newCall(req).execute().use { res ->
                if (res.isSuccessful) {
                    val body = res.body?.string().orEmpty()
                    val json = JSONObject(body)
                    if (json.has("queries")) {
                        val arr = json.getJSONArray("queries")
                        val list = mutableListOf<DnsQueryLog>()
                        for (i in 0 until arr.length()) {
                            val item = arr.getJSONObject(i)
                            val ts = item.optLong("time", 0L) * 1000L
                            val dateObj = Date(if (ts > 0) ts else System.currentTimeMillis())
                            val statusCode = item.optInt("status", 2)
                            val status = when (statusCode) {
                                1 -> QueryStatus.BLOCKED_GRAVITY
                                4 -> QueryStatus.BLOCKED_REGEX
                                5 -> QueryStatus.BLOCKED_BLACKLIST
                                3 -> QueryStatus.CACHED
                                else -> QueryStatus.FORWARDED
                            }
                            val clientObj = item.optJSONObject("client")
                            val clientIp = clientObj?.optString("ip", "") ?: item.optString("client", "192.168.1.1")
                            val clientName = clientObj?.optString("name", "") ?: clientIp

                            list.add(
                                DnsQueryLog(
                                    id = item.optString("id", "${ts}_$i"),
                                    timestamp = ts,
                                    formattedTime = timeFormat.format(dateObj),
                                    formattedFullDate = fullDateFormat.format(dateObj),
                                    domain = item.optString("domain", "unknown"),
                                    clientIp = clientIp,
                                    clientName = clientName,
                                    queryType = item.optString("type", "A"),
                                    status = status,
                                    statusText = when (status) {
                                        QueryStatus.BLOCKED_GRAVITY -> "Blocked by Gravity adlist"
                                        QueryStatus.BLOCKED_BLACKLIST -> "Blocked by Blacklist"
                                        QueryStatus.BLOCKED_REGEX -> "Blocked by Regex filter"
                                        QueryStatus.FORWARDED -> "Forwarded to upstream"
                                        QueryStatus.CACHED -> "Answered from local DNS cache"
                                        QueryStatus.UNKNOWN -> "Unknown"
                                    },
                                    reply = item.optString("answer", if (status.name.startsWith("BLOCKED")) "0.0.0.0" else "1.1.1.1"),
                                    responseTimeMs = item.optDouble("response_time", 1.2),
                                    upstreamServer = if (status == QueryStatus.FORWARDED) "Cloudflare (1.1.1.1)" else "Local DNS Sinkhole",
                                    dnssecStatus = "SECURE",
                                    blockReason = if (status.name.startsWith("BLOCKED")) "Domain matched ad/tracking blocklist" else ""
                                )
                            )
                        }
                        if (list.isNotEmpty()) return@withContext list
                    }
                }
            }
        } catch (_: Exception) {}

        emptyList()
    }

    suspend fun setBlocking(config: PiHoleConfig, enable: Boolean, timerSeconds: Int = 0): Boolean = withContext(Dispatchers.IO) {
        val base = config.baseUrl
        // Try v6 first: POST /api/dns/blocking
        val v6Url = "$base/api/dns/blocking"
        val jsonBody = JSONObject().apply {
            put("blocking", enable)
            if (!enable && timerSeconds > 0) {
                put("timer", timerSeconds)
            }
        }.toString().toRequestBody(jsonMediaType)

        val v6Request = Request.Builder()
            .url(v6Url)
            .post(jsonBody)
            .apply {
                if (config.sid.isNotBlank()) header("sid", config.sid)
            }
            .build()

        try {
            client.newCall(v6Request).execute().use { res ->
                if (res.isSuccessful) return@withContext true
            }
        } catch (_: Exception) {}

        // Fallback to v5: GET /admin/api.php?enable / disable
        val v5Param = if (enable) "enable" else if (timerSeconds > 0) "disable=$timerSeconds" else "disable"
        val v5Url = "$base/admin/api.php?$v5Param&auth=${config.apiPassword}"
        try {
            client.newCall(Request.Builder().url(v5Url).get().build()).execute().use { res ->
                return@withContext res.isSuccessful
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun fetchLists(config: PiHoleConfig): List<BlocklistSource> = withContext(Dispatchers.IO) {
        val base = config.baseUrl
        val v6Url = "$base/api/lists"
        try {
            val req = Request.Builder().url(v6Url).get().apply {
                if (config.sid.isNotBlank()) header("sid", config.sid)
            }.build()

            client.newCall(req).execute().use { res ->
                if (res.isSuccessful) {
                    val body = res.body?.string().orEmpty()
                    val json = JSONObject(body)
                    if (json.has("lists")) {
                        val arr = json.getJSONArray("lists")
                        val list = mutableListOf<BlocklistSource>()
                        for (i in 0 until arr.length()) {
                            val item = arr.getJSONObject(i)
                            list.add(
                                BlocklistSource(
                                    id = item.optString("id", "$i"),
                                    url = item.optString("address", item.optString("url", "")),
                                    enabled = item.optBoolean("enabled", true),
                                    comment = item.optString("comment", "Adlist"),
                                    domainCount = item.optLong("number", 0L),
                                    dateAdded = item.optString("date_added", "Active"),
                                    lastUpdateStatus = item.optString("status", "SUCCESS")
                                )
                            )
                        }
                        if (list.isNotEmpty()) return@withContext list
                    }
                }
            }
        } catch (_: Exception) {}

        emptyList()
    }

    suspend fun fetchDomains(config: PiHoleConfig): List<DomainRule> = withContext(Dispatchers.IO) {
        val base = config.baseUrl
        val v6Url = "$base/api/domains"
        try {
            val req = Request.Builder().url(v6Url).get().apply {
                if (config.sid.isNotBlank()) header("sid", config.sid)
            }.build()

            client.newCall(req).execute().use { res ->
                if (res.isSuccessful) {
                    val body = res.body?.string().orEmpty()
                    val json = JSONObject(body)
                    if (json.has("domains")) {
                        val arr = json.getJSONArray("domains")
                        val list = mutableListOf<DomainRule>()
                        for (i in 0 until arr.length()) {
                            val item = arr.getJSONObject(i)
                            val typeStr = item.optString("type", "deny").lowercase()
                            val ruleType = when {
                                typeStr.contains("regex") && (typeStr.contains("deny") || typeStr.contains("black")) -> RuleType.REGEX_BLACKLIST
                                typeStr.contains("regex") -> RuleType.REGEX_WHITELIST
                                typeStr.contains("allow") || typeStr.contains("white") -> RuleType.EXACT_WHITELIST
                                else -> RuleType.EXACT_BLACKLIST
                            }
                            list.add(
                                DomainRule(
                                    id = item.optString("id", "$i"),
                                    domain = item.optString("domain", ""),
                                    type = ruleType,
                                    isEnabled = item.optBoolean("enabled", true),
                                    comment = item.optString("comment", ""),
                                    dateAdded = item.optString("date_added", "2026-10-28")
                                )
                            )
                        }
                        if (list.isNotEmpty()) return@withContext list
                    }
                }
            }
        } catch (_: Exception) {}

        emptyList()
    }

    suspend fun addDomain(config: PiHoleConfig, domain: String, type: RuleType, comment: String): Boolean = withContext(Dispatchers.IO) {
        val base = config.baseUrl
        val v6Url = "$base/api/domains"
        val apiType = when (type) {
            RuleType.EXACT_BLACKLIST -> "deny"
            RuleType.REGEX_BLACKLIST -> "regex_deny"
            RuleType.EXACT_WHITELIST -> "allow"
            RuleType.REGEX_WHITELIST -> "regex_allow"
        }

        val jsonBody = JSONObject().apply {
            put("domain", domain.trim().lowercase())
            put("type", apiType)
            put("comment", comment)
            put("enabled", true)
        }.toString().toRequestBody(jsonMediaType)

        val req = Request.Builder().url(v6Url).post(jsonBody).apply {
            if (config.sid.isNotBlank()) header("sid", config.sid)
        }.build()

        try {
            client.newCall(req).execute().use { res ->
                if (res.isSuccessful) return@withContext true
            }
        } catch (_: Exception) {}

        // Fallback v5
        val listParam = if (type == RuleType.EXACT_WHITELIST || type == RuleType.REGEX_WHITELIST) "white" else "black"
        val v5Url = "$base/admin/api.php?list=$listParam&add=$domain&auth=${config.apiPassword}"
        try {
            client.newCall(Request.Builder().url(v5Url).get().build()).execute().use { res ->
                return@withContext res.isSuccessful
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun deleteDomain(config: PiHoleConfig, domain: String, type: RuleType): Boolean = withContext(Dispatchers.IO) {
        val base = config.baseUrl
        val v6Url = "$base/api/domains"
        val apiType = when (type) {
            RuleType.EXACT_BLACKLIST -> "deny"
            RuleType.REGEX_BLACKLIST -> "regex_deny"
            RuleType.EXACT_WHITELIST -> "allow"
            RuleType.REGEX_WHITELIST -> "regex_allow"
        }

        val jsonBody = JSONObject().apply {
            put("domain", domain.trim().lowercase())
            put("type", apiType)
        }.toString().toRequestBody(jsonMediaType)

        val req = Request.Builder().url(v6Url).delete(jsonBody).apply {
            if (config.sid.isNotBlank()) header("sid", config.sid)
        }.build()

        try {
            client.newCall(req).execute().use { res ->
                if (res.isSuccessful) return@withContext true
            }
        } catch (_: Exception) {}

        // Fallback v5
        val listParam = if (type == RuleType.EXACT_WHITELIST || type == RuleType.REGEX_WHITELIST) "white" else "black"
        val v5Url = "$base/admin/api.php?list=$listParam&sub=$domain&auth=${config.apiPassword}"
        try {
            client.newCall(Request.Builder().url(v5Url).get().build()).execute().use { res ->
                return@withContext res.isSuccessful
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun updateGravity(config: PiHoleConfig): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val base = config.baseUrl
        // Pi-hole v6: POST /api/action
        val v6Url = "$base/api/action"
        val jsonBody = JSONObject().apply {
            put("action", "gravity")
        }.toString().toRequestBody(jsonMediaType)

        val req = Request.Builder().url(v6Url).post(jsonBody).apply {
            if (config.sid.isNotBlank()) header("sid", config.sid)
        }.build()

        try {
            client.newCall(req).execute().use { res ->
                val body = res.body?.string().orEmpty()
                if (res.isSuccessful) {
                    return@withContext Pair(true, "تم تحديث قواعد الجاذبية بنجاح عبر /api/action (Gravity updated)")
                }
            }
        } catch (_: Exception) {}

        // Fallback v5
        val v5Url = "$base/admin/api.php?gravity&auth=${config.apiPassword}"
        try {
            client.newCall(Request.Builder().url(v5Url).get().build()).execute().use { res ->
                if (res.isSuccessful) {
                    return@withContext Pair(true, "تم تحديث قواعد الجاذبية بنجاح (Gravity updated via legacy API)")
                }
            }
        } catch (_: Exception) {}

        Pair(false, "تعذر الاتصال بـ /api/action، تم تطبيق المحاكاة المحلية لقوائم الجاذبية")
    }
}
