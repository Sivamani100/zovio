package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import io.github.rabehx.iconsax.Iconsax
import io.github.rabehx.iconsax.outline.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import com.example.utils.PermissionHelper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Toast
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.draw.alpha
import kotlin.math.roundToInt
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onScanRequest: () -> Unit = {},
    onNavigateToTab: (Int) -> Unit = {},
    onBottomBarVisibility: (Boolean) -> Unit = {},
    initialSubView: String = "main",
    onSubViewChange: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var isPermissionGranted by remember { mutableStateOf(PermissionHelper.isNotificationAccessGranted(context)) }

    var isSmsPermissionGranted by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECEIVE_SMS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val smsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            isSmsPermissionGranted = permissions[android.Manifest.permission.RECEIVE_SMS] == true
        }
    )

    val todaySum by viewModel.todayTotal.collectAsState()
    val isServiceEnabled by viewModel.isServiceEnabled.collectAsState()
    val allPayments by viewModel.allPayments.collectAsState()
    val userNameState by viewModel.userName.collectAsState()

    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val todayPayments = remember(allPayments) {
        allPayments.filter { it.timestamp >= todayStart }
    }
    val paymentGraphTotal = todaySum

    // Subpage routing state synced with parent
    var currentSubView by remember { mutableStateOf(initialSubView) }
    LaunchedEffect(initialSubView) {
        currentSubView = initialSubView
    }

    val handleSubViewChange: (String) -> Unit = { newView ->
        currentSubView = newView
        onSubViewChange(newView)
    }

    var drawerOpen by remember { mutableStateOf(false) }

    // Notify parent about bottom bar visibility
    LaunchedEffect(currentSubView, drawerOpen) {
        onBottomBarVisibility(currentSubView == "main" && !drawerOpen)
    }

    // Persistent card color index (using remember to keep state live)
    var activeCardColorIndex by remember { mutableStateOf(0) }
    val dragOffsetY = remember { androidx.compose.animation.core.Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    // Dynamic permission checker on UI reactivation
    DisposableEffect(Unit) {
        isPermissionGranted = PermissionHelper.isNotificationAccessGranted(context)
        onDispose {}
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentSubView,
            transitionSpec = {
                if (targetState != "main") {
                    (slideInHorizontally { width -> width } + fadeIn(animationSpec = tween(300))) togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut(animationSpec = tween(300))
                } else {
                    (slideInHorizontally { width -> -width } + fadeIn(animationSpec = tween(300))) togetherWith
                            slideOutHorizontally { width -> width } + fadeOut(animationSpec = tween(300))
                }
            },
            modifier = Modifier.fillMaxSize().background(FinanceBg),
            label = "HomeSubViewTransitions"
        ) { subView ->
            when (subView) {
                "qr_manager" -> {
                    QrManagerScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize(),
                        onNavigateToTab = onNavigateToTab,
                        onBackClick = { handleSubViewChange("main") }
                    )
                }
                "main" -> {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {},
                                navigationIcon = {
                                    IconButton(onClick = { drawerOpen = true }) {
                                        // Styled Hamburger menu icon
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(start = 8.dp)
                                        ) {
                                            Box(modifier = Modifier.size(width = 18.dp, height = 2.dp).background(FinanceBlack))
                                            Box(modifier = Modifier.size(width = 24.dp, height = 2.dp).background(FinanceBlack))
                                            Box(modifier = Modifier.size(width = 14.dp, height = 2.dp).background(FinanceBlack))
                                        }
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color.Transparent
                                ),
                                actions = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.padding(end = 16.dp)
                                    ) {
                                        IconButton(
                                            onClick = { handleSubViewChange("notifications") },
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Icon(
                                                imageVector = Iconsax.Outline.Notification,
                                                contentDescription = "Notifications",
                                                tint = FinanceBlack,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        // (Profile avatar removed per design request)
                                    }
                                }
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
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // 1. Elegant Greeting
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(
                                    text = "Hello $userNameState!",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FinanceBlack,
                                    letterSpacing = (-0.5).sp
                                )
                                Text(
                                    text = "Let's save your money.",
                                    fontSize = 13.sp,
                                    color = FinanceGrey
                                )
                            }

                            // 2. Overlapping credit-cards balance view with Drag gesture
                            val dragAmountY = dragOffsetY.value
                            val currentCardColor = CardColorsList[activeCardColorIndex]
                            val nextCardColor = CardColorsList[(activeCardColorIndex + 1) % CardColorsList.size]

                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(230.dp),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    // Back Card (Yellow/Orange Mastercard design)
                                    val backScale = remember(dragAmountY) {
                                        (0.92f + (dragAmountY / 500f) * 0.08f).coerceAtMost(1f)
                                    }
                                    val backAlpha = remember(dragAmountY) {
                                        (0.7f + (dragAmountY / 500f) * 0.3f).coerceAtMost(1f)
                                    }

                                    Card(
                                        shape = RoundedCornerShape(24.dp),
                                        colors = CardDefaults.cardColors(containerColor = FinanceYellow),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(170.dp)
                                            .padding(horizontal = 12.dp)
                                            .scale(backScale)
                                            .alpha(backAlpha)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
                                            // Mastercard logo (overlapping circles)
                                            Row(
                                                modifier = Modifier.align(Alignment.TopStart),
                                                horizontalArrangement = Arrangement.spacedBy((-8).dp)
                                            ) {
                                                Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.6f)))
                                                Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.4f)))
                                            }
                                            Text(
                                                text = "•••• •••• 2585",
                                                color = FinanceBlack,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                modifier = Modifier.align(Alignment.TopEnd)
                                            )
                                        }
                                    }

                                    // Front Card (Purple Apple Pay design)
                                    Card(
                                        shape = RoundedCornerShape(24.dp),
                                        colors = CardDefaults.cardColors(containerColor = currentCardColor),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .offset { IntOffset(0, (18.dp.toPx() + dragAmountY).roundToInt()) }
                                            .shadow(8.dp, RoundedCornerShape(24.dp))
                                            .pointerInput(Unit) {
                                                detectVerticalDragGestures(
                                                    onDragEnd = {
                                                        coroutineScope.launch {
                                                            if (dragOffsetY.value > 220f) {
                                                                activeCardColorIndex = (activeCardColorIndex + 1) % CardColorsList.size
                                                                Toast.makeText(context, "Card Accent Swapped!", Toast.LENGTH_SHORT).show()
                                                            }
                                                            dragOffsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
                                                        }
                                                    },
                                                    onDragCancel = {
                                                        coroutineScope.launch {
                                                            dragOffsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
                                                        }
                                                    },
                                                    onVerticalDrag = { _, dragAmount ->
                                                        coroutineScope.launch {
                                                            dragOffsetY.snapTo((dragOffsetY.value + dragAmount).coerceIn(0f, 400f))
                                                        }
                                                    }
                                                )
                                            }
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                                            Column(
                                                modifier = Modifier.fillMaxSize(),
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                // Top Row: App name & Live status
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color.White))
                                                        Text(
                                                            text = "UPI Announcer",
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp
                                                        )
                                                    }

                                                    Text(
                                                        text = "Live",
                                                        color = FinanceRed,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    )
                                                }

                                                // Middle: Balance info
                                                Column {
                                                    Text(
                                                        text = "Balance",
                                                        color = Color.White.copy(alpha = 0.6f),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    val displayBalanceVal = if (todaySum == 0.0) 0.0 else todaySum
                                                    Text(
                                                        text = "₹%,.2f".format(displayBalanceVal),
                                                        color = Color.White,
                                                        fontSize = 40.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        letterSpacing = (-0.5).sp
                                                    )
                                                }

                                                // Bottom Row: User name & Sound On button
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Spacer(modifier = Modifier.weight(1f))

                                                    Box(
                                                        modifier = Modifier
                                                            .height(40.dp)
                                                            .clip(RoundedCornerShape(20.dp))
                                                            .background(FinanceBlack)
                                                            .clickable {
                                                                val speechText = if (todaySum == 0.0) {
                                                                    "You have received zero rupees today."
                                                                } else {
                                                                    "Today amount received is ₹%.2f".format(todaySum)
                                                                }
                                                                viewModel.speakText(speechText)
                                                            }
                                                            .padding(horizontal = 18.dp, vertical = 6.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Iconsax.Outline.VolumeHigh,
                                                                contentDescription = "Sound On - Read today amount",
                                                                tint = Color.White,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                            Text(
                                                                text = "Sound On",
                                                                color = Color.White,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 12.sp
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // 3. Today Hourly Payment Graph Card
                                TodayHourlyBarChartCardCustom(
                                    rawPayments = todayPayments,
                                    todaySum = paymentGraphTotal
                                )

                            }

                            // 5. Recent Payments (Dynamic captured database payments)
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Recent Payments",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FinanceBlack
                                    )
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(FinancePurpleLight)
                                            .clickable { onNavigateToTab(1) } // Navigate to Transactions/History tab
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "View All",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = FinancePurple
                                        )
                                    }
                                }

                                Card(
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = FinanceWhite),
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        if (allPayments.isEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 32.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Iconsax.Outline.Notification,
                                                        contentDescription = null,
                                                        tint = FinanceGrey.copy(alpha = 0.4f),
                                                        modifier = Modifier.size(40.dp)
                                                    )
                                                    Text(
                                                        text = "No recent payments captured yet.",
                                                        fontSize = 12.sp,
                                                        color = FinanceGrey
                                                    )
                                                }
                                            }
                                        } else {
                                            val recents = allPayments.take(4)
                                            recents.forEachIndexed { index, payment ->
                                                val formattedDate = try {
                                                    SimpleDateFormat("hh:mm a • dd MMM, yyyy", Locale.getDefault()).format(Date(payment.timestamp))
                                                } catch (e: Exception) {
                                                    "Time info"
                                                }

                                                val appColor = when (payment.appSource.lowercase()) {
                                                    "phonepe" -> FinancePurple
                                                    "google pay" -> FinanceGreen
                                                    "paytm" -> Color(0xFF60A5FA)
                                                    "bhim" -> FinanceYellow
                                                    else -> Color(0xFFEC4899)
                                                }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    // App colored short code / Circle
                                                    Box(
                                                        modifier = Modifier
                                                            .size(46.dp)
                                                            .clip(CircleShape)
                                                            .background(appColor.copy(alpha = 0.12f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = payment.appSource.take(1).uppercase(),
                                                            fontWeight = FontWeight.Black,
                                                            fontSize = 16.sp,
                                                            color = appColor
                                                        )
                                                    }

                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = payment.senderName ?: "Unknown Sender",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 14.sp,
                                                            color = FinanceBlack
                                                        )
                                                        Text(
                                                            text = "${payment.appSource} • $formattedDate",
                                                            fontSize = 11.sp,
                                                            color = FinanceGrey
                                                        )
                                                    }

                                                    Text(
                                                        text = "+ ₹%.2f".format(payment.amount),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = FinanceGreen
                                                    )
                                                }

                                                if (index < recents.size - 1) {
                                                    HorizontalDivider(color = FinanceBg, thickness = 0.5.dp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Bottom spacer to ensure content isn't hidden behind floating nav
                            Spacer(modifier = Modifier.height(90.dp))
                        }
                    }
                }

                // DEDICATED VIEW: Notification logs
                "notifications" -> {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text("Notification Logs", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FinanceBlack)
                                },
                                navigationIcon = {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { handleSubViewChange("main") }
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Iconsax.Outline.ArrowLeft2,
                                            contentDescription = "Back",
                                            tint = FinancePurple,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Home", color = FinancePurple, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                },
                                actions = {
                                    if (allPayments.isNotEmpty()) {
                                        TextButton(onClick = { viewModel.clearAllPayments() }) {
                                            Text("Clear Logs", color = FinanceRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
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
                            Text(
                                text = "REALTIME RECEIVED TRANSACTIONS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = FinanceGrey,
                                letterSpacing = 0.5.sp
                            )

                            if (allPayments.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(
                                            imageVector = Iconsax.Outline.Notification,
                                            contentDescription = null,
                                            tint = FinanceGrey.copy(alpha = 0.4f),
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Text("No notifications capture events logged.", fontSize = 12.sp, color = FinanceGrey)
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    allPayments.forEach { payment ->
                                        RecentReceiptRow(payment = payment)
                                    }
                                }
                            }
                        }
                    }
                }

                // DEDICATED VIEW: Visuals Bento Customizer
                "visuals" -> {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text("Bento Visual Customizer", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FinanceBlack)
                                },
                                navigationIcon = {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { handleSubViewChange("main") }
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Iconsax.Outline.ArrowLeft2,
                                            contentDescription = "Back",
                                            tint = FinancePurple,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Home", color = FinancePurple, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
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
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("APP SKIN THEME SELECTOR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FinanceGrey)
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(FinanceBg)
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Soft Lavender Pastel", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FinanceBlack)
                                        Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(FinancePurple))
                                    }

                                    Text(
                                        "A gorgeous custom purple tint that minimizes screen strain and looks beautiful in any environment.",
                                        fontSize = 11.sp,
                                        color = FinanceGrey,
                                        lineHeight = 15.sp
                                    )
                                }
                            }

                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = FinanceWhite),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("CARD PREVIEWS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FinanceGrey)

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        CardColorsList.forEachIndexed { idx, color ->
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                                    .border(
                                                        width = if (activeCardColorIndex == idx) 2.dp else 0.dp,
                                                        color = if (activeCardColorIndex == idx) FinanceBlack else Color.Transparent,
                                                        shape = CircleShape
                                                    )
                                                    .clickable { activeCardColorIndex = idx }
                                            )
                                        }
                                    }

                                    Text(
                                        "Select any of the 7 vibrant financial colors above. You can also cycle through them directly from the Home screen dashboard using a vertical swipe drag gesture on the card itself!",
                                        fontSize = 11.sp,
                                        color = FinanceGrey,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // DEDICATED VIEW: Announcer Sandbox Controller
                "sandbox" -> {
                    var testAmt by remember { mutableStateOf("150") }
                    var testSender by remember { mutableStateOf("Rajesh Patel") }
                    var testApp by remember { mutableStateOf("PhonePe") }

                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text("Announcer Sandbox", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FinanceBlack)
                                },
                                navigationIcon = {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { handleSubViewChange("main") }
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Iconsax.Outline.ArrowLeft2,
                                            contentDescription = "Back",
                                            tint = FinancePurple,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Home", color = FinancePurple, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
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
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("SIMULATION PARAMETERS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FinanceGrey)

                                    OutlinedTextField(
                                        value = testAmt,
                                        onValueChange = { testAmt = it },
                                        label = { Text("Diagnostic Amount (₹)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = FinancePurple,
                                            unfocusedBorderColor = FinanceGrey.copy(alpha = 0.4f)
                                        )
                                    )

                                    OutlinedTextField(
                                        value = testSender,
                                        onValueChange = { testSender = it },
                                        label = { Text("Customer Name") },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = FinancePurple,
                                            unfocusedBorderColor = FinanceGrey.copy(alpha = 0.4f)
                                        )
                                    )

                                    Text("UPI APP SELECTION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FinanceGrey)
                                    val apps = listOf("PhonePe", "Google Pay", "Paytm", "BHIM")
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        apps.forEach { app ->
                                            val isAppSelected = testApp == app
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (isAppSelected) FinancePurpleLight else FinanceBg)
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (isAppSelected) FinancePurple else Color.Transparent,
                                                        shape = RoundedCornerShape(10.dp)
                                                    )
                                                    .clickable { testApp = app }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(app, fontSize = 11.sp, color = if (isAppSelected) FinancePurple else FinanceBlack, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    val amt = testAmt.toDoubleOrNull() ?: 10.0
                                    viewModel.simulatePayment(amt, testSender, testApp)
                                    Toast.makeText(context, "Voice simulation triggered!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FinanceBlack)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Iconsax.Outline.VolumeHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Text("Trigger Vocal Announcement", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // DEDICATED VIEW: Soundbox Connection Node
                "soundbox" -> {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text("Virtual Soundbox Node", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FinanceBlack)
                                },
                                navigationIcon = {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { handleSubViewChange("main") }
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Iconsax.Outline.ArrowLeft2,
                                            contentDescription = "Back",
                                            tint = FinancePurple,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Home", color = FinancePurple, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
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
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("CONNECTED WIFI BROADCASTER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FinanceGrey)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("WiFi Speaker Mode", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FinanceBlack)
                                            Text("IP Address: 192.168.1.42", fontSize = 11.sp, color = FinanceGrey)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(FinanceGreenLight)
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text("ONLINE", color = FinanceGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    HorizontalDivider(color = FinanceBg)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Ping Speed Latency", fontSize = 12.sp, color = FinanceGrey)
                                        Text("12ms (EXCELLENT)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FinanceGreen)
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.speakText("Soundbox connection node verified. Signal speed is normal.")
                                            Toast.makeText(context, "Pinging Soundbox device...", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = FinancePurpleLight),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Ping Test Speaker", color = FinancePurple, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // DEDICATED VIEW: Emergency Voice Alarms
                "alarms" -> {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text("Emergency Voice Alarms", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FinanceBlack)
                                },
                                navigationIcon = {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { handleSubViewChange("main") }
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Iconsax.Outline.ArrowLeft2,
                                            contentDescription = "Back",
                                            tint = FinancePurple,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Home", color = FinancePurple, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
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
                            Text(
                                text = "SELECT HIGH PRIORITY MESSAGE ANNOUNCEMENT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = FinanceGrey,
                                letterSpacing = 0.5.sp
                            )

                            val presets = listOf(
                                "Shop closing notice" to "Attention! The shop is closing in five minutes. Please complete your purchases.",
                                "Technical Sniffer notification" to "System Alert: The transaction sniffer is running normally in background mode.",
                                "Battery warning notice" to "Power Alert: Battery saver mode detected. Announcer performance may be delayed.",
                                "Cellular status test alert" to "Link established: Offline cellular soundbox fallback active."
                            )

                            presets.forEach { (label, text) ->
                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = FinanceWhite),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.speakText(text)
                                            Toast.makeText(context, "Announcing preset alert...", Toast.LENGTH_SHORT).show()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FinanceBlack)
                                            Text(text, fontSize = 11.sp, color = FinanceGrey, maxLines = 1)
                                        }
                                        Icon(imageVector = Iconsax.Outline.VolumeHigh, contentDescription = null, tint = FinancePurple, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. SLIDING SIDEBAR NAVIGATION DRAWER OVERLAY
        AnimatedVisibility(
            visible = drawerOpen,
            enter = fadeIn(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(250))
        ) {
            // Clickable backdrop
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable { drawerOpen = false }
            ) {
                // Sliding Drawer Panel
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(300.dp)
                        .align(Alignment.CenterStart)
                        .clip(RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp))
                        .background(FinanceWhite)
                        .clickable(enabled = false) {} // block clickthrough
                        .padding(vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            "Zovio Hub",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            color = FinancePurple,
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            "Connected Speaker & Simulator Node",
                                            fontSize = 11.sp,
                                            color = FinanceGrey
                                        )
                                    }

                                    IconButton(
                                        onClick = { drawerOpen = false },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(FinanceGrey.copy(alpha = 0.12f), shape = CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Iconsax.Outline.CloseCircle,
                                            contentDescription = "Close drawer",
                                            tint = FinanceBlack,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(FinancePurpleLight)
                                        .padding(horizontal = 16.dp, vertical = 14.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(FinancePurple.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Iconsax.Outline.Notification,
                                                contentDescription = null,
                                                tint = FinancePurple,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Column {
                                            Text("Quick navigation", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FinancePurple)
                                            Text("Tap to switch between dashboard modes.", fontSize = 10.sp, color = FinanceGrey)
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = FinanceBg, modifier = Modifier.padding(horizontal = 24.dp))

                            // Features Menu
                            val menuItems = listOf(
                                Triple("qr_manager", "Widget QR Manager", Iconsax.Outline.ScanBarcode),
                                Triple("visuals", "Bento Visual Customizer", Iconsax.Outline.Setting4),
                                Triple("sandbox", "Announcer Sandbox", Iconsax.Outline.VolumeHigh),
                                Triple("soundbox", "Soundbox Connection Node", Iconsax.Outline.Speaker),
                                Triple("alarms", "Voice Alarm Presets", Iconsax.Outline.Notification)
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                menuItems.forEach { (subView, title, icon) ->
                                    val selected = currentSubView == subView
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(if (selected) FinancePurpleLight else Color.Transparent)
                                            .clickable {
                                                handleSubViewChange(subView)
                                                drawerOpen = false
                                            }
                                            .padding(horizontal = 20.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (selected) FinancePurple else FinanceGrey.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = if (selected) Color.White else FinancePurple,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Text(
                                            text = title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (selected) FinancePurple else FinanceBlack
                                        )
                                    }
                                }
                            }
                        }

                        // Drawer Footer
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            HorizontalDivider(color = FinanceBg)
                            Text("Announcer Mode V1.42", fontSize = 10.sp, color = FinanceGrey, fontWeight = FontWeight.Medium)
                            Text("Engineered for offline reliability.", fontSize = 10.sp, color = FinanceGrey)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TodayHourlyBarChartCardCustom(
    rawPayments: List<com.example.data.db.PaymentEntity>,
    todaySum: Double,
    title: String = "Today Received"
) {
    // Show a recent-hour window centered on the current hour (6 bars)
    val now = Calendar.getInstance()
    val currentHour = now.get(Calendar.HOUR_OF_DAY)
    val startHour = (currentHour - 2).coerceAtLeast(0)
    val hoursRange = (startHour..(startHour + 5).coerceAtMost(23)).toList()

    val hoursList = remember(rawPayments) {
        hoursRange.map { hour ->
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val start = cal.timeInMillis
            val end = start + 3600000L
            val sum = rawPayments.filter { it.timestamp in start until end }.sumOf { it.amount }
            val label = when {
                hour == 0 -> "12am"
                hour < 12 -> "${hour}am"
                hour == 12 -> "12pm"
                else -> "${hour - 12}pm"
            }
            Pair(label, sum)
        }
    }

    val maxVal = remember(hoursList) { val m = hoursList.maxOfOrNull { it.second } ?: 0.0; if (m == 0.0) 1.0 else m }
    val activeIndex = remember(hoursList) { val mv = hoursList.maxOf { it.second }; if (mv > 0.0) hoursList.indexOfFirst { it.second == mv } else hoursList.size / 2 }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = FinanceWhite),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = title, fontSize = 11.sp, color = FinanceGrey, fontWeight = FontWeight.Medium)
                    Text(text = "₹%.2f".format(todaySum), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = FinanceBlack)
                }

                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(FinanceBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Iconsax.Outline.Chart1, contentDescription = "Chart", tint = FinanceBlack, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                hoursList.forEachIndexed { idx, (label, sum) ->
                    val isActive = idx == activeIndex
                    val heightRatio = (sum / maxVal).toFloat().coerceIn(0.08f, 1.0f)

                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        if (isActive) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(FinanceYellow).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text(text = "₹%.0f".format(sum), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = FinanceBlack)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        } else {
                            Box(modifier = Modifier.height(18.dp))
                        }

                        Box(modifier = Modifier.width(14.dp).fillMaxHeight(0.6f * heightRatio).clip(CircleShape).background(if (isActive) FinanceBlack else FinanceGrey.copy(alpha = 0.2f)))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = label, fontSize = 10.sp, color = FinanceGrey, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun QuickControlCircle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.width(76.dp)
    ) {
        // Choose an accent color based on the label for a more colorful home screen
        val accent = when (label.lowercase()) {
            "speak" -> FinancePurple
            "scan qr" -> FinanceGreen
            "logs" -> Color(0xFF60A5FA)
            "more" -> FinanceYellow
            else -> FinancePurple
        }

        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(accent)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = FinanceBlack,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun RecentReceiptRow(payment: com.example.data.db.PaymentEntity) {
    val formattedDate = remember(payment.timestamp) {
        try {
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(payment.timestamp))
        } catch (e: Exception) {
            "Time info"
        }
    }

    val appColor = when (payment.appSource.lowercase()) {
        "phonepe" -> FinancePurple
        "google pay" -> FinanceGreen
        "paytm" -> Color(0xFF60A5FA)
        "bhim" -> FinanceYellow
        else -> Color(0xFFEC4899)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = appColor.copy(alpha = 0.06f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // App colored short code
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(appColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = payment.appSource.take(1).uppercase(),
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = appColor
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = payment.senderName ?: "Unknown Sender",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = FinanceBlack
                )
                Text(
                    text = "${payment.appSource} • $formattedDate",
                    fontSize = 11.sp,
                    color = appColor.copy(alpha = 0.9f)
                )
            }

            Text(
                text = "+ ₹%.2f".format(payment.amount),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = appColor
            )
        }
    }
}

@Composable
fun InteractiveStatusItem(
    isPermissionGranted: Boolean,
    isServiceEnabled: Boolean,
    onGrantClick: () -> Unit
) {
    val bgColor = if (!isPermissionGranted) FinanceRedLight else if (isServiceEnabled) FinanceGreenLight else FinanceBg
    val tintColor = if (!isPermissionGranted) FinanceRed else if (isServiceEnabled) FinanceGreen else FinanceGrey
    val titleText = if (!isPermissionGranted) "Action Required: Access Missing" else if (isServiceEnabled) "Voice Alerts Operating" else "Listener Paused"
    val subText = if (!isPermissionGranted) "Tap to grant notification reading permission" else if (isServiceEnabled) "Verbal receipts play immediately on payment detection" else "Announcements are paused. Resume in controls."

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = FinanceWhite),
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (!isPermissionGranted) Iconsax.Outline.Warning2 else if (isServiceEnabled) Iconsax.Outline.TickCircle else Iconsax.Outline.PauseCircle,
                    contentDescription = "Status",
                    tint = tintColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titleText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = FinanceBlack
                )
                Text(
                    text = subText,
                    fontSize = 11.sp,
                    color = FinanceGrey,
                    lineHeight = 14.sp
                )
            }

            if (!isPermissionGranted) {
                IconButton(
                    onClick = onGrantClick,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(FinanceRedLight)
                ) {
                    Icon(
                        imageVector = Iconsax.Outline.ArrowRight,
                        contentDescription = "Grant",
                        tint = FinanceRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CellularBackupSoundboxCard(
    isSmsPermissionGranted: Boolean,
    onRequestPermissions: () -> Unit
) {
    val bgColor = if (isSmsPermissionGranted) FinanceGreenLight else Color(0xFFFFFBEA)
    val tintColor = if (isSmsPermissionGranted) FinanceGreen else Color(0xFFE65100)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = FinanceWhite),
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSmsPermissionGranted) Iconsax.Outline.TickCircle else Iconsax.Outline.Sms,
                    contentDescription = "SMS Status",
                    tint = tintColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isSmsPermissionGranted) "Cellular Backup Soundbox Active" else "Enable Cellular Backup (SMS Mode)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = FinanceBlack
                )
                Text(
                    text = if (isSmsPermissionGranted) {
                        "SMS backup active! Speaks transactions instantly if offline."
                    } else {
                        "Reads official bank payment SMS alerts if internet gets slow/delayed."
                    },
                    fontSize = 11.sp,
                    color = FinanceGrey,
                    lineHeight = 14.sp
                )
            }

            if (!isSmsPermissionGranted) {
                IconButton(
                    onClick = onRequestPermissions,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(FinanceYellowLight)
                ) {
                    Icon(
                        imageVector = Iconsax.Outline.ArrowRight,
                        contentDescription = "Authorize SMS",
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
