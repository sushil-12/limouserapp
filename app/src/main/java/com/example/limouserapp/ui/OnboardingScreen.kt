package com.example.limouserapp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.limouserapp.R
import com.example.limouserapp.ui.theme.*

@Composable
fun OnboardingScreen(onGetStarted: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Background image
        Image(
            painter = painterResource(id = R.drawable.onboarding_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Dark gradient overlay for better text visibility
        Box(
            modifier = Modifier
                .fillMaxSize()
               .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            AppColors.GradientStart,
                            AppColors.GradientMiddle,
                            AppColors.GradientEnd
                        ),
                        startY = 0.4f,
                        endY = 1f
                    )
                )
        )

        // Content positioned at bottom left - Responsive layout
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = AppDimensions.contentPadding,
                    end = AppDimensions.contentPadding,
                    bottom = AppDimensions.bottomPadding
                )
                .align(Alignment.BottomStart)
        ) {
            // Main headline - "Sit back & go"
            Text(
                text = "Sit back & go",
                style = AppTextStyles.headlineLarge.copy(color = AppColors.White),
                modifier = Modifier.fillMaxWidth()
            )

            // Sub headline - "wherever you want"
            Text(
                text = "wherever you want",
                style = AppTextStyles.headlineLarge.copy(color = AppColors.LimoOrange),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(AppSpacing.lg))

            // Description text - Responsive with proper line height
            Text(
                text = "Book a ride that offers exceptional value and a top-notch experience with 1800limo.",
                style = AppTextStyles.bodyLarge.copy(color = AppColors.White),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(end = AppSpacing.md)
            )

            Spacer(Modifier.height(AppSpacing.xxxxxl))

            // CTA Button with arrow - Fully responsive design
            Button(
                onClick = onGetStarted,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.LimoOrange),
                shape = RoundedCornerShape(AppDimensions.buttonCornerRadius),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = AppDimensions.buttonMaxWidth)
                    .heightIn(
                        min = AppDimensions.buttonMinHeight,
                        max = AppDimensions.buttonMaxHeight
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = AppDimensions.buttonPaddingHorizontal,
                            vertical = AppDimensions.buttonPaddingVertical
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Let's get started",
                        style = AppTextStyles.buttonLarge.copy(color = AppColors.White)
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.right_arrow),
                        contentDescription = "Arrow",
                        tint = AppColors.White,
                        modifier = Modifier.size(AppDimensions.iconSize)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreview() {
    OnboardingScreen(onGetStarted = {})
}
