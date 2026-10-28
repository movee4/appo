package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DnsQueryLog
import com.example.model.QueryStatus
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
fun QueryDetailSheet(
    queryLog: DnsQueryLog?,
    onDismiss: () -> Unit,
    onQuickBlock: (String) -> Unit,
    onQuickWhitelist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (queryLog == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val isBlocked = queryLog.status.name.startsWith("BLOCKED")
    val badgeColor = when (queryLog.status) {
        QueryStatus.BLOCKED_GRAVITY, QueryStatus.BLOCKED_BLACKLIST, QueryStatus.BLOCKED_REGEX -> ShieldRed
        QueryStatus.FORWARDED -> StatusForwarded
        QueryStatus.CACHED -> StatusCached
        QueryStatus.UNKNOWN -> TextMutedDark
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F172A),
        scrimColor = Color.Black.copy(alpha = 0.6f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF334155))
            )
        },
        modifier = modifier.testTag("query_detail_sheet")
    ) {
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header: Title & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(badgeColor.copy(alpha = 0.15f))
                            .border(1.dp, badgeColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isBlocked) Icons.Default.Block else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "تفاصيل استعلام DNS",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = TextPrimaryDark,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = if (isBlocked) "طلب محجوب بواسطة Pi-hole" else "طلب مسموح عبر الشبكة",
                            style = MaterialTheme.typography.bodySmall.copy(color = badgeColor, fontSize = 11.sp)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondaryDark)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Domain Name Display Card with Copy button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberCardDark)
                    .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "اسم النطاق المطلوب (Domain):",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMutedDark, fontSize = 11.sp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = queryLog.domain,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 15.sp
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("domain", queryLog.domain))
                            Toast.makeText(context, "تم نسخ النطاق: ${queryLog.domain}", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = NeonCyan, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Source Device Card (IP + Hostname)
            DetailSectionCard(title = "الجهاز المصدر (Client Device)", icon = Icons.Default.Devices) {
                DetailRow(label = "اسم الجهاز (Hostname)", value = queryLog.clientName)
                DetailRow(label = "عنوان IP المصدر", value = queryLog.clientIp, isMonospace = true)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Technical Query Information
            DetailSectionCard(title = "بيانات استعلام الـ DNS", icon = Icons.Default.Dns) {
                DetailRow(label = "نوع السجل (Record Type)", value = queryLog.queryType)
                DetailRow(label = "حالة الحظر / المعالجة", value = queryLog.statusText, valueColor = badgeColor)
                DetailRow(label = "استجابة الخادم (Reply)", value = queryLog.reply, isMonospace = true)
                DetailRow(label = "الخادم المصدري (Upstream)", value = queryLog.upstreamServer)
                DetailRow(label = "زمن المعالجة (Latency)", value = "${queryLog.responseTimeMs} ms", isMonospace = true)
                DetailRow(label = "حالة الأمان DNSSEC", value = queryLog.dnssecStatus)
                DetailRow(
                    label = "التوقيت المسجل",
                    value = if (queryLog.formattedFullDate.isNotBlank()) queryLog.formattedFullDate else queryLog.formattedTime
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons: Quick Block / Quick Whitelist
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isBlocked) {
                    Button(
                        onClick = {
                            onQuickBlock(queryLog.domain)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ShieldRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("detail_block_btn")
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حظر النطاق (Blacklist)", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            onQuickWhitelist(queryLog.domain)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ShieldGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("detail_whitelist_btn")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("سماح النطاق (Whitelist)", fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondaryDark),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(46.dp)
                ) {
                    Text("إغلاق")
                }
            }
        }
    }
}

@Composable
private fun DetailSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberCardDark)
            .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = CyberCardBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    isMonospace: Boolean = false,
    valueColor: Color = TextPrimaryDark
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark, fontSize = 12.sp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                color = valueColor,
                fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
        )
    }
}
