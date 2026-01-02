package com.example.limouserapp.ui.booking.comprehensivebooking

import android.R.attr.delay
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.limouserapp.R
import com.example.limouserapp.ui.theme.GoogleSansFamily
import com.example.limouserapp.ui.theme.LimoBlack
import com.example.limouserapp.ui.theme.LimoOrange
import kotlinx.coroutines.delay

/**
 * Section Header - matches Gemini's design
 */

private const val TAG = "EditableTextField"
@Composable
fun SectionHeader(title: String) {
    Text(
        title,
        style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = LimoOrange),
        modifier = Modifier.padding(bottom = 16.dp)
    )
}

/**
 * Styled Dropdown - matches Gemini's design with grey background and border
 */
@Composable
fun StyledDropdown(
    label: String,
    value: String,
    options: List<String>,
    expanded: Boolean,
    onExpand: (Boolean) -> Unit,
    onSelect: (String) -> Unit,
    isError: Boolean = false,
    placeholder: String? = null,
    isRequired: Boolean = false,
    errorMessage: String? = null
) {
    Column {
        Text(label, style = TextStyle(fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold))
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Color(0xFFF5F5F5), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                .border(1.dp, if (isError) Color.Red else Color(0xFFE0E0E0), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                .clickable { onExpand(true) }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(value, style = TextStyle(fontSize = 14.sp, color = LimoBlack))
                Icon(
                    painter = painterResource(R.drawable.dropdown_arrow),
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(10.dp)
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { onExpand(false) }) {
                options.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); onExpand(false) })
                }
            }
        }
        // Show error message below field
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                color = Color.Red,
                fontSize = 12.sp,
                fontFamily = GoogleSansFamily
            )
        }
    }
}

/**
 * Styled Input - matches Gemini's design with grey background and border
 */
@Composable
fun StyledInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    placeholder: String?=null,
    isRequired: Boolean = false,
    errorMessage: String? = null
) {
    // 1. Hold internal state
    var textFieldValueState by remember { mutableStateOf(TextFieldValue(text = value)) }

    // 2. Track if we need to force "Select All" on the next update (fix for tap race condition)
    var isFocused by remember { mutableStateOf(false) }
    var pendingSelectionUpdate by remember { mutableStateOf(false) }

    // 3. Sync external state
    if (value != textFieldValueState.text) {
        // Only update text, preserve selection unless it creates an invalid range
        textFieldValueState = textFieldValueState.copy(text = value)
    }

    Column {
        Text(
            label,
            style = TextStyle(fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                .border(1.dp, if (isError) Color.Red else Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = textFieldValueState,
                onValueChange = { newTfv ->
                    var finalState = newTfv

                    // THE FIX: If we have a pending selection update from a focus event,
                    // and the text hasn't changed (meaning it's just a cursor move from the tap),
                    // we enforce "Select All" and consume the flag.
                    if (pendingSelectionUpdate) {
                        if (newTfv.text == textFieldValueState.text) {
                            finalState = newTfv.copy(
                                selection = androidx.compose.ui.text.TextRange(0, newTfv.text.length)
                            )
                        }
                        // Always clear the flag after the first interaction
                        pendingSelectionUpdate = false
                    }

                    textFieldValueState = finalState
                    onValueChange(finalState.text)
                },
                textStyle = TextStyle(fontSize = 14.sp, color = LimoBlack),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused != isFocused) {
                            isFocused = focusState.isFocused
                            if (isFocused) {
                                // 1. Set flag to intercept the subsequent tap-cursor-placement
                                pendingSelectionUpdate = true
                                // 2. Immediate update for non-touch focus (e.g. keyboard navigation)
                                textFieldValueState = textFieldValueState.copy(
                                    selection = androidx.compose.ui.text.TextRange(0, textFieldValueState.text.length)
                                )
                            }
                        }
                    },
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                singleLine = true,
                cursorBrush = androidx.compose.ui.graphics.SolidColor(LimoOrange)
            )
        }
        // Show error message below field
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                color = Color.Red,
                fontSize = 12.sp,
                fontFamily = GoogleSansFamily
            )
        }
    }
}

/**
 * Editable Text Field - iOS-style text field with gray background
 */
@Composable
fun EditableTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier,
    labelFontSize: TextUnit = 12.sp,
    textFontSize: TextUnit = 16.sp,
    error: String? = null,
    onErrorCleared: (() -> Unit)? = null
) {
    // 1. Internal State
    var textFieldValueState by remember { mutableStateOf(TextFieldValue(text = value)) }

    // 2. Focus State
    var isFocused by remember { mutableStateOf(false) }

    // 3. Sync External Changes
    if (value != textFieldValueState.text) {
        textFieldValueState = textFieldValueState.copy(text = value)
    }

    // 4. FIX: Use LaunchedEffect to handle the race condition
    LaunchedEffect(isFocused) {
        if (isFocused) {
            Log.d(TAG, "[$label] Focus gained. Waiting for touch event to settle...")

            // Wait 50ms to allow the native touch event (which places the cursor) to finish
            delay(50)

            // Now overwrite the selection
            val textLength = textFieldValueState.text.length
            if (textLength > 0) {
                textFieldValueState = textFieldValueState.copy(
                    selection = androidx.compose.ui.text.TextRange(0, textLength)
                )
                Log.d(TAG, "[$label] SELECT ALL applied (0 to $textLength)")
            }
        }
    }

    val errorClearCallback = remember(onErrorCleared) { onErrorCleared }

    Column(modifier = modifier) {
        Text(
            label,
            style = TextStyle(fontSize = labelFontSize, color = Color.Gray, fontWeight = FontWeight.SemiBold, fontFamily = GoogleSansFamily)
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                .border(
                    1.dp,
                    if (error != null) Color.Red else Color(0xFFE0E0E0),
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = textFieldValueState,
                onValueChange = { newTfv ->
                    textFieldValueState = newTfv
                    errorClearCallback?.invoke()
                    onValueChange(newTfv.text)
                },
                textStyle = TextStyle(fontSize = textFontSize, color = LimoBlack, fontFamily = GoogleSansFamily),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (isFocused != focusState.isFocused) {
                            isFocused = focusState.isFocused
                            Log.d(TAG, "[$label] Focus State Changed: $isFocused")
                        }
                    },
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                singleLine = true,
                cursorBrush = SolidColor(LimoBlack)
            )
        }

        if (error != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = error,
                color = Color.Red,
                fontSize = 12.sp,
                fontFamily = GoogleSansFamily
            )
        }
    }
}

/**
 * Booking Section - Reusable section container (no card - matches Figma)
 */
@Composable
fun BookingSection(
    title: String,
    titleColor: Color = LimoBlack,
    titleSize: androidx.compose.ui.unit.TextUnit = 12.sp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            title,
            style = TextStyle(fontSize = titleSize, fontWeight = FontWeight.Bold, color = titleColor)
        )
        HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
        content()
    }
}

/**
 * Info Field - displays label and value
 */
@Composable
fun InfoField(label: String, value: String, alignEnd: Boolean = false) {
    Column(
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Label: Gray, smaller, regular weight
        Text(
            text = label,
            style = TextStyle(
                fontSize = 12.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
        )
        
        // Spacing between Label and Value
        Spacer(modifier = Modifier.height(4.dp))
        
        // Value: Black, larger, BOLD weight
        Text(
            text = value,
            style = TextStyle(
                fontSize = 14.sp,
                color = LimoBlack,
                fontWeight = FontWeight.Bold // Matches Figma emphasis
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Tag - small tag component for displaying labels
 */
@Composable
fun Tag(text: String) {
    Box(
        modifier = Modifier
            .background(Color(0xFFF5F5F5), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text,
            style = TextStyle(fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        )
    }
}

/**
 * Editable Field - iOS-style field with gray background
 */
@Composable
fun EditableField(
    label: String,
    value: String,
    onClick: (() -> Unit)?,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    Column(modifier = modifier) {
        Text(label, style = TextStyle(fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold))
        Spacer(Modifier.height(6.dp))
        
        if (onClick != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                    .border(1.dp, if (isError) Color.Red else Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                    .clickable(onClick = onClick)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        value,
                        style = TextStyle(fontSize = 14.sp, color = LimoBlack),
                        modifier = Modifier.weight(1f)
                    )
                    if (icon != null) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.dropdown_arrow),
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
            
            // Show error message below field if present
            if (isError && errorMessage != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = errorMessage,
                    style = TextStyle(fontSize = 12.sp, color = Color.Red),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    value,
                    style = TextStyle(fontSize = 14.sp, color = LimoBlack)
                )
            }
        }
    }
}

