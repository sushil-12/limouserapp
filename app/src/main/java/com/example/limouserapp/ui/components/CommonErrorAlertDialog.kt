package com.example.limouserapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.limouserapp.ui.theme.LimoGreen
import com.example.limouserapp.ui.theme.LimoOrange
import com.example.limouserapp.ui.theme.LimoRed

// Ensure standard colors are available if not in your theme
val LimoBlue = Color(0xFF2196F3)

enum class AlertType { SUCCESS, ERROR, WARNING, INFO }

@Composable
fun CommonErrorAlertDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    type: AlertType,
    title: String,
    message: String,
    confirmText: String = "OK",
    onConfirm: () -> Unit = onDismiss,
    dismissText: String? = null,
    onDismissClick: (() -> Unit)? = null
) {
    if (!isVisible) return

    val mainColor = when (type) {
        AlertType.SUCCESS -> LimoGreen
        AlertType.ERROR -> LimoRed
        AlertType.WARNING -> LimoOrange
        AlertType.INFO -> LimoBlue
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 300.dp) // <--- KEY FIX: Stops it from getting too wide
                .fillMaxWidth(0.85f)   // Will only fill up to 300dp or 85%, whichever is smaller
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp), // Much tighter padding
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Small Icon
                Box(
                    modifier = Modifier
                        .size(40.dp) // Reduced from 56dp
                        .background(
                            color = mainColor.copy(alpha = 0.12f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getIconForType(type),
                        contentDescription = null,
                        tint = mainColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp)) // Reduced spacing

                // 2. Compact Text
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy( // Smaller title style
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp)) // Reduced spacing before button

                // 3. Compact Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (dismissText != null) {
                        OutlinedButton(
                            onClick = { onDismissClick?.invoke() ?: onDismiss() },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp), // Compact height
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            contentPadding = PaddingValues(0.dp) // Reduces internal button bloat
                        ) {
                            Text(
                                text = dismissText,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp), // Compact height
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = mainColor,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = confirmText,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun getIconForType(type: AlertType): ImageVector {
    return when (type) {
        AlertType.SUCCESS -> Icons.Rounded.CheckCircle
        AlertType.ERROR -> Icons.Rounded.Error
        AlertType.WARNING -> Icons.Rounded.Warning
        AlertType.INFO -> Icons.Rounded.Info
    }
}

// --- Convenience Wrappers (Unchanged) ---

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
