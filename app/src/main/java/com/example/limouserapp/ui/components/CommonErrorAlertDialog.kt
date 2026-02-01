package com.example.limouserapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// --- iOS Constants ---
private val IosBlue = Color(0xFF007AFF)
private val IosRed = Color(0xFFFF3B30)
private val IosSeparator = Color(0xFF3F3F3F).copy(alpha = 0.2f)
private val IosDialogBackground = Color(0xFFF2F2F2) // Slightly off-white
private val IosDialogDarkBackground = Color(0xFF1E1E1E) // Dark mode equivalent

@Composable
fun CommonErrorAlertDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    type: AlertType, // Kept for logic, but styling is now cleaner
    title: String,
    message: String,
    confirmText: String = "OK",
    onConfirm: () -> Unit = onDismiss,
    dismissText: String? = null,
    onDismissClick: (() -> Unit)? = null
) {
    if (!isVisible) return

    val isDarkMode = androidx.compose.foundation.isSystemInDarkTheme()
    val backgroundColor = if (isDarkMode) IosDialogDarkBackground else IosDialogBackground
    val contentColor = if (isDarkMode) Color.White else Color.Black

    // Logic: If it's an error/warning, maybe make the confirm button Red?
    // Otherwise default to iOS Blue.
    val confirmColor = when (type) {
        AlertType.ERROR -> IosRed
        else -> IosBlue
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false // Crucial for custom sizing
        )
    ) {
        Box(
            modifier = Modifier
                .width(270.dp) // Standard iOS Alert width
                .clip(RoundedCornerShape(14.dp)) // Standard iOS Corner Radius
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- Top Content Section ---
                Column(
                    modifier = Modifier.padding(top = 20.dp, start = 16.dp, end = 16.dp, bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = contentColor,
                        textAlign = TextAlign.Center
                    )

                    if (message.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                lineHeight = 16.sp
                            ),
                            color = contentColor.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // --- Button Grid Area ---
                // Horizontal Divider separating content from buttons
                HorizontalDivider(color = IosSeparator, thickness = 0.5.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp) // Standard iOS Button Height
                ) {
                    // 1. Dismiss Button (Optional)
                    if (dismissText != null) {
                        IosDialogButton(
                            text = dismissText,
                            color = IosBlue, // Dismiss is usually Blue (or standard text)
                            fontWeight = FontWeight.Normal, // Dismiss is usually normal weight
                            onClick = { onDismissClick?.invoke() ?: onDismiss() },
                            modifier = Modifier.weight(1f)
                        )

                        // Vertical Divider between buttons
                        VerticalDivider(color = IosSeparator, thickness = 0.5.dp)
                    }

                    // 2. Confirm Button
                    IosDialogButton(
                        text = confirmText,
                        color = confirmColor,
                        fontWeight = FontWeight.SemiBold, // Primary action is usually bolder
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun IosDialogButton(
    text: String,
    color: Color,
    fontWeight: FontWeight,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color.Gray)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 17.sp,
                fontWeight = fontWeight
            ),
            color = color
        )
    }
}

// --- Enum & Convenience Wrappers (Kept identical for compatibility) ---

enum class AlertType { SUCCESS, ERROR, WARNING, INFO }

@Composable
fun SuccessAlertDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    title: String = "Success",
    message: String,
    confirmText: String = "OK"
) {
    CommonErrorAlertDialog(
        isVisible = isVisible,
        onDismiss = onDismiss,
        type = AlertType.SUCCESS,
        title = title,
        message = message,
        confirmText = confirmText
    )
}

@Composable
fun ErrorAlertDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    title: String = "Error",
    message: String,
    confirmText: String = "Retry"
) {
    CommonErrorAlertDialog(
        isVisible = isVisible,
        onDismiss = onDismiss,
        type = AlertType.ERROR,
        title = title,
        message = message,
        confirmText = confirmText
    )
}

@Composable
fun WarningAlertDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    title: String = "Warning",
    message: String,
    confirmText: String = "Proceed",
    dismissText: String? = "Cancel",
    onDismissClick: (() -> Unit)? = null
) {
    CommonErrorAlertDialog(
        isVisible = isVisible,
        onDismiss = onDismiss,
        type = AlertType.WARNING,
        title = title,
        message = message,
        confirmText = confirmText,
        dismissText = dismissText,
        onDismissClick = onDismissClick
    )
}