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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.model.BlocklistSource
import com.example.model.DomainRule
import com.example.model.GravityUpdateState
import com.example.model.RuleType
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
fun DomainRulesScreen(
    rules: List<DomainRule>,
    blocklists: List<BlocklistSource>,
    gravityState: GravityUpdateState,
    onAddRule: (domain: String, type: RuleType, comment: String) -> Unit,
    onRemoveRule: (id: String) -> Unit,
    onToggleRule: (id: String) -> Unit,
    onUpdateGravity: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Blacklist, 1 = Whitelist, 2 = Adlists (/api/lists)
    var showAddDialog by remember { mutableStateOf(false) }
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    val filteredRules = rules.filter { rule ->
        when (selectedTab) {
            0 -> rule.type == RuleType.EXACT_BLACKLIST || rule.type == RuleType.REGEX_BLACKLIST
            1 -> rule.type == RuleType.EXACT_WHITELIST || rule.type == RuleType.REGEX_WHITELIST
            else -> false
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        floatingActionButton = {
            if (selectedTab != 2) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = if (selectedTab == 0) ShieldRed else ShieldGreen,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("add_rule_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Rule")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedTab == 0) "إضافة حظر" else "إضافة سماح",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Three Tabs: Blacklist, Whitelist, and Adlists (/api/lists)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF111827),
                contentColor = NeonCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = when (selectedTab) {
                            0 -> ShieldRed
                            1 -> ShieldGreen
                            else -> ElectricBlue
                        }
                    )
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Block, contentDescription = null, tint = ShieldRed, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("الحظر (Blacklist)", fontSize = 11.sp)
                        }
                    },
                    modifier = Modifier.testTag("tab_blacklist")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ShieldGreen, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("السماح (Whitelist)", fontSize = 11.sp)
                        }
                    },
                    modifier = Modifier.testTag("tab_whitelist")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FormatListBulleted, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("قوائم الحجب (/api/lists)", fontSize = 10.sp)
                        }
                    },
                    modifier = Modifier.testTag("tab_adlists")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // TAB 2: Adlists (/api/lists) and Gravity Update (/api/action)
            if (selectedTab == 2) {
                // Gravity Update Control Card (المتطلب: تحديث القوائم gravity update عبر /api/action)
                GravityUpdateCard(
                    gravityState = gravityState,
                    onUpdateGravity = onUpdateGravity,
                    numberFormat = numberFormat
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "قوائم الحجب المشترك بها (/api/lists):",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(blocklists, key = { it.id }) { listSource ->
                        AdlistCard(listSource = listSource, numberFormat = numberFormat)
                    }
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            } else {
                // TAB 0 & 1: Custom Rules (Blacklist & Whitelist via /api/domains)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedTab == 0) "نطاقات القائمة السوداء (/api/domains)" else "نطاقات القائمة البيضاء (/api/domains)",
                        style = MaterialTheme.typography.titleSmall.copy(color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${filteredRules.size} قاعدة",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark, fontSize = 11.sp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (filteredRules.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Rule, contentDescription = null, tint = TextMutedDark, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (selectedTab == 0) "لا توجد نطاقات محظورة مخصصة حالياً" else "لا توجد نطاقات مسموحة مخصصة حالياً",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondaryDark)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "اضغط على زر الإضافة بالأسفل لإنشاء قاعدة جديدة عبر /api/domains",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMutedDark, fontSize = 11.sp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredRules, key = { it.id }) { rule ->
                            RuleItemCard(
                                rule = rule,
                                onToggle = { onToggleRule(rule.id) },
                                onDelete = { onRemoveRule(rule.id) }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(72.dp))
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddRuleDialog(
            defaultTab = selectedTab,
            onDismiss = { showAddDialog = false },
            onAdd = { domain, type, comment ->
                onAddRule(domain, type, comment)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun GravityUpdateCard(
    gravityState: GravityUpdateState,
    onUpdateGravity: () -> Unit,
    numberFormat: NumberFormat
) {
    val isUpdating = gravityState is GravityUpdateState.InProgress

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF131D33))
            .border(1.dp, ElectricBlue.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(16.dp)
            .testTag("gravity_update_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(ElectricBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "تحديث قواعد الجاذبية (Update Gravity)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "إرسال أمر التحديث عبر /api/action",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark, fontSize = 11.sp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "يقوم خادم Pi-hole بتنزيل جميع قوائم Adlists ودمجها وإزالة التكرار لتحديث قاعدة بيانات الحجب.",
            style = MaterialTheme.typography.bodySmall.copy(color = TextMutedDark, fontSize = 12.sp, lineHeight = 17.sp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Trigger Button (المتطلب: تحديث القوائم gravity update عبر /api/action)
        Button(
            onClick = onUpdateGravity,
            enabled = !isUpdating,
            colors = ButtonDefaults.buttonColors(
                containerColor = ElectricBlue,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("update_gravity_action_btn")
        ) {
            if (isUpdating) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("جاري التحديث عبر /api/action...")
            } else {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("تحديث قوائم الجاذبية الآن (Update Gravity)", fontWeight = FontWeight.Bold)
            }
        }

        // Result Message Feedback
        AnimatedVisibility(visible = gravityState is GravityUpdateState.Completed) {
            if (gravityState is GravityUpdateState.Completed) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (gravityState.success) ShieldGreen.copy(alpha = 0.15f) else ShieldRed.copy(alpha = 0.15f))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (gravityState.success) Icons.Default.CheckCircle else Icons.Default.Block,
                        contentDescription = null,
                        tint = if (gravityState.success) ShieldGreen else ShieldRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = gravityState.message,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextPrimaryDark,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun AdlistCard(
    listSource: BlocklistSource,
    numberFormat: NumberFormat
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberCardDark)
            .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
            .testTag("adlist_card_${listSource.id}")
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
                Icon(Icons.Default.Link, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = listSource.comment.ifBlank { "قائمة حجب خارجية" },
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(ShieldGreen.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${numberFormat.format(listSource.domainCount)} نطاق",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ShieldGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Adlist URL
        Text(
            text = listSource.url,
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextSecondaryDark,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            ),
            maxLines = 2
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "تاريخ الإضافة: ${listSource.dateAdded}",
                style = MaterialTheme.typography.labelSmall.copy(color = TextMutedDark, fontSize = 10.sp)
            )
            Text(
                text = "الحالة: ${listSource.lastUpdateStatus}",
                style = MaterialTheme.typography.labelSmall.copy(color = NeonCyan, fontSize = 10.sp)
            )
        }
    }
}

@Composable
private fun RuleItemCard(
    rule: DomainRule,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val isRegex = rule.type == RuleType.REGEX_BLACKLIST || rule.type == RuleType.REGEX_WHITELIST

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberCardDark)
            .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag("rule_card_${rule.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = rule.domain,
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = if (rule.isEnabled) TextPrimaryDark else TextMutedDark,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        )
                    )
                    if (isRegex) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF3B2865))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text("REGEX", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFC084FC), fontSize = 9.sp))
                        }
                    }
                }

                if (rule.comment.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${rule.comment} • ${rule.dateAdded}",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMutedDark, fontSize = 11.sp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonCyan,
                        checkedTrackColor = ElectricBlue.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.testTag("toggle_rule_switch_${rule.id}")
                )

                // Delete button via /api/domains
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_rule_btn_${rule.id}")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ShieldRed.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun AddRuleDialog(
    defaultTab: Int,
    onDismiss: () -> Unit,
    onAdd: (domain: String, type: RuleType, comment: String) -> Unit
) {
    var domain by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var isBlacklist by remember { mutableStateOf(defaultTab == 0) }
    var isRegex by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("إضافة نطاق عبر /api/domains", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = domain,
                    onValueChange = { domain = it },
                    label = { Text("النطاق (Domain)") },
                    placeholder = { Text(if (isRegex) ".*\\.tracking\\.com" else "ads.example.com") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_rule_domain_input")
                )

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("ملاحظة توضيحية (اختياري)") },
                    placeholder = { Text("مثال: حظر إعلانات محددة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // List Type Selection: Blocklist or Whitelist
                Text("نوع القائمة:", style = MaterialTheme.typography.labelMedium.copy(color = TextSecondaryDark))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = isBlacklist,
                        onClick = { isBlacklist = true },
                        colors = RadioButtonDefaults.colors(selectedColor = ShieldRed)
                    )
                    Text("حظر (Blacklist)", color = TextPrimaryDark, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = !isBlacklist,
                        onClick = { isBlacklist = false },
                        colors = RadioButtonDefaults.colors(selectedColor = ShieldGreen)
                    )
                    Text("سماح (Whitelist)", color = TextPrimaryDark, fontSize = 13.sp)
                }

                // Match Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("مطابقة تعبير نمطي (Regex):", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark))
                    Switch(
                        checked = isRegex,
                        onCheckedChange = { isRegex = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (domain.isNotBlank()) {
                        val ruleType = when {
                            isBlacklist && isRegex -> RuleType.REGEX_BLACKLIST
                            isBlacklist && !isRegex -> RuleType.EXACT_BLACKLIST
                            !isBlacklist && isRegex -> RuleType.REGEX_WHITELIST
                            else -> RuleType.EXACT_WHITELIST
                        }
                        onAdd(domain.trim(), ruleType, comment.trim())
                    }
                },
                enabled = domain.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isBlacklist) ShieldRed else ShieldGreen,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("confirm_add_rule_btn")
            ) {
                Text(if (isBlacklist) "إضافة للحظر" else "إضافة للسماح")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        },
        containerColor = Color(0xFF131C31),
        titleContentColor = TextPrimaryDark,
        textContentColor = TextSecondaryDark
    )
}
