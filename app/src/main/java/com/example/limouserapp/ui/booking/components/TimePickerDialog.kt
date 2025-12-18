package com.example.limouserapp.ui.booking.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
// NOTE: Ensure these imports point to your actual theme module
import com.example.limouserapp.ui.theme.LimoBlack
import com.example.limouserapp.ui.theme.LimoOrange
import com.example.limouserapp.ui.theme.LimoWhite
import java.util.*

/**
 * Time Picker Dialog - Clock and keyboard input
 * Reusable component for selecting time in booking screens
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class) // Opt-in for TimePickerState and TimePicker
fun TimePickerDialog(
    selectedTime: Date,
    onTimeSelected: (Date) -> Unit,
    onDismiss: () -> Unit
) {
    var showKeyboardInput by remember { mutableStateOf(false) }

    // 1. Get initial 24h values from selectedTime for TimePickerState
    val initialCalendar = Calendar.getInstance().apply { time = selectedTime }
    val initialHour24 = initialCalendar.get(Calendar.HOUR_OF_DAY)
    val initialMinute = initialCalendar.get(Calendar.MINUTE)

    // TimePickerState uses 24-hour format internally
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour24,
        initialMinute = initialMinute,
        is24Hour = false // Assumes 12-hour format is desired
    )

    // 2. State for the custom Keyboard view (which uses 12-hour format)
    var selectedHour12 by remember {
        val hour = initialHour24
        val hour12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        mutableIntStateOf(hour12)
    }
    var selectedMinuteKeyboard by remember { mutableIntStateOf(initialMinute) }
    var isAMKeyboard by remember { mutableStateOf(initialHour24 < 12) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .size(width = 328.dp, height = if (showKeyboardInput) 256.dp else 524.dp)
                .background(LimoWhite, RoundedCornerShape(12.dp))
                .border(1.dp, LimoBlack.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
        ) {
            if (showKeyboardInput) {
                // Keyboard Input View
                KeyboardTimeInputView(
                    selectedHour = selectedHour12,
                    selectedMinute = selectedMinuteKeyboard,
                    isAM = isAMKeyboard,
                    onHourChange = { selectedHour12 = it },
                    onMinuteChange = { selectedMinuteKeyboard = it },
                    onAMPMChange = { isAMKeyboard = it },
                    onClose = { showKeyboardInput = false },
                    onConfirm = {
                        val newTime = updateTimeFromComponents(selectedTime, selectedHour12, selectedMinuteKeyboard, isAMKeyboard)
                        onTimeSelected(newTime)
                        onDismiss()
                    }
                )
            } else {
                // Clock View
                ClockTimePickerMaterial3(
                    state = timePickerState,
                    onKeyboardTap = {
                        // Synchronize Material 3 state (24h) back to Keyboard state (12h) before switching
                        val hour24 = timePickerState.hour
                        selectedHour12 = if (hour24 == 0) 12 else if (hour24 > 12) hour24 - 12 else hour24
                        selectedMinuteKeyboard = timePickerState.minute
                        isAMKeyboard = hour24 < 12

                        showKeyboardInput = true
                    },
                    onConfirm = {
                        val newTime = Calendar.getInstance().apply {
                            time = selectedTime
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.time

                        onTimeSelected(newTime)
                        onDismiss()
                    },
                    onCancel = onDismiss
                )
            }
        }
    }
}

/**
 * NEW Composable: Wrapper for Material 3 TimePicker.
 * FIX: Removed custom TimeSource to resolve "Unresolved reference" error.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ClockTimePickerMaterial3(
    state: TimePickerState,
    onKeyboardTap: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Material 3 TimePicker
        TimePicker(
            state = state,
            modifier = Modifier.weight(1f),
            // The timeSource parameter is removed.
            colors = TimePickerDefaults.colors(
                // Numbers and text inside the clock dial
                clockDialSelectedContentColor = LimoWhite,
                clockDialUnselectedContentColor = LimoBlack.copy(alpha = 0.6f),
                // Clock dial background (the gray circle)
                clockDialColor = LimoOrange.copy(alpha = 0.05f),
                // Clock hand and selected number background
                selectorColor = LimoOrange,

                // Period Selector (AM/PM box)
                periodSelectorBorderColor = LimoBlack.copy(alpha = 0.2f),
                periodSelectorSelectedContainerColor = LimoOrange,
                periodSelectorUnselectedContainerColor = LimoWhite,
                periodSelectorSelectedContentColor = LimoWhite,
                periodSelectorUnselectedContentColor = LimoBlack,


                // Input mode (the 08 : 15 fields)
                timeSelectorSelectedContainerColor = LimoOrange,
                timeSelectorUnselectedContainerColor = LimoBlack.copy(alpha = 0.05f),
                timeSelectorSelectedContentColor = LimoWhite,
                timeSelectorUnselectedContentColor = LimoBlack
            )
        )

        // Bottom controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Keyboard icon
            IconButton(
                onClick = onKeyboardTap,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.Keyboard,
                    contentDescription = "Keyboard",
                    tint = LimoBlack,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onCancel) {
                    Text(
                        "CANCEL",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = LimoBlack.copy(alpha = 0.7f)
                        )
                    )
                }

                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LimoOrange
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "DONE",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LimoWhite
                        )
                    )
                }
            }
        }
    }
}


/**
 * Keyboard Time Input View (SMOOTHNESS FIX APPLIED)
 */
@Composable
private fun KeyboardTimeInputView(
    selectedHour: Int,
    selectedMinute: Int,
    isAM: Boolean,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onAMPMChange: (Boolean) -> Unit,
    onClose: () -> Unit,
    onConfirm: () -> Unit
) {
    // 1. Local State Initialization (using remember with keys for one-time setup)
    var hourText by remember(selectedHour) { mutableStateOf(selectedHour.toString()) }
    var minuteText by remember(selectedMinute) { mutableStateOf(String.format("%02d", selectedMinute)) }
    var isAMState by remember(isAM) { mutableStateOf(isAM) }

    var isHourFieldActive by remember { mutableStateOf(true) }
    var showError by remember { mutableStateOf(false) }

    val hourFocusRequester = remember { FocusRequester() }
    val minuteFocusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                "ENTER TIME",
                style = TextStyle(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = LimoBlack.copy(alpha = 0.6f),
                    letterSpacing = 1.5.sp
                )
            )
        }

        // Time input fields
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hour input
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = hourText,
                    onValueChange = { text ->
                        // Only update the local text state here for smooth typing.
                        val filteredText = text.filter { it.isDigit() }
                        hourText = filteredText.take(2)

                        // Optional: Auto-shift focus to minutes if hour input seems complete and valid
                        if (filteredText.length == 2 && filteredText.toIntOrNull() in 1..12) {
                            minuteFocusRequester.requestFocus()
                        }
                    },
                    modifier = Modifier
                        .size(width = 96.dp, height = 80.dp)
                        .focusRequester(hourFocusRequester)
                        .onFocusChanged { focusState ->
                            isHourFieldActive = focusState.isFocused
                            if (!focusState.isFocused) {
                                // Validate and update parent state ONLY when focus is lost
                                val hour = hourText.toIntOrNull()
                                val validatedHour = when {
                                    hour in 1..12 -> hour!!
                                    else -> 12 // Default/Fix to 12 if invalid
                                }
                                hourText = validatedHour.toString() // Fix the displayed text
                                onHourChange(validatedHour) // Update parent state once
                            }
                        },
                    textStyle = TextStyle(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Normal,
                        color = LimoBlack,
                        textAlign = TextAlign.Center
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = LimoWhite,
                        unfocusedContainerColor = LimoWhite,
                        focusedBorderColor = LimoOrange,
                        unfocusedBorderColor = LimoBlack.copy(alpha = 0.2f),
                        focusedTextColor = LimoBlack,
                        unfocusedTextColor = LimoBlack
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Text(
                    "Hour",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = LimoBlack.copy(alpha = 0.6f)
                    )
                )
            }

            Text(
                ":",
                style = TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Normal,
                    color = LimoBlack
                )
            )

            // Minute input
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = minuteText,
                    onValueChange = { text ->
                        // Only update the local text state here for smooth typing.
                        minuteText = text.filter { it.isDigit() }.take(2)
                    },
                    modifier = Modifier
                        .size(width = 96.dp, height = 80.dp)
                        .focusRequester(minuteFocusRequester)
                        .onFocusChanged { focusState ->
                            isHourFieldActive = !focusState.isFocused
                            if (!focusState.isFocused) {
                                // Validate and update parent state ONLY when focus is lost
                                val minute = minuteText.toIntOrNull()
                                val validatedMinute = when {
                                    minute in 0..59 -> minute!!
                                    else -> 0 // Default/Fix to 0 if invalid
                                }
                                minuteText = String.format("%02d", validatedMinute) // Fix the displayed text
                                onMinuteChange(validatedMinute) // Update parent state once
                            }
                        },
                    textStyle = TextStyle(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Normal,
                        color = LimoBlack,
                        textAlign = TextAlign.Center
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    placeholder = { Text("00", color = LimoBlack.copy(alpha = 0.3f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = LimoBlack.copy(alpha = 0.05f),
                        unfocusedContainerColor = LimoBlack.copy(alpha = 0.05f),
                        focusedBorderColor = LimoOrange,
                        unfocusedBorderColor = LimoBlack.copy(alpha = 0.2f),
                        focusedTextColor = LimoBlack,
                        unfocusedTextColor = LimoBlack
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Text(
                    "Minute",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = LimoBlack.copy(alpha = 0.6f)
                    )
                )
            }

            // AM/PM selector
            Column(
                modifier = Modifier
                    .border(1.dp, LimoBlack.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
            ) {
                // AM Button
                Button(
                    onClick = {
                        isAMState = true
                        onAMPMChange(true) // Update parent state immediately
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAMState) LimoOrange else LimoWhite
                    ),
                    modifier = Modifier.width(52.dp).height(40.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text(
                        "AM",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isAMState) LimoWhite else LimoBlack.copy(alpha = 0.7f)
                        )
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.width(52.dp),
                    color = LimoBlack.copy(alpha = 0.2f)
                )

                // PM Button
                Button(
                    onClick = {
                        isAMState = false
                        onAMPMChange(false) // Update parent state immediately
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isAMState) LimoOrange else LimoWhite
                    ),
                    modifier = Modifier.width(52.dp).height(40.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text(
                        "PM",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (!isAMState) LimoWhite else LimoBlack.copy(alpha = 0.7f)
                        )
                    )
                }
            }
        }

        if (showError) {
            Text(
                "Please enter a valid time",
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = LimoOrange
                ),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Bottom controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Clock icon (switches back to clock view)
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = "Clock",
                    tint = LimoBlack.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // CANCEL and OK buttons
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TextButton(onClick = onClose) {
                    Text(
                        "CANCEL",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = LimoBlack.copy(alpha = 0.7f)
                        )
                    )
                }

                Button(
                    onClick = {
                        // Validate and update parent state with final text field values
                        val hour = hourText.toIntOrNull()
                        val minute = minuteText.toIntOrNull()

                        if (hour in 1..12 && minute in 0..59) {
                            showError = false

                            // Ensure latest values are hoisted to parent state for calculation
                            onHourChange(hour!!)
                            onMinuteChange(minute!!)

                            onConfirm() // Calls parent's Date calculation and dismisses
                        } else {
                            showError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LimoOrange
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "OK",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = LimoWhite
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun updateTimeFromComponents(baseDate: Date, hour: Int, minute: Int, isAM: Boolean): Date {
    val calendar = Calendar.getInstance()
    calendar.time = baseDate

    var hour24 = hour
    if (!isAM && hour != 12) {
        hour24 = hour + 12
    } else if (isAM && hour == 12) {
        hour24 = 0
    }

    calendar.set(Calendar.HOUR_OF_DAY, hour24)
    calendar.set(Calendar.MINUTE, minute)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    return calendar.time
}

/**
 * Preview Composable that hosts the TimePickerDialog for visual iteration.
 */
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,dpi=420")
@Composable
private fun TimePickerDialogClockPreview() {
    val initialTime = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 8); set(Calendar.MINUTE, 30) }.time
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LimoBlack.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            TimePickerDialogContent(
                selectedTime = initialTime,
                showKeyboardInput = false,
                onTimeSelected = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,dpi=420")
@Composable
private fun TimePickerDialogKeyboardPreview() {
    val initialTime = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 15); set(Calendar.MINUTE, 45) }.time
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LimoBlack.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            TimePickerDialogContent(
                selectedTime = initialTime,
                showKeyboardInput = true,
                onTimeSelected = {},
                onDismiss = {}
            )
        }
    }
}

/**
 * Helper function to expose the Dialog content for direct previewing, bypassing the actual Dialog() wrapper.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TimePickerDialogContent(
    selectedTime: Date,
    showKeyboardInput: Boolean,
    onTimeSelected: (Date) -> Unit,
    onDismiss: () -> Unit
) {
    // 1. Get initial 24h values from selectedTime for TimePickerState
    val initialCalendar = Calendar.getInstance().apply { time = selectedTime }
    val initialHour24 = initialCalendar.get(Calendar.HOUR_OF_DAY)
    val initialMinute = initialCalendar.get(Calendar.MINUTE)

    // TimePickerState uses 24-hour format internally
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour24,
        initialMinute = initialMinute,
        is24Hour = false
    )

    // 2. State for the custom Keyboard view (which uses 12-hour format)
    var selectedHour12 by remember {
        val hour = initialHour24
        val hour12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        mutableIntStateOf(hour12)
    }
    var selectedMinuteKeyboard by remember { mutableIntStateOf(initialMinute) }
    var isAMKeyboard by remember { mutableStateOf(initialHour24 < 12) }

    // Logic to handle showKeyboardInput state switching for the previewer
    var localShowKeyboardInput by remember { mutableStateOf(showKeyboardInput) }

    Box(
        modifier = Modifier
            .size(width = 328.dp, height = if (localShowKeyboardInput) 256.dp else 524.dp)
            .background(LimoWhite, RoundedCornerShape(12.dp))
            .border(1.dp, LimoBlack.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
    ) {
        if (localShowKeyboardInput) {
            // Keyboard Input View
            KeyboardTimeInputView(
                selectedHour = selectedHour12,
                selectedMinute = selectedMinuteKeyboard,
                isAM = isAMKeyboard,
                onHourChange = { selectedHour12 = it },
                onMinuteChange = { selectedMinuteKeyboard = it },
                onAMPMChange = { isAMKeyboard = it },
                onClose = { localShowKeyboardInput = false },
                onConfirm = {
                    val newTime = updateTimeFromComponents(selectedTime, selectedHour12, selectedMinuteKeyboard, isAMKeyboard)
                    onTimeSelected(newTime)
                    onDismiss()
                }
            )
        } else {
            // Clock View
            ClockTimePickerMaterial3(
                state = timePickerState,
                onKeyboardTap = {
                    // Sync state from TimePicker to keyboard state before switching
                    val hour24 = timePickerState.hour
                    selectedHour12 = if (hour24 == 0) 12 else if (hour24 > 12) hour24 - 12 else hour24
                    selectedMinuteKeyboard = timePickerState.minute
                    isAMKeyboard = hour24 < 12

                    localShowKeyboardInput = true
                },
                onConfirm = {
                    val newTime = Calendar.getInstance().apply {
                        time = selectedTime
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time
                    onTimeSelected(newTime)
                    onDismiss()
                },
                onCancel = onDismiss
            )
        }
    }
}