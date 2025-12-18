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
import com.example.limouserapp.ui.theme.LimoOrange
import com.example.limouserapp.ui.theme.GoogleSansFamily

/**
 * Location input card matching Figma design specs
 * Width: ~300dp, Height: ~79dp
 * Background: #F5F5F5, Border: 1dp #121212, Corner radius: 8dp
 */
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
    onPickupMapClick: () -> Unit,
    onDestinationMapClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, LimoBlack),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier.height(79.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.5.dp, top = 13.dp, end = 10.dp, bottom = 13.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Indicators
            LocationIndicators()

            Spacer(modifier = Modifier.width(14.dp))

            // Text fields
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // Pickup text aligned with top icon
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
                    // Clear icon for pickup
                    if (pickupValue.isNotEmpty() && showPickupClear) {
                        Spacer(modifier = Modifier.width(8.dp))
                        ClearIcon(onClick = onPickupClear)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Divider(
                    color = LimoBlack.copy(alpha = 0.06f),
                    thickness = 1.dp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Destination text aligned with bottom icon
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
                    // Clear icon for destination
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
 * Visual indicators (both squares) with connecting line
 * Top: orange square (#F3933D, 12dp) with white inner square (6.21dp)
 * Bottom: orange square (#F3933D, 12dp) with white inner square (5.87dp)
 * Line: black line (#121212, 1dp width, 31dp height)
 */
@Composable
private fun LocationIndicators() {
    Box(
        modifier = Modifier
            .width(12.dp)
//            .padding(top = 13.17.dp)
    ) {
        // Pickup indicator
        Icon(
            painter = painterResource(id = R.drawable.pickup_circle),
            contentDescription = "Pickup",
            tint = Color.Unspecified,
            modifier = Modifier
                .size(12.dp)
                .align(Alignment.TopStart)
        )

        // Connecting line – placed below pickup and before dropoff
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(30.dp) // 30dp = line length between icons
                .align(Alignment.TopCenter)
                .offset(y = 12.dp + 1.dp) // start just below pickup icon with a small gap
                .background(LimoBlack)
        )

        // Dropoff indicator
        Icon(
            painter = painterResource(id = R.drawable.dropoff_square),
            contentDescription = "Dropoff",
            tint = Color.Unspecified,
            modifier = Modifier
                .size(12.dp)
                .align(Alignment.TopStart)
                .offset(y = 12.dp + 30.dp + 2.dp) // pickup height + line height + spacing
        )
    }
}



/**
 * Location text field with Google Sans typography
 * 16sp, weight 400, color #121212
 */
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
            Box {
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

