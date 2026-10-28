package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.EncryptedSettingsDataStore
import com.example.data.repository.PiHoleRepository
import com.example.model.BlocklistSource
import com.example.model.ConnectionTestResult
import com.example.model.DnsQueryLog
import com.example.model.DomainRule
import com.example.model.GravityUpdateState
import com.example.model.NasStatus
import com.example.model.NetworkClient
import com.example.model.PiHoleConfig
import com.example.model.PiHoleStats
import com.example.model.QueryStatus
import com.example.model.RuleType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MainTab {
    DASHBOARD,
    QUERIES,
    CLIENTS,
    RULES,
    DIAGNOSTICS,
    SETTINGS,
    ABOUT
}

enum class QueryFilter {
    ALL,
    BLOCKED,
    ALLOWED
}

sealed interface ConnectionTestState {
    data object Idle : ConnectionTestState
    data object Testing : ConnectionTestState
    data class Completed(val result: ConnectionTestResult) : ConnectionTestState
}

class NetworkCommanderViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = EncryptedSettingsDataStore(application.applicationContext)
    private val repository = PiHoleRepository(dataStore)

    val config: StateFlow<PiHoleConfig> = repository.config
    val stats: StateFlow<PiHoleStats> = repository.stats
    val clients: StateFlow<List<NetworkClient>> = repository.clients
    val rules: StateFlow<List<DomainRule>> = repository.rules
    val blocklists: StateFlow<List<BlocklistSource>> = repository.blocklists
    val gravityUpdateState: StateFlow<GravityUpdateState> = repository.gravityUpdateState
    val nasStatus: StateFlow<NasStatus> = repository.nasStatus
    val isLiveConnected: StateFlow<Boolean> = repository.isLiveConnected

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _activeTab = MutableStateFlow(MainTab.DASHBOARD)
    val activeTab: StateFlow<MainTab> = _activeTab.asStateFlow()

    private val _connectionTestState = MutableStateFlow<ConnectionTestState>(ConnectionTestState.Idle)
    val connectionTestState: StateFlow<ConnectionTestState> = _connectionTestState.asStateFlow()

    private val _querySearch = MutableStateFlow("")
    val querySearch: StateFlow<String> = _querySearch.asStateFlow()

    private val _queryFilter = MutableStateFlow(QueryFilter.ALL)
    val queryFilter: StateFlow<QueryFilter> = _queryFilter.asStateFlow()

    private val _pingResult = MutableStateFlow<String?>(null)
    val pingResult: StateFlow<String?> = _pingResult.asStateFlow()

    private val _wolMessage = MutableStateFlow<String?>(null)
    val wolMessage: StateFlow<String?> = _wolMessage.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Filtered queries flow
    val filteredQueries: StateFlow<List<DnsQueryLog>> = combine(
        repository.queryLogs,
        _querySearch,
        _queryFilter
    ) { logs, search, filter ->
        logs.filter { log ->
            val matchesSearch = search.isBlank() ||
                    log.domain.contains(search, ignoreCase = true) ||
                    log.clientIp.contains(search, ignoreCase = true) ||
                    log.clientName.contains(search, ignoreCase = true)

            val matchesFilter = when (filter) {
                QueryFilter.ALL -> true
                QueryFilter.BLOCKED -> log.status.name.startsWith("BLOCKED")
                QueryFilter.ALLOWED -> !log.status.name.startsWith("BLOCKED")
            }

            matchesSearch && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTab(tab: MainTab) {
        _activeTab.value = tab
    }

    fun setQuerySearch(text: String) {
        _querySearch.value = text
    }

    fun setQueryFilter(filter: QueryFilter) {
        _queryFilter.value = filter
    }

    fun dismissUserMessage() {
        _userMessage.value = null
    }

    fun testConnection(cfg: PiHoleConfig) {
        viewModelScope.launch {
            _connectionTestState.value = ConnectionTestState.Testing
            val result = repository.testConnection(cfg)
            _connectionTestState.value = ConnectionTestState.Completed(result)
        }
    }

    fun saveConfig(cfg: PiHoleConfig) {
        viewModelScope.launch {
            repository.updateConfig(cfg)
            _userMessage.value = "Configuration saved successfully!"
        }
    }

    fun setBlockingState(enable: Boolean, timerSeconds: Int = 0) {
        viewModelScope.launch {
            repository.setBlockingState(enable, timerSeconds)
            _userMessage.value = if (enable) "DNS Blocking Enabled" else if (timerSeconds > 0) "DNS Blocking paused for ${timerSeconds}s" else "DNS Blocking Disabled"
        }
    }

    fun quickBlockDomain(domain: String) {
        viewModelScope.launch {
            repository.addDomainRule(domain, RuleType.EXACT_BLACKLIST, "Blocked from Query Log")
            _userMessage.value = "Added $domain to Blacklist"
        }
    }

    fun quickWhitelistDomain(domain: String) {
        viewModelScope.launch {
            repository.addDomainRule(domain, RuleType.EXACT_WHITELIST, "Whitelisted from Query Log")
            _userMessage.value = "Added $domain to Whitelist"
        }
    }

    fun addDomainRule(domain: String, type: RuleType, comment: String) {
        viewModelScope.launch {
            repository.addDomainRule(domain, type, comment)
            _userMessage.value = "Rule added for $domain"
        }
    }

    fun updateGravity() {
        viewModelScope.launch {
            val (success, msg) = repository.updateGravity()
            _userMessage.value = msg
        }
    }

    fun resetGravityState() {
        repository.resetGravityState()
    }

    fun removeDomainRule(id: String) {
        viewModelScope.launch {
            repository.removeDomainRule(id)
            _userMessage.value = "تم حذف النطاق عبر /api/domains"
        }
    }

    fun toggleDomainRule(id: String) {
        repository.toggleDomainRule(id)
    }

    fun updateClientNickname(ip: String, nickname: String) {
        viewModelScope.launch {
            repository.updateClientNickname(ip, nickname)
            _userMessage.value = "Nickname updated for $ip"
        }
    }

    fun pingNas() {
        viewModelScope.launch {
            _pingResult.value = "Pinging NAS (${config.value.nasHost})..."
            val (online, latency) = repository.pingNas()
            _pingResult.value = if (online) "NAS Online! Latency: ${latency}ms" else "NAS unreachable at ${config.value.nasHost}"
        }
    }

    fun sendWakeOnLan() {
        viewModelScope.launch {
            _wolMessage.value = "Sending Wake-on-LAN packet to ${config.value.nasMacAddress}..."
            val result = repository.sendWakeOnLanToNas()
            _wolMessage.value = result.getOrElse { "Failed: ${it.localizedMessage}" }
        }
    }

    fun refreshQueries() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.refreshAll()
            kotlinx.coroutines.delay(400)
            _isRefreshing.value = false
            _userMessage.value = "تم تحديث سجل الاستعلامات"
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            repository.refreshAll()
            _userMessage.value = "Network stats refreshed"
        }
    }
}
