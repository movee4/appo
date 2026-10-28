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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberBgDark
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCardDark
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ShieldGreen
import com.example.ui.theme.ShieldRed
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutDisclaimerScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CyberBgDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "عن التطبيق وإخلاء المسؤولية",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("about_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NeonCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CyberSurfaceDark,
                    titleContentColor = TextPrimaryDark
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Branding Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF131D33))
                    .border(1.dp, CyberCardBorder, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(NeonCyan.copy(alpha = 0.15f))
                        .border(1.5.dp, NeonCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Network Commander Shield",
                        tint = NeonCyan,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Network Commander",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.Bold
                    )
                )

                Text(
                    text = "أداة إدارة وأمان الشبكة المحلية الشخصية • الإصدار 1.0",
                    style = MaterialTheme.typography.bodySmall.copy(color = NeonCyan, fontSize = 12.sp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Personal Network Admin Tool for NAS Pi-hole Integration",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMutedDark, fontSize = 10.sp)
                )
            }

            // Section 1: Network Ownership & Legal Disclaimer (توضيح أن الشبكة ملك المستخدم وأنه المسؤول عن استخدامها)
            DisclaimerCard(
                title = "ملكية الشبكة والمسؤولية القانونية",
                icon = Icons.Default.Gavel,
                accentColor = WarningAmber
            ) {
                Text(
                    text = "• هذا التطبيق مصمم حصرياً للاستخدام الشخصي لإدارة الشبكات المحلية الخاصة (Home / Authorized Local Networks) المملوكة للمستخدم أو المصرح له بإدارتها رسمياً.\n\n" +
                            "• يُقر المستخدم بأن الشبكة المحلية وأجهزتها وخادم الـ NAS ملك له، وأنه يتحمل وحده كامل المسؤولية القانونية والأخلاقية عن تكوين خادم Pi-hole وقواعد حظر وسماح النطاقات.\n\n" +
                            "• لا يتحمل مطورو التطبيق أي مسؤولية عن أي سوء استخدام أو تعطيل للخدمات ناتج عن قواعد حجب خاطئة أو استخدام التطبيق خارج الشبكات المصرح بها.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondaryDark,
                        lineHeight = 20.sp,
                        fontSize = 13.sp
                    )
                )
            }

            // Section 2: Encrypted Traffic Privacy & Limitations (المحتوى المشفر وأسماء النطاقات فقط)
            DisclaimerCard(
                title = "حدود المراقبة وحماية المحتوى المشفر (HTTPS / TLS)",
                icon = Icons.Default.Lock,
                accentColor = NeonCyan
            ) {
                Text(
                    text = "أمان وخصوصية مستخدمي الشبكة محمية بالكامل بموجب معايير التشفير القياسية:",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // What is logged
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A))
                        .padding(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = ElectricBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "ما يتم تسجيله فقط (DNS Metadata):",
                            style = MaterialTheme.typography.labelMedium.copy(color = ElectricBlue, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "يتم تسجيل أسماء النطاقات المطلوبة فقط (مثل google.com أو adservice.com)، وعنوان IP الخاص بالجهاز المصدر، وتوقيت الطلب، وذلك لتحديد ما إذا كان النطاق في قائمة الحظر أم لا.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark, fontSize = 12.sp, lineHeight = 17.sp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // What is NEVER accessible (Strictly protected by encryption)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E1424))
                        .padding(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = ShieldRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "ما لا يمكن للتطبيق أو Pi-hole قراءته إطلاقاً:",
                            style = MaterialTheme.typography.labelMedium.copy(color = ShieldRed, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "لا يمكن قراءة أو اعتراض محتوى الاتصالات المشفر (HTTPS / TLS) نهائياً، ويشمل ذلك:\n" +
                                    "✗ محتوى الرسائل والمحادثات.\n" +
                                    "✗ الفيديوهات والصور المنقولة.\n" +
                                    "✗ كلمات البحث والصفحات الفرعية داخل المواقع.\n" +
                                    "✗ كلمات المرور والبيانات البنكية والشخصية.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark, fontSize = 12.sp, lineHeight = 18.sp)
                        )
                    }
                }
            }

            // Section 3: Nature of Real DNS-level Monitoring (شرح المراقبة الفعلية)
            DisclaimerCard(
                title = "طبيعة المراقبة الفعلية (DNS Sinkhole Mechanism)",
                icon = Icons.Default.Security,
                accentColor = ShieldGreen
            ) {
                Text(
                    text = "• التطبيق لا يلتقط ترافيك الأجهزة بنفسه ولا يستخدم أي تقنيات تنصت على الحزم (Packet Sniffing / Deep Packet Inspection).\n\n" +
                            "• المراقبة الفعلية تتم بصورة غير تدخلية على مستوى خادم أسماء النطاقات (DNS Sinkhole) الذي يعمل على الـ NAS؛ حيث يطلب الجهاز معرفة IP النطاق، فإذا كان النطاق إعلاناً أو برمجية خبيثة، يعيد Pi-hole عنوان 0.0.0.0 لمنع تحميله.\n\n" +
                            "• الهدف من هذه المراقبة حماية أجهزة الشبكة من التهديدات السيبرانية والتصيد الاحتيالي، تقليل استهلاك البيانات، وحجب الإعلانات المزعجة لجميع أجهزة المنزل.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondaryDark,
                        lineHeight = 20.sp,
                        fontSize = 13.sp
                    )
                )
            }

            // Section 4: Local Privacy Guarantee
            DisclaimerCard(
                title = "معالجة محلية 100% بدون سحابة",
                icon = Icons.Default.Info,
                accentColor = ElectricBlue
            ) {
                Text(
                    text = "جميع الاتصالات وكلمات المرور والبيانات المعروضة تُعالج محلياً داخل شبكة الواي فاي المنزلية بين هاتفك وخادم Pi-hole على الـ NAS. التطبيق لا يرسل أي استعلامات أو بيانات telemetry إلى أي خوادم خارجية.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondaryDark, fontSize = 12.sp, lineHeight = 18.sp)
                )
            }

            // Acknowledgment Button
            Button(
                onClick = onNavigateBack,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = Color(0xFF0A192F)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("accept_about_btn")
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("فهمت وموافق على سياسة الاستخدام", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DisclaimerCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
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
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = CyberCardBorder.copy(alpha = 0.6f), thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}
