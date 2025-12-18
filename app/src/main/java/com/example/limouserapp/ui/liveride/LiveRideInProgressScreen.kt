package com.example.limouserapp.ui.liveride

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Live Ride In Progress Screen
 * Driver-app style (copied design): full-screen map + bottom sheet scaffold + shared UI components.
 */
@Composable
fun LiveRideInProgressScreen(
    bookingId: String?,
    viewModel: LiveRideViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
    onNavigateToChat: (bookingId: Int) -> Unit = {}
) {
    RideInProgressScreen(
        bookingId = bookingId,
        onBack = onBackClick,
        onNavigateToChat = onNavigateToChat,
        viewModel = viewModel
    )
}