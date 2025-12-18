package com.example.limouserapp.ui.liveride

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.limouserapp.ui.components.LiveRideMapView
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.border
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import timber.log.Timber

/**
 * COPIED / ADAPTED FROM DRIVER APP:
 * Same BottomSheet + map-behind-sheet layout and the same component designs.
 *
 * Differences vs driver app:
 * - Powered by `LiveRideViewModel.uiState`
 * - No driver-only actions (Arrived/Start/Complete). We show customer-facing info instead.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideInProgressScreen(
    bookingId: String?,
    onBack: () -> Unit = {},
    onNavigateToChat: (bookingId: Int) -> Unit = {},
    viewModel: LiveRideViewModel = hiltViewModel()
) {
    // Initialize viewModel with bookingId if available
    LaunchedEffect(bookingId) {
        bookingId?.let { viewModel.initializeWithBookingId(it) }
    }

    val uiState by viewModel.uiState.collectAsState()

    // Ride completion dialog state
    var showCompletionDialog by remember { mutableStateOf(false) }
    var hasShownCompletionDialog by remember { mutableStateOf(false) }

    // Show completion dialog when ride ends (only once)
    LaunchedEffect(uiState.activeRide?.status) {
        val status = uiState.activeRide?.status
        Timber.d("Ride status changed to: $status")
        if (status == "ended" && !hasShownCompletionDialog) {
            Timber.d("Showing ride completion dialog")
            showCompletionDialog = true
            hasShownCompletionDialog = true
        }
    }

    val context = LocalContext.current
    fun dial(number: String?) {
        val n = number?.trim().orEmpty()
        if (n.isBlank()) return
        context.startActivity(Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$n") })
    }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true
        )
    )

    val ride = uiState.activeRide
    Timber.d("RideInProgressScreen - activeRide: $ride, status: ${ride?.status}")
    if (ride == null) {
        Timber.d("RideInProgressScreen - activeRide is null, showing loading")
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val eta = uiState.estimatedTime.ifBlank { "—" }
    val dist = uiState.distance.ifBlank { "—" }
    val driverName = ride.driverName?.ifBlank { null } ?: "Driver"
    val driverPhone = ride.driverPhone?.ifBlank { null }
    val bookingNumber = "Booking #${ride.bookingId}"

    // Debug logging
    Timber.d("RideInProgressScreen - bookingId: ${ride.bookingId}, bookingNumber: $bookingNumber")

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContainerColor = Color.White,
        sheetShape = RideInProgressUiTokens.SheetShape,
        sheetShadowElevation = 16.dp,
        sheetPeekHeight = 230.dp,
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
            ) {
                Timber.d("RideInProgressScreen - UI status checks: isRideEnded=${uiState.isRideEnded}, isRideStarted=${uiState.isRideStarted}, isArrivedAtPickup=${uiState.isArrivedAtPickup}, isEnRouteToPickup=${uiState.isEnRouteToPickup}")
                when {
                    uiState.isRideEnded -> {
                        Timber.d("RideInProgressScreen - Showing ride ended UI")
                        StatusHeaderBanner("Ride Completed")
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                text = bookingNumber,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                            Spacer(Modifier.height(8.dp))
                            MetricHeader(eta = eta, distance = dist, subTitle = "Thanks for riding with us")
                            Spacer(Modifier.height(24.dp))
                            TripTimelineView(pickup = ride.pickupAddress, dropoff = ride.dropoffAddress)
                            Spacer(Modifier.height(24.dp))
                            ShareTripButton(onClick = { /* TODO: share */ })
                            Spacer(Modifier.height(32.dp))
                        }
                    }

                    uiState.isRideStarted -> {
                        StatusHeaderBanner("Ride In Progress")
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                text = bookingNumber,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                            Spacer(Modifier.height(8.dp))
                            MetricHeader(eta = eta, distance = dist, subTitle = "Heading to your destination")
                            Spacer(Modifier.height(24.dp))

                            ConnectWithPassengerRow(
                                passengerName = driverName,
                                onCall = { dial(driverPhone) },
                                onChat = { ride.bookingId.toIntOrNull()?.let { onNavigateToChat(it) } }
                            )
                            Spacer(Modifier.height(24.dp))

                            TripTimelineView(pickup = ride.pickupAddress, dropoff = ride.dropoffAddress)
                            Spacer(Modifier.height(24.dp))

                            if (uiState.dropoffArrivalDetected) {
                                StatusHeaderBanner("Arriving At Drop-off Location")
                            }
                            Spacer(Modifier.height(16.dp))

                            ShareTripButton(onClick = { /* TODO: share */ })
                            Spacer(Modifier.height(32.dp))
                        }
                    }

                    uiState.isArrivedAtPickup -> {
                        StatusHeaderBanner("Driver Arrived At Pickup")
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                text = bookingNumber,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                            Spacer(Modifier.height(8.dp))
                            MetricHeader(eta = eta, distance = dist, subTitle = "Meet your driver at pickup")
                            Spacer(Modifier.height(24.dp))

                            

                            // OTP for ride verification
                            Text(
                                text = "Your 4-digit ride PIN",
                                color = RideInProgressUiTokens.TextGrey
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "1234",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Provide this OTP to your driver for starting the ride. Do not share OTP on call or message. Please share OTP once you sit in the car.",
                                style = MaterialTheme.typography.bodySmall,
                                color = RideInProgressUiTokens.TextGrey,
                                lineHeight = 16.sp
                            )
                            Spacer(Modifier.height(24.dp))

                            TripTimelineView(pickup = ride.pickupAddress, dropoff = ride.dropoffAddress)
                            Spacer(Modifier.height(24.dp))
                            ShareTripButton(onClick = { /* TODO: share */ })
                            Spacer(Modifier.height(32.dp))
                        }
                    }

                    else -> {
                        // en_route_pu
                        val header = if (uiState.pickupArrivalDetected) {
                            "Driver Is Near The Pickup Location"
                        } else {
                            "Driver On The Way To Pickup Location"
                        }
                        StatusHeaderBanner(header)

                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                text = bookingNumber,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                            Spacer(Modifier.height(8.dp))
                            MetricHeader(eta = eta, distance = dist, subTitle = "Picking you up")
                            Spacer(Modifier.height(24.dp))

                            ConnectWithPassengerRow(
                                passengerName = driverName,
                                onCall = { dial(driverPhone) },
                                onChat = { ride.bookingId.toIntOrNull()?.let { onNavigateToChat(it) } }
                            )
                            Spacer(Modifier.height(24.dp))

                            TripTimelineView(pickup = ride.pickupAddress, dropoff = ride.dropoffAddress)
                            Spacer(Modifier.height(24.dp))

                            ShareTripButton(onClick = { /* TODO: share */ })
                            Spacer(Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Card(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(0.dp)) {
                LiveRideMapView(viewModel = viewModel, modifier = Modifier.fillMaxSize())
            }
            Spacer(modifier = Modifier.statusBarsPadding())
        }
    }

    // Ride completion dialog
    RideCompletionDialog(
        isPresented = showCompletionDialog,
        onRatingSubmitted = { rating, feedback ->
            // TODO: Submit rating to backend
            Timber.d("Rating submitted: $rating, feedback: $feedback")
        },
        onDismiss = {
            showCompletionDialog = false
            // Navigate back to dashboard
            onBack()
        }
    )
}

// Ride Completion Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RideCompletionDialog(
    isPresented: Boolean,
    onRatingSubmitted: (RatingType, String?) -> Unit,
    onDismiss: () -> Unit
) {
    if (!isPresented) return

    // Background overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable { /* Prevent dismissing */ }
    ) {
        // Dialog content
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .align(Alignment.Center),
            shape = RoundedCornerShape(20.dp)
        ) {
            RideCompletionDialogContent(
                onRatingSubmitted = onRatingSubmitted,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun RideCompletionDialogContent(
    onRatingSubmitted: (RatingType, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedRating by remember { mutableStateOf(RatingType.NONE) }
    var feedbackText by remember { mutableStateOf("") }
    var showFeedbackField by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
    ) {
        // Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            // Success icon
            Icon(
                imageVector = Icons.Default.ThumbUp,
                contentDescription = "Success",
                tint = Color.Green,
                modifier = Modifier.size(60.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = "Ride Completed!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "Hope you enjoyed your ride!",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }

        // Rating section
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Text(
                text = "How was your ride?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Like/Unlike buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Unlike button
                RatingButton(
                    ratingType = RatingType.UNLIKE,
                    selectedRating = selectedRating,
                    onRatingSelected = { rating ->
                        selectedRating = rating
                        showFeedbackField = (rating == RatingType.UNLIKE)
                        if (rating == RatingType.LIKE) {
                            feedbackText = ""
                        }
                    }
                )

                Spacer(modifier = Modifier.width(40.dp))

                // Like button
                RatingButton(
                    ratingType = RatingType.LIKE,
                    selectedRating = selectedRating,
                    onRatingSelected = { rating ->
                        selectedRating = rating
                        showFeedbackField = false
                        feedbackText = ""
                        // Auto-submit for like
                        onRatingSubmitted(RatingType.LIKE, null)
                        onDismiss()
                    }
                )
            }

            // Feedback text field (only shown for unlike)
            AnimatedVisibility(
                visible = showFeedbackField,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(
                        text = "Tell us what went wrong",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = feedbackText,
                        onValueChange = { feedbackText = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6,
                        placeholder = { Text("Please describe your experience...") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF5F5F5),
                            unfocusedContainerColor = Color(0xFFF5F5F5),
                            focusedIndicatorColor = Color.Gray,
                            unfocusedIndicatorColor = Color.Gray.copy(alpha = 0.3f),
                            cursorColor = Color.Gray
                        )
                    )
                }
            }
        }

        // Action buttons
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            // Submit button
            Button(
                onClick = {
                    if (selectedRating != RatingType.NONE && !isSubmitting) {
                        isSubmitting = true
                        val feedback = if (selectedRating == RatingType.UNLIKE) feedbackText else null
                        onRatingSubmitted(selectedRating, feedback)
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedRating != RatingType.NONE &&
                         !(selectedRating == RatingType.UNLIKE && feedbackText.trim().isEmpty()),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (selectedRating) {
                        RatingType.LIKE -> Color.Green
                        RatingType.UNLIKE -> Color.Red
                        else -> Color.Gray
                    }
                )
            ) {
                if (isSubmitting) {
                    LinearProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (selectedRating == RatingType.LIKE) Icons.Default.ThumbUp else Icons.Default.Send,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSubmitting) "Submitting..." else if (selectedRating == RatingType.LIKE) "Great!" else "Submit Feedback",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingButton(
    ratingType: RatingType,
    selectedRating: RatingType,
    onRatingSelected: (RatingType) -> Unit
) {
    val isSelected = selectedRating == ratingType
    val color = when (ratingType) {
        RatingType.LIKE -> if (isSelected) Color.Green else Color.Gray
        RatingType.UNLIKE -> if (isSelected) Color.Red else Color.Gray
        else -> Color.Gray
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onRatingSelected(ratingType) }
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = color.copy(alpha = if (isSelected) 0.1f else 0.05f),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 2.dp,
                    color = if (isSelected) color else Color.Gray.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (ratingType) {
                    RatingType.LIKE -> Icons.Default.ThumbUp
                    RatingType.UNLIKE -> Icons.Default.ThumbDown
                    else -> Icons.Default.ThumbUp
                },
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = when (ratingType) {
                RatingType.LIKE -> "Happy"
                RatingType.UNLIKE -> "Unhappy"
                else -> ""
            },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

private enum class RatingType {
    NONE, LIKE, UNLIKE
}

@Preview(showBackground = true)
@Composable
private fun RideInProgressScreenPreview() {
    Box(modifier = Modifier.background(Color.White)) {
        Text("Preview not wired (requires Hilt ViewModel)")
    }
}


