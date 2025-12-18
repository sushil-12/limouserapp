package com.example.limouserapp.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.limouserapp.ui.theme.LimoWhite
import kotlinx.coroutines.launch

/**
 * Contact Bottom Sheet
 * Shows contact options (Call, Message) when clicking on phone number
 * Matches iOS native action sheet style - simple, clean, minimal design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactBottomSheet(
    phoneNumber: String,
    driverName: String = "",
    isVisible: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Sheet state
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    
    // Show/hide bottom sheet based on isVisible
    LaunchedEffect(isVisible) {
        if (isVisible) {
            sheetState.show()
        } else {
            sheetState.hide()
        }
    }
    
    // Clean phone number (remove spaces, dashes, etc.)
    val cleanPhoneNumber = phoneNumber.replace(Regex("[\\s\\-\\(\\)]"), "")
    
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    onDismiss()
                }
            },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            containerColor = LimoWhite,
            dragHandle = {
                // Drag indicator - matches iOS style
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(5.dp)
                        .background(
                            Color.Gray.copy(alpha = 0.3f),
                            RoundedCornerShape(2.5.dp)
                        )
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
            ) {
                // Contact options - iOS style: simple text buttons with dividers
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color.White,
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    // Call option - first item (rounded top corners)
                    ContactOptionButton(
                        text = "Call",
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:$cleanPhoneNumber")
                            }
                            context.startActivity(intent)
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                onDismiss()
                            }
                        },
                        showDivider = true,
                        isFirstItem = true,
                        isLastItem = false
                    )
                    
                    // Message/SMS option - last item (rounded bottom corners)
                    ContactOptionButton(
                        text = "Message",
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("smsto:$cleanPhoneNumber")
                            }
                            context.startActivity(intent)
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                onDismiss()
                            }
                        },
                        showDivider = false,
                        isFirstItem = false,
                        isLastItem = true
                    )
                }
                
                // Cancel button - iOS style: separate rounded container
                Spacer(modifier = Modifier.height(8.dp))
                
                ContactOptionButton(
                    text = "Cancel",
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            onDismiss()
                        }
                    },
                    showDivider = false,
                    isCancelButton = true,
                    isFirstItem = true,
                    isLastItem = true
                )
            }
        }
    }
}

/**
 * Contact option button - iOS native action sheet style
 * Simple text button with optional divider
 */
@Composable
private fun ContactOptionButton(
    text: String,
    onClick: () -> Unit,
    showDivider: Boolean,
    isCancelButton: Boolean = false,
    isFirstItem: Boolean = false,
    isLastItem: Boolean = false
) {
    val shape = when {
        isCancelButton -> RoundedCornerShape(12.dp)
        isFirstItem && isLastItem -> RoundedCornerShape(12.dp)
        isFirstItem -> RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
        isLastItem -> RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
        else -> RoundedCornerShape(0.dp)
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, shape)
    ) {
        // Divider (iOS style: thin gray line)
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(Color.Gray.copy(alpha = 0.3f))
            )
        }
        
        // Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = if (isCancelButton) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isCancelButton) Color.Black else Color(0xFF007AFF) // iOS blue for actions
                )
            )
        }
    }
}

