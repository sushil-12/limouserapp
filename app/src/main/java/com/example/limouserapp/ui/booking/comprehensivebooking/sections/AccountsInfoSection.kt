package com.example.limouserapp.ui.booking.comprehensivebooking.sections

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.limouserapp.data.model.dashboard.ProfileData

// Use your theme colors here, falling back to standard if not defined
val TextLabelColor = Color(0xFF888888) // Light Gray for Labels
val TextValueColor = Color(0xFF111111) // Dark Black for Values

@Composable
fun AccountsInfoSection(profileData: ProfileData?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        if (profileData == null) {
            AccountsInfoShimmer()
        } else {
            AccountsInfoContent(profileData)
        }
    }
}

@Composable
private fun AccountsInfoContent(data: ProfileData) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp), // Generous padding as per screenshot
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Row 1: Name & Mobile
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f)) {
                AccountInfoField(label = "NAME", value = data.fullName ?: "-")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Box(modifier = Modifier.weight(1f)) {
                val mobile = if (!data.mobileIsd.isNullOrEmpty()) {
                    "${data.mobileIsd} ${data.mobile}"
                } else {
                    data.mobile ?: "-"
                }
                AccountInfoField(label = "MOBILE", value = mobile)
            }
        }

        // Row 2: Email & Country
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f)) {
                AccountInfoField(label = "EMAIL", value = data.email ?: "-")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Box(modifier = Modifier.weight(1f)) {
                val country = data.country ?: "-"
                AccountInfoField(label = "COUNTRY", value = country)
            }
        }

        // Row 3: Zip (Standalone)
        AccountInfoField(label = "ZIP", value = data.zip ?: "-")

        // Row 4: Address
        val fullAddress = listOfNotNull(data.street, data.city, data.state, data.zip)
            .joinToString(", ")
            .takeIf { it.isNotEmpty() } ?: data.address ?: "-"
            
        AccountInfoField(label = "ADDRESS", value = fullAddress)
    }
}

@Composable
private fun AccountInfoField(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextLabelColor,
                letterSpacing = 0.5.sp
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextValueColor,
                letterSpacing = 0.1.sp
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// --- Light Shimmer Implementation ---

@Composable
private fun AccountsInfoShimmer() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Row 1
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                ShimmerLabel()
                Spacer(modifier = Modifier.height(6.dp))
                ShimmerLine(widthFraction = 0.7f)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                ShimmerLabel()
                Spacer(modifier = Modifier.height(6.dp))
                ShimmerLine(widthFraction = 0.8f)
            }
        }

        // Row 2
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                ShimmerLabel()
                Spacer(modifier = Modifier.height(6.dp))
                ShimmerLine(widthFraction = 0.9f)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                ShimmerLabel()
                Spacer(modifier = Modifier.height(6.dp))
                ShimmerLine(widthFraction = 0.5f)
            }
        }

        // Row 3
        Column {
            ShimmerLabel()
            Spacer(modifier = Modifier.height(6.dp))
            ShimmerLine(widthFraction = 0.2f)
        }

        // Row 4
        Column {
            ShimmerLabel()
            Spacer(modifier = Modifier.height(6.dp))
            ShimmerLine(widthFraction = 1f)
            Spacer(modifier = Modifier.height(4.dp))
            ShimmerLine(widthFraction = 0.6f)
        }
    }
}

@Composable
private fun ShimmerLabel() {
    Box(
        modifier = Modifier
            .width(40.dp)
            .height(10.dp)
            .clip(RoundedCornerShape(4.dp))
            .lightShimmerEffect()
    )
}

@Composable
private fun ShimmerLine(widthFraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(16.dp)
            .clip(RoundedCornerShape(4.dp))
            .lightShimmerEffect()
    )
}

/**
 * Ultra-Light Shimmer for a subtle loading state
 */
private fun Modifier.lightShimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "Shimmer")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200), // Slightly slower for elegance
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerOffset"
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFF9F9F9), // Very Light Gray
                Color(0xFFFFFFFF), // White
                Color(0xFFF9F9F9), // Very Light Gray
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    ).onGloballyPositioned {
        size = it.size
    }
}