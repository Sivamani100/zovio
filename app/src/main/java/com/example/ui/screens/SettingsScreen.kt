package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import io.github.rabehx.iconsax.Iconsax
import io.github.rabehx.iconsax.outline.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.preferences.UserPreferences
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import com.example.utils.PermissionHelper

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onNavigateToTab: (Int) -> Unit = {},
    onBottomBarVisibility: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current

    val isServiceEnabled by viewModel.isServiceEnabled.collectAsState()
    val savedLanguage by viewModel.selectedLanguage.collectAsState()
    val announcementStyle by viewModel.announcementStyle.collectAsState()
    val volumeBoost by viewModel.volumeBoost.collectAsState()
    val appTheme by viewModel.appTheme.collectAsState()

    var currentSubPage by remember { mutableStateOf("main") } // "main", "voice", "stability", "diagnostics", "tutorial", "theme", "support"

    // Notify parent about bottom bar visibility
    LaunchedEffect(currentSubPage) {
        onBottomBarVisibility(currentSubPage == "main")
    }

    var dropdownExpanded by remember { mutableStateOf(false) }

    val languageNamesMap = UserPreferences.SUPPORTED_LANGUAGES.entries.associate { it.value to it.key }
    val selectedLanguageName = languageNamesMap[savedLanguage] ?: "English"

    // Fluid custom slide-in and slide-out transitions matching premium aesthetics
    AnimatedContent(
        targetState = currentSubPage,
        transitionSpec = {
            if (targetState != "main") {
                // Navigate into sub-page: Slide in from right
                (slideInHorizontally { width -> width } + fadeIn(animationSpec = tween(300))) togetherWith
                        slideOutHorizontally { width -> -width } + fadeOut(animationSpec = tween(300))
            } else {
                // Return to main: Slide in from left
                (slideInHorizontally { width -> -width } + fadeIn(animationSpec = tween(300))) togetherWith
                        slideOutHorizontally { width -> width } + fadeOut(animationSpec = tween(300))
            }
        },
        modifier = modifier.fillMaxSize().background(FinanceBg),
        label = "SettingsPageTransitions"
    ) { page ->
        when (page) {
            "main" -> {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    "Settings",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = FinanceBlack,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            },
                            // No unwanted back button since this is a primary tab destination
                            actions = {
                                Box(
                                    modifier = Modifier
                                        .padding(end = 12.dp)
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(FinanceWhite)
                                        .border(0.5.dp, FinanceGrey.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Iconsax.Outline.More,
                                        contentDescription = "More",
                                        tint = FinanceBlack,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent
                            )
                        )
                    },
                    containerColor = FinanceBg
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Global Fast Toggle Switchboard Card
                        Text(
                            "Fast Controls",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = FinanceGrey,
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = FinanceWhite),
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                // Global switch
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Voice Announcements", 
                                            fontWeight = FontWeight.Bold, 
                                            fontSize = 14.sp, 
                                            color = FinanceBlack
                                        )
                                        Text(
                                            "Globally pause or play receipt alerts", 
                                            fontSize = 11.sp, 
                                            color = FinanceGrey
                                        )
                                    }
                                    Switch(
                                        checked = isServiceEnabled,
                                        onCheckedChange = { viewModel.setServiceEnabled(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = FinancePurple,
                                            uncheckedThumbColor = Color.White,
                                            uncheckedTrackColor = FinanceGrey.copy(alpha = 0.3f),
                                            uncheckedBorderColor = Color.Transparent
                                        )
                                    )
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = FinanceBg)

                                // Volume boost switch
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "200% Speaker Vol Boost", 
                                            fontWeight = FontWeight.Bold, 
                                            fontSize = 14.sp, 
                                            color = FinanceBlack
                                        )
                                        Text(
                                            "Amplifies voice alerts in noisy environments", 
                                            fontSize = 11.sp, 
                                            color = FinanceGrey
                                        )
                                    }
                                    Switch(
                                        checked = volumeBoost,
                                        onCheckedChange = { viewModel.setVolumeBoost(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = FinancePurple,
                                            uncheckedThumbColor = Color.White,
                                            uncheckedTrackColor = FinanceGrey.copy(alpha = 0.3f),
                                            uncheckedBorderColor = Color.Transparent
                                        )
                                    )
                                }
                            }
                        }

                        // 2. System Sub-pages Directory
                        Text(
                            "Quick Access Settings",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = FinanceGrey,
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = FinanceWhite),
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                // Sub-page 1: Voice & Accent
                                SettingsListRow(
                                    icon = Iconsax.Outline.LanguageSquare,
                                    title = "Voice & Language",
                                    subText = selectedLanguageName,
                                    onClick = { currentSubPage = "voice" }
                                )

                                HorizontalDivider(modifier = Modifier.padding(horizontal = 18.dp), color = FinanceBg)

                                SettingsListRow(
                                    icon = Iconsax.Outline.ShieldTick,
                                    title = "Battery Optimization",
                                    subText = "Keep voice alerts running in the background",
                                    onClick = { currentSubPage = "stability" }
                                )

                                HorizontalDivider(modifier = Modifier.padding(horizontal = 18.dp), color = FinanceBg)

                                SettingsListRow(
                                    icon = Iconsax.Outline.Setting4,
                                    title = "Diagnostics",
                                    subText = "Check listener and voice engine status",
                                    onClick = { currentSubPage = "diagnostics" }
                                )

                                HorizontalDivider(modifier = Modifier.padding(horizontal = 18.dp), color = FinanceBg)

                                SettingsListRow(
                                    icon = Iconsax.Outline.Moon,
                                    title = "Theme & Appearance",
                                    subText = appTheme.replaceFirstChar { it.uppercaseChar() },
                                    onClick = { currentSubPage = "theme" }
                                )

                                HorizontalDivider(modifier = Modifier.padding(horizontal = 18.dp), color = FinanceBg)

                                SettingsListRow(
                                    icon = Iconsax.Outline.Refresh2,
                                    title = "Guided Setup",
                                    subText = "Restart onboarding walkthrough",
                                    onClick = { currentSubPage = "tutorial" }
                                )

                                HorizontalDivider(modifier = Modifier.padding(horizontal = 18.dp), color = FinanceBg)

                                SettingsListRow(
                                    icon = Iconsax.Outline.DocumentText,
                                    title = "Terms & Conditions",
                                    subText = "Read Zovio's usage terms",
                                    onClick = { currentSubPage = "terms" }
                                )

                                HorizontalDivider(modifier = Modifier.padding(horizontal = 18.dp), color = FinanceBg)

                                SettingsListRow(
                                    icon = Iconsax.Outline.Support,
                                    title = "Support & Help",
                                    subText = "Email support or read quick tips",
                                    onClick = { currentSubPage = "support" }
                                )
                            }
                        }

                        // Premium offline secure info card
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = FinanceYellowLight),
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(18.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(imageVector = Iconsax.Outline.ShieldTick, contentDescription = null, tint = FinanceBlack, modifier = Modifier.size(20.dp))
                                Column {
                                    Text("100% Secure Offline soundbox engine", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FinanceBlack)
                                    Text(
                                        "This soundbox client processes notification payments offline without sending financial data or SMS statements to remote cloud networks.",
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        color = FinanceBlack.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(48.dp))
                    }
                }
            }

            "voice" -> {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text("Voice Settings", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FinanceBlack)
                            },
                            navigationIcon = {
                                IosBackButton(onClick = { currentSubPage = "main" })
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                        )
                    },
                    containerColor = FinanceBg
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = FinanceWhite),
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(imageVector = Iconsax.Outline.LanguageSquare, contentDescription = null, tint = FinancePurple, modifier = Modifier.size(18.dp))
                                    Text("Spoken Language Accent", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FinanceBlack)
                                }
                                
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(0.5.dp, FinanceGrey.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(FinanceWhite)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { dropdownExpanded = !dropdownExpanded }
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(FinancePurple)
                                            )
                                            Text(selectedLanguageName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FinanceBlack)
                                        }
                                        Icon(
                                            imageVector = if (dropdownExpanded) Iconsax.Outline.ArrowUp2 else Iconsax.Outline.ArrowDown2,
                                            contentDescription = null,
                                            tint = FinanceBlack,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    AnimatedVisibility(
                                        visible = dropdownExpanded,
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(FinanceBg.copy(alpha = 0.4f))
                                                .padding(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            UserPreferences.SUPPORTED_LANGUAGES.keys.forEach { langName ->
                                                val isSelected = langName == selectedLanguageName
                                                val rowBg by animateColorAsState(
                                                    targetValue = if (isSelected) FinancePurpleLight else Color.Transparent
                                                )
                                                val textColor by animateColorAsState(
                                                    targetValue = if (isSelected) FinancePurple else FinanceBlack
                                                )
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(rowBg)
                                                        .clickable {
                                                            val localeCode = UserPreferences.SUPPORTED_LANGUAGES[langName] ?: "en_IN"
                                                            viewModel.setSelectedLanguage(localeCode)
                                                            dropdownExpanded = false
                                                        }
                                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(16.dp)
                                                            .border(
                                                                width = if (isSelected) 5.dp else 1.dp,
                                                                color = if (isSelected) FinancePurple else FinanceGrey.copy(alpha = 0.4f),
                                                                shape = CircleShape
                                                            )
                                                            .background(Color.Transparent)
                                                    )
                                                    Text(
                                                        text = langName,
                                                        fontSize = 13.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = textColor
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        viewModel.simulatePayment(125.00, "Suresh Patel", "PhonePe")
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = FinancePurpleLight,
                                        contentColor = FinancePurple
                                    )
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(imageVector = Iconsax.Outline.VolumeHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text("Test Spoken Accent (Simulate ₹125)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = FinanceWhite),
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                // Phrasing Style selection
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(imageVector = Iconsax.Outline.Microphone2, contentDescription = null, tint = FinancePurple, modifier = Modifier.size(18.dp))
                                    Text("Announcement Phrasing Style", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FinanceBlack)
                                }

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Detailed
                                    val isDetailed = announcementStyle == "detailed"
                                    val detailedBgColor by animateColorAsState(
                                        targetValue = if (isDetailed) FinancePurpleLight else FinanceBg
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(detailedBgColor)
                                            .border(
                                                width = if (isDetailed) 1.dp else 0.5.dp,
                                                color = if (isDetailed) FinancePurple else FinanceGrey.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .clickable { viewModel.setAnnouncementStyle("detailed") }
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(if (isDetailed) FinancePurple else FinanceGrey.copy(alpha = 0.1f))
                                                .border(1.dp, if (isDetailed) FinancePurple else FinanceGrey.copy(alpha = 0.3f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isDetailed) {
                                                Icon(
                                                    imageVector = Iconsax.Outline.TickCircle,
                                                    contentDescription = null,
                                                    tint = FinanceWhite,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text("Detailed Announcement", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FinanceBlack)
                                            Text("Full text: 'Payment of ₹250 received from Suresh'", fontSize = 11.sp, color = FinanceGrey)
                                        }
                                    }

                                    // Short
                                    val isShort = announcementStyle == "short"
                                    val shortBgColor by animateColorAsState(
                                        targetValue = if (isShort) FinancePurpleLight else FinanceBg
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(shortBgColor)
                                            .border(
                                                width = if (isShort) 1.dp else 0.5.dp,
                                                color = if (isShort) FinancePurple else FinanceGrey.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .clickable { viewModel.setAnnouncementStyle("short") }
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(if (isShort) FinancePurple else FinanceGrey.copy(alpha = 0.1f))
                                                .border(1.dp, if (isShort) FinancePurple else FinanceGrey.copy(alpha = 0.3f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isShort) {
                                                Icon(
                                                    imageVector = Iconsax.Outline.TickCircle,
                                                    contentDescription = null,
                                                    tint = FinanceWhite,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text("Short Announcement", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FinanceBlack)
                                            Text("Summary: '₹250 received'", fontSize = 11.sp, color = FinanceGrey)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "stability" -> {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text("Stability & Whitelists", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FinanceBlack)
                            },
                            navigationIcon = {
                                IosBackButton(onClick = { currentSubPage = "main" })
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                        )
                    },
                    containerColor = FinanceBg
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = FinanceWhite),
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Android battery restrictions might pause local sniffer listener background services. To ensure continuous announced receipt alerts, configure battery whitelisting options.",
                                    fontSize = 12.sp,
                                    color = FinanceGrey,
                                    lineHeight = 16.sp
                                )

                                Button(
                                    onClick = { PermissionHelper.openBatteryOptimizationSettings(context) },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = FinanceBlack)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = Iconsax.Outline.Setting4, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Text("Bypass Battery Restrictions", fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "diagnostics" -> {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text("System Diagnostics", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FinanceBlack)
                            },
                            navigationIcon = {
                                IosBackButton(onClick = { currentSubPage = "main" })
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                        )
                    },
                    containerColor = FinanceBg
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = FinanceWhite),
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    "COMPILER SYSTEM STATUSES",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FinanceGrey,
                                    letterSpacing = 0.5.sp
                                )

                                DiagnosticTableRow(item = "Android Sound Engine", status = "ONLINE", isValid = true)
                                DiagnosticTableRow(item = "Active Spoken Accent", status = selectedLanguageName, isValid = true)
                                DiagnosticTableRow(item = "Transaction SNIFFER Node", status = if (isServiceEnabled) "RUNNING" else "PAUSED", isValid = isServiceEnabled)
                                DiagnosticTableRow(item = "Offline Database Instance", status = "ROOM SQLITE", isValid = true)
                                DiagnosticTableRow(item = "Vocal Volume Booster", status = if (volumeBoost) "BOOST ON" else "PASSIVE", isValid = volumeBoost)
                            }
                        }
                    }
                }
            }

            "tutorial" -> {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text("Guided Setup", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FinanceBlack)
                            },
                            navigationIcon = {
                                IosBackButton(onClick = { currentSubPage = "main" })
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                        )
                    },
                    containerColor = FinanceBg
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = FinanceWhite),
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Reset Onboarding walkthrough or re-review guided tutorials.",
                                    fontSize = 12.sp,
                                    color = FinanceGrey,
                                    lineHeight = 16.sp
                                )

                                Button(
                                    onClick = {
                                        viewModel.setOnboardingCompleted(false)
                                        Toast.makeText(context, "Guided setup reset! App will relaunch onboarding.", Toast.LENGTH_LONG).show()
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = FinancePurpleLight, contentColor = FinancePurple)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = Iconsax.Outline.Refresh2, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text("Reset Onboarding Flow", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "theme" -> {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text("Theme & Appearance", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FinanceBlack)
                            },
                            navigationIcon = {
                                IosBackButton(onClick = { currentSubPage = "main" })
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                        )
                    },
                    containerColor = FinanceBg
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = FinanceWhite),
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Choose app theme for the announcer and UI.",
                                    fontSize = 12.sp,
                                    color = FinanceGrey,
                                    lineHeight = 16.sp
                                )

                                listOf("system", "light", "dark").forEach { themeOption ->
                                    val displayName = themeOption.replaceFirstChar { it.uppercaseChar() }
                                    val isSelected = appTheme == themeOption
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isSelected) FinancePurpleLight else FinanceBg)
                                            .border(
                                                width = if (isSelected) 1.dp else 0.5.dp,
                                                color = if (isSelected) FinancePurple else FinanceGrey.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .clickable { viewModel.setAppTheme(themeOption) }
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(displayName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FinanceBlack)
                                            Text(
                                                text = when (themeOption) {
                                                    "light" -> "Bright app styling for daytime use."
                                                    "dark" -> "Dark mode for low-light environments."
                                                    else -> "Use device setting or automatic theme."
                                                },
                                                fontSize = 11.sp,
                                                color = FinanceGrey
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(imageVector = Iconsax.Outline.TickCircle, contentDescription = null, tint = FinancePurple, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "support" -> {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text("Support & Help", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FinanceBlack)
                            },
                            navigationIcon = {
                                IosBackButton(onClick = { currentSubPage = "main" })
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                        )
                    },
                    containerColor = FinanceBg
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = FinanceWhite),
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "If you need help with voice alerts, notification access, or missed announcements, open support email.",
                                    fontSize = 12.sp,
                                    color = FinanceGrey,
                                    lineHeight = 16.sp
                                )

                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("mailto:support@zovio.app")
                                            putExtra(Intent.EXTRA_SUBJECT, "Zovio Support")
                                        }
                                        if (intent.resolveActivity(context.packageManager) != null) {
                                            context.startActivity(intent)
                                        } else {
                                            Toast.makeText(context, "No email app available.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = FinancePurple, contentColor = Color.White)
                                ) {
                                    Icon(imageVector = Iconsax.Outline.Message, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Text("Contact Support", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            "terms" -> {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text("Terms & Conditions", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FinanceBlack)
                            },
                            navigationIcon = {
                                IosBackButton(onClick = { currentSubPage = "main" })
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                        )
                    },
                    containerColor = FinanceBg
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = FinanceWhite),
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text(
                                    text = "Zovio is designed to announce UPI payment alerts locally on your device. By using this app, you agree to allow permission access required for notifications, battery optimization, and SMS fallback.",
                                    fontSize = 13.sp,
                                    color = FinanceBlack,
                                    lineHeight = 18.sp
                                )
                                Text(
                                    text = "• Notification access is used only to detect incoming payment alerts in the background.",
                                    fontSize = 12.sp,
                                    color = FinanceGrey,
                                    lineHeight = 18.sp
                                )
                                Text(
                                    text = "• Battery optimization exemption helps Zovio continue announcing while your phone is idle.",
                                    fontSize = 12.sp,
                                    color = FinanceGrey,
                                    lineHeight = 18.sp
                                )
                                Text(
                                    text = "• SMS receive permission is used as a backup channel for payment alerts when notifications are unavailable.",
                                    fontSize = 12.sp,
                                    color = FinanceGrey,
                                    lineHeight = 18.sp
                                )
                                Text(
                                    text = "• Zovio does not send your payment data to external servers; all announcements are processed locally.",
                                    fontSize = 12.sp,
                                    color = FinanceGrey,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Sleek, high-end list directory row Composable
@Composable
fun SettingsListRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(FinancePurpleLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = FinancePurple,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = FinanceBlack
                )
                Text(
                    text = subText,
                    fontSize = 11.sp,
                    color = FinanceGrey
                )
            }
        }
        Icon(
            imageVector = Iconsax.Outline.ArrowRight,
            contentDescription = "Navigate",
            tint = FinanceGrey.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
        )
    }
}

// Clean, minimalistic, sleek iOS Back Button
@Composable
fun IosBackButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Iconsax.Outline.ArrowLeft2,
            contentDescription = "Back",
            tint = FinancePurple,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "Settings",
            color = FinancePurple,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DiagnosticTableRow(item: String, status: String, isValid: Boolean) {
    val statusColor = if (isValid) FinanceGreen else FinanceYellow
    val statusBg = if (isValid) FinanceGreenLight else FinanceYellowLight

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FinanceBg)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Text(
                text = item,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = FinanceBlack
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(statusBg)
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = status,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor,
                letterSpacing = 0.3.sp
            )
        }
    }
}
