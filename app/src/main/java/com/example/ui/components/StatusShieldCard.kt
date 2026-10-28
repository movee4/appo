package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BlockingStatus
import com.example.model.PiHoleStats
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCardDark
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ShieldGreen
import com.example.ui.theme.ShieldRed
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.theme.WarningAmber

@Composable
fun StatusShieldCard(
    stats: PiHoleStats,
    onSetBlocking: (enable: Boolean, timer: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isBlockingEnabled = stats.blockingStatus == BlockingStatus.ENABLED

    val statusColor by animateColorAsState(
        targetValue = when (stats.blockingStatus) {
            BlockingStatus.ENABLED -> ShieldGreen
            BlockingStatus.PAUSED -> WarningAmber
            BlockingStatus.DISABLED -> ShieldRed
        },
        label = "statusColor"
    )

    val backgroundGradient = Brush.radialGradient(
        colors = listOf(
            statusColor.copy(alpha = 0.18f),
            CyberCardDark
        ),
        radius = 600f
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundGradient)
            .border(1.dp, CyberCardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("status_shield_card")
    ) {
        // Status Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Glowing Indicator
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.2f))
                    .border(1.5.dp, statusColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (stats.blockingStatus) {
                        BlockingStatus.ENABLED -> Icons.Default.Security
                        BlockingStatus.PAUSED -> Icons.Default.HourglassTop
                        BlockingStatus.DISABLED -> Icons.Default.Block
                    },
                    contentDescription = "Protection Status",
                    tint = statusColor,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (stats.blockingStatus) {
                            BlockingStatus.ENABLED -> "حظر التهديدات نشط (Enabled)"
                            BlockingStatus.PAUSED -> "الحظر متوقف مؤقتاً (${stats.pauseRemainingSeconds} ثانية)"
                            BlockingStatus.DISABLED -> "الحظر العام معطل (Disabled)"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "تحكم مباشر عبر /api/dns/blocking • استجابة: ${stats.latencyMs}ms",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark, fontSize = 11.sp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Master Enable/Disable Blocking Switch & Button (المتطلب: زر تبديل الحجب العام)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0F172A))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "تبديل الحجب العام (Master Blocking)",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                )
                Text(
                    text = if (isBlockingEnabled) "الحظر نشط على شبكة Wi-Fi" else "الحظر متوقف، الإعلانات مسموحة",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMutedDark, fontSize = 11.sp)
                )
            }

            Switch(
                checked = isBlockingEnabled,
                onCheckedChange = { checked ->
                    onSetBlocking(checked, 0)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = ShieldGreen,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = ShieldRed.copy(alpha = 0.6f)
                ),
                modifier = Modifier.testTag("master_blocking_switch")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Big Primary Toggle Button for fast action
        Button(
            onClick = {
                onSetBlocking(!isBlockingEnabled, 0)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isBlockingEnabled) ShieldRed.copy(alpha = 0.85f) else ShieldGreen,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("toggle_blocking_main_btn")
        ) {
            Icon(
                imageVector = if (isBlockingEnabled) Icons.Default.Block else Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isBlockingEnabled) "تعطيل الحجب العام (Disable Blocking)" else "تفعيل الحجب العام (Enable Blocking)",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Pause Controls
        Text(
            text = "إيقاف مؤقت سريع (Quick Pause Timers):",
            style = MaterialTheme.typography.labelSmall.copy(color = TextMutedDark, fontSize = 11.sp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickPauseButton(
                label = "30s",
                onClick = { onSetBlocking(false, 30) },
                modifier = Modifier.weight(1f),
                testTag = "pause_30s_btn"
            )
            QuickPauseButton(
                label = "5m",
                onClick = { onSetBlocking(false, 300) },
                modifier = Modifier.weight(1f),
                testTag = "pause_5m_btn"
            )
            QuickPauseButton(
                label = "15m",
                onClick = { onSetBlocking(false, 900) },
                modifier = Modifier.weight(1f),
                testTag = "pause_15m_btn"
            )
        }
    }
}

@Composable
private fun QuickPauseButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    FilledTonalButton(
        onClick = onClick,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = Color(0xFF1E293B),
            contentColor = NeonCyan
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.testTag(testTag)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}
