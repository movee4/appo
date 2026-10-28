package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DnsQueryLog
import com.example.model.QueryStatus
import com.example.ui.QueryFilter
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCardDark
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ShieldGreen
import com.example.ui.theme.ShieldRed
import com.example.ui.theme.StatusCached
import com.example.ui.theme.StatusForwarded
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueryLogScreen(
    queries: List<DnsQueryLog>,
    searchQuery: String,
    filter: QueryFilter,
    isRefreshing: Boolean,
    onSearchChange: (String) -> Unit,
    onFilterChange: (QueryFilter) -> Unit,
    onRefresh: () -> Unit,
    onQuickBlock: (String) -> Unit,
    onQuickWhitelist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedQueryForDetails by remember { mutableStateOf<DnsQueryLog?>(null) }
    val pullRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = pullRefreshState,
        modifier = modifier
            .fillMaxSize()
            .testTag("query_log_pull_to_refresh_box")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Search Bar: searches by Domain name, Client IP, or Client Name
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("بحث بالنطاق أو اسم الجهاز أو الـ IP...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMutedDark)
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = CyberCardBorder,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark,
                    cursorColor = NeonCyan
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("query_search_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips Row: All / Blocked (محجوب) / Allowed (مسموح)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = filter == QueryFilter.ALL,
                    onClick = { onFilterChange(QueryFilter.ALL) },
                    label = { Text("الكل (${queries.size})") },
                    colors = chipColors(),
                    modifier = Modifier.testTag("filter_all_chip")
                )
                FilterChip(
                    selected = filter == QueryFilter.BLOCKED,
                    onClick = { onFilterChange(QueryFilter.BLOCKED) },
                    label = { Text("المحجوبة (Blocked)") },
                    colors = chipColors(),
                    modifier = Modifier.testTag("filter_blocked_chip")
                )
                FilterChip(
                    selected = filter == QueryFilter.ALLOWED,
                    onClick = { onFilterChange(QueryFilter.ALLOWED) },
                    label = { Text("المسموحة (Allowed)") },
                    colors = chipColors(),
                    modifier = Modifier.testTag("filter_allowed_chip")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scrollable List of DNS Queries from /api/queries
            if (queries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = null,
                            tint = TextMutedDark,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "لا توجد استعلامات مطابقة للبحث أو الفلتر",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondaryDark)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "اسحب للأسفل (Pull to refresh) لتحديث السجل من الخادم",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMutedDark, fontSize = 11.sp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(queries, key = { it.id }) { log ->
                        QueryLogItemCard(
                            log = log,
                            onClick = { selectedQueryForDetails = log },
                            onBlock = { onQuickBlock(log.domain) },
                            onWhitelist = { onQuickWhitelist(log.domain) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // Detail Screen / Sheet shown when clicking on a query
    selectedQueryForDetails?.let { log ->
        QueryDetailSheet(
            queryLog = log,
            onDismiss = { selectedQueryForDetails = null },
            onQuickBlock = onQuickBlock,
            onQuickWhitelist = onQuickWhitelist
        )
    }
}

@Composable
private fun QueryLogItemCard(
    log: DnsQueryLog,
    onClick: () -> Unit,
    onBlock: () -> Unit,
    onWhitelist: () -> Unit
) {
    val isBlocked = log.status.name.startsWith("BLOCKED")
    val badgeColor = when (log.status) {
        QueryStatus.BLOCKED_GRAVITY, QueryStatus.BLOCKED_BLACKLIST, QueryStatus.BLOCKED_REGEX -> ShieldRed
        QueryStatus.FORWARDED -> StatusForwarded
        QueryStatus.CACHED -> StatusCached
        QueryStatus.UNKNOWN -> TextMutedDark
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberCardDark)
            .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag("query_card_${log.id}")
    ) {
        // Row 1: Time, Source Device (IP + Name), and Record Type
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Time
                Text(
                    text = log.formattedTime,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMutedDark,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))

                // Source Device: IP + Device Name
                Icon(
                    imageVector = Icons.Default.Devices,
                    contentDescription = null,
                    tint = ElectricBlue,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${log.clientName} (${log.clientIp})",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    ),
                    maxLines = 1
                )
            }

            // Record Type badge (A, AAAA, PTR, HTTPS, etc.)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = log.queryType,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Row 2: Domain Name
        Text(
            text = log.domain,
            style = MaterialTheme.typography.titleSmall.copy(
                color = TextPrimaryDark,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Row 3: Blocking Status (مسموح / محجوب) and Quick Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(badgeColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isBlocked) "محجوب (${log.statusText})" else "مسموح (${log.statusText})",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = badgeColor,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                )
            }

            // Quick Action Buttons
            if (!isBlocked) {
                OutlinedButton(
                    onClick = onBlock,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ShieldRed),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("block_btn_${log.domain}")
                ) {
                    Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حظر", fontSize = 11.sp)
                }
            } else {
                OutlinedButton(
                    onClick = onWhitelist,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ShieldGreen),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("whitelist_btn_${log.domain}")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("سماح", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
    selectedLabelColor = NeonCyan,
    containerColor = Color(0xFF162035),
    labelColor = TextSecondaryDark
)
