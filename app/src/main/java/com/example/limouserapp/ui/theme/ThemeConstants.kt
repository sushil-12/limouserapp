package com.example.limouserapp.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Color Constants - Using original brand colors
object AppColors {
    val LimoBlack = Color(0xFF121212)
    val LimoOrange = Color(0xFFF3933D)
    val White = Color(0xFFFFFFFF)
    val Transparent = Color(0x00000000)
    
    // Country picker colors
    val CountryPickerBackground = Color(0xFFF3F3F3)
    
    // Gradient Colors - Minimal gradient
    val GradientStart = Color.Transparent
    val GradientMiddle = Color(0x20000000)
    val GradientEnd = Color(0x40000000)
}

// Spacing Constants - Responsive design
object AppSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    val xxxxl = 40.dp
    val xxxxxl = 48.dp
    val xxxxxxl = 64.dp
    val xxxxxxxl = 80.dp
}

// Dimension Constants - Responsive design
object AppDimensions {
    // Responsive button dimensions
    val buttonHeight = 52.dp // Compact professional height
    val buttonCornerRadius = 12.dp // Modern rounded corners
    val iconSize = 16.dp
    val MainScreenPadding = 32.dp
    val contentPadding = 24.dp
    val bottomPadding = 80.dp
    
    // Responsive top padding for different screen sizes
    val topPaddingSmall = 32.dp
    val topPaddingMedium = 48.dp
    val topPaddingLarge = 64.dp
    
    // Responsive button padding - Reduced for better appearance
    val buttonPaddingVertical = 8.dp // Compact vertical padding
    val buttonPaddingHorizontal = 4.dp // Balanced horizontal padding
    
    // Responsive breakpoints
    val maxContentWidth = 400.dp
    val minContentPadding = 16.dp
    val maxContentPadding = 32.dp
    
    // Button responsive constraints
    val buttonMinHeight = 48.dp
    val buttonMaxHeight = 56.dp
    val buttonMaxWidth = 280.dp // Prevent button from being too wide on large screens
    
    // Country picker dimensions
    val countryPickerWidth = 78.dp
    val countryPickerHeight = 56.dp // Match phone input height
    val countryPickerCornerRadius = 8.dp
    val countryPickerPaddingVertical = 8.dp // Increased to match phone input
    val countryPickerPaddingHorizontal = 10.dp
    val countryPickerGap = 8.dp
    
    // Phone input field dimensions - Fixed for proper text display
    val phoneInputWidth = 275.dp
    val phoneInputHeight = 48.dp // Increased height to prevent text cutoff
    val phoneInputCornerRadius = 8.dp
    val phoneInputPaddingVertical = 8.dp // Increased vertical padding
    val phoneInputPaddingHorizontal = 12.dp // Increased for better text spacing
    val phoneInputBorderWidth = 1.dp
}

// Text Size Constants
object AppTextSizes {
    val headlineLarge = 40.sp
    val headlineMedium = 32.sp
    val bodyLarge = 16.sp
    val bodyMedium = 14.sp
    val buttonLarge = 16.sp
}

// Line Height Constants (110% of font size)
object AppLineHeights {
    val headlineLarge = 44.sp // 110% of 40sp
    val headlineMedium = 36.sp // 110% of 32sp
    val bodyLarge = 22.sp // 110% of 16sp
    val bodyMedium = 20.sp // 110% of 14sp
    val buttonLarge = 20.sp // 110% of 16sp
}
