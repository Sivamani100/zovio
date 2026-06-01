package com.zovio.announcer.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.zovio.announcer.ui.theme.*
import com.zovio.announcer.utils.PermissionHelper
import com.zovio.announcer.utils.TtsManager
import io.github.rabehx.iconsax.Iconsax
import io.github.rabehx.iconsax.outline.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    onOnboardingComplete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val ttsManager = remember { TtsManager(context) }
    DisposableEffect(Unit) {
        ttsManager.init()
        onDispose {
            ttsManager.shutdown()
        }
    }

    var isNotificationAccessGranted by remember { mutableStateOf(PermissionHelper.isNotificationAccessGranted(context)) }
    var currentStep by remember { mutableStateOf(0) }
    val totalSteps = 5 // Intro, coverage, secure alerts, notification, name input

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isNotificationAccessGranted = PermissionHelper.isNotificationAccessGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var userNameInput by remember { mutableStateOf("") }
    val isNameValid by remember(userNameInput) { mutableStateOf(userNameInput.trim().isNotEmpty()) }

    // Dynamic animated background colors based on step
    val slideColors = listOf(
        Color(0xFFFFE043), // Slide 0: Vibrant Yellow
        Color(0xFFE1D5F5), // Slide 1: Soft Lavender
        Color(0xFFFF7543), // Slide 2: Vibrant Coral
        Color(0xFF8ED4E6), // Slide 3: Soft Cyan for secure alerts
        Color(0xFFC0EBF1)  // Slide 4: Soft Teal (Name Screen)
    )
    val animatedBgColor by animateColorAsState(
        targetValue = slideColors.getOrElse(currentStep) { Color(0xFFFFE043) },
        animationSpec = tween(durationMillis = 500)
    )

    Scaffold(
        containerColor = animatedBgColor,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            
            // Top Nav / Skip Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 0) {
                    IconButton(
                        onClick = { currentStep-- },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            imageVector = Iconsax.Outline.ArrowLeft2,
                            contentDescription = "Back",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(36.dp))
                }

                // Can skip to the final name entry screen
                if (currentStep < totalSteps - 1) {
                    TextButton(
                        onClick = { currentStep = totalSteps - 1 },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Black)
                    ) {
                        Text("Skip", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                } else {
                    Spacer(modifier = Modifier.size(36.dp))
                }
            }

            // Animated slide content (Header, subtitle, and illustration)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width } + fadeIn() togetherWith
                                    slideOutHorizontally { width -> -width } + fadeOut())
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                    slideOutHorizontally { width -> width } + fadeOut())
                        }
                    },
                    label = "OnboardingSlideChange"
                ) { step ->
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.Start
                    ) {
                        // Title area
                        when (step) {
                            0 -> IntroduceHubTitle(onSoundClick = {
                                ttsManager.speak("You are going to receive the payment sound through this app.")
                            })
                            1 -> AppCoveragePreviewTitle()
                            2 -> SecureAlertsTitle()
                            3 -> EnableAccessTitle()
                            4 -> AskNameTitle()
                        }

                        // Illustration / image content
                        Box(
                            modifier = Modifier
                                .weight(1.15f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            when (step) {
                                0 -> QuirkyLockIllustration()
                                1 -> AppCoverageIllustration()
                                2 -> QuirkyLockWithoutKeyIllustration()
                                3 -> QuirkySecurityIllustration(isGranted = isNotificationAccessGranted, onGrantClick = {
                                    PermissionHelper.openNotificationAccessSettings(context)
                                })
                                4 -> QuirkyNameIllustration(
                                    userNameInput = userNameInput,
                                    onNameChange = { userNameInput = it }
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Navigation Area
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page Indicator Dots
                PageIndicatorDots(currentStep = currentStep, totalSteps = totalSteps)

                // Black capsule button with dynamic labels
                val buttonText = when (currentStep) {
                    0 -> "Get Started"
                    1 -> "Continue"
                    2 -> "Continue"
                    3 -> if (isNotificationAccessGranted) "Continue" else "Grant Notification Access"
                    else -> if (isNameValid) "Launch Hub" else "Enter your name"
                }

                val buttonEnabled = currentStep != totalSteps - 1 || isNameValid

                Button(
                    onClick = {
                        when (currentStep) {
                            2 -> {
                                currentStep++
                            }
                            3 -> {
                                if (isNotificationAccessGranted) {
                                    currentStep++
                                } else {
                                    PermissionHelper.openNotificationAccessSettings(context)
                                }
                            }
                            4 -> {
                                if (isNameValid) {
                                    onOnboardingComplete(userNameInput.trim())
                                }
                            }
                            else -> currentStep++
                        }
                    },
                    enabled = buttonEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .height(56.dp)
                        .shadow(4.dp, RoundedCornerShape(28.dp)),
                    contentPadding = PaddingValues(horizontal = 30.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = buttonText,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (currentStep == totalSteps - 1 && !isNameValid) {
                    Text(
                        text = "Please enter your name to continue.",
                        color = Color.Black.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun PageIndicatorDots(currentStep: Int, totalSteps: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until totalSteps) {
            val isCurrent = i == currentStep
            val dotWidth by animateDpAsState(
                targetValue = if (isCurrent) 20.dp else 6.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )
            val dotColor by animateColorAsState(
                targetValue = if (isCurrent) Color.Black else Color.Black.copy(alpha = 0.2f)
            )
            Box(
                modifier = Modifier
                    .width(dotWidth)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

// Titles
@Composable
fun IntroduceHubTitle(onSoundClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Payment",
            fontSize = 38.sp,
            fontWeight = FontWeight.Black,
            color = Color.Black,
            letterSpacing = (-1).sp,
            lineHeight = 44.sp
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Alerts",
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black,
                letterSpacing = (-1).sp,
                lineHeight = 44.sp
            )
            // Black Switch/Capsule Inline
            Box(
                modifier = Modifier
                    .width(76.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
                    .padding(4.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF7543))
                        .clickable { onSoundClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Iconsax.Outline.VolumeHigh,
                        contentDescription = "Play demo sound",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        Text(
            text = "With Zovio",
            fontSize = 38.sp,
            fontWeight = FontWeight.Black,
            color = Color.Black,
            letterSpacing = (-1).sp,
            lineHeight = 44.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Zovio turns your phone into a UPI payment announcer. It listens for alerts and speaks incoming payments aloud instantly, even offline.",
            fontSize = 14.sp,
            color = Color.Black.copy(alpha = 0.75f),
            fontWeight = FontWeight.Medium,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun AppCoveragePreviewTitle() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Always-on",
            fontSize = 38.sp,
            fontWeight = FontWeight.Black,
            color = Color.Black,
            letterSpacing = (-1).sp,
            lineHeight = 44.sp
        )
        Text(
            text = "UPI",
            fontSize = 38.sp,
            fontWeight = FontWeight.Black,
            color = Color.Black,
            letterSpacing = (-1).sp,
            lineHeight = 44.sp
        )
        Text(
            text = "Payment Alerts",
            fontSize = 38.sp,
            fontWeight = FontWeight.Black,
            color = Color.Black,
            letterSpacing = (-1).sp,
            lineHeight = 44.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Zovio listens for incoming UPI payments and announces them instantly, even when your screen is off.",
            fontSize = 14.sp,
            color = Color.Black.copy(alpha = 0.75f),
            fontWeight = FontWeight.Medium,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun EnableAccessTitle() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Notification",
            fontSize = 38.sp,
            fontWeight = FontWeight.Black,
            color = Color.Black,
            letterSpacing = (-1).sp,
            lineHeight = 44.sp
        )
        Text(
            text = "Permission",
            fontSize = 38.sp,
            fontWeight = FontWeight.Black,
            color = Color.Black,
            letterSpacing = (-1).sp,
            lineHeight = 44.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Grant access so Zovio can listen for incoming UPI alerts and announce payments immediately.",
            fontSize = 14.sp,
            color = Color.Black.copy(alpha = 0.75f),
            fontWeight = FontWeight.Medium,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun SecureAlertsTitle() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Secure",
            fontSize = 38.sp,
            fontWeight = FontWeight.Black,
            color = Color.Black,
            letterSpacing = (-1).sp,
            lineHeight = 44.sp
        )
        Text(
            text = "Payment",
            fontSize = 38.sp,
            fontWeight = FontWeight.Black,
            color = Color.Black,
            letterSpacing = (-1).sp,
            lineHeight = 44.sp
        )
        Text(
            text = "Monitoring",
            fontSize = 38.sp,
            fontWeight = FontWeight.Black,
            color = Color.Black,
            letterSpacing = (-1).sp,
            lineHeight = 44.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Zovio keeps UPI payment alerts secure and reliable, so your phone can notify you even when the screen is locked.",
            fontSize = 14.sp,
            color = Color.Black.copy(alpha = 0.75f),
            fontWeight = FontWeight.Medium,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun AskNameTitle() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Welcome to",
            fontSize = 38.sp,
            fontWeight = FontWeight.Black,
            color = Color.Black,
            letterSpacing = (-1).sp,
            lineHeight = 44.sp
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Zovio",
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black,
                letterSpacing = (-1).sp,
                lineHeight = 44.sp
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Iconsax.Outline.User,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Enter your name to personalize Zovio’s payment announcements and complete setup.",
            fontSize = 14.sp,
            color = Color.Black.copy(alpha = 0.75f),
            fontWeight = FontWeight.Medium,
            lineHeight = 18.sp
        )
    }
}

// Custom Premium quirky line art illustration drawings
@Composable
fun QuirkyLockIllustration() {
    val context = LocalContext.current

    // Prefer a drawable resource named `lock` (res/drawable/lock.png). This lets designers
    // drop a PNG into res/drawable and it will be used automatically.
    val drawableId = remember(context) {
        context.resources.getIdentifier("lock", "drawable", context.packageName)
    }

    // If drawable resource not found, try packaged assets/Illustrations/Lock.png
    val lockBitmap = remember(context, drawableId) {
        if (drawableId != 0) return@remember null
        try {
            context.assets.open("Illustrations/Lock.png").use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp),
        contentAlignment = Alignment.Center
    ) {
        val imageModifier = Modifier
            .fillMaxWidth(0.7f)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))

        when {
            drawableId != 0 -> {
                Image(
                    painter = painterResource(id = drawableId),
                    contentDescription = "Lock illustration",
                    modifier = imageModifier,
                    contentScale = ContentScale.Fit
                )
            }
            lockBitmap != null -> {
                Image(
                    bitmap = lockBitmap.asImageBitmap(),
                    contentDescription = "Lock illustration",
                    modifier = imageModifier,
                    contentScale = ContentScale.Fit
                )
            }
            else -> {
                Icon(
                    imageVector = Iconsax.Outline.Lock,
                    contentDescription = "Lock",
                    tint = Color.Black,
                    modifier = Modifier.size(120.dp)
                )
            }
        }
    }
}

@Composable
fun AppCoverageIllustration() {
    val context = LocalContext.current
    val radioBitmap = remember(context) {
        try {
            context.assets.open("Illustrations/radio.png").use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.25f))
                .shadow(10.dp, CircleShape)
        )

        if (radioBitmap != null) {
            Image(
                bitmap = radioBitmap.asImageBitmap(),
                contentDescription = "Radio illustration",
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(28.dp)),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.Black.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Iconsax.Outline.Radio,
                    contentDescription = "Radio illustration fallback",
                    tint = Color.Black,
                    modifier = Modifier.size(86.dp)
                )
            }
        }
    }
}

@Composable
fun QuirkySecurityIllustration(
    isGranted: Boolean,
    onGrantClick: () -> Unit
) {
    val context = LocalContext.current
    val smsBitmap = remember(context) {
        try {
            context.assets.open("Illustrations/SMS.png").use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (smsBitmap != null) {
            Image(
                bitmap = smsBitmap.asImageBitmap(),
                contentDescription = "SMS illustration",
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(260.dp)
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Fit
            )
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(260.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.05f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Iconsax.Outline.Message,
                        contentDescription = "SMS illustration fallback",
                        modifier = Modifier.size(92.dp),
                        tint = Color.Black.copy(alpha = 0.35f)
                    )
                }
            }
        }

        Text(
            text = if (isGranted) "Notification access granted" else "Notification access not granted",
            fontWeight = FontWeight.Black,
            fontSize = 16.sp,
            color = if (isGranted) Color(0xFF4ADE80) else Color(0xFFF87171),
            modifier = Modifier.padding(vertical = 12.dp)
        )
    }
}

@Composable
fun QuirkyLockWithoutKeyIllustration() {
    val context = LocalContext.current
    val lockBitmap = remember(context) {
        try {
            context.assets.open("Illustrations/Lock without key.png").use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp),
        contentAlignment = Alignment.Center
    ) {
        if (lockBitmap != null) {
            Image(
                bitmap = lockBitmap.asImageBitmap(),
                contentDescription = "Secure lock illustration",
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Fit
            )
        } else {
            Icon(
                imageVector = Iconsax.Outline.LockSlash,
                contentDescription = "Secure lock illustration fallback",
                tint = Color.Black,
                modifier = Modifier.size(120.dp)
            )
        }
    }
}

@Composable
fun QuirkyNameIllustration(
    userNameInput: String,
    onNameChange: (String) -> Unit
) {
    val context = LocalContext.current
    val personBitmap = remember(context) {
        try {
            context.assets.open("Illustrations/Person.png").use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (personBitmap != null) {
            Image(
                bitmap = personBitmap.asImageBitmap(),
                contentDescription = "Person illustration",
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(260.dp)
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Iconsax.Outline.UserTag,
                    contentDescription = "Person illustration fallback",
                    tint = Color.Black.copy(alpha = 0.35f),
                    modifier = Modifier.size(72.dp)
                )
            }
        }

        // Input field with thick black outlines
        OutlinedTextField(
            value = userNameInput,
            onValueChange = onNameChange,
            placeholder = { Text("Your Name (e.g. Siva)", color = Color.Black.copy(alpha = 0.4f)) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(3.dp, Color.Black, RoundedCornerShape(16.dp)),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            ),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.Black
            )
        )
    }
}
