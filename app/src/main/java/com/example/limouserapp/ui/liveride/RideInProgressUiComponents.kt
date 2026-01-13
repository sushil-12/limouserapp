package com.example.limouserapp.ui.liveride

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * COPIED FROM DRIVER APP:
 * Same tokens + composables to keep the exact same design language in limouserapp.
 */
internal object RideInProgressUiTokens {
    val Orange = Color(0xFFF2994A)
    val GreenText = Color(0xFF27AE60)
    val GreenBg = Color(0xFFE8F5E9)
    val GreenButton = Color(0xFF27AE60)
    val TextBlack = Color(0xFF1D1D1D)
    val TextGrey = Color(0xFF6B6B6B)
    val LightGrayButtonBg = Color(0xFFF5F5F5)
    val SheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
}

// --- Headers ---

@Composable
fun StatusHeaderBanner(text: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(24.dp),
        color = RideInProgressUiTokens.GreenBg.copy(alpha = 0.15f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text.uppercase(),
                color = RideInProgressUiTokens.GreenText,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun BlackTimerPill(timeText: String) {
    Box(
        modifier = Modifier
            .background(Color.Black, RoundedCornerShape(24.dp))
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Text(
            text = timeText,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            letterSpacing = 2.sp
        )
    }
}

@Composable
fun MetricHeader(
    eta: String,
    distance: String,
    subTitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$eta • $distance",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = RideInProgressUiTokens.TextBlack
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subTitle,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = RideInProgressUiTokens.TextGrey
        )
    }
}

// --- Action Rows ---

@Composable
fun ConnectWithPassengerRow(
    passengerName: String,
    onCall: () -> Unit,
    onChat: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        FilledIconButton(
            onClick = onCall,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color(0xFFF5F5F5)
            ),
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = "Call",
                tint = Color.Black
            )
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Connect With",
                fontSize = 12.sp,
                color = Color.Gray
            )
            Text(
                text = passengerName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        FilledIconButton(
            onClick = onChat,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color(0xFFF5F5F5)
            ),
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.CenterEnd)
        ) {
            Icon(
                imageVector = Icons.Default.Message,
                contentDescription = "Chat",
                tint = Color.Black
            )
        }
    }
}

@Composable
fun WaitingForPassengerRow(
    passengerName: String,
    onCall: () -> Unit,
    onChat: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        FilledIconButton(
            onClick = onCall,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color(0xFFF5F5F5)
            ),
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = "Call",
                tint = Color.Black
            )
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Waiting For Rider",
                fontSize = 14.sp,
                color = Color.Gray
            )
            Text(
                text = passengerName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        FilledIconButton(
            onClick = onChat,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color(0xFFF5F5F5)
            ),
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.CenterEnd)
        ) {
            Icon(
                imageVector = Icons.Default.Message,
                contentDescription = "Chat",
                tint = Color.Black
            )
        }
    }
}

// --- Trip Details Timeline ---

@Composable
fun TripTimelineView(
    pickup: String,
    dropoff: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Ride Details",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = RideInProgressUiTokens.TextBlack
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Column(
                modifier = Modifier
                    .width(24.dp)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .border(4.dp, RideInProgressUiTokens.Orange, CircleShape)
                )
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .width(2.dp)
                        .padding(vertical = 4.dp)
                ) {
                    drawLine(
                        color = Color.LightGray,
                        start = Offset(0f, 0f),
                        end = Offset(0f, size.height),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
                        strokeWidth = 4f
                    )
                }
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .border(4.dp, RideInProgressUiTokens.Orange, CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Pickup from $pickup",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = RideInProgressUiTokens.TextBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Column {
                    Text(
                        text = "Drop-off at $dropoff",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = RideInProgressUiTokens.TextBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// --- Buttons & Inputs ---

@Composable
fun ShareTripButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(25.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
    ) {
        Icon(
            Icons.Default.Share,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = RideInProgressUiTokens.TextGrey
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Share Trip Status", color = RideInProgressUiTokens.TextGrey)
    }
}

@Composable
fun FullWidthActionButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    isOutline: Boolean = false,
    enabled: Boolean = true
) {
    if (isOutline) {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, color),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = color.copy(alpha = 0.5f)
            )
        ) {
            Text(text, color = color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    } else {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = color,
                disabledContainerColor = color.copy(alpha = 0.4f),
                disabledContentColor = Color.White.copy(alpha = 0.7f)
            )
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(32.dp)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    }
                }
                Text(
                    text = text,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun PinInputRow(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusEvent { state ->
                if (state.isFocused) {
                    scope.launch { bringIntoViewRequester.bringIntoView() }
                }
            },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        decorationBox = {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                repeat(4) { index ->
                    val char = value.getOrNull(index)?.toString() ?: ""
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .size(50.dp)
                            .background(RideInProgressUiTokens.LightGrayButtonBg, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (char.isNotEmpty()) {
                            Text(
                                text = char,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun StatusHeaderBannerPreview() {
    StatusHeaderBanner(text = "Driver Arrived")
}

@Preview(showBackground = true)
@Composable
fun BlackTimerPillPreview() {
    Box(modifier = Modifier.padding(16.dp)) {
        BlackTimerPill(timeText = "02 : 15")
    }
}

@Preview(showBackground = true)
@Composable
fun MetricHeaderPreview() {
    MetricHeader(
        eta = "12 min",
        distance = "6.4 km",
        subTitle = "Arriving at pickup location"
    )
}

@Preview(showBackground = true)
@Composable
fun ConnectWithPassengerRowPreview() {
    Box(modifier = Modifier.padding(16.dp)) {
        ConnectWithPassengerRow(
            passengerName = "John Anderson",
            onCall = {},
            onChat = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FullWidthActionButtonFilledPreview() {
    Box(modifier = Modifier.padding(16.dp)) {
        FullWidthActionButton(
            text = "Start Trip",
            color = RideInProgressUiTokens.GreenButton,
            icon = Icons.Default.Phone,
            onClick = {}
        )
    }
}


