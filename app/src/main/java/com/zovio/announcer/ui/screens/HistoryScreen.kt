package com.zovio.announcer.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import io.github.rabehx.iconsax.Iconsax
import io.github.rabehx.iconsax.outline.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zovio.announcer.data.db.PaymentEntity
import com.zovio.announcer.ui.MainViewModel
import com.zovio.announcer.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onNavigateToTab: (Int) -> Unit = {}
) {
    val rawPayments by viewModel.allPayments.collectAsState()
    val monthSum by viewModel.monthTotal.collectAsState()
    val weekSum by viewModel.weekTotal.collectAsState()

    var selectedFilter by remember { mutableStateOf("All") }
    var activeDiagnosticsId by remember { mutableStateOf<Long?>(null) }

    val appFilters = listOf("All", "PhonePe", "Google Pay", "Paytm")

    var selectedDateFilter by remember { mutableStateOf<Calendar?>(null) }
    var showCustomDatePicker by remember { mutableStateOf(false) }

    // Filtered Payments
    val filteredPayments = remember(rawPayments, selectedFilter, selectedDateFilter) {
        var list = if (selectedFilter == "All") {
            rawPayments
        } else {
            rawPayments.filter { it.appSource.equals(selectedFilter, ignoreCase = true) }
        }
        selectedDateFilter?.let { filterCal ->
            list = list.filter { payment ->
                val paymentCal = Calendar.getInstance().apply { timeInMillis = payment.timestamp }
                paymentCal.get(Calendar.YEAR) == filterCal.get(Calendar.YEAR) &&
                paymentCal.get(Calendar.DAY_OF_YEAR) == filterCal.get(Calendar.DAY_OF_YEAR)
            }
        }
        list
    }

    // Recipient simulation config states
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    if (showDeleteConfirmDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showDeleteConfirmDialog = false }
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = FinanceWhite,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(FinanceRed.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Iconsax.Outline.Trash,
                            contentDescription = "Warning",
                            tint = FinanceRed,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Text(
                        text = "Clear Voice Logs?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = FinanceBlack,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Are you sure you want to permanently delete all intercepted UPI payment logs? This action completely wipes history and cannot be undone.",
                        fontSize = 12.sp,
                        color = FinanceGrey,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { showDeleteConfirmDialog = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FinanceWhite,
                                contentColor = FinanceGrey
                            ),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, FinanceGrey.copy(alpha = 0.3f)),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.clearAllPayments()
                                showDeleteConfirmDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FinanceRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Text("Delete All", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Transactions",
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
                actions = {
                    // Sweep / Delete options menu
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(FinanceWhite)
                            .clickable { if (rawPayments.isNotEmpty()) showDeleteConfirmDialog = true }
                            .border(0.5.dp, FinanceGrey.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (rawPayments.isNotEmpty()) Iconsax.Outline.Trash else Iconsax.Outline.More,
                            contentDescription = "Menu Options",
                            tint = if (rawPayments.isNotEmpty()) FinanceRed else FinanceBlack,
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
        ) {
            // App source selector
            ScrollableFilterRow(
                filtersList = appFilters,
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    label = "Total Alerts",
                    value = filteredPayments.size.toString(),
                    accentColor = FinancePurple,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    label = "Last 7 Days",
                    value = "₹%.0f".format(weekSum),
                    accentColor = FinanceGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Weekly dynamic chart card
                item {
                    WeeklyBarChartCard(
                        rawPayments = filteredPayments,
                        weekSum = filteredPayments.sumOf { it.amount },
                        title = "Past 7 Days"
                    )
                }

                // Recipient transfer sandbox config card ("Transfer to" mockup)

                // Dynamic Voice Logs List Title
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Alerts",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = FinanceBlack
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (selectedDateFilter != null) {
                                val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(selectedDateFilter!!.time)
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(FinancePurpleLight)
                                        .clickable { selectedDateFilter = null }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(dateStr, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FinancePurple)
                                        Icon(Iconsax.Outline.CloseCircle, contentDescription = "Clear", tint = FinancePurple, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                            
                            // Calendar filter trigger
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(FinanceWhite)
                                    .clickable { showCustomDatePicker = true }
                                    .border(0.5.dp, FinanceGrey.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Iconsax.Outline.Calendar1,
                                    contentDescription = "Filter by Date",
                                    tint = if (selectedDateFilter != null) FinancePurple else FinanceBlack,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                val displayList = filteredPayments
                val isExpense = false

                if (displayList.isEmpty()) {
                    item {
                        EmptyStateView(filterLabel = selectedFilter)
                    }
                } else {
                    items(
                        items = displayList,
                        key = { it.id }
                    ) { payment ->
                        PaymentRowItem(
                            payment = payment,
                            isExpanded = activeDiagnosticsId == payment.id,
                            onItemClick = {
                                activeDiagnosticsId = if (activeDiagnosticsId == payment.id) null else payment.id
                            }
                        )
                    }
                }
            }
        }
        
        if (showCustomDatePicker) {
            CustomDatePickerDialog(
                initialDate = selectedDateFilter,
                onDateSelected = { date ->
                    selectedDateFilter = date
                    showCustomDatePicker = false
                },
                onDismiss = { showCustomDatePicker = false }
            )
        }
    }
}

@Composable
fun ScrollableFilterRow(
    filtersList: List<String>,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        filtersList.forEach { filterItem ->
            val isSelected = filterItem == selectedFilter
            val background = if (isSelected) FinanceBlack else FinanceWhite
            val contentColor = if (isSelected) Color.White else FinanceBlack

            Box(
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(background)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) Color.Transparent else FinanceGrey.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { onFilterSelected(filterItem) }
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = filterItem,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
fun SummaryCard(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.12f)),
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                color = FinanceBlack,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StatusChip(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun WeeklyBarChartCard(
    rawPayments: List<PaymentEntity>,
    weekSum: Double,
    title: String = "Total Expenses"
) {
    // Dynamically calculate totals for the last 7 days from rawPayments in-memory flow
    val daysList = remember(rawPayments) {
        val currentWeekStart = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -6)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        
        (0..6).map { offset ->
            val dayCal = Calendar.getInstance().apply {
                time = currentWeekStart.time
                add(Calendar.DAY_OF_YEAR, offset)
            }
            val start = dayCal.timeInMillis
            val end = start + 86400000L // 24h
            
            val sum = rawPayments.filter { it.timestamp in start until end }.sumOf { it.amount }
            val dayName = when (dayCal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "Mon"
                Calendar.TUESDAY -> "Tue"
                Calendar.WEDNESDAY -> "Wed"
                Calendar.THURSDAY -> "Thu"
                Calendar.FRIDAY -> "Fri"
                Calendar.SATURDAY -> "Sat"
                Calendar.SUNDAY -> "Sun"
                else -> "Day"
            }
            Pair(dayName, sum)
        }
    }

    val maxVal = remember(daysList) {
        val max = daysList.maxOfOrNull { it.second } ?: 0.0
        if (max == 0.0) 1.0 else max
    }

    // Highlight day with maximum sum, or Friday (index 4) if all sums are 0
    val activeIndex = remember(daysList) {
        val maxVolume = daysList.maxOf { it.second }
        if (maxVolume > 0.0) daysList.indexOfFirst { it.second == maxVolume } else 4
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = FinancePurpleLight),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Stats line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        color = FinanceGrey,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "₹%.2f".format(weekSum),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = FinanceBlack
                    )
                }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(FinanceWhite.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Iconsax.Outline.ReceiptItem,
                        contentDescription = "Chart",
                        tint = FinancePurple,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusChip(label = "Expense", color = FinancePurple)
                StatusChip(label = "Trends", color = FinanceGreen)
            }
            Spacer(modifier = Modifier.height(18.dp))

            // Bars Chart drawing area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                daysList.forEachIndexed { idx, (dayName, sum) ->
                    val isActive = idx == activeIndex
                    val heightRatio = (sum / maxVal).toFloat().coerceIn(0.08f, 1.0f)
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Tooltip on top of active bar
                        if (isActive) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(FinanceYellow)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "₹%.0f".format(sum),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FinanceBlack
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        } else {
                            // Empty spacer to align height
                            Box(modifier = Modifier.height(18.dp))
                        }

                        // Bar capsule
                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                .fillMaxHeight(0.6f * heightRatio)
                                .clip(CircleShape)
                                .background(if (isActive) FinancePurple else FinancePurple.copy(alpha = 0.3f))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Text(
                            text = dayName,
                            fontSize = 10.sp,
                            color = FinanceGrey,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun PaymentRowItem(
    payment: PaymentEntity,
    isExpanded: Boolean,
    onItemClick: () -> Unit,
    isExpense: Boolean = false
) {
    val formattedDate = remember(payment.timestamp) {
        try {
            val datePart = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(payment.timestamp))
            val timePart = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(payment.timestamp))
            "$datePart at $timePart"
        } catch (e: Exception) {
            "Date info"
        }
    }

    val appColor = when (payment.appSource.lowercase()) {
        "phonepe" -> FinancePurple
        "google pay" -> FinanceGreen
        "paytm" -> Color(0xFF60A5FA)
        "bhim" -> FinanceYellow
        else -> Color(0xFFEC4899)
    }

    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { onItemClick() }
            .border(1.dp, FinanceGrey.copy(alpha = 0.12f), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = FinanceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
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

                // Title and subtitle
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = payment.senderName ?: "Unknown Sender",
                        fontWeight = FontWeight.Bold,
                        color = FinanceBlack,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(appColor.copy(alpha = 0.14f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = payment.appSource,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColor
                            )
                        }
                        Text(
                            text = formattedDate,
                            fontSize = 11.sp,
                            color = FinanceGrey
                        )
                    }
                }

                // Amount display
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isExpense) "- ₹%.2f".format(payment.amount) else "+ ₹%.2f".format(payment.amount),
                        fontWeight = FontWeight.Bold,
                        color = if (isExpense) FinanceRed else FinanceGreen,
                        fontSize = 14.sp
                    )
                    Icon(
                        imageVector = Iconsax.Outline.ArrowDown2,
                        contentDescription = "Expand",
                        tint = FinanceGrey,
                        modifier = Modifier
                            .size(16.dp)
                            .graphicsLayer(rotationZ = rotationAngle)
                    )
                }
            }

            // Expand raw notification sniffer logs details
            if (isExpanded) {
                Column(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(FinanceBg)
                        .padding(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Iconsax.Outline.InfoCircle,
                            contentDescription = "Info",
                            tint = FinancePurple,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            "Raw Sniffed Notification:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = FinancePurple
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = payment.rawNotificationText,
                        fontSize = 11.sp,
                        color = FinanceBlack,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(
    filterLabel: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(FinancePurpleLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Iconsax.Outline.ReceiptItem,
                    contentDescription = "No payments",
                    tint = FinancePurple,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                "No Logs Available",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = FinanceBlack
            )

            Text(
                text = if (filterLabel == "All") {
                    "No UPI payments intercepted yet. Play diagnostic voice logs to see items."
                } else {
                    "No payment alerts found corresponding to $filterLabel source."
                },
                fontSize = 12.sp,
                color = FinanceGrey,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 24.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CustomDatePickerDialog(
    initialDate: Calendar?,
    onDateSelected: (Calendar) -> Unit,
    onDismiss: () -> Unit
) {
    var calendarState by remember { mutableStateOf(Calendar.getInstance().apply { 
        initialDate?.let { timeInMillis = it.timeInMillis }
    }) }
    
    val currentMonthName = remember(calendarState) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendarState.time)
    }

    val daysInMonth = remember(calendarState) {
        val tempCal = calendarState.clone() as Calendar
        tempCal.set(Calendar.DAY_OF_MONTH, 1)
        val maxDays = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val startOffset = when (tempCal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> 0
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            else -> 0
        }
        Pair(maxDays, startOffset)
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = FinanceWhite,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header (Month Navigation)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val nextCal = calendarState.clone() as Calendar
                            nextCal.add(Calendar.MONTH, -1)
                            calendarState = nextCal
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Iconsax.Outline.ArrowLeft2, contentDescription = "Prev Month", tint = FinanceBlack)
                    }

                    Text(
                        text = currentMonthName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = FinanceBlack
                    )

                    IconButton(
                        onClick = {
                            val nextCal = calendarState.clone() as Calendar
                            nextCal.add(Calendar.MONTH, 1)
                            calendarState = nextCal
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Iconsax.Outline.ArrowRight2, contentDescription = "Next Month", tint = FinanceBlack)
                    }
                }

                // Days of week header labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val daysLabels = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
                    daysLabels.forEach { label ->
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = FinanceGrey,
                            modifier = Modifier.width(36.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Days grid drawing
                val (maxDays, startOffset) = daysInMonth
                val totalCells = maxDays + startOffset
                val rowsCount = (totalCells + 6) / 7

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (row in 0 until rowsCount) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (col in 0 until 7) {
                                val cellIdx = row * 7 + col
                                val dayNum = cellIdx - startOffset + 1
                                if (dayNum in 1..maxDays) {
                                    val isSelected = initialDate != null &&
                                            initialDate.get(Calendar.YEAR) == calendarState.get(Calendar.YEAR) &&
                                            initialDate.get(Calendar.MONTH) == calendarState.get(Calendar.MONTH) &&
                                            initialDate.get(Calendar.DAY_OF_MONTH) == dayNum
                                    
                                    val todayCal = Calendar.getInstance()
                                    val isToday = todayCal.get(Calendar.YEAR) == calendarState.get(Calendar.YEAR) &&
                                            todayCal.get(Calendar.MONTH) == calendarState.get(Calendar.MONTH) &&
                                            todayCal.get(Calendar.DAY_OF_MONTH) == dayNum

                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) FinancePurple else Color.Transparent)
                                            .border(
                                                width = if (isToday && !isSelected) 1.dp else 0.dp,
                                                color = if (isToday && !isSelected) FinancePurple else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                val selectedCal = calendarState.clone() as Calendar
                                                selectedCal.set(Calendar.DAY_OF_MONTH, dayNum)
                                                onDateSelected(selectedCal)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayNum.toString(),
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else if (isToday) FinancePurple else FinanceBlack
                                        )
                                    }
                                } else {
                                    Box(modifier = Modifier.size(36.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
