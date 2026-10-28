package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NasStatus
import com.example.model.PiHoleStats
import com.example.ui.components.ArchitecturalNoticeCard
import com.example.ui.components.History24hChart
import com.example.ui.components.MetricStatCard
import com.example.ui.components.NasDeviceCard
import com.example.ui.components.QueryTypeChart
import com.example.ui.components.StatusShieldCard
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCardDark
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ShieldGreen
import com.example.ui.theme.ShieldRed
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DashboardScreen(
    stats: PiHoleStats,
    nasStatus: NasStatus,
    onSetBlocking: (enable: Boolean, timer: Int) -> Unit,
    onPingNas: () -> Unit,
    onSendWol: () -> Unit,
    onOpenWebAdmin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val numberFormat = NumberFormat.getNumberInstance(Locale.US)
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Architectural transparency banner
        ArchitecturalNoticeCard()

        // Primary Status Card with General Blocking toggle (/api/dns/blocking) and quick pause presets
        StatusShieldCard(
            stats = stats,
            onSetBlocking = onSetBlocking
        )

        // Live Stats from /api/stats/summary (المتطلب: إحصائيات حية من /api/stats/summary)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricStatCard(
                title = "إجمالي الاستعلامات",
                value = numberFormat.format(stats.totalQueries),
                subtitle = "من /api/stats/summary",
                icon = Icons.Default.Public,
                accentColor = NeonCyan,
                modifier = Modifier.weight(1f),
                testTag = "total_queries_metric"
            )
            MetricStatCard(
                title = "نسبة الحجب",
                value = "${stats.percentBlocked}%",
                subtitle = "معدل الحظر الكلي",
                icon = Icons.Default.Percent,
                accentColor = ElectricBlue,
                modifier = Modifier.weight(1f),
                testTag = "percent_blocked_metric"
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricStatCard(
                title = "الأجهزة النشطة",
                value = "${stats.activeClients} جهاز",
                subtitle = "من إجمالي ${stats.totalClients} متصل",
                icon = Icons.Default.Devices,
                accentColor = ShieldGreen,
                modifier = Modifier.weight(1f),
                testTag = "active_clients_metric"
            )
            MetricStatCard(
                title = "الطلبات المحجوبة",
                value = numberFormat.format(stats.queriesBlocked),
                subtitle = "إعلانات وتتبع محجوب",
                icon = Icons.Default.Block,
                accentColor = ShieldRed,
                modifier = Modifier.weight(1f),
                testTag = "blocked_queries_metric"
            )
        }

        // 24-Hour Queries Chart (المتطلب: رسم بياني بسيط Canvas لاستعلامات آخر 24 ساعة من /api/history)
        if (stats.history24h.isNotEmpty()) {
            History24hChart(historyPoints = stats.history24h)
        }

        // Query Types Distribution Chart
        if (stats.queryTypeDistribution.isNotEmpty()) {
            QueryTypeChart(distribution = stats.queryTypeDistribution)
        }

        // NAS Device status & controls
        NasDeviceCard(
            nasStatus = nasStatus,
            onPingNas = onPingNas,
            onSendWol = onSendWol,
            onOpenWebAdmin = onOpenWebAdmin
        )

        // Upstream Servers Summary
        if (stats.upstreamServers.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CyberCardDark)
                    .border(1.dp, CyberCardBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "خوادم الـ DNS المصدرية (Upstream DNS Servers)",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))

                stats.upstreamServers.forEach { server ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(server.name, style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimaryDark, fontSize = 13.sp))
                            Text(server.ip, style = MaterialTheme.typography.bodySmall.copy(color = TextMutedDark, fontSize = 11.sp))
                        }
                        Text(
                            text = "${server.percentage}%",
                            style = MaterialTheme.typography.labelMedium.copy(color = NeonCyan, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
