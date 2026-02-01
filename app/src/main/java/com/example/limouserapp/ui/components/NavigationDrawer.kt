package com.example.limouserapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.limouserapp.R
import com.example.limouserapp.data.model.dashboard.UserProfile
import com.example.limouserapp.ui.theme.LimoRed
import com.example.limouserapp.ui.theme.LimoWhite
import timber.log.Timber

/**
 * Navigation drawer for dashboard
 * Enhanced version matching iOS UserDashboardDrawerMenu
 */
@Composable
fun NavigationDrawer(
    userProfile: UserProfile?,
    isProfileLoading: Boolean,
    onClose: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToBookings: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToMyCards: () -> Unit = {},
    onNavigateToInvoices: () -> Unit = {},
    onNavigateToInbox: () -> Unit = {},
    onNavigateToCreateBooking: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToFaq: () -> Unit = {},
    onNavigateToTutorials: () -> Unit = {},
    onNavigateToDashboard: (openDrawer: Boolean, isCreateBooking: Boolean) -> Unit = { _, _ -> },
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = true,
        enter = slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = tween(300)
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = tween(300)
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onClose() }
        ) {
            // Drawer content - full screen from left
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .clickable(enabled = false) { /* Prevent closing when clicking drawer content */ }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(20.dp)
                ) {
                    // User Profile Section with close button
                    UserProfileSectionWithClose(
                        userProfile = userProfile,
                        isLoading = isProfileLoading,
                        onClose = onClose,
                        onProfileClick = {
                            onClose()
                            onNavigateToProfile()
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Menu Items
                    Column(
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        UserMenuOption(
                            title = "My Bookings",
                            onClick = {
                                onClose()
                                onNavigateToBookings()
                            }
                        )
                        
                        UserMenuOption(
                            title = "Create Booking",
                            onClick = {
                                onClose()
                                onNavigateToDashboard(false, true) // Navigate to Dashboard to initiate Create Booking
                            }
                        )
                        
                        UserMenuOption(
                            title = "My Cards",
                            onClick = {
                                onClose()
                                onNavigateToMyCards()
                            }
                        )
                        
                        UserMenuOption(
                            title = "Invoices",
                            onClick = {
                                onClose()
                                onNavigateToInvoices()
                            }
                        )
                        
//                        UserMenuOption(
//                            title = "Inbox",
//                            badge = "3", // TODO: Get actual badge count
//                            onClick = {
//                                onClose()
//                                onNavigateToInbox()
//                            }
//                        )
                        
                        UserMenuOption(
                            title = "Notifications",
                            onClick = {
                                onClose()
                                onNavigateToNotifications()
                            }
                        )
                        
                        UserMenuOption(
                            title = "Account Settings",
                            onClick = {
                                onClose()
                                onNavigateToSettings()
                            }
                        )
                        
                        UserMenuOption(
                            title = "FAQ",
                            onClick = {
                                onClose()
                                onNavigateToFaq()
                            }
                        )

                        UserMenuOption(
                            title = "Tutorials",
                            onClick = {
                                onClose()
                                onNavigateToTutorials()
                            }
                        )
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // Logout option
                        UserLogoutOption(
                            onClick = {
                                onLogout()
                                onClose()
                            }
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .background(LimoWhite)
                        )
                    }
                    
                    // App Version
                    Text(
                        text = "Version 1.0.0",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 16.dp)
                    )
                }
            }
        }
    }
}

/**
 * User profile section matching Figma design
 */
@Composable
private fun UserProfileSectionWithClose(
    userProfile: UserProfile?,
    isLoading: Boolean,
    onClose: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        // User Info Column
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (isLoading) {
                // Shimmer loading placeholders
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(16.dp)
                        .background(Color.Gray.copy(alpha = 0.3f))
                )
                Spacer(modifier = Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(12.dp)
                        .background(Color.Gray.copy(alpha = 0.3f))
                )
            } else {
                // Name and Rating Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // User Name
                    Text(
                        text = userProfile?.fullName ?: "User",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF121212),
                        lineHeight = 24.sp, // 150% of 16sp = 24sp
                        modifier = Modifier.weight(1f),
                    )
                    
                    // Rating Badge
//                    if (userProfile?.rating != null && userProfile.rating > 0) {
//                        Row(
//                            modifier = Modifier
//                                .background(
//                                    Color(0xFFF3933D).copy(alpha = 0.1f),
//                                    RoundedCornerShape(4.dp)
//                                )
//                                .padding(horizontal = 4.dp),
//                            verticalAlignment = Alignment.CenterVertically,
//                            horizontalArrangement = Arrangement.spacedBy(2.dp)
//                        ) {
//                            Icon(
//                                imageVector = Icons.Default.Star,
//                                contentDescription = "Rating",
//                                modifier = Modifier.size(14.dp),
//                                tint = Color(0xFFF3933D)
//                            )
//                            Text(
//                                text = String.format("%.1f", userProfile.rating),
//                                fontSize = 12.sp,
//                                fontWeight = FontWeight.Medium, // Font weight 510
//                                color = Color(0xFFF3933D),
//                                lineHeight = 18.sp // 150% of 12sp = 18sp
//                            )
//                        }
//                    }
                }
                
                // Phone Number
                val phoneText = userProfile?.let {
                    when {
                        !it.phoneCountryCode.isNullOrEmpty() -> "${it.phoneCountryCode} ${it.phone}"
                        !it.phone.isNullOrEmpty() -> it.phone
                        else -> ""
                    }
                } ?: ""
                Timber.d("Phone Text: $phoneText",  )
                
                Text(
                    text = phoneText.ifEmpty { "No phone number" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold, // Font weight 590
                    color = Color(0xFF121212).copy(alpha = 0.6f),
                    lineHeight = 18.sp // 150% of 12sp = 18sp
                )
            }
        }
        
        // Close button
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(48.dp)
                .background(LimoRed, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                modifier = Modifier.size(18.dp),
                tint = LimoWhite
            )
        }
    }
}

/**
 * Menu option component matching Figma design
 */
@Composable
private fun UserMenuOption(
    title: String,
    badge: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .height(54.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        //8709411234
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Menu item text with Figma typography
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold, // Font weight 590 (SemiBold)
                color = Color(0xFF121212),
                lineHeight = 30.sp, // 150% of 20sp = 30sp
                modifier = Modifier.weight(1f)
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge
                badge?.let { badgeText ->
                    Text(
                        text = badgeText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        modifier = Modifier
                            .background(Color(0xFFFF9800), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                
                // Arrow icon (rotated left arrow)
                Icon(
                    painter = painterResource(id = R.drawable.icon_left_menu_icon),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.Unspecified // Use original color from XML (#D9D9D9)
                )
            }
        }
    }
}

/**
 * Logout option component matching iOS design
 */
@Composable
private fun UserLogoutOption(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.line_md_logout),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Color.Unspecified // Use original color from XML (#CE0000)
        )
        
        Text(
            text = "Logout",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFCE0000),
        )
        
        Spacer(modifier = Modifier.weight(1f))

    }
}
