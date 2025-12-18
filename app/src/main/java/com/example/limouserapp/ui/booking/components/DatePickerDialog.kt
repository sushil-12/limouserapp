package com.example.limouserapp.ui.booking.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.limouserapp.ui.theme.LimoBlack
import java.text.SimpleDateFormat
import java.util.*

/**
 * Date Picker Dialog - Calendar view
 * Reusable component for selecting dates in booking screens
 */
@Composable
fun DatePickerDialog(
    selectedDate: Date,
    onDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(8.dp))
                .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .size(width = 328.dp, height = 688.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                val calendar = Calendar.getInstance()
                calendar.time = selectedDate

                // Show current month and next 23 months (24 total)
                val months = remember {
                    (0..23).map { i ->
                        Calendar.getInstance().apply {
                            time = Date()
                            add(Calendar.MONTH, i)
                            set(Calendar.DAY_OF_MONTH, 1)
                        }
                    }
                }

                LazyColumn {
                    items(months.size) { index ->
                        MonthCalendarView(
                            month = months[index].time,
                            selectedDate = selectedDate,
                            onDateSelected = onDateSelected
                        )
                        if (index < months.size - 1) {
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Bottom buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                        TextButton(onClick = onDismiss) {
                            Text(
                                "CANCEL",
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF9C27B0) // Purple/blue color
                                )
                            )
                        }

                        TextButton(onClick = onDismiss) {
                            Text(
                                "OK",
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF9C27B0)
                                )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun MonthCalendarView(
    month: Date,
    selectedDate: Date,
    onDateSelected: (Date) -> Unit
) {
    val calendar = Calendar.getInstance()
    calendar.time = month

    val monthFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val dayFormatter = SimpleDateFormat("d", Locale.getDefault())

    val year = calendar.get(Calendar.YEAR)
    val monthOfYear = calendar.get(Calendar.MONTH)

    calendar.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    val today = Calendar.getInstance()
    today.set(Calendar.HOUR_OF_DAY, 0)
    today.set(Calendar.MINUTE, 0)
    today.set(Calendar.SECOND, 0)
    today.set(Calendar.MILLISECOND, 0)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Month title
        Text(
            monthFormatter.format(month),
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = LimoBlack
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Weekday headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                Text(
                    day,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (day == "Sun" || day == "Sat") Color.Red else LimoBlack
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Calendar grid
        val daysList = mutableListOf<Date?>()
        // Add empty spaces for days before first day
        for (i in 1 until firstDayOfWeek) {
            daysList.add(null)
        }
        // Add all days in month
        for (day in 1..daysInMonth) {
            val date = Calendar.getInstance().apply {
                set(year, monthOfYear, day)
            }.time
            daysList.add(date)
        }

        // Create 7 columns for days of week
        val weeks = daysList.chunked(7)
        weeks.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                week.forEach { date ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (date != null) {
                            val selectedCal = Calendar.getInstance().apply { time = selectedDate }
                            val dateCal = Calendar.getInstance().apply { time = date }
                            val isSelected =
                                dateCal.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR) &&
                                        dateCal.get(Calendar.MONTH) == selectedCal.get(Calendar.MONTH) &&
                                        dateCal.get(Calendar.DAY_OF_MONTH) == selectedCal.get(Calendar.DAY_OF_MONTH)

                            val isPast = Calendar.getInstance().apply {
                                time = date
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.time.before(today.time)

                            DayButton(
                                day = dayFormatter.format(date),
                                isSelected = isSelected,
                                isPast = isPast,
                                onClick = { if (!isPast) onDateSelected(date) }
                            )
                        }
                    }
                }
                // Fill remaining slots if week has less than 7 days
                repeat(7 - week.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DayButton(
    day: String,
    isSelected: Boolean,
    isPast: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !isPast,
        modifier = Modifier.size(40.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) LimoBlack else Color.Transparent
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            day,
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = when {
                    isPast -> Color.Gray.copy(alpha = 0.3f)
                    isSelected -> Color.White
                    else -> LimoBlack // Darker color for future days
                }
            )
        )
    }
}

