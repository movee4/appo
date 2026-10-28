package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NetworkClient
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
fun ClientsScreen(
    clients: List<NetworkClient>,
    onUpdateNickname: (ip: String, nickname: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingClient by remember { mutableStateOf<NetworkClient?>(null) }
    var nicknameInput by remember { mutableStateOf("") }
    val numberFormat = NumberFormat.getNumberInstance(Locale.US)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Summary header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CyberCardDark)
                .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp))
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "الأجهزة المتصلة بالشبكة (${clients.size})",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "جميع الأجهزة التي تستخدم خادم الـ DNS المحلي",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark, fontSize = 11.sp)
                )
            }
            Icon(Icons.Default.Devices, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(26.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(clients, key = { it.ip }) { client ->
                ClientCard(
                    client = client,
                    numberFormat = numberFormat,
                    onEditNickname = {
                        editingClient = client
                        nicknameInput = client.customNickname.ifBlank { client.hostname }
                    }
                )
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Edit Nickname Dialog
    editingClient?.let { client ->
        AlertDialog(
            onDismissRequest = { editingClient = null },
            title = {
                Text("تسمية الجهاز (${client.ip})", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            },
            text = {
                Column {
                    Text(
                        text = "اختر اسماً يسهل التعرف عليه لهذا الجهاز في سجلات الشبكة:",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nicknameInput,
                        onValueChange = { nicknameInput = it },
                        placeholder = { Text("مثال: تلفزيون الصالة / هاتف محمد") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("client_nickname_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateNickname(client.ip, nicknameInput.trim())
                        editingClient = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF0A192F)),
                    modifier = Modifier.testTag("save_nickname_confirm_btn")
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { editingClient = null }) {
                    Text("إلغاء")
                }
            },
            containerColor = Color(0xFF131C31),
            titleContentColor = TextPrimaryDark,
            textContentColor = TextSecondaryDark
        )
    }
}

@Composable
private fun ClientCard(
    client: NetworkClient,
    numberFormat: NumberFormat,
    onEditNickname: () -> Unit
) {
    val deviceIcon = when {
        client.displayName.contains("TV", ignoreCase = true) -> Icons.Default.Tv
        client.displayName.contains("Pixel", ignoreCase = true) || client.displayName.contains("Phone", ignoreCase = true) -> Icons.Default.PhoneAndroid
        client.displayName.contains("NAS", ignoreCase = true) -> Icons.Default.Router
        else -> Icons.Default.Computer
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberCardDark)
            .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
            .testTag("client_card_${client.ip}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ElectricBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(deviceIcon, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = client.displayName,
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = TextPrimaryDark,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (client.isOnline) ShieldGreen else TextMutedDark)
                        )
                    }

                    Text(
                        text = "${client.ip} • ${client.vendor}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondaryDark,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }

            IconButton(
                onClick = onEditNickname,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("edit_client_btn_${client.ip}")
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit nickname", tint = NeonCyan, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Queries and Blocked stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "الطلبات: ${numberFormat.format(client.totalQueries)}",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark, fontSize = 11.sp)
            )
            Text(
                text = "المحظورة: ${numberFormat.format(client.blockedQueries)} (${client.percentBlocked}%)",
                style = MaterialTheme.typography.bodySmall.copy(color = ShieldRed, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Block percentage progress bar
        LinearProgressIndicator(
            progress = { (client.percentBlocked / 100f).coerceIn(0f, 1f) },
            color = ShieldRed,
            trackColor = Color(0xFF1E293B),
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
        )
    }
}
