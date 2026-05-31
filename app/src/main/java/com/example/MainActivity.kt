package com.example

import android.content.Context
import android.content.ContextWrapper
import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import io.github.rabehx.iconsax.Iconsax
import io.github.rabehx.iconsax.outline.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.ui.MainViewModel
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.QrManagerScreen
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import android.widget.Toast
import android.net.Uri
import android.content.Intent
import com.example.utils.UpiScannerHelper
import com.example.ui.theme.*
import com.example.utils.PermissionHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Ensure status bar icons are dark so they are visible on white backgrounds
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        // Detect if started directly via homescreen widget camera click action launcher
        val startScanner = intent?.getBooleanExtra("START_SCANNER", false) ?: false
        val startFromPin = intent?.getBooleanExtra("START_SCANNER_FROM_PIN", false) ?: false
        var shouldInstantScan = startScanner || startFromPin

        if (shouldInstantScan) {
            intent?.removeExtra("START_SCANNER")
            intent?.removeExtra("START_SCANNER_FROM_PIN")
        }

        setContent {
            MyApplicationTheme(darkTheme = false) {
                var onboardingCompleted by remember { mutableStateOf(false) }
                val uiOnboardingDone by viewModel.onboardingDone.collectAsState()

                // Check local permissions state dynamically
                val context = LocalContext.current
                var hasAccessPermission by remember {
                    mutableStateOf(PermissionHelper.isNotificationAccessGranted(context))
                }

                // Check again on active startup 
                LaunchedEffect(uiOnboardingDone) {
                    onboardingCompleted = uiOnboardingDone
                }

                // Periodic check when the activity resumes
                DisposableEffect(Unit) {
                    hasAccessPermission = PermissionHelper.isNotificationAccessGranted(context)
                    onDispose {}
                }

                // Global sheet state managed at host activity level
                var scannedPayload by remember { mutableStateOf<String?>(null) }
                val triggerScanner: () -> Unit = {
                    UpiScannerHelper.startBarcodeScanner(
                        context = context,
                        onSuccess = { payload ->
                            scannedPayload = payload
                        },
                        onCancelOrFailure = { err ->
                            Toast.makeText(context, "Scanning failed: $err", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // Trigger instant-scan automatically if initiated via widget shortcut extra
                LaunchedEffect(Unit) {
                    if (shouldInstantScan) {
                        shouldInstantScan = false
                        triggerScanner()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Show Onboarding screen only if it has not been completed yet
                    if (!onboardingCompleted) {
                        OnboardingScreen(
                            onOnboardingComplete = { name ->
                                onboardingCompleted = true
                                // Record in preferences persistently
                                viewModel.setUserName(name)
                                viewModel.setOnboardingCompleted(true)
                            }
                        )
                    } else {
                        // Main Tabbed dashboard layout passing downstream scanner triggers
                        MainTabsContainer(
                            viewModel = viewModel,
                            scannedPayload = scannedPayload,
                            onScanRequest = triggerScanner,
                            onDismissScanSheet = { scannedPayload = null }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Simple trigger to force recompositions of permission observers in our Compose trees
        lifecycleScope.launch {
            val isGranted = PermissionHelper.isNotificationAccessGranted(this@MainActivity)
            if (isGranted) {
                // Keep states updated locally
            }
        }
    }
}

fun Context.findActivity(): androidx.activity.ComponentActivity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is androidx.activity.ComponentActivity) {
            return ctx
        }
        ctx = ctx.baseContext
    }
    return null
}

@Composable
fun MainTabsContainer(
    viewModel: MainViewModel,
    scannedPayload: String?,
    onScanRequest: () -> Unit,
    onDismissScanSheet: () -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    var homeSubView by remember { mutableStateOf("main") }

    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    
    // Auto-navigate to QR tab if started from the Homescreen widget action intent
    LaunchedEffect(activity) {
        val intent = activity?.intent
        val navigateToQr = intent?.getBooleanExtra("NAVIGATE_TO_QR", false) ?: false
        if (navigateToQr) {
            selectedTabIndex = 3
            homeSubView = "main"
            intent?.removeExtra("NAVIGATE_TO_QR")
        }
    }

    var showBottomBar by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FinanceBg)
            .statusBarsPadding()
    ) {
        // Content area - fills entire screen
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when (selectedTabIndex) {
                0 -> HomeScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize(),
                    onScanRequest = onScanRequest,
                    onNavigateToTab = { index ->
                        selectedTabIndex = index
                        homeSubView = "main"
                    },
                    onBottomBarVisibility = { showBottomBar = it },
                    initialSubView = homeSubView,
                    onSubViewChange = { homeSubView = it }
                )
                1 -> HistoryScreen(
                    viewModel = viewModel, 
                    modifier = Modifier.fillMaxSize(),
                    onNavigateToTab = { index ->
                        selectedTabIndex = index
                        homeSubView = "main"
                    }
                )
                2 -> AnalyticsScreen(
                    viewModel = viewModel, 
                    modifier = Modifier.fillMaxSize(),
                    onNavigateToTab = { index ->
                        selectedTabIndex = index
                        homeSubView = "main"
                    }
                )
                3 -> QrManagerScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize(),
                    onNavigateToTab = { index ->
                        selectedTabIndex = index
                        homeSubView = "main"
                    }
                )
                4 -> SettingsScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize(),
                    onBottomBarVisibility = { showBottomBar = it }
                )
            }

            // Display UPI payment bottom sheet if a QR scanner payload is present
            if (scannedPayload != null) {
                UpiAppSelectorSheet(
                    rawPayload = scannedPayload,
                    onDismiss = onDismissScanSheet
                )
            }
        }

        // Floating bottom capsule nav bar - overlaid at bottom
        AnimatedVisibility(
            visible = showBottomBar,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(animationSpec = tween(250)),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(animationSpec = tween(250)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = FinanceNavBg,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .height(64.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val tabs = listOf(
                            Triple(0, Iconsax.Outline.Home1, "Home"),
                            Triple(1, Iconsax.Outline.Wallet2, "Transactions"),
                            Triple(2, Iconsax.Outline.Chart1, "Analytics"),
                            Triple(3, Iconsax.Outline.ScanBarcode, "QR"),
                            Triple(4, Iconsax.Outline.Setting2, "Settings")
                        )

                        tabs.forEach { (index, icon, label) ->
                            val isSelected = selectedTabIndex == index
                            if (isSelected) {
                                Row(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .clickable { 
                                            selectedTabIndex = index
                                            homeSubView = "main"
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = FinanceBlack,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = label,
                                        color = FinanceBlack,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = { 
                                        selectedTabIndex = index 
                                        homeSubView = "main"
                                    },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = FinanceGrey.copy(alpha = 0.8f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpiAppSelectorSheet(
    rawPayload: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val parsedData = remember(rawPayload) { UpiScannerHelper.parseUpiUri(rawPayload) }
    
    // Scan all installed UPI apps asynchronously to prevent main-thread freezing
    var appsList by remember { mutableStateOf<List<com.example.utils.UpiAppInfo>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(true) }
    
    LaunchedEffect(context) {
        val list = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            UpiScannerHelper.queryInstalledUpiApps(context)
        }
        appsList = list
        isLoadingApps = false
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = FinanceWhite,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(FinanceGrey.copy(alpha = 0.3f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Elegant Visual Icon
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(FinancePurpleLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Iconsax.Outline.MoneyRecive,
                    contentDescription = "UPI Payment Icon",
                    tint = FinancePurple,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            // Header Content
            if (parsedData != null) {
                Text(
                    text = "Select App to Pay",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = FinanceBlack,
                    textAlign = TextAlign.Center
                )
                
                // Display UPI details
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = FinanceBg),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val amount = parsedData.amount
                        if (!amount.isNullOrBlank()) {
                            Text(
                                "₹ $amount",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = FinancePurple
                            )
                        } else {
                            Text(
                                "Dynamic Merchant Scan",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = FinanceGrey
                            )
                        }
                        
                        val payee = parsedData.payeeName
                        if (!payee.isNullOrBlank()) {
                            Text(
                                payee,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = FinanceBlack
                            )
                        }
                        
                        Text(
                            parsedData.upiId,
                            fontSize = 12.sp,
                            color = FinanceGrey
                        )
                        
                        val note = parsedData.note
                        if (!note.isNullOrBlank()) {
                            Text(
                                "Note: $note",
                                fontSize = 12.sp,
                                color = FinanceGrey
                            )
                        }
                    }
                }
            } else {
                // If it is regular QR payload e.g. plain text or URL link
                Text(
                    text = "Scanned Content",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = FinanceBlack,
                    textAlign = TextAlign.Center
                )
                
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = FinanceBg),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            rawPayload,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = FinanceBlack,
                            textAlign = TextAlign.Center
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Scanned Text", rawPayload))
                                    Toast.makeText(context, "Copied to Clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = FinancePurple, contentColor = Color.White),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(imageVector = Iconsax.Outline.Copy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Copy", fontWeight = FontWeight.Bold)
                                }
                            }
                            if (rawPayload.startsWith("http://") || rawPayload.startsWith("https://")) {
                                Button(
                                    onClick = {
                                        try {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(rawPayload)))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Cannot open: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = FinanceBlack, contentColor = Color.White),
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(imageVector = Iconsax.Outline.Export, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text("Open Link", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Apps list title
            Text(
                text = "INSTALLED UPI PAYMENT OPTIONS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = FinanceGrey,
                modifier = Modifier.align(Alignment.Start)
            )
            
            if (isLoadingApps) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = FinancePurple,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Checking secure payment options...",
                            fontSize = 11.sp,
                            color = FinanceGrey,
                            modifier = Modifier.graphicsLayer(alpha = pulseAlpha)
                        )
                    }
                }
            } else if (appsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No installed UPI apps found (e.g. Google Pay, PhonePe, Paytm). Please configure standard apps first.",
                        fontSize = 12.sp,
                        color = FinanceGrey,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                // Display UPI apps in a beautiful list with their icons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    appsList.forEach { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(FinanceBg)
                                .animateContentSize()
                                .clickable {
                                    UpiScannerHelper.launchUpiPayment(context, rawPayload, app)
                                    onDismiss()
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                bitmap = app.icon.asImageBitmap(),
                                contentDescription = app.name,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = FinanceBlack
                                )
                                Text(
                                    text = app.packageName,
                                    fontSize = 10.sp,
                                    color = FinanceGrey
                                )
                            }
                            
                            Icon(
                                imageVector = Iconsax.Outline.ArrowRight,
                                contentDescription = "Launch Payment",
                                tint = FinanceGrey.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
