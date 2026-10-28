package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.example.data.network.NetworkDiagnosticHelper
import com.example.model.PiHoleConfig
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
import kotlinx.coroutines.launch

@Composable
fun DiagnosticsScreen(
    config: PiHoleConfig,
    pingMessage: String?,
    wolMessage: String?,
    onPingNas: () -> Unit,
    onSendWol: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var customHost by remember { mutableStateOf("192.168.1.1") }
    var customPingResult by remember { mutableStateOf<String?>(null) }
    var isPingingCustom by remember { mutableStateOf(false) }

    var portScanResults by remember { mutableStateOf<Map<Int, Boolean>?>(null) }
    var isScanningPorts by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Pi-hole & NAS Web Shortcuts
        DiagnosticCard(
            title = "روابط الإدارة السريعة (Quick Web Links)",
            icon = Icons.Default.OpenInBrowser
        ) {
            Text(
                text = "الوصول المباشر إلى واجهة Pi-hole الرسمية وتوثيق الـ REST API المنشور على الـ NAS:",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark, fontSize = 12.sp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(config.adminWebUrl))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF0A192F)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_admin_web_btn")
                ) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("واجهة /admin", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(config.apiDocsUrl))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricBlue),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_api_docs_btn")
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("توثيق /api/docs", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section: NAS Wake-on-LAN
        DiagnosticCard(
            title = "إيقاظ جهاز الـ NAS عبر الشبكة (Wake-on-LAN)",
            icon = Icons.Default.PowerSettingsNew
        ) {
            Text(
                text = "إرسال حزمة Magic Packet (UDP port 9/7) إلى عنوان MAC الخاص بالـ NAS لتشغيله عن بُعد:",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark, fontSize = 12.sp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "عنوان MAC الهدف: ${config.nasMacAddress.ifBlank { "غير مدخل (يرجى إدخاله من الإعدادات)" }}",
                style = MaterialTheme.typography.labelSmall.copy(color = NeonCyan, fontFamily = FontFamily.Monospace)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onSendWol,
                colors = ButtonDefaults.buttonColors(containerColor = WarningAmber, contentColor = Color(0xFF1E1B4B)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("send_wol_magic_packet_btn")
            ) {
                Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("إرسال حزمة WoL Magic Packet", fontWeight = FontWeight.Bold)
            }

            if (!wolMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = wolMessage,
                    style = MaterialTheme.typography.bodySmall.copy(color = ShieldGreen, fontSize = 11.sp)
                )
            }
        }

        // Section: Port Scanner on NAS
        DiagnosticCard(
            title = "فحص منافذ الـ NAS الرئيسية (Service Port Probe)",
            icon = Icons.Default.Terminal
        ) {
            Text(
                text = "التحقق من جاهزية خدمات الـ NAS (DNS 53, HTTP 80, DSM 5000, SSH 22):",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark, fontSize = 12.sp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            FilledTonalButton(
                onClick = {
                    scope.launch {
                        isScanningPorts = true
                        val ports = listOf(53, 80, 443, 5000, 22, 445)
                        val results = mutableMapOf<Int, Boolean>()
                        ports.forEach { port ->
                            results[port] = NetworkDiagnosticHelper.probePort(config.nasHost, port, 1200)
                        }
                        portScanResults = results
                        isScanningPorts = false
                    }
                },
                enabled = !isScanningPorts,
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF1E293B), contentColor = NeonCyan),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("probe_nas_ports_btn")
            ) {
                if (isScanningPorts) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NeonCyan, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("جاري فحص المنافذ...")
                } else {
                    Text("فحص منافذ ${config.nasHost}")
                }
            }

            portScanResults?.let { results ->
                Spacer(modifier = Modifier.height(10.dp))
                val portNames = mapOf(
                    53 to "DNS Server (Pi-hole FTL)",
                    80 to "HTTP Web Admin",
                    443 to "HTTPS Secure Web",
                    5000 to "Synology DSM Console",
                    22 to "SSH Secure Shell",
                    445 to "SMB File Sharing"
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    results.forEach { (port, isOpen) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0F172A))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "منفذ $port (${portNames[port] ?: "Service"}):",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextPrimaryDark, fontSize = 12.sp)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isOpen) ShieldGreen else ShieldRed)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isOpen) "مفتوح (Open)" else "مغلق (Closed)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isOpen) ShieldGreen else ShieldRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Custom Host Ping
        DiagnosticCard(
            title = "أداة فحص زمن الوصول (Ping Tool)",
            icon = Icons.Default.NetworkPing
        ) {
            Text(
                text = "اختبار الاتصال بأي جهاز محلي أو بوابة الراوتر (Gateway):",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark, fontSize = 12.sp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customHost,
                    onValueChange = { customHost = it },
                    placeholder = { Text("192.168.1.1") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ping_host_input")
                )

                Button(
                    onClick = {
                        scope.launch {
                            isPingingCustom = true
                            val (reachable, latency) = NetworkDiagnosticHelper.pingHost(customHost.trim(), 80, 2000)
                            customPingResult = if (reachable) "استجابة سريعة! زمن الوصول: ${latency}ms" else "فشل الاتصال: المضيف غير متاح"
                            isPingingCustom = false
                        }
                    },
                    enabled = !isPingingCustom && customHost.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF0A192F)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("run_ping_btn")
                ) {
                    if (isPingingCustom) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF0A192F), strokeWidth = 2.dp)
                    } else {
                        Text("Ping")
                    }
                }
            }

            customPingResult?.let { res ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = res,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (res.contains("استجابة")) ShieldGreen else ShieldRed,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun DiagnosticCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CyberCardDark)
            .border(1.dp, CyberCardBorder, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}
