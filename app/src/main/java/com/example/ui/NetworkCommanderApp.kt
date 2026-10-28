package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import com.example.ui.screens.AboutDisclaimerScreen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.ClientsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.DomainRulesScreen
import com.example.ui.screens.QueryLogScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CyberBgDark
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ShieldGreen
import com.example.ui.theme.ShieldRed
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkCommanderApp(
    viewModel: NetworkCommanderViewModel,
    modifier: Modifier = Modifier
) {
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val nasStatus by viewModel.nasStatus.collectAsStateWithLifecycle()
    val testState by viewModel.connectionTestState.collectAsStateWithLifecycle()
    val filteredQueries by viewModel.filteredQueries.collectAsStateWithLifecycle()
    val querySearch by viewModel.querySearch.collectAsStateWithLifecycle()
    val queryFilter by viewModel.queryFilter.collectAsStateWithLifecycle()
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    val blocklists by viewModel.blocklists.collectAsStateWithLifecycle()
    val gravityState by viewModel.gravityUpdateState.collectAsStateWithLifecycle()
    val pingResult by viewModel.pingResult.collectAsStateWithLifecycle()
    val wolMessage by viewModel.wolMessage.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val isLiveConnected by viewModel.isLiveConnected.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Handle back button: if not on DASHBOARD, return to DASHBOARD
    BackHandler(enabled = activeTab != MainTab.DASHBOARD) {
        viewModel.selectTab(MainTab.DASHBOARD)
    }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissUserMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CyberBgDark,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonCyan.copy(alpha = 0.15f))
                                .border(1.dp, NeonCyan, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "Network Commander",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = TextPrimaryDark,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isLiveConnected || config.isDemoMode) ShieldGreen else ShieldRed)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (config.isDemoMode) androidx.compose.ui.res.stringResource(com.example.R.string.status_simulation_mode) else "${config.host}:${config.port}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondaryDark,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshAll() },
                        modifier = Modifier.testTag("refresh_action_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.action_refresh), tint = NeonCyan)
                    }

                    IconButton(
                        onClick = { viewModel.selectTab(MainTab.ABOUT) },
                        modifier = Modifier.testTag("about_top_action_btn")
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.action_about),
                            tint = if (activeTab == MainTab.ABOUT) NeonCyan else TextSecondaryDark
                        )
                    }

                    IconButton(
                        onClick = { viewModel.selectTab(MainTab.SETTINGS) },
                        modifier = Modifier.testTag("settings_action_btn")
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.nav_settings),
                            tint = if (activeTab == MainTab.SETTINGS) NeonCyan else TextSecondaryDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CyberSurfaceDark,
                    titleContentColor = TextPrimaryDark
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = CyberSurfaceDark,
                contentColor = TextPrimaryDark,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == MainTab.DASHBOARD,
                    onClick = { viewModel.selectTab(MainTab.DASHBOARD) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.nav_dashboard)) },
                    label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.nav_dashboard), fontSize = 10.sp) },
                    colors = customNavColors(),
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )
                NavigationBarItem(
                    selected = activeTab == MainTab.QUERIES,
                    onClick = { viewModel.selectTab(MainTab.QUERIES) },
                    icon = { Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.nav_queries)) },
                    label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.nav_queries), fontSize = 10.sp) },
                    colors = customNavColors(),
                    modifier = Modifier.testTag("nav_tab_queries")
                )
                NavigationBarItem(
                    selected = activeTab == MainTab.CLIENTS,
                    onClick = { viewModel.selectTab(MainTab.CLIENTS) },
                    icon = { Icon(Icons.Default.Devices, contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.nav_clients)) },
                    label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.nav_clients), fontSize = 10.sp) },
                    colors = customNavColors(),
                    modifier = Modifier.testTag("nav_tab_clients")
                )
                NavigationBarItem(
                    selected = activeTab == MainTab.RULES,
                    onClick = { viewModel.selectTab(MainTab.RULES) },
                    icon = { Icon(Icons.Default.Shield, contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.nav_rules)) },
                    label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.nav_rules), fontSize = 10.sp) },
                    colors = customNavColors(),
                    modifier = Modifier.testTag("nav_tab_rules")
                )
                NavigationBarItem(
                    selected = activeTab == MainTab.DIAGNOSTICS,
                    onClick = { viewModel.selectTab(MainTab.DIAGNOSTICS) },
                    icon = { Icon(Icons.Default.Build, contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.nav_tools)) },
                    label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.nav_tools), fontSize = 10.sp) },
                    colors = customNavColors(),
                    modifier = Modifier.testTag("nav_tab_diagnostics")
                )
                NavigationBarItem(
                    selected = activeTab == MainTab.SETTINGS,
                    onClick = { viewModel.selectTab(MainTab.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.nav_settings)) },
                    label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.nav_settings), fontSize = 10.sp) },
                    colors = customNavColors(),
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Crossfade(
            targetState = activeTab,
            label = "tab_crossfade",
            modifier = Modifier.padding(innerPadding)
        ) { tab ->
            when (tab) {
                MainTab.DASHBOARD -> DashboardScreen(
                    stats = stats,
                    nasStatus = nasStatus,
                    onSetBlocking = { enable, timer -> viewModel.setBlockingState(enable, timer) },
                    onPingNas = { viewModel.pingNas() },
                    onSendWol = { viewModel.sendWakeOnLan() },
                    onOpenWebAdmin = { viewModel.selectTab(MainTab.DIAGNOSTICS) }
                )
                MainTab.QUERIES -> QueryLogScreen(
                    queries = filteredQueries,
                    searchQuery = querySearch,
                    filter = queryFilter,
                    isRefreshing = isRefreshing,
                    onSearchChange = { viewModel.setQuerySearch(it) },
                    onFilterChange = { viewModel.setQueryFilter(it) },
                    onRefresh = { viewModel.refreshQueries() },
                    onQuickBlock = { viewModel.quickBlockDomain(it) },
                    onQuickWhitelist = { viewModel.quickWhitelistDomain(it) }
                )
                MainTab.CLIENTS -> ClientsScreen(
                    clients = clients,
                    onUpdateNickname = { ip, nick -> viewModel.updateClientNickname(ip, nick) }
                )
                MainTab.RULES -> DomainRulesScreen(
                    rules = rules,
                    blocklists = blocklists,
                    gravityState = gravityState,
                    onAddRule = { domain, type, comment -> viewModel.addDomainRule(domain, type, comment) },
                    onRemoveRule = { viewModel.removeDomainRule(it) },
                    onToggleRule = { viewModel.toggleDomainRule(it) },
                    onUpdateGravity = { viewModel.updateGravity() }
                )
                MainTab.DIAGNOSTICS -> DiagnosticsScreen(
                    config = config,
                    pingMessage = pingResult,
                    wolMessage = wolMessage,
                    onPingNas = { viewModel.pingNas() },
                    onSendWol = { viewModel.sendWakeOnLan() }
                )
                MainTab.SETTINGS -> SettingsScreen(
                    currentConfig = config,
                    testState = testState,
                    onTestConnection = { viewModel.testConnection(it) },
                    onSaveConfig = { viewModel.saveConfig(it) },
                    onOpenAbout = { viewModel.selectTab(MainTab.ABOUT) }
                )
                MainTab.ABOUT -> AboutDisclaimerScreen(
                    onNavigateBack = { viewModel.selectTab(MainTab.SETTINGS) }
                )
            }
        }
    }
}

@Composable
private fun customNavColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = NeonCyan,
    selectedTextColor = NeonCyan,
    indicatorColor = Color(0xFF1E2D4A),
    unselectedIconColor = TextSecondaryDark,
    unselectedTextColor = TextMutedDark
)
