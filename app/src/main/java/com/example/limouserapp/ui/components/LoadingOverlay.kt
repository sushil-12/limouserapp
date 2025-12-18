package com.example.limouserapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.limouserapp.ui.theme.AppColors

/**
 * Professional full-screen loading overlay
 * Similar to Uber's loading state - elegant and non-intrusive
 */
@Composable
fun LoadingOverlay(
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    // Smooth fade animation
    val alpha by animateFloatAsState(
        targetValue = if (isLoading) 0.85f else 0f,
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ),
        label = "loading_overlay_alpha"
    )

    // Scale animation for the spinner
    val scale by animateFloatAsState(
        targetValue = if (isLoading) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "loading_spinner_scale"
    )

    if (alpha > 0f) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    color = Color.Black.copy(alpha = alpha)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Circular progress indicator with Uber-style appearance
            CircularProgressIndicator(
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                color = AppColors.LimoOrange,
                strokeWidth = 3.5.dp,
                trackColor = Color.White.copy(alpha = 0.2f)
            )
        }
    }
}

