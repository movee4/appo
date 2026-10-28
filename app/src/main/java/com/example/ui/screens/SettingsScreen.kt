package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionTestResult
import com.example.model.PiHoleConfig
import com.example.ui.ConnectionTestState
import com.example.ui.components.ArchitecturalNoticeCard
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCardDark
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ShieldGreen
import com.example.ui.theme.ShieldRed
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@Composable
fun SettingsScreen(
    currentConfig: PiHoleConfig,
    testState: ConnectionTestState,
    onTestConnection: (PiHoleConfig) -> Unit,
    onSaveConfig: (PiHoleConfig) -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var host by remember(currentConfig) { mutableStateOf(currentConfig.host) }
    var portStr by remember(currentConfig) { mutableStateOf(currentConfig.port.toString()) }
    var useHttps by remember(currentConfig) { mutableStateOf(currentConfig.useHttps) }
    var password by remember(currentConfig) { mutableStateOf(currentConfig.apiPassword) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var nasHost by remember(currentConfig) { mutableStateOf(currentConfig.nasHost) }
    var nasMac by remember(currentConfig) { mutableStateOf(currentConfig.nasMacAddress) }
    var nasName by remember(currentConfig) { mutableStateOf(currentConfig.nasDeviceName) }
    var isDemoMode by remember(currentConfig) { mutableStateOf(currentConfig.isDemoMode) }

    val scrollState = rememberScrollState()

    fun buildConfig(): PiHoleConfig {
        val parsedPort = portStr.toIntOrNull() ?: if (useHttps) 443 else 80
        return currentConfig.copy(
            host = host.trim(),
            port = parsedPort,
            useHttps = useHttps,
            apiPassword = password,
            nasHost = nasHost.trim(),
            nasMacAddress = nasMac.trim(),
            nasDeviceName = nasName.trim(),
            isDemoMode = isDemoMode
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Architectural Notice Banner
        ArchitecturalNoticeCard()

        // Section 1: Pi-hole Server Connection
        SectionContainer(
            title = "إعدادات خادم Pi-hole (Server Setup)",
            icon = Icons.Default.Dns
        ) {
            Text(
                text = "خادم Pi-hole يعمل على NAS بعنوان IP ثابت ويقدم REST API لتسجيل استعلامات DNS لجميع أجهزة الشبكة.",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark, fontSize = 12.sp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // IP Address field
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text("عنوان IP الخادم (Server IP)") },
                placeholder = { Text("192.168.1.100") },
                leadingIcon = { Icon(Icons.Default.Router, contentDescription = null, tint = NeonCyan) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                colors = customTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pihole_ip_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Port & HTTPS Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = portStr,
                    onValueChange = { portStr = it },
                    label = { Text("المنفذ (Port)") },
                    placeholder = { Text("80") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = customTextFieldColors(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pihole_port_input")
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("HTTPS", style = MaterialTheme.typography.labelMedium.copy(color = TextPrimaryDark))
                        Text(if (useHttps) "مشفر (SSL)" else "HTTP عادي", style = MaterialTheme.typography.bodySmall.copy(color = TextMutedDark, fontSize = 10.sp))
                    }
                    Switch(
                        checked = useHttps,
                        onCheckedChange = { useHttps = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonCyan,
                            checkedTrackColor = ElectricBlue.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("pihole_https_toggle")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Password / App Token field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("كلمة مرور API أو App Password") },
                placeholder = { Text("أدخل كلمة مرور واجهة Pi-hole") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = NeonCyan) },
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                            tint = TextMutedDark
                        )
                    }
                },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = customTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pihole_password_input")
            )

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "يتم حفظ كلمة المرور مشفرة محلياً بواسطة Encrypted DataStore لحماية أمان شبكتك.",
                style = MaterialTheme.typography.bodySmall.copy(color = TextMutedDark, fontSize = 11.sp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Test Connection Button & Result
            OutlinedButton(
                onClick = { onTestConnection(buildConfig()) },
                enabled = testState !is ConnectionTestState.Testing,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = NeonCyan
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("test_connection_btn")
            ) {
                if (testState is ConnectionTestState.Testing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = NeonCyan,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("جاري الاتصال والتحقق من /api/auth...")
                } else {
                    Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("اختبار الاتصال (Test Connection)", fontWeight = FontWeight.Bold)
                }
            }

            // Connection Test Result Display
            AnimatedVisibility(visible = testState is ConnectionTestState.Completed) {
                if (testState is ConnectionTestState.Completed) {
                    val res = testState.result
                    ConnectionResultBox(result = res)
                }
            }
        }

        // Section 2: NAS Status & Wake-on-LAN Settings (إعداد اختياري للـ NAS)
        SectionContainer(
            title = "إعدادات الـ NAS المضيف (اختياري)",
            icon = Icons.Default.Storage
        ) {
            Text(
                text = "أدخل عنوان IP و MAC الخاص بجهاز الـ NAS لعرض حالته وإرسال حزم الإيقاظ (Wake-on-LAN).",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark, fontSize = 12.sp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = nasName,
                onValueChange = { nasName = it },
                label = { Text("اسم جهاز الـ NAS") },
                placeholder = { Text("Synology DS920+ / TrueNAS") },
                singleLine = true,
                colors = customTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("nas_name_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = nasHost,
                onValueChange = { nasHost = it },
                label = { Text("عنوان IP للـ NAS") },
                placeholder = { Text("192.168.1.100") },
                singleLine = true,
                colors = customTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("nas_ip_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = nasMac,
                onValueChange = { nasMac = it },
                label = { Text("عنوان MAC للـ NAS (لـ Wake-on-LAN)") },
                placeholder = { Text("00:11:32:8A:2B:1C") },
                singleLine = true,
                colors = customTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("nas_mac_input")
            )
        }

        // Section 3: Simulation / Demo Mode
        SectionContainer(
            title = "وضع المحاكاة التفاعلي (Simulation Mode)",
            icon = Icons.Default.Security
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "محاكاة ترافيك DNS الحي (Demo Mode)",
                        style = MaterialTheme.typography.titleSmall.copy(color = TextPrimaryDark)
                    )
                    Text(
                        text = "توليد بيانات واستعلامات تفاعلية لتجربة جميع شاشات التطبيق حتى بدون اتصال بخادم فعلي.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark, fontSize = 11.sp)
                    )
                }

                Switch(
                    checked = isDemoMode,
                    onCheckedChange = { isDemoMode = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonCyan,
                        checkedTrackColor = ElectricBlue.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.testTag("demo_mode_toggle")
                )
            }
        }

        // Section 4: About & Legal Disclaimer
        SectionContainer(
            title = "عن التطبيق وإخلاء المسؤولية القانونية",
            icon = Icons.Default.Info
        ) {
            Text(
                text = "توضيح ملكية الشبكة للمستخدم، وحدود المراقبة (تسجيل أسماء النطاقات فقط دون إمكانية قراءة المحتوى المشفر مثل الرسائل والفيديوهات).",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark, fontSize = 12.sp, lineHeight = 17.sp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onOpenAbout,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_about_disclaimer_btn")
            ) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("عرض شاشة إخلاء المسؤولية وسياسة الخصوصية", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        // Save Button
        Button(
            onClick = { onSaveConfig(buildConfig()) },
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonCyan,
                contentColor = Color(0xFF0A192F)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("save_settings_btn")
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "حفظ الإعدادات المشفرة (Save Configuration)",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun SectionContainer(
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
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun ConnectionResultBox(result: ConnectionTestResult) {
    val borderColor = if (result.isSuccess) ShieldGreen else ShieldRed
    val bgColor = if (result.isSuccess) ShieldGreen.copy(alpha = 0.1f) else ShieldRed.copy(alpha = 0.1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .padding(12.dp)
            .testTag("connection_result_box")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (result.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = borderColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = result.message,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "النسخة: ${result.detectedApiVersion}",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark, fontSize = 11.sp)
            )
            Text(
                text = "زمن الاستجابة: ${result.latencyMs}ms",
                style = MaterialTheme.typography.bodySmall.copy(color = NeonCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            )
        }

        if (result.details.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = result.details,
                style = MaterialTheme.typography.bodySmall.copy(color = TextMutedDark, fontSize = 11.sp)
            )
        }
    }
}

@Composable
private fun customTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NeonCyan,
    unfocusedBorderColor = CyberCardBorder,
    focusedLabelColor = NeonCyan,
    unfocusedLabelColor = TextSecondaryDark,
    focusedTextColor = TextPrimaryDark,
    unfocusedTextColor = TextPrimaryDark,
    cursorColor = NeonCyan
)
