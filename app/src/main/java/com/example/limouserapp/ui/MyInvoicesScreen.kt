package com.example.limouserapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.example.limouserapp.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.limouserapp.data.model.dashboard.Invoice
import com.example.limouserapp.ui.components.InvoiceCard
import com.example.limouserapp.ui.components.ContactBottomSheet
import com.example.limouserapp.ui.components.ShimmerBox
import com.example.limouserapp.ui.components.ShimmerText
import com.example.limouserapp.ui.components.shimmerEffect
import com.example.limouserapp.ui.theme.LimoOrange
import com.example.limouserapp.ui.theme.LimoRed
import com.example.limouserapp.ui.theme.LimoWhite
import com.example.limouserapp.ui.viewmodel.InvoiceViewModel
import com.example.limouserapp.ui.viewmodel.TimePeriod
import java.text.SimpleDateFormat
import java.util.*

// -- Constants --
private val ScreenBackgroundColor = LimoWhite
private val CardBackgroundColor = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyInvoicesScreen(
    onBackClick: () -> Unit = {},
    onViewInvoiceSummary: (Int) -> Unit = {},
    viewModel: InvoiceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSearchBar by remember { mutableStateOf(false) }
    var showContactSheet by remember { mutableStateOf(false) }
    var contactPhoneNumber by remember { mutableStateOf("") }
    var contactDriverName by remember { mutableStateOf("") }

    // Clear search when closing search bar
    LaunchedEffect(showSearchBar) {
        if (!showSearchBar) viewModel.clearSearch()
    }

    // Main Scaffold
    Scaffold(
        containerColor = ScreenBackgroundColor,
        topBar = {
            // Custom Header that handles Safe Area (Status Bar)
            LocalCommonHeaderWithSearch(
                title = "My Invoices",
                onBackClick = onBackClick,
                onSearchClick = {
                    showSearchBar = !showSearchBar
                    if (!showSearchBar) viewModel.clearSearch()
                },
                isSearching = showSearchBar
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            // Search Bar (Visible only when toggled)
            if (showSearchBar) {
                SearchBar(
                    searchText = uiState.searchText,
                    onSearchTextChange = { viewModel.handleSearch(it) },
                    onClearSearch = { viewModel.clearSearch() }
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp) // Gap between cards
            ) {

                // 1. Controls Section (Summary + Time Selector + Date Picker)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardBackgroundColor)
                            .padding(bottom = 16.dp)
                    ) {
                        // Summary Card
                        SummaryCard(
                            dateRange = viewModel.dateRangeString,
                            onPreviousPeriod = { handlePreviousNavigation(viewModel, uiState.selectedTimePeriod) },
                            onNextPeriod = { handleNextNavigation(viewModel, uiState.selectedTimePeriod) },
                            canGoPrevious = true,
                            canGoNext = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Time Period Selector
                        TimePeriodSelector(
                            selectedTimePeriod = uiState.selectedTimePeriod,
                            onTimePeriodChange = viewModel::handleTimePeriodChange,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        // Custom Date Range Picker (Only visible if CUSTOM is selected)
                        if (uiState.selectedTimePeriod == TimePeriod.CUSTOM) {
                            Spacer(modifier = Modifier.height(12.dp))
                            CustomDateRangePicker(
                                startDate = uiState.selectedWeekStart,
                                endDate = uiState.selectedWeekEnd,
                                onDateRangeSelected = { startDate, endDate ->
                                    viewModel.handleDateRangeSelection(startDate, endDate)
                                },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }

                // 2. Invoices List
                when {
                    uiState.isLoading && uiState.invoices.isEmpty() -> {
                        items(3) { // Show 3 shimmer cards
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                InvoiceCardShimmer()
                            }
                        }
                    }
                    uiState.showError && uiState.invoices.isEmpty() -> {
                        item {
                            ErrorView(
                                message = uiState.errorMessage,
                                onRetry = viewModel::refreshInvoices
                            )
                        }
                    }
                    uiState.invoices.isEmpty() -> {
                        item { EmptyInvoicesView() }
                    }
                    else -> {
                        // NO GROUPING: Just list the invoices directly
                        items(uiState.invoices) { invoice ->
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                InvoiceCard(
                                    invoice = invoice,
                                    onViewInvoiceSummary = { onViewInvoiceSummary(invoice.invoiceNumber) },
                                    onDriverPhoneClick = { phoneNumber ->
                                        contactPhoneNumber = phoneNumber
                                        contactDriverName = invoice.driverName
                                        showContactSheet = true
                                    }
                                )
                            }
                        }

                        // Load More Button
                        if (uiState.currentPage < uiState.totalPages) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (uiState.isLoading) {
                                        // Shimmer button placeholder
                                        ShimmerBox(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    } else {
                                        Button(
                                            onClick = viewModel::loadNextPage,
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                            border = BorderStroke(1.dp, Color.Gray),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Load More", color = Color.Black)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Sheet for Driver Contact
        ContactBottomSheet(
            phoneNumber = contactPhoneNumber,
            driverName = contactDriverName,
            isVisible = showContactSheet,
            onDismiss = { showContactSheet = false }
        )
    }
}

// MARK: - Shimmer Components

@Composable
fun InvoiceCardShimmer() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp), // Add slight vertical spacing between cards
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE5E5E5)) // Subtle border
    ) {
        Column {
            // 1. Header Shimmer
            // We use a light gray background to mimic the header area, but softer than Black
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF9F9F9)) // Very light gray header background
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date Placeholder
                Box(
                    modifier = Modifier
                        .height(16.dp)
                        .width(150.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
                // Badge Placeholder
                Box(
                    modifier = Modifier
                        .height(20.dp)
                        .width(80.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
            }

            // 2. Summary Shimmer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // ID
                Box(modifier = Modifier.size(width = 50.dp, height = 14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                // Status
                Box(modifier = Modifier.size(width = 60.dp, height = 14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                // Price
                Box(modifier = Modifier.size(width = 70.dp, height = 14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
            }

            // Divider
            Divider(color = Color(0xFFF0F0F0), thickness = 1.dp)

            // 3. Route Details Shimmer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Timeline Visual
                Column(
                    modifier = Modifier.height(60.dp), // Fixed height for timeline visual
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).shimmerEffect())
                    Box(modifier = Modifier.width(2.dp).height(30.dp).shimmerEffect())
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).shimmerEffect())
                }

                // Address Text
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Pickup Line
                    Box(modifier = Modifier.fillMaxWidth(0.9f).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                    Spacer(modifier = Modifier.height(24.dp))
                    // Dropoff Line
                    Box(modifier = Modifier.fillMaxWidth(0.7f).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                }
            }

            // 4. Driver Info Shimmer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFAFAFA))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .shimmerEffect()
                )

                // Name & Phone lines
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.fillMaxWidth(0.4f).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                    Box(modifier = Modifier.fillMaxWidth(0.3f).height(10.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                }
            }

            // 5. Action Buttons Shimmer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Button 1
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .shimmerEffect()
                )
                // Button 2
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .shimmerEffect()
                )
                // More Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .shimmerEffect()
                )
            }
        }
    }
}

// MARK: - Empty State

@Composable
private fun EmptyInvoicesView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 80.dp, start = 48.dp, end = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Illustration - reuse the same icon or use Description icon
        Image(
            painter = painterResource(id = R.drawable.ic_no_booking),
            contentDescription = "No Invoices",
            modifier = Modifier.size(180.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Title
        Text(
            text = "No Invoices Found",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Body Text
        Text(
            text = "There are no invoices for the selected date range. Try selecting a different period.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun ErrorView(message: String?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Error, contentDescription = null, tint = LimoRed)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = message ?: "Unknown Error")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

// MARK: - Components

@Composable
private fun SearchBar(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onClearSearch: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search invoices...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = onClearSearch) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF0F0F0),
                unfocusedContainerColor = Color(0xFFF0F0F0),
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = LimoOrange
            ),
            shape = RoundedCornerShape(10.dp)
        )
    }
}

@Composable
private fun SummaryCard(
    dateRange: String,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    canGoPrevious: Boolean,
    canGoNext: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE5E5E5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Button
            IconButton(
                onClick = onPreviousPeriod,
                enabled = canGoPrevious,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous",
                    tint = if (canGoPrevious) Color.Black else Color.LightGray,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Date Text
            Text(
                text = dateRange,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            )

            // Next Button
            IconButton(
                onClick = onNextPeriod,
                enabled = canGoNext,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next",
                    tint = if (canGoNext) Color.Black else Color.LightGray,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun TimePeriodSelector(
    selectedTimePeriod: TimePeriod,
    onTimePeriodChange: (TimePeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFEEEEEE), RoundedCornerShape(8.dp))
            .padding(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            TimePeriod.values().forEach { period ->
                val isSelected = selectedTimePeriod == period
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) Color.White else Color.Transparent)
                        .clickable { onTimePeriodChange(period) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = period.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.Black else Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomDateRangePicker(
    startDate: Date,
    endDate: Date,
    onDateRangeSelected: (Date, Date) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCalendarPicker by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { showCalendarPicker = true },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.LightGray)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.DateRange, null, tint = LimoOrange, modifier = Modifier.padding(end = 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Custom Range", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(
                    "${formatDate(startDate)} - ${formatDate(endDate)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
        }
    }

    if (showCalendarPicker) {
        CustomDateRangePickerDialog(
            startDate = startDate,
            endDate = endDate,
            onDateRangeSelected = { start, end ->
                onDateRangeSelected(start, end)
                showCalendarPicker = false
            },
            onDismiss = { showCalendarPicker = false }
        )
    }
}

@Composable
private fun CustomDateRangePickerDialog(
    startDate: Date,
    endDate: Date,
    onDateRangeSelected: (Date, Date) -> Unit,
    onDismiss: () -> Unit
) {
    var tempStartDate by remember { mutableStateOf<Date?>(startDate) }
    var tempEndDate by remember { mutableStateOf<Date?>(endDate) }
    var currentMonth by remember { mutableStateOf(startDate) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Dialog Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Select Dates", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Month Navigation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            val cal = Calendar.getInstance().apply { time = currentMonth }
                            cal.add(Calendar.MONTH, -1)
                            currentMonth = cal.time
                        }) { Icon(Icons.Default.ChevronLeft, null) }

                        Text(
                            text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentMonth),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        IconButton(onClick = {
                            val cal = Calendar.getInstance().apply { time = currentMonth }
                            cal.add(Calendar.MONTH, 1)
                            currentMonth = cal.time
                        }) { Icon(Icons.Default.ChevronRight, null) }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Calendar Grid
                    MonthCalendarRangeView(
                        month = currentMonth,
                        startDate = tempStartDate,
                        endDate = tempEndDate,
                        onDateSelected = { date ->
                            when {
                                tempStartDate == null -> { tempStartDate = date; tempEndDate = null }
                                tempEndDate != null -> { tempStartDate = date; tempEndDate = null }
                                date.before(tempStartDate!!) -> { tempEndDate = tempStartDate; tempStartDate = date }
                                else -> { tempEndDate = date }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Dialog Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5F5F5)),
                            elevation = ButtonDefaults.buttonElevation(0.dp)
                        ) { Text("Cancel", color = Color.Black) }
                        Button(
                            onClick = {
                                val finalStart = tempStartDate ?: startDate
                                val finalEnd = tempEndDate ?: finalStart
                                onDateRangeSelected(finalStart, finalEnd)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = LimoOrange)
                        ) { Text("Apply", color = Color.White) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthCalendarRangeView(
    month: Date,
    startDate: Date?,
    endDate: Date?,
    onDateSelected: (Date) -> Unit
) {
    val calendar = Calendar.getInstance()
    calendar.time = month
    calendar.set(Calendar.DAY_OF_MONTH, 1)

    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val currentYear = calendar.get(Calendar.YEAR)
    val currentMonth = calendar.get(Calendar.MONTH)

    Row(Modifier.fillMaxWidth()) {
        listOf("S", "M", "T", "W", "T", "F", "S").forEach {
            Text(
                it,
                Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }

    Spacer(Modifier.height(8.dp))

    var dayCounter = 1
    Column {
        for (row in 0 until 6) {
            if (dayCounter > daysInMonth) break
            Row(Modifier.fillMaxWidth()) {
                for (col in 1..7) {
                    if ((row == 0 && col < firstDayOfWeek) || dayCounter > daysInMonth) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        val date = Calendar.getInstance().apply {
                            set(currentYear, currentMonth, dayCounter, 0, 0, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.time

                        val isStart = startDate != null && isSameDay(date, startDate)
                        val isEnd = endDate != null && isSameDay(date, endDate)
                        val isInRange = startDate != null && endDate != null &&
                                date.after(startDate) && date.before(endDate)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isStart || isEnd || isInRange) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            color = if (isStart || isEnd) LimoOrange else LimoOrange.copy(alpha = 0.2f),
                                            shape = if (isStart || isEnd) CircleShape else RoundedCornerShape(0.dp)
                                        )
                                )
                            }
                            Text(
                                text = dayCounter.toString(),
                                color = if (isStart || isEnd) Color.White else Color.Black,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { onDateSelected(date) }
                                    .padding(4.dp),
                                fontSize = 14.sp
                            )
                        }
                        dayCounter++
                    }
                }
            }
        }
    }
}

// MARK: - Navigation Helpers

private fun handlePreviousNavigation(viewModel: InvoiceViewModel, timePeriod: TimePeriod) {
    when (timePeriod) {
        TimePeriod.WEEKLY -> viewModel.goToPreviousWeek()
        TimePeriod.MONTHLY -> viewModel.goToPreviousMonth()
        TimePeriod.YEARLY -> viewModel.goToPreviousYear()
        else -> {}
    }
}

private fun handleNextNavigation(viewModel: InvoiceViewModel, timePeriod: TimePeriod) {
    when (timePeriod) {
        TimePeriod.WEEKLY -> viewModel.goToNextWeek()
        TimePeriod.MONTHLY -> viewModel.goToNextMonth()
        TimePeriod.YEARLY -> viewModel.goToNextYear()
        else -> {}
    }
}

private fun formatDate(date: Date): String {
    return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
}

private fun isSameDay(date1: Date, date2: Date): Boolean {
    val c1 = Calendar.getInstance().apply { time = date1 }
    val c2 = Calendar.getInstance().apply { time = date2 }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}

