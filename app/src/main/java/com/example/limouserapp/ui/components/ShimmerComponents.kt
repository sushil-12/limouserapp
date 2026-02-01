package com.example.limouserapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Shimmer effect colors - lighter version
 */
private val shimmerColors = listOf(
    Color.White.copy(alpha = 0.2f),
    Color.White.copy(alpha = 0.4f),
    Color.White.copy(alpha = 0.2f)
)

/**
 * Lighter shimmer effect colors for subtle loading
 */
private val lightShimmerColors = listOf(
    Color.White.copy(alpha = 0.1f),
    Color.White.copy(alpha = 0.2f),
    Color.White.copy(alpha = 0.1f)
)

/**
 * Base shimmer modifier that applies the shimmer animation
 */
@Composable
fun Modifier.shimmerEffect(light: Boolean = false): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerTranslateAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val colors = if (light) lightShimmerColors else shimmerColors

    return this.then(
        Modifier.background(
            brush = Brush.linearGradient(
                colors = colors,
                start = Offset(shimmerTranslateAnim - 400f, shimmerTranslateAnim - 400f),
                end = Offset(shimmerTranslateAnim, shimmerTranslateAnim)
            )
        )
    )
}

/**
 * Shimmer box component - animated shimmer box
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: androidx.compose.foundation.shape.RoundedCornerShape = RoundedCornerShape(8.dp),
    light: Boolean = true // Default to lighter shimmers
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color.Gray.copy(alpha = if (light) 0.15f else 0.2f))
            .shimmerEffect(light = light)
    )
}

/**
 * Shimmer text placeholder - shimmer placeholder for text
 */
@Composable
fun ShimmerText(
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 16.dp
) {
    ShimmerBox(
        modifier = modifier.height(height),
        shape = RoundedCornerShape(4.dp)
    )
}

/**
 * Shimmer card placeholder - shimmer placeholder for cards
 */
@Composable
fun ShimmerCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {
        Spacer(modifier = Modifier.height(12.dp))
        ShimmerText(modifier = Modifier.fillMaxWidth(0.8f), height = 16.dp)
        Spacer(modifier = Modifier.height(8.dp))
        ShimmerText(modifier = Modifier.fillMaxWidth(0.6f), height = 14.dp)
        Spacer(modifier = Modifier.height(8.dp))
        ShimmerText(modifier = Modifier.fillMaxWidth(0.9f), height = 14.dp)
    }
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(Color.Gray.copy(alpha = 0.1f))
        ) {
            content()
        }
    }
}

/**
 * Shimmer list item - for list loading states
 */
@Composable
fun ShimmerListItem(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon/Image placeholder
        ShimmerBox(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Text content
        Column(modifier = Modifier.weight(1f)) {
            ShimmerText(
                modifier = Modifier.fillMaxWidth(0.7f),
                height = 16.dp
            )
            Spacer(modifier = Modifier.height(6.dp))
            ShimmerText(
                modifier = Modifier.fillMaxWidth(0.5f),
                height = 14.dp
            )
        }
    }
}

/**
 * Circular shimmer component - replaces CircularProgressIndicator
 */
@Composable
fun ShimmerCircle(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 24.dp,
    strokeWidth: androidx.compose.ui.unit.Dp = 2.dp,
    light: Boolean = true
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .shimmerEffect(light = light)
            .background(Color.Transparent)
            .border(
                width = strokeWidth,
                brush = Brush.linearGradient(
                    colors = if (light) lightShimmerColors else shimmerColors
                ),
                shape = androidx.compose.foundation.shape.CircleShape
            )
    )
}


/**
 * Button shimmer - for loading states in buttons
 */
@Composable
fun ShimmerButton(
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 50.dp,
    width: androidx.compose.ui.unit.Dp? = null
) {
    val buttonModifier = if (width != null) {
        modifier.width(width).height(height)
    } else {
        modifier.fillMaxWidth().height(height)
    }

    ShimmerBox(
        modifier = buttonModifier,
        shape = RoundedCornerShape(50)
    )
}

