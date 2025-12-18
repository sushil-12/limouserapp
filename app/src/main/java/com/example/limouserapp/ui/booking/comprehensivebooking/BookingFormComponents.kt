package com.example.limouserapp.ui.booking.comprehensivebooking

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.limouserapp.R
import com.example.limouserapp.ui.theme.LimoBlack
import com.example.limouserapp.ui.theme.LimoOrange

/**
 * Section Header - matches Gemini's design
 */
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
    onSelect: (String) -> Unit
) {
    Column {
        Text(label, style = TextStyle(fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold))
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Color(0xFFF5F5F5), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFFE0E0E0), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
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
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    var shouldSelectAll by remember { mutableStateOf(false) }

    Column {
        Text(label, style = TextStyle(fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold))
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Color(0xFFF5F5F5), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFFE0E0E0), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = TextFieldValue(
                    text = value,
                    selection = if (shouldSelectAll && value.isNotEmpty()) {
                        androidx.compose.ui.text.TextRange(0, value.length)
                    } else {
                        androidx.compose.ui.text.TextRange(value.length)
                    }
                ),
                onValueChange = { textFieldValue ->
                    // Once user starts typing, disable select-all
                    shouldSelectAll = false
                    onValueChange(textFieldValue.text)
                },
                textStyle = TextStyle(fontSize = 14.sp, color = LimoBlack),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        val wasFocused = isFocused
                        isFocused = focusState.isFocused
                        // Select all text only on first focus, and only if field has content
                        if (!wasFocused && focusState.isFocused && value.isNotEmpty()) {
                            shouldSelectAll = true
                        } else if (!focusState.isFocused) {
                            // Reset select-all flag when field loses focus
                            shouldSelectAll = false
                        }
                    },
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                singleLine = true
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
    labelFontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
    textFontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    error: String? = null,
    onErrorCleared: (() -> Unit)? = null
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    var shouldSelectAll by remember { mutableStateOf(false) }

    // Store callback to avoid scoping issues
    val errorClearCallback = remember(onErrorCleared) { onErrorCleared }
    
    Column(modifier = modifier) {
        Text(label, style = TextStyle(fontSize = labelFontSize, color = Color.Gray, fontWeight = FontWeight.SemiBold))
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Color(0xFFF5F5F5), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                .border(
                    1.dp,
                    if (error != null) Color.Red else Color(0xFFE0E0E0),
                    androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = TextFieldValue(
                    text = value,
                    selection = if (shouldSelectAll && value.isNotEmpty()) {
                        androidx.compose.ui.text.TextRange(0, value.length)
                    } else {
                        androidx.compose.ui.text.TextRange(value.length)
                    }
                ),
                onValueChange = { textFieldValue ->
                    // Once user starts typing, disable select-all and clear error
                    shouldSelectAll = false
                    errorClearCallback?.invoke()
                    onValueChange(textFieldValue.text)
                },
                textStyle = TextStyle(fontSize = textFontSize, color = LimoBlack),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        val wasFocused = isFocused
                        isFocused = focusState.isFocused
                        // Select all text only on first focus, and only if field has content
                        if (!wasFocused && focusState.isFocused && value.isNotEmpty()) {
                            shouldSelectAll = true
                        } else if (!focusState.isFocused) {
                            // Reset select-all flag when field loses focus
                            shouldSelectAll = false
                        }
                    },
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                singleLine = true
            )
        }

        // Error message
        error?.let { errorMessage ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                color = Color.Red,
                fontSize = 12.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
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
    modifier: Modifier = Modifier
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
                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
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

