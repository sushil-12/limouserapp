package com.example.limouserapp.ui.booking.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.limouserapp.R
import com.example.limouserapp.ui.theme.LimoBlack
import com.example.limouserapp.ui.theme.GoogleSansFamily

@Composable
fun LocationInputCard(
    pickupValue: String,
    destinationValue: String,
    onPickupValueChange: (String) -> Unit,
    onDestinationValueChange: (String) -> Unit,
    onPickupFocusChanged: (Boolean) -> Unit,
    onDestinationFocusChanged: (Boolean) -> Unit,
    showPickupClear: Boolean,
    showDestinationClear: Boolean,
    onPickupClear: () -> Unit,
    onDestinationClear: () -> Unit,
    // Unused params removed from signature for cleaner code if not used,
    // or keep them if you plan to attach clickable modifiers later.
    onPickupMapClick: () -> Unit,
    onDestinationMapClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, LimoBlack),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        // FIX 1: Removed fixed height(79.dp). Let content define height.
        modifier = modifier.wrapContentHeight()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // FIX 2: IntrinsicSize.Min allows child columns to match heights
                .height(IntrinsicSize.Min)
                .padding(vertical = 16.dp), // Increased padding slightly for breathing room
            verticalAlignment = Alignment.Top
        ) {

            // Indicators Column
            Box(
                modifier = Modifier
                    .padding(start = 12.5.dp)
                    .width(12.dp)
                    .fillMaxHeight(), // Stretches to match the text column
                contentAlignment = Alignment.TopCenter
            ) {
                LocationIndicators()
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text fields Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // Pickup text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LocationText(
                        value = pickupValue,
                        onValueChange = onPickupValueChange,
                        placeholder = "Pickup location",
                        onFocusChanged = onPickupFocusChanged,
                        modifier = Modifier.weight(1f)
                    )
                    if (pickupValue.isNotEmpty() && showPickupClear) {
                        Spacer(modifier = Modifier.width(8.dp))
                        ClearIcon(onClick = onPickupClear)
                    }
                }

                // Spacing and Divider
                Spacer(modifier = Modifier.height(12.dp))
                Divider(
                    color = LimoBlack.copy(alpha = 0.06f),
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Destination text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LocationText(
                        value = destinationValue,
                        onValueChange = onDestinationValueChange,
                        placeholder = "Where to?",
                        onFocusChanged = onDestinationFocusChanged,
                        modifier = Modifier.weight(1f)
                    )
                    if (destinationValue.isNotEmpty() && showDestinationClear) {
                        Spacer(modifier = Modifier.width(8.dp))
                        ClearIcon(onClick = onDestinationClear)
                    }
                }
            }
        }
    }
}

/**
 * FIX 3: Refactored Indicators to be dynamic.
 * Instead of hardcoded Box offsets, we use a Column with weight.
 * This ensures the line connects the dots regardless of the text container height.
 */
@Composable
private fun LocationIndicators() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxHeight()
    ) {
        // 1. Top Icon (Pickup)
        // We wrap in a Box of fixed height to align center with the Text Field's first line
        Box(
            modifier = Modifier.height(20.dp), // Approximate height of single line text
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.pickup_circle),
                contentDescription = "Pickup",
                tint = Color.Unspecified,
                modifier = Modifier.size(12.dp)
            )
        }

        // 2. Connecting Line (Dynamic Height)
        // weight(1f) makes it fill all available space between top and bottom icons
        Box(
            modifier = Modifier
                .width(1.dp)
                .weight(1f)
                .background(LimoBlack)
        )

        // 3. Bottom Icon (Dropoff)
        Box(
            modifier = Modifier.height(20.dp), // Approximate height of single line text
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.dropoff_square),
                contentDescription = "Dropoff",
                tint = Color.Unspecified,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

// ... LocationText and ClearIcon remain unchanged ...
@Composable
private fun LocationText(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .onFocusChanged { focusState ->
                onFocusChanged(focusState.isFocused)
            },
        textStyle = TextStyle(
            fontFamily = GoogleSansFamily,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = LimoBlack
        ),
        singleLine = true,
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = TextStyle(
                            fontFamily = GoogleSansFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF9A9A9A)
                        )
                    )
                }
                innerTextField()
            }
        }
    )
}