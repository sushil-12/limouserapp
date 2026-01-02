package com.example.limouserapp.ui.booking.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.limouserapp.ui.theme.GoogleSansFamily
import com.example.limouserapp.ui.theme.LimoBlack
import com.example.limouserapp.ui.theme.LimoOrange
import com.example.limouserapp.ui.theme.LimoWhite
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.time.temporal.TemporalAdjusters
import java.util.*

// --- 1. Domain Logic: Holiday Management ---
object HolidayRepository {
    fun getHolidayName(date: LocalDate): String? {
        // Fixed Dates
        if (date.month == Month.JANUARY && date.dayOfMonth == 1) return "New Year's Day"
        if (date.month == Month.JULY && date.dayOfMonth == 4) return "Independence Day"
        if (date.month == Month.DECEMBER && date.dayOfMonth == 25) return "Christmas"
        if (date.month == Month.DECEMBER && date.dayOfMonth == 31) return "New Year's Eve"
        if (date.month == Month.NOVEMBER && date.dayOfMonth == 11) return "Veterans Day"

        // Floating Dates
        // Thanksgiving: 4th Thursday in November
        if (date.month == Month.NOVEMBER && date.dayOfWeek == DayOfWeek.THURSDAY) {
            val thanksgiving = LocalDate.of(date.year, Month.NOVEMBER, 1)
                .with(TemporalAdjusters.dayOfWeekInMonth(4, DayOfWeek.THURSDAY))
            if (date == thanksgiving) return "Thanksgiving"
        }
        // Labor Day: 1st Monday in September
        if (date.month == Month.SEPTEMBER && date.dayOfWeek == DayOfWeek.MONDAY) {
            val laborDay = LocalDate.of(date.year, Month.SEPTEMBER, 1)
                .with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY))
            if (date == laborDay) return "Labor Day"
        }
        // Memorial Day: Last Monday in May
        if (date.month == Month.MAY && date.dayOfWeek == DayOfWeek.MONDAY) {
            val memorialDay = LocalDate.of(date.year, Month.MAY, 1)
                .with(TemporalAdjusters.lastInMonth(DayOfWeek.MONDAY))
            if (date == memorialDay) return "Memorial Day"
        }

        return null
    }

    fun isHoliday(date: LocalDate): Boolean {
        return getHolidayName(date) != null
    }
}

// --- 2. Main Component ---

@Composable
fun RoundTripDatePickerDialog(
    initialStartDate: Date,
    initialEndDate: Date?,
    onDateRangeSelected: (Date, Date) -> Unit,
    onDismiss: () -> Unit
) {
    val startDateState = remember { mutableStateOf<LocalDate?>(initialStartDate.toLocalDate()) }
    val endDateState = remember { mutableStateOf<LocalDate?>(initialEndDate?.toLocalDate()) }

    // Generate next 12 months for vertical scroll
    val months = remember {
        val current = YearMonth.now()
        (0..12).map { current.plusMonths(it.toLong()) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp, bottom = 24.dp, start = 16.dp, end = 16.dp)
                .clip(RoundedCornerShape(20.dp)), // Slightly softer corners
            color = LimoWhite,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // 1. Clean Header
                DateSelectionHeader(
                    startDate = startDateState.value,
                    endDate = endDateState.value,
                    onClose = onDismiss
                )

                HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))

                // 2. Info / Instructions Section
                CalendarInfoSection()

                // 3. Days of Week Sticky Header
                DaysOfWeekHeader()

                // 4. Scrollable Calendar
                Box(modifier = Modifier.weight(1f)) {
                    VerticalCalendarList(
                        months = months,
                        startDate = startDateState.value,
                        endDate = endDateState.value,
                        onDateClick = { date ->
                            handleDateSelection(date, startDateState, endDateState)
                        }
                    )
                }

                // 5. Confirm Footer
                ConfirmFooter(
                    startDate = startDateState.value,
                    endDate = endDateState.value,
                    onConfirm = { start, end ->
                        onDateRangeSelected(start.toDate(), end.toDate())
                    }
                )
            }
        }
    }
}

// --- 3. Logic Helpers ---

private fun handleDateSelection(
    date: LocalDate,
    startDate: MutableState<LocalDate?>,
    endDate: MutableState<LocalDate?>
) {
    val start = startDate.value
    val end = endDate.value

    if (start == null) {
        startDate.value = date
        endDate.value = null
    } else if (end == null) {
        if (date.isBefore(start)) {
            startDate.value = date // User picked a date before start, reset start
        } else {
            // Allows selecting the same date as return (Double tap logic)
            endDate.value = date
        }
    } else {
        // Reset if both were selected and user taps a new date
        startDate.value = date
        endDate.value = null
    }
}

// --- 4. Sub-Components ---

@Composable
private fun VerticalCalendarList(
    months: List<YearMonth>,
    startDate: LocalDate?,
    endDate: LocalDate?,
    onDateClick: (LocalDate) -> Unit
) {
    LazyColumn(
        state = rememberLazyListState(),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(months) { month ->
            MonthView(
                month = month,
                startDate = startDate,
                endDate = endDate,
                onDateClick = onDateClick
            )
        }
    }
}

@Composable
private fun MonthView(
    month: YearMonth,
    startDate: LocalDate?,
    endDate: LocalDate?,
    onDateClick: (LocalDate) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
        // Month Title - Minimalist
        Text(
            text = "${month.month.getDisplayName(JavaTextStyle.FULL, Locale.getDefault())} ${month.year}",
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = GoogleSansFamily,
                color = LimoBlack
            ),
            modifier = Modifier.padding(start = 24.dp, bottom = 20.dp)
        )

        val firstDayOfMonth = month.atDay(1)
        val daysInMonth = month.lengthOfMonth()
        val startOffset = firstDayOfMonth.dayOfWeek.value % 7
        val totalCells = startOffset + daysInMonth
        val rows = (totalCells + 6) / 7

        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val dayIndex = (row * 7) + col
                        if (dayIndex >= startOffset && dayIndex < startOffset + daysInMonth) {
                            val day = dayIndex - startOffset + 1
                            val date = month.atDay(day)
                            DayCell(
                                date = date,
                                startDate = startDate,
                                endDate = endDate,
                                onDateClick = onDateClick,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    startDate: LocalDate?,
    endDate: LocalDate?,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier
) {
    val today = LocalDate.now()
    val isPast = date.isBefore(today)
    val isSelectedStart = startDate == date
    val isSelectedEnd = endDate == date
    val isInRange = startDate != null && endDate != null && date.isAfter(startDate) && date.isBefore(endDate)

    val holidayName = HolidayRepository.getHolidayName(date)
    val isHoliday = holidayName != null

    // Background selection shapes
    val rangeBackgroundModifier = when {
        isSelectedStart && endDate != null && endDate != startDate -> Modifier
            .background(LimoBlack.copy(alpha = 0.05f), RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp))
        isSelectedEnd && startDate != null && startDate != endDate -> Modifier
            .background(LimoBlack.copy(alpha = 0.05f), RoundedCornerShape(topEnd = 50.dp, bottomEnd = 50.dp))
        isInRange -> Modifier
            .background(LimoBlack.copy(alpha = 0.05f))
        else -> Modifier
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(vertical = 4.dp)
            .then(rangeBackgroundModifier)
            .clip(CircleShape)
            .clickable(enabled = !isPast) { onDateClick(date) },
        contentAlignment = Alignment.Center
    ) {
        // Selection Circle
        if (isSelectedStart || isSelectedEnd) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LimoBlack, CircleShape)
            )
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = if (isSelectedStart || isSelectedEnd) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = GoogleSansFamily,
                        color = when {
                            isSelectedStart || isSelectedEnd -> Color.White
                            isPast -> Color.LightGray
                            isHoliday -> LimoOrange // Orange text for holidays
                            else -> LimoBlack
                        }
                    )
                )
            }

            // Holiday ($) Indicator
            if (isHoliday) {
                // If selected, the $ is white, otherwise orange
                val dollarColor = if (isSelectedStart || isSelectedEnd) Color.White.copy(alpha = 0.8f) else LimoOrange

                Text(
                    text = "$",
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = dollarColor
                    ),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun CalendarInfoSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF9F9F9)) // Very subtle grey background
            .padding(vertical = 12.dp, horizontal = 20.dp)
    ) {
        // Tip 1: Double Tap
        Text(
            text = "Tip: Double-tap to select the same date as the return date.",
            style = TextStyle(
                fontSize = 12.sp,
                color = Color.Gray,
                fontFamily = GoogleSansFamily
            )
        )
        Spacer(modifier = Modifier.height(6.dp))

        // Tip 2: Holiday Charge
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = LimoOrange,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Holidays ($) will have an additional charge.",
                style = TextStyle(
                    fontSize = 12.sp,
                    color = LimoBlack,
                    fontWeight = FontWeight.Medium,
                    fontFamily = GoogleSansFamily
                )
            )
        }
    }
}

@Composable
private fun DateSelectionHeader(
    startDate: LocalDate?,
    endDate: LocalDate?,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        // Top Row: Title + Close
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Select Dates",
                style = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = GoogleSansFamily,
                    color = LimoBlack
                )
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = LimoBlack)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Dates Display
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            HeaderDateDisplay(
                label = "Depart",
                date = startDate,
                isActive = startDate == null || endDate == null,
                modifier = Modifier.weight(1f)
            )

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(Color.LightGray.copy(alpha = 0.5f))
                    .align(Alignment.CenterVertically)
            )

            HeaderDateDisplay(
                label = "Return",
                date = endDate,
                isActive = startDate != null && endDate == null,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 24.dp)
            )
        }
    }
}

@Composable
private fun HeaderDateDisplay(
    label: String,
    date: LocalDate?,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val holidayName = date?.let { HolidayRepository.getHolidayName(it) }

    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = TextStyle(
                fontSize = 11.sp,
                color = if (isActive) LimoOrange else Color.Gray,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontFamily = GoogleSansFamily
            )
        )
        Spacer(modifier = Modifier.height(4.dp))

        if (date != null) {
            Text(
                text = date.format(DateTimeFormatter.ofPattern("EEE, MMM d")),
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LimoBlack,
                    fontFamily = GoogleSansFamily
                )
            )
            // Display Holiday Name clearly here
            if (holidayName != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.AttachMoney,
                        contentDescription = null,
                        tint = LimoOrange,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = holidayName,
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = LimoOrange,
                            fontWeight = FontWeight.Medium,
                            fontFamily = GoogleSansFamily
                        )
                    )
                }
            }
        } else {
            Text(
                text = "Select Date",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.LightGray,
                    fontFamily = GoogleSansFamily
                )
            )
        }
    }
}

@Composable
private fun DaysOfWeekHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        DayOfWeek.values().forEach { day ->
            Text(
                text = day.getDisplayName(JavaTextStyle.SHORT, Locale.US).take(1),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = TextStyle(
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    fontFamily = GoogleSansFamily
                )
            )
        }
    }
}

@Composable
private fun ConfirmFooter(
    startDate: LocalDate?,
    endDate: LocalDate?,
    onConfirm: (LocalDate, LocalDate) -> Unit
) {
    val isEnabled = startDate != null && endDate != null

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(LimoWhite)
            .padding(16.dp)
    ) {
        Button(
            onClick = {
                if (startDate != null && endDate != null) {
                    onConfirm(startDate, endDate)
                }
            },
            enabled = isEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp), // Modern soft curve
            colors = ButtonDefaults.buttonColors(
                containerColor = LimoBlack,
                disabledContainerColor = Color(0xFFF0F0F0),
                disabledContentColor = Color.Gray
            ),
            elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            Text(
                text = "Confirm Dates",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = GoogleSansFamily
            )
        }
    }
}

// Helper Extensions
private fun Date.toLocalDate(): LocalDate = this.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
private fun LocalDate.toDate(): Date = Date.from(this.atStartOfDay(ZoneId.systemDefault()).toInstant())