package com.example.limouserapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.limouserapp.data.model.dashboard.UserBooking
import com.example.limouserapp.ui.theme.LimoOrange
import com.example.limouserapp.ui.utils.BookingFormatter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min
import com.example.limouserapp.ui.components.ShimmerBox
import com.example.limouserapp.ui.components.ShimmerText

/**
 * Bottom sheet for dashboard with booking slider and schedule ride functionality
 * Uber-style draggable bottom sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardBottomSheet(
    isExpanded: Boolean,
    upcomingBookings: List<UserBooking>,
    isLoading: Boolean,
    onToggleExpansion: () -> Unit,
    onRefresh: () -> Unit,
    onCreateBooking: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var sheetHeight by remember { mutableStateOf(0.25f) }
    var isDragging by remember { mutableStateOf(false) }
    
    // Update sheet height based on expansion state
    LaunchedEffect(isExpanded) {
        if (!isDragging) {
            sheetHeight = if (isExpanded) 0.7f else 0.25f
        }
    }
    
    val animatedHeight by animateFloatAsState(
        targetValue = sheetHeight,
        animationSpec = tween(300),
        label = "bottom_sheet_height"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(animatedHeight)
            .background(
                Color.White,
                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { 
                        isDragging = false
                        // Snap to nearest position
                        sheetHeight = if (sheetHeight > 0.5f) 0.7f else 0.25f
                    }
                ) { _, dragAmount ->
                    val dragAmountDp = with(density) { dragAmount.y.toDp() }
                    val screenHeightDp = with(density) { size.height.toDp() }
                    val dragPercentage = -dragAmountDp / screenHeightDp
                    
                    sheetHeight = (sheetHeight + dragPercentage).coerceIn(0.15f, 0.8f)
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Handle bar
            HandleBar(
                onToggleExpansion = onToggleExpansion,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (sheetHeight > 0.5f) {
                // Expanded content
                ExpandedContent(
                    upcomingBookings = upcomingBookings,
                    isLoading = isLoading,
                    onRefresh = onRefresh,
                    onCreateBooking = onCreateBooking
                )
            } else {
                // Collapsed content
                CollapsedContent(
                    upcomingBookings = upcomingBookings,
                    onCreateBooking = onCreateBooking
                )
            }
        }
    }
}

/**
 * Handle bar for dragging the bottom sheet
 */
@Composable
private fun HandleBar(
    onToggleExpansion: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(40.dp)
            .height(4.dp)
            .background(
                Color.Gray.copy(alpha = 0.5f),
                RoundedCornerShape(2.dp)
            )
            .clickable { onToggleExpansion() }
    )
}

/**
 * Collapsed content showing quick overview
 */
@Composable
private fun CollapsedContent(
    upcomingBookings: List<UserBooking>,
    onCreateBooking: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Quick info
        Column {
            Text(
                text = "Upcoming Rides",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${upcomingBookings.size} scheduled",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
        
        // Create booking button
        Button(
            onClick = onCreateBooking,
            colors = ButtonDefaults.buttonColors(containerColor = LimoOrange)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Create Booking",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Book Now")
        }
    }
}

/**
 * Expanded content with full booking list
 */
@Composable
private fun ExpandedContent(
    upcomingBookings: List<UserBooking>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onCreateBooking: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Bookings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Refresh button
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = LimoOrange
                    )
                }
                
                // Create booking button
                Button(
                    onClick = onCreateBooking,
                    colors = ButtonDefaults.buttonColors(containerColor = LimoOrange)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create Booking",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Book")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Bookings list - Horizontal scroll
        if (isLoading) {
            LoadingState()
        } else if (upcomingBookings.isEmpty()) {
            EmptyState(onCreateBooking = onCreateBooking)
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                items(upcomingBookings) { booking ->
                    UserBookingCard(
                        booking = booking,
                        onEditBooking = null, // No edit in dashboard
                        onCancelBooking = null, // No cancel in dashboard
                        onDriverPhoneClick = null, // No phone in dashboard
                        modifier = Modifier
//                            .width(340.dp) // Fixed width for horizontal scroll
                            .fillMaxHeight() // Take available height
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}

// BookingCard component removed - now using UserBookingCard instead

/**
 * Loading state - show shimmer booking cards
 */
@Composable
private fun LoadingState() {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        items(2) {
            BookingCardShimmerHorizontal()
        }
    }
}

@Composable
private fun BookingCardShimmerHorizontal() {
    Card(
        modifier = Modifier
            .width(340.dp)
            .fillMaxHeight(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            // Top Header shimmer
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            )
            
            // Booking Summary shimmer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE6E6E6))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ShimmerText(modifier = Modifier.width(60.dp), height = 14.dp)
                ShimmerText(modifier = Modifier.width(80.dp), height = 14.dp)
                ShimmerText(modifier = Modifier.width(100.dp), height = 14.dp)
            }
            
            // Route Details shimmer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ShimmerBox(
                        modifier = Modifier.size(16.dp),
                        shape = CircleShape
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        ShimmerText(modifier = Modifier.fillMaxWidth(0.8f), height = 14.dp)
                        ShimmerText(modifier = Modifier.fillMaxWidth(0.6f), height = 12.dp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ShimmerBox(
                        modifier = Modifier.size(16.dp),
                        shape = CircleShape
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        ShimmerText(modifier = Modifier.fillMaxWidth(0.8f), height = 14.dp)
                        ShimmerText(modifier = Modifier.fillMaxWidth(0.6f), height = 12.dp)
                    }
                }
            }
        }
    }
}

/**
 * Empty state
 */
@Composable
private fun EmptyState(
    onCreateBooking: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = "No Bookings",
            modifier = Modifier.size(64.dp),
            tint = Color.Gray.copy(alpha = 0.5f)
        )
        
        Text(
            text = "No upcoming bookings",
            style = MaterialTheme.typography.titleMedium,
            color = Color.Gray
        )
        
        Text(
            text = "Book your first ride to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        
        Button(
            onClick = onCreateBooking,
            colors = ButtonDefaults.buttonColors(containerColor = LimoOrange)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Create Booking",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Book Now")
        }
    }
}

// Helper functions removed - now using BookingFormatter utilities
