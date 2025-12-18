package com.example.limouserapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Google Sans Font Family - Using system default font (Google Sans on Android)
val GoogleSansFamily = FontFamily.Default

// Typography Styles
object AppTextStyles {
    // Headline Styles
    val headlineLarge = TextStyle(
        fontFamily = GoogleSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp,
        lineHeight = 44.sp, // 110% of 40sp
        letterSpacing = 0.sp
    )
    
    val headlineMedium = TextStyle(
        fontFamily = GoogleSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    )
    
    // Phone entry screen headlines
    val phoneEntryHeadline = TextStyle(
        fontFamily = GoogleSansFamily,
        fontWeight = FontWeight.SemiBold, // 590 weight
        fontSize = 28.sp,
        lineHeight = 42.sp, // 150% of 28sp
        letterSpacing = 0.sp
    )
    
    // Phone entry description text
    val phoneEntryDescription = TextStyle(
        fontFamily = GoogleSansFamily,
        fontWeight = FontWeight.Normal, // 400 weight
        fontSize = 14.sp,
        lineHeight = 18.sp, // 150% of 12sp
        letterSpacing = 0.sp
    )
    
    // Phone number input text
    val phoneNumberInput = TextStyle(
        fontFamily = GoogleSansFamily,
        fontWeight = FontWeight.Normal, // 400 weight
        fontSize = 16.sp,
//        lineHeight = 20.sp, // 20px line height
    )
    
    // Body Styles
    val bodyLarge = TextStyle(
        fontFamily = GoogleSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 20.8.sp, // 130% of 16sp
        letterSpacing = 0.sp
    )
    
    val bodyMedium = TextStyle(
        fontFamily = GoogleSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
        color = Color(0xFF121212)
    )
    
    // Button Styles - Professional button text
    val buttonLarge = TextStyle(
        fontFamily = GoogleSansFamily,
        fontWeight = FontWeight.SemiBold, // 590 weight
        fontSize = 16.sp,
        lineHeight = 20.sp, // 20px line height
        letterSpacing = (-0.23).sp // -0.23px letter spacing
    )
    // Add this inside AppTextStyles
    val captionSmall = TextStyle(
        fontFamily = GoogleSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        color = Color(0xFF9A9A9A)
    )
}

// Material 3 Typography with Google Sans
val AppTypography = Typography(
    headlineLarge = AppTextStyles.headlineLarge,
    headlineMedium = AppTextStyles.headlineMedium,
    bodyLarge = AppTextStyles.bodyLarge,
    bodyMedium = AppTextStyles.bodyMedium,
    labelLarge = AppTextStyles.buttonLarge
)
