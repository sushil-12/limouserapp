package com.example.limouserapp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import com.example.limouserapp.R
import com.example.limouserapp.ui.theme.LimoBlack

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var scale by remember { mutableStateOf(0.3f) }
    var alpha by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        // Start with fade in and zoom in simultaneously
        alpha = 1f
        scale = 1.0f
        kotlinx.coroutines.delay(1200)
        
        // Hold for a moment
        kotlinx.coroutines.delay(500)
        
        // Fade out
        alpha = 0f
        kotlinx.coroutines.delay(300)
        
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LimoBlack),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "1-800-LIMO.COM Logo",
            modifier = Modifier
                .scale(scale)
                .alpha(alpha)
                .fillMaxWidth(0.6f) // increases size relative to screen width
                .aspectRatio(1f)    // or set actual ratio of your logo, for example 1f means square
        )

    }
}


