package com.zovio.announcer.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zovio.announcer.ui.MainViewModel
import com.zovio.announcer.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onNavigateToTab: (Int) -> Unit = {}
) {
    val payments by viewModel.allPayments.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val todaySum by viewModel.todayTotal.collectAsState()
    val monthSum by viewModel.monthTotal.collectAsState()

    // Map payment distributions
    val appData = remember(payments) {
        val distribution = mutableMapOf(
            "PhonePe" to 0.0,
            "Google Pay" to 0.0,
            "Paytm" to 0.0,
            "BHIM" to 0.0,
            "Navi" to 0.0
        )
        
        var otherSum = 0.0
        payments.forEach { payment ->
            val source = payment.appSource.trim()
            val cleanSource = when {
                source.equals("PhonePe", ignoreCase = true) -> "PhonePe"
                source.equals("Google Pay", ignoreCase = true) || source.equals("GPay", ignoreCase = true) -> "Google Pay"
                source.equals("Paytm", ignoreCase = true) -> "Paytm"
                source.equals("BHIM", ignoreCase = true) -> "BHIM"
                source.equals("Navi", ignoreCase = true) -> "Navi"
                else -> "Others"
            }
            if (cleanSource == "Others") {
                otherSum += payment.amount
            } else {
                distribution[cleanSource] = (distribution[cleanSource] ?: 0.0) + payment.amount
            }
        }
        
        val finalMap = distribution.filter { it.value > 0.0 }.toMutableMap()
        if (otherSum > 0.0) {
            finalMap["Others"] = otherSum
        }
        finalMap
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Analytics",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = FinanceBlack,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onNavigateToTab(0) }) {
                        Icon(
                            imageVector = Iconsax.Outline.ArrowLeft2,
                            contentDescription = "Back to Home",
                            tint = FinanceBlack,
                            modifier = Modifier.size(20.dp)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryTile(
                    label = "Today",
                    value = "₹%.2f".format(todaySum),
                    accentColor = FinancePurple,
                    modifier = Modifier.weight(1f)
                )
                SummaryTile(
                    label = "This Month",
                    value = "₹%.2f".format(monthSum),
                    accentColor = FinanceGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = FinanceWhite),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Payment Distribution",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = FinanceBlack
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val totalVolume = if (payments.isEmpty()) 1.0 else payments.sumOf { it.amount }
                        Box(
                            modifier = Modifier.size(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val appColors = mapOf(
                                "PhonePe" to FinancePurple,
                                "Google Pay" to FinanceGreen,
                                "Paytm" to Color(0xFF60A5FA),
                                "BHIM" to FinanceYellow,
                                "Navi" to Color(0xFFEC4899),
                                "Others" to FinanceGrey
                            )

                            Canvas(modifier = Modifier.size(100.dp)) {
                                if (appData.isEmpty()) {
                                    drawArc(
                                        color = Color.LightGray.copy(alpha = 0.3f),
                                        startAngle = -90f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                } else {
                                    var startAngle = -90f
                                    appData.forEach { (appName, volume) ->
                                        val sweepAngle = ((volume / totalVolume) * 360f).toFloat()
                                        drawArc(
                                            color = appColors[appName] ?: FinanceGrey,
                                            startAngle = startAngle,
                                            sweepAngle = sweepAngle,
                                            useCenter = false,
                                            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
                                            size = Size(size.width, size.height)
                                        )
                                        startAngle += sweepAngle
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Total",
                                    fontSize = 10.sp,
                                    color = FinanceGrey
                                )
                                Text(
                                    text = "₹%.0f".format(totalVolume),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FinanceBlack
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            appData.forEach { (appName, amount) ->
                                val color = when (appName) {
                                    "PhonePe" -> FinancePurple
                                    "Google Pay" -> FinanceGreen
                                    "Paytm" -> Color(0xFF60A5FA)
                                    "BHIM" -> FinanceYellow
                                    "Navi" -> Color(0xFFEC4899)
                                    else -> FinanceGrey
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                        )
                                        Column {
                                            Text(
                                                text = appName,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = FinanceBlack
                                            )
                                            Text(
                                                text = "${(amount / totalVolume * 100).toInt()}%",
                                                fontSize = 10.sp,
                                                color = FinanceGrey
                                            )
                                        }
                                    }
                                    Text(
                                        text = "₹%.2f".format(amount),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FinanceBlack
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Operational Metrics",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = FinanceGrey,
                    modifier = Modifier.padding(start = 4.dp)
                )

                val averageAmount = if (payments.isEmpty()) 0.0 else payments.sumOf { it.amount } / payments.size

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CategoryMetricGridCard(
                            icon = Iconsax.Outline.VolumeHigh,
                            tint = FinancePurple,
                            itemsCount = "$totalCount Alerts",
                            categoryLabel = "Voice Alerts",
                            value = "$totalCount Announced",
                            modifier = Modifier.weight(1f)
                        )

                        CategoryMetricGridCard(
                            icon = Iconsax.Outline.Wallet2,
                            tint = Color(0xFF60A5FA),
                            itemsCount = "Average",
                            categoryLabel = "Ticket Size",
                            value = "₹%.1f".format(averageAmount),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CategoryMetricGridCard(
                            icon = Iconsax.Outline.Flash1,
                            tint = FinanceGreen,
                            itemsCount = "Active",
                            categoryLabel = "Service State",
                            value = "Continuous",
                            modifier = Modifier.weight(1f)
                        )

                        CategoryMetricGridCard(
                            icon = Iconsax.Outline.ShieldTick,
                            tint = FinanceYellow,
                            itemsCount = "100% Safe",
                            categoryLabel = "Privacy Mode",
                            value = "Local DB",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun CategoryMetricGridCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    itemsCount: String,
    categoryLabel: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = FinanceWhite),
        modifier = modifier
            .height(130.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable {
                isPressed = true
                coroutineScope.launch {
                    kotlinx.coroutines.delay(100)
                    isPressed = false
                }
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = categoryLabel,
                        tint = tint,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = itemsCount,
                    fontSize = 10.sp,
                    color = FinanceGrey,
                    fontWeight = FontWeight.Bold
                )
            }

            // Bottom Section
            Column {
                Text(
                    text = categoryLabel,
                    fontSize = 11.sp,
                    color = FinanceGrey,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = FinanceBlack
                )
            }
        }
    }
}

@Composable
fun SummaryTile(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = FinanceWhite),
        modifier = modifier.height(100.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = FinanceGrey,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = FinanceBlack
            )
            Box(
                modifier = Modifier
                    .height(4.dp)
                    .fillMaxWidth(0.35f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor)
            )
        }
    }
}
