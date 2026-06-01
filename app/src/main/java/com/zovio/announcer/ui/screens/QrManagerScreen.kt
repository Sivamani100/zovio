package com.zovio.announcer.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.barcode.BarcodeScanning
import java.util.concurrent.TimeUnit
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import io.github.rabehx.iconsax.Iconsax
import io.github.rabehx.iconsax.outline.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zovio.announcer.ui.MainViewModel
import com.zovio.announcer.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrManagerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onNavigateToTab: (Int) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val activeQrType by viewModel.activeQrType.collectAsState()

    val providers = listOf(
        "PhonePe",
        "Paytm",
        "Google Pay",
        "BHIM UPI",
        "WhatsApp Pay",
        "CRED"
    )

    var refreshTrigger by remember { mutableStateOf(0) }
    var selectingProvider by remember { mutableStateOf<String?>(null) }
    var selectedProvider by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val provider = selectingProvider
        if (uri != null && provider != null) {
            val success = saveQrImage(context, uri, provider)
            if (success) {
                Toast.makeText(context, "$provider QR Code uploaded successfully!", Toast.LENGTH_SHORT).show()
                if (activeQrType == "none") {
                    viewModel.setActiveQrType(provider)
                } else {
                    com.zovio.announcer.service.QrWidgetProvider.triggerWidgetUpdate(context, activeQrType)
                }
                refreshTrigger++
            } else {
                Toast.makeText(context, "Failed to save QR code image.", Toast.LENGTH_SHORT).show()
            }
        }
        selectingProvider = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Widget QR Manager",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = FinanceBlack,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onBackClick() }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Iconsax.Outline.ArrowLeft2,
                            contentDescription = "Back",
                            tint = FinancePurple,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
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
        containerColor = FinanceBg,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(FinanceBg)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Homescreen quick card removed per design request

            // 2. Widget Preview Simulator
            Text(
                "WIDGET SIMULATOR PREVIEW",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = FinanceGrey,
                modifier = Modifier.padding(start = 4.dp)
            )

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val previewProvider = selectedProvider ?: if (activeQrType != "none") activeQrType else null
                val previewBitmap = remember(previewProvider, refreshTrigger) {
                    previewProvider?.let { loadQrBitmap(context, it) }
                }

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = FinanceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .size(220.dp)
                        .border(
                            width = if (previewBitmap != null) 2.dp else 1.dp,
                            color = if (previewBitmap != null) FinancePurple.copy(alpha = 0.6f) else FinanceGrey.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (previewBitmap != null) {
                            Image(
                                bitmap = previewBitmap.asImageBitmap(),
                                contentDescription = "Active QR Code",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (previewProvider != null) {
                                    Text(
                                        "QR not uploaded for $previewProvider",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = FinanceGrey,
                                        textAlign = TextAlign.Center
                                    )
                                } else {
                                    Icon(
                                        imageVector = Iconsax.Outline.ScanBarcode,
                                        contentDescription = null,
                                        tint = FinanceGrey.copy(alpha = 0.4f),
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Text(
                                        "No Active QR Selected",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = FinanceGrey,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (selectedProvider != null) {
                Text(
                    "Showing preview for $selectedProvider",
                    fontSize = 12.sp,
                    color = FinanceGrey,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }
            
            // Pin Widgets
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = FinanceWhite),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "ADD WIDGET SHORTCUTS TO HOMESCREEN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = FinancePurple,
                        letterSpacing = 0.5.sp
                    )

                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                try {
                                    val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
                                    if (appWidgetManager != null && appWidgetManager.isRequestPinAppWidgetSupported) {
                                        val myProvider = ComponentName(context, "com.zovio.announcer.service.QrWidgetProvider")
                                        val successIntent = Intent(context, com.zovio.announcer.MainActivity::class.java).apply {
                                            putExtra("NAVIGATE_TO_QR", true)
                                        }
                                        val successPendingIntent = PendingIntent.getActivity(
                                            context,
                                            1001,
                                            successIntent,
                                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                        )
                                        appWidgetManager.requestPinAppWidget(myProvider, null, successPendingIntent)
                                        Toast.makeText(context, "Prompting system to pin QR display widget...", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Widget pinning is not supported by your launcher.", Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, "Widget pinning is supported on Android 8.0+ devices.", Toast.LENGTH_LONG).show()
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FinancePurple),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Iconsax.Outline.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("PIN ACTIVE QR WIDGET", fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                try {
                                    val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
                                    if (appWidgetManager != null && appWidgetManager.isRequestPinAppWidgetSupported) {
                                        val scannerProvider = ComponentName(context, "com.zovio.announcer.service.QrScannerWidgetProvider")
                                        val successIntent = Intent(context, com.zovio.announcer.MainActivity::class.java).apply {
                                            putExtra("START_SCANNER_FROM_PIN", true)
                                        }
                                        val successPendingIntent = PendingIntent.getActivity(
                                            context,
                                            1002,
                                            successIntent,
                                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                        )
                                        appWidgetManager.requestPinAppWidget(scannerProvider, null, successPendingIntent)
                                        Toast.makeText(context, "Prompting system to pin Instant Scanner widget...", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Widget pinning is not supported by your launcher.", Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, "Widget pinning is supported on Android 8.0+ devices.", Toast.LENGTH_LONG).show()
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FinanceBlack),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Iconsax.Outline.ScanBarcode, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("PIN INSTANT SCANNER WIDGET", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Uploads Manager List
            Text(
                "MANAGE ALL UPLOADS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = FinanceGrey,
                modifier = Modifier.padding(start = 4.dp)
            )

            val badgePulseTransition = rememberInfiniteTransition(label = "badgePulse")
            val badgeScale by badgePulseTransition.animateFloat(
                initialValue = 0.95f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "badgeScale"
            )

            Text(
                "TAP AN APP ICON TO UPLOAD",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = FinanceGrey,
                modifier = Modifier.padding(start = 4.dp)
            )

            providers.chunked(5).forEach { providerRow ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    providerRow.forEach { provider ->
                        val isUploaded = remember(provider, refreshTrigger) {
                            isQrImageUploaded(context, provider)
                        }
                        ProviderIconButton(
                            provider = provider,
                            isUploaded = isUploaded,
                            isActive = activeQrType == provider,
                            onClick = {
                                if (isUploaded) {
                                    selectedProvider = provider
                                } else {
                                    selectingProvider = provider
                                    photoPickerLauncher.launch("image/*")
                                }
                            }
                        )
                    }
                    if (providerRow.size < 5) {
                        repeat(5 - providerRow.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            if (selectedProvider != null) {
                val selectedUploaded = isQrImageUploaded(context, selectedProvider!!)
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = FinanceWhite),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Upload ${selectedProvider} QR",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = FinanceBlack
                            )
                            IconButton(
                                onClick = { selectedProvider = null },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Iconsax.Outline.CloseSquare,
                                    contentDescription = "Close upload subpage",
                                    tint = FinanceGrey
                                )
                            }
                        }

                        Text(
                            if (selectedUploaded) "Manage the uploaded ${selectedProvider} QR or replace it with a new file."
                            else "Tap the button below to upload a ${selectedProvider} QR image and activate it for the widget.",
                            fontSize = 12.sp,
                            color = FinanceGrey,
                            lineHeight = 18.sp
                        )

                        if (!selectedUploaded) {
                            Button(
                                onClick = {
                                    selectingProvider = selectedProvider
                                    photoPickerLauncher.launch("image/*")
                                },
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FinancePurple),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("UPLOAD ${selectedProvider?.uppercase()}", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        selectingProvider = selectedProvider
                                        photoPickerLauncher.launch("image/*")
                                    },
                                    shape = RoundedCornerShape(24.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = FinancePurple),
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Text("REPLACE QR", fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val done = deleteQrImage(context, selectedProvider!!)
                                        if (done) {
                                            Toast.makeText(context, "Deleted ${selectedProvider} QR Code.", Toast.LENGTH_SHORT).show()
                                            if (activeQrType == selectedProvider) {
                                                viewModel.setActiveQrType("none")
                                            } else {
                                                com.zovio.announcer.service.QrWidgetProvider.triggerWidgetUpdate(context, activeQrType)
                                            }
                                            refreshTrigger++
                                        }
                                    },
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Text("DELETE", fontWeight = FontWeight.Bold, color = FinanceGrey)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

// Cropping and saving helper utilities maintained identically to original logic
private fun cropQrFromBitmap(context: Context, original: Bitmap): Bitmap {
    try {
        val image = InputImage.fromBitmap(original, 0)
        val scanner = BarcodeScanning.getClient()
        val task = scanner.process(image)
        val barcodes = Tasks.await(task, 4, TimeUnit.SECONDS)
        
        if (!barcodes.isNullOrEmpty()) {
            val qrBarcode = barcodes.maxByOrNull { it.boundingBox?.width() ?: 0 }
            val rect = qrBarcode?.boundingBox
            if (rect != null) {
                val margin = (rect.width() * 0.12f).toInt().coerceAtLeast(10)
                var left = rect.left - margin
                var top = rect.top - margin
                var right = rect.right + margin
                var bottom = rect.bottom + margin
                left = left.coerceIn(0, original.width)
                top = top.coerceIn(0, original.height)
                right = right.coerceIn(left + 10, original.width)
                bottom = bottom.coerceIn(top + 10, original.height)
                return Bitmap.createBitmap(original, left, top, right - left, bottom - top)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    
    return try {
        val size = minOf(original.width, original.height)
        val x = (original.width - size) / 2
        val y = (original.height - size) / 2
        Bitmap.createBitmap(original, x, y, size, size)
    } catch (e: Exception) {
        original
    }
}

private fun saveQrImage(context: Context, uri: Uri, provider: String): Boolean {
    val qrsDir = File(context.filesDir, "qrs")
    if (!qrsDir.exists()) {
        qrsDir.mkdirs()
    }
    val cleanKey = provider.lowercase().replace(" ", "_")
    val outputFile = File(qrsDir, "qr_$cleanKey.png")

    return try {
        val originalBitmap = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        }
        
        if (originalBitmap != null) {
            val croppedBitmap = cropQrFromBitmap(context, originalBitmap)
            outputFile.outputStream().use { outputStream ->
                croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            true
        } else {
            false
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

private fun deleteQrImage(context: Context, provider: String): Boolean {
    val qrsDir = File(context.filesDir, "qrs")
    val cleanKey = provider.lowercase().replace(" ", "_")
    val file = File(qrsDir, "qr_$cleanKey.png")
    return if (file.exists()) {
        file.delete()
    } else {
        false
    }
}

private fun isQrImageUploaded(context: Context, provider: String): Boolean {
    val qrsDir = File(context.filesDir, "qrs")
    val cleanKey = provider.lowercase().replace(" ", "_")
    val file = File(qrsDir, "qr_$cleanKey.png")
    return file.exists() && file.length() > 0
}

@Composable
fun RowScope.ProviderIconButton(
    provider: String,
    isUploaded: Boolean,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val resourceId = when (provider.lowercase()) {
        "phonepe" -> com.zovio.announcer.R.drawable.phonepe
        "paytm", "navi" -> com.zovio.announcer.R.drawable.paytm
        "google pay", "gpay" -> com.zovio.announcer.R.drawable.gpay
        "bhim upi", "bhim" -> com.zovio.announcer.R.drawable.bhim
        "whatsapp pay", "whatsapppay" -> com.zovio.announcer.R.drawable.whatsapppay
        "cred" -> com.zovio.announcer.R.drawable.cred
        else -> com.zovio.announcer.R.drawable.gpay
    }

    Box(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(FinanceWhite)
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) FinancePurple else FinanceGrey.copy(alpha = 0.18f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = resourceId),
            contentDescription = "$provider logo",
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp)),
            contentScale = ContentScale.Crop
        )
    }
}

private fun loadQrBitmap(context: Context, provider: String): Bitmap? {
    val qrsDir = File(context.filesDir, "qrs")
    val cleanKey = provider.lowercase().replace(" ", "_")
    val file = File(qrsDir, "qr_$cleanKey.png")
    return if (file.exists() && file.length() > 0) {
        try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            null
        }
    } else {
        null
    }
}
