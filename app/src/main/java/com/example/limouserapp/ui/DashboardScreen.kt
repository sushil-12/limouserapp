package com.example.limouserapp.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import timber.log.Timber
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed // Required for the shimmer effect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.limouserapp.ui.components.GoogleMapView
import com.example.limouserapp.ui.components.NavigationDrawer
import com.example.limouserapp.ui.components.SocketStatusIndicator
import com.example.limouserapp.ui.components.UserBookingCard
import com.example.limouserapp.ui.components.ContactBottomSheet
import com.example.limouserapp.ui.theme.LimoOrange
import com.example.limouserapp.ui.viewmodel.DashboardViewModel
import com.example.limouserapp.data.model.dashboard.UserProfile
import dagger.hilt.android.EntryPointAccessors
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.example.limouserapp.di.DirectionsServiceEntryPoint
import com.example.limouserapp.di.UserStateManagerEntryPoint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavHostController,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToBookings: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToCreateBooking: () -> Unit = {},
    onNavigateToMyCards: () -> Unit = {},
    onNavigateToInvoices: () -> Unit = {},
    onNavigateToEditBooking: (Int) -> Unit = {},
    onNavigateToScheduleRide: () -> Unit = {},
    onLogout: () -> Unit = {},
    hasLocationPermission: Boolean = false,
    onRequestLocationPermission: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToLiveRide: (bookingId: String?) -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToFaq: () -> Unit = {},
    onNavigateToTutorials: () -> Unit = {},
    onViewInvoice: (Int) -> Unit = {},
    initialEditBookingId: Int? = null,
    initialRepeatBookingId: Int? = null,
    initialIsReturnFlow: Boolean = false,
    shouldOpenDrawer: Boolean = false
) {
    val context = LocalContext.current
    val directionsService = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            DirectionsServiceEntryPoint::class.java
        ).directionsService()
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Get UserStateManager to check profile update flag
    val userStateManager = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            com.example.limouserapp.di.UserStateManagerEntryPoint::class.java
        ).userStateManager()
    }

    // Track the last route to detect when Dashboard becomes active
    var lastRoute by remember { mutableStateOf<String?>(null) }
    val currentRoute = navController.currentDestination?.route
    
    // Optimized: Only refresh profile when returning from AccountSettings (not on every Dashboard activation)
    LaunchedEffect(currentRoute) {
        Timber.d("DashboardScreen: Route changed - currentRoute=$currentRoute, lastRoute=$lastRoute")
        
        // Check if route contains "dashboard" (handles query parameters)
        val isDashboardRoute = currentRoute?.contains("dashboard") == true
        
        // Only check when route actually changes TO dashboard (not on every recomposition)
        if (isDashboardRoute && lastRoute?.contains("dashboard") != true) {
            Timber.d("DashboardScreen: Dashboard route activated, checking profile update flag")
            checkAndRefreshProfileIfNeeded(userStateManager, viewModel)
        }
        // Update last route
        lastRoute = currentRoute
    }
    
    // Also check on initial composition (fallback for when user stays on AccountSettings and navigates back)
    LaunchedEffect(Unit) {
        Timber.d("DashboardScreen: Initial composition, checking profile update flag")
        // Small delay to ensure UserStateManager is ready
        delay(100)
        checkAndRefreshProfileIfNeeded(userStateManager, viewModel)
    }

    // Open drawer if requested
    LaunchedEffect(shouldOpenDrawer) {
        if (shouldOpenDrawer && !uiState.isNavigationDrawerOpen) {
            viewModel.toggleNavigationDrawer()
        }
    }

    // Bottom Sheet State
    var sheetHeight by remember { mutableStateOf(0.35f) }
    var destination by remember { mutableStateOf("") }
    var showBookingSheet by remember { mutableStateOf(false) }

    // Booking Flow States
    var showTimeSelection by remember { mutableStateOf(false) }
    var showPaxInfo by remember { mutableStateOf(false) }
    var showMasterVehicleSelection by remember { mutableStateOf(false) }
    var showVehicleListing by remember { mutableStateOf(false) }
    var showVehicleDetails by remember { mutableStateOf(false) }
    var selectedMasterVehicle by remember { mutableStateOf<com.example.limouserapp.data.model.booking.Vehicle?>(null) }
    var selectedVehicle by remember { mutableStateOf<com.example.limouserapp.data.model.booking.Vehicle?>(null) }
    var filterSelection by remember { mutableStateOf(com.example.limouserapp.ui.booking.components.FilterSelectionState()) }
    var showComprehensiveBooking by remember { mutableStateOf(false) }
    var showBookingSuccess by remember { mutableStateOf(false) }
    var pendingRideData by remember { mutableStateOf<com.example.limouserapp.data.model.booking.RideData?>(null) }
    var pendingReservationData by remember { mutableStateOf<com.example.limouserapp.data.model.booking.ReservationData?>(null) }
    var pendingIsEditMode by remember { mutableStateOf(false) }

    // Booking Action States
    var editBookingId by remember { mutableStateOf<Int?>(null) }
    var repeatBookingId by remember { mutableStateOf<Int?>(null) }
    var isReturnFlow by remember { mutableStateOf(false) }
    var showContactSheet by remember { mutableStateOf(false) }
    var contactPhoneNumber by remember { mutableStateOf("") }
    var contactDriverName by remember { mutableStateOf("") }

    // Logic Handlers
    val handleEditBooking: (Int) -> Unit = remember {
        { bookingId ->
            editBookingId = bookingId
            // FIXED: Added missing selectedPickupAirport and selectedDestinationAirport
            pendingRideData = com.example.limouserapp.data.model.booking.RideData(
                serviceType = "one_way", pickupType = "city", dropoffType = "city",
                pickupLocation = "", destinationLocation = "", pickupDate = "", pickupTime = "",
                noOfPassenger = 1, noOfLuggage = 1, bookingHour = "0", noOfVehicles = 1,
                pickupLat = 0.0, pickupLong = 0.0, destinationLat = 0.0, destinationLong = 0.0,
                selectedPickupAirport = "", selectedDestinationAirport = ""
            )
            selectedVehicle = com.example.limouserapp.data.model.booking.Vehicle(
                id = 0, name = "Loading...", image = null, capacity = 0, luggage = 0, price = 0.0
            )
            showComprehensiveBooking = true
        }
    }

    val handleRepeatBooking: (Int, Boolean) -> Unit = remember {
        { bookingId, returnFlow ->
            repeatBookingId = bookingId
            isReturnFlow = returnFlow
            // FIXED: Added missing selectedPickupAirport and selectedDestinationAirport
            pendingRideData = com.example.limouserapp.data.model.booking.RideData(
                serviceType = "one_way", pickupType = "city", dropoffType = "city",
                pickupLocation = "", destinationLocation = "", pickupDate = "", pickupTime = "",
                noOfPassenger = 1, noOfLuggage = 1, bookingHour = "0", noOfVehicles = 1,
                pickupLat = 0.0, pickupLong = 0.0, destinationLat = 0.0, destinationLong = 0.0,
                selectedPickupAirport = "", selectedDestinationAirport = ""
            )
            selectedVehicle = com.example.limouserapp.data.model.booking.Vehicle(
                id = 0, name = "Loading...", image = null, capacity = 0, luggage = 0, price = 0.0
            )
            showComprehensiveBooking = true
        }
    }

    // Initial Action Effects
    LaunchedEffect(initialEditBookingId) {
        if (initialEditBookingId != null && !showComprehensiveBooking) handleEditBooking(initialEditBookingId)
    }

    LaunchedEffect(initialRepeatBookingId, initialIsReturnFlow) {
        if (initialRepeatBookingId != null && !showComprehensiveBooking) handleRepeatBooking(initialRepeatBookingId, initialIsReturnFlow)
    }

    // Active Ride Navigation
    val activeRide by viewModel.activeRide.collectAsStateWithLifecycle()
    val shouldNavigateToLiveRide by viewModel.shouldNavigateToLiveRide.collectAsStateWithLifecycle()

    LaunchedEffect(activeRide, shouldNavigateToLiveRide) {
        if ((activeRide?.bookingId?.isNotEmpty() == true) || shouldNavigateToLiveRide) {
            onNavigateToLiveRide(activeRide?.bookingId)
        }
    }

    // Clear Errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let { viewModel.clearError() }
    }

    // UI Structure
    Box(modifier = Modifier.fillMaxSize()) {

        // 1. Google Map Background
        GoogleMapView(
            mapRegion = uiState.mapRegion,
            userLocation = uiState.userLocation,
            carLocations = uiState.carLocations,
            onLocationUpdate = viewModel::updateUserLocation,
            onMapRegionUpdate = viewModel::updateMapRegion,
            hasLocationPermission = hasLocationPermission,
            onRequestLocationPermission = onRequestLocationPermission
        )

        // 2. Top UI Layers (Menu & Status)
        // Menu Button
        Card(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 16.dp, top = 16.dp)
                .align(Alignment.TopStart)
                .size(48.dp),
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            IconButton(
                onClick = { viewModel.toggleNavigationDrawer() },
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.Black)
            }
        }

        // Socket Indicator
        SocketStatusIndicator(
            connectionStatus = uiState.connectionStatus,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .align(Alignment.TopEnd)
                .padding(end = 16.dp, top = 16.dp)
        )

        // 3. Draggable Bottom Sheet
        val configuration = LocalConfiguration.current
        val screenHeightPx = with(LocalDensity.current) { configuration.screenHeightDp.dp.toPx() }
        val minHeight = 0.35f
        val maxHeight = 0.6f // Reduced from 0.85f (30% reduction)
        var currentPage by remember { mutableStateOf(0) }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(sheetHeight)
                .shadow(16.dp, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color.White)
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        val newHeight = (sheetHeight - (delta / screenHeightPx)).coerceIn(minHeight, maxHeight)
                        sheetHeight = newHeight
                    },
                    onDragStopped = {
                        val midpoint = (minHeight + maxHeight) / 2f
                        sheetHeight = if (sheetHeight >= midpoint) maxHeight else minHeight
                    }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Drag Handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                )

                Spacer(Modifier.height(20.dp))

                // Header with dynamic sizing
                HomeHeader(HomeUiState(uiState.userProfile))

                Spacer(Modifier.height(16.dp))

                // Search Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF5F5F5),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onNavigateToScheduleRide() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, null, tint = Color.Black)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Tap here to Schedule",
                            style = TextStyle(fontSize = 16.sp, color = Color.Gray)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Recent Bookings Section
                Text(
                    text = "Recent Bookings",
                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (uiState.isLoading) {
                    // Shimmer Loading Row
                    LazyRow(
                        contentPadding = PaddingValues(end = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(2) {
                            DashboardCardShimmer()
                        }
                    }
                } else if (uiState.upcomingBookings.isNotEmpty()) {
                    // Actual Bookings Row
                    val listState = rememberLazyListState()
                    LaunchedEffect(listState.firstVisibleItemIndex) {
                        currentPage = listState.firstVisibleItemIndex
                    }

                    LazyRow(
                        state = listState,
                        contentPadding = PaddingValues(end = 16.dp), // Add padding to end so last card isn't flush
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(uiState.upcomingBookings.take(5)) { booking ->
                            UserBookingCard(
                                booking = booking,
                                onEditBooking = { handleEditBooking(booking.bookingId) },
                                onRepeatBooking = { handleRepeatBooking(booking.bookingId, false) },
                                onReturnBooking = { handleRepeatBooking(booking.bookingId, true) },
                                onDriverPhoneClick = { phoneNumber ->
                                    contactPhoneNumber = phoneNumber
                                    contactDriverName = booking.fullDriverName.ifEmpty { "" }
                                    showContactSheet = true
                                },
                                onViewInvoice = onViewInvoice,
                                modifier = Modifier.fillParentMaxWidth(1f)
                            )
                        }
                    }

                    // Pagination Dots
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        repeat(uiState.upcomingBookings.take(5).size) { index ->
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (index == currentPage) Color.Black else Color.LightGray)
                            )
                        }
                    }
                } else {
                    // Empty State
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No recent bookings found", color = Color.Gray)
                    }
                }
            }
        }

        // 4. Overlays & Modals
        if (uiState.isNavigationDrawerOpen) {
            NavigationDrawer(
                userProfile = uiState.userProfile,
                isProfileLoading = uiState.profileLoading,
                onClose = { viewModel.closeNavigationDrawer() },
                onNavigateToProfile = { viewModel.closeNavigationDrawer(); onNavigateToProfile() },
                onNavigateToBookings = { viewModel.closeNavigationDrawer(); onNavigateToBookings() },
                onNavigateToSettings = { viewModel.closeNavigationDrawer(); onNavigateToSettings() },
                onNavigateToMyCards = { viewModel.closeNavigationDrawer(); onNavigateToMyCards() },
                onNavigateToInvoices = { viewModel.closeNavigationDrawer(); onNavigateToInvoices() },
                onNavigateToInbox = { viewModel.closeNavigationDrawer() },
                onNavigateToNotifications = { viewModel.closeNavigationDrawer(); onNavigateToNotifications() },
                onNavigateToFaq = { viewModel.closeNavigationDrawer(); onNavigateToFaq() },
                onNavigateToTutorials = { viewModel.closeNavigationDrawer(); onNavigateToTutorials() },
                onNavigateToDashboard = { openDrawer, isCreateBooking ->
                    viewModel.closeNavigationDrawer()
                    if (isCreateBooking) {
                        onNavigateToScheduleRide()
                        pendingRideData = null
                        showTimeSelection = false
                    }
                    if (openDrawer) viewModel.toggleNavigationDrawer()
                },
                onLogout = { viewModel.closeNavigationDrawer(); onLogout() }
            )
        }

        // Booking Flow Screens - ScheduleRideScreen now handled via navigation destination

        if (showTimeSelection && pendingRideData != null) {
            com.example.limouserapp.ui.booking.TimeSelectionScreen(
                rideData = pendingRideData!!,
                onDismiss = { showTimeSelection = false; showBookingSheet = true },
                onNavigateToPaxLuggageVehicle = { updated ->
                    pendingRideData = updated
                    showTimeSelection = false
                    showPaxInfo = true
                },
                directionsService = directionsService
            )
        }

        if (showPaxInfo && pendingRideData != null) {
            com.example.limouserapp.ui.booking.PaxLuggageVehicleScreen(
                rideData = pendingRideData!!,
                onDismiss = { showPaxInfo = false; showTimeSelection = true },
                onNext = { paxUpdated ->
                    pendingRideData = paxUpdated
                    showPaxInfo = false
                    showMasterVehicleSelection = true
                }
            )
        }

        if (showMasterVehicleSelection && pendingRideData != null) {
            com.example.limouserapp.ui.booking.MasterVehicleSelectionScreen(
                rideData = pendingRideData!!,
                onDismiss = { showMasterVehicleSelection = false; showPaxInfo = true },
                onMasterVehicleSelected = { mv ->
                    showMasterVehicleSelection = false
                    selectedMasterVehicle = mv
                    showVehicleListing = true
                },
                initialFilterSelection = filterSelection
            )
        }

        if (showVehicleListing && pendingRideData != null && selectedMasterVehicle != null) {
            com.example.limouserapp.ui.booking.VehicleListingScreen(
                rideData = pendingRideData!!,
                selectedMasterVehicleId = selectedMasterVehicle!!.id,
                initialFilterSelection = filterSelection,
                onDismiss = { showVehicleListing = false; showMasterVehicleSelection = true },
                onVehicleSelected = { v -> showVehicleListing = false; selectedVehicle = v; showVehicleDetails = true },
                onVehicleDetails = { v -> showVehicleListing = false; selectedVehicle = v; showVehicleDetails = true },
                onBookNow = { v -> showVehicleListing = false; selectedVehicle = v; showComprehensiveBooking = true }
            )
        }

        if (showVehicleDetails && pendingRideData != null && selectedVehicle != null) {
            com.example.limouserapp.ui.booking.VehicleDetailsScreen(
                rideData = pendingRideData!!,
                vehicle = selectedVehicle!!,
                onDismiss = { showVehicleDetails = false; showVehicleListing = true },
                onBookNow = { showVehicleDetails = false; showComprehensiveBooking = true }
            )
        }

        if (showComprehensiveBooking && pendingRideData != null && selectedVehicle != null) {
            com.example.limouserapp.ui.booking.ComprehensiveBookingScreen(
                rideData = pendingRideData!!,
                vehicle = selectedVehicle!!,
                onDismiss = { 
                    showComprehensiveBooking = false
                    showVehicleListing = true // Navigate back to vehicle listing screen
                    editBookingId = null
                    repeatBookingId = null 
                },
                onSuccess = { reservationData ->
                    showComprehensiveBooking = false
                    // Store reservation data and edit mode state for success screen
                    pendingReservationData = reservationData
                    // Note: editBookingId is cleared after success, so we need to track it before clearing
                    val wasEditMode = editBookingId != null
                    editBookingId = null
                    repeatBookingId = null
                    // Store edit mode state before showing success screen
                    pendingIsEditMode = wasEditMode
                    showBookingSuccess = true
                    viewModel.refreshDashboard()
                },
                isEditMode = editBookingId != null,
                editBookingId = editBookingId,
                isRepeatMode = repeatBookingId != null,
                repeatBookingId = repeatBookingId,
                isReturnFlow = isReturnFlow
            )
        }

        if (showBookingSuccess) {
            val hasDriver = (selectedVehicle?.driverInformation?.id ?: 0) > 0
            val isMasterVehicle = selectedVehicle?.isMasterVehicle ?: false
            com.example.limouserapp.ui.booking.BookingSuccessScreen(
                onOK = {
                    showBookingSuccess = false
                    pendingRideData = null
                    pendingReservationData = null
                    pendingIsEditMode = false
                    selectedVehicle = null
                    selectedMasterVehicle = null
                },
                hasDriverAssigned = hasDriver,
                isMasterVehicle = isMasterVehicle,
                isEditMode = pendingIsEditMode,
                reservationId = pendingReservationData?.reservationId,
                orderId = pendingReservationData?.orderId,
                returnReservationId = pendingReservationData?.returnReservationId
            )
        }

        ContactBottomSheet(
            phoneNumber = contactPhoneNumber,
            driverName = contactDriverName,
            isVisible = showContactSheet,
            onDismiss = { showContactSheet = false }
        )
    }
}

/**
 * Helper function to check and refresh profile if needed
 */
private fun checkAndRefreshProfileIfNeeded(
    userStateManager: com.example.limouserapp.data.local.UserStateManager,
    viewModel: DashboardViewModel
) {
    val profileUpdated = userStateManager.getProfileUpdatedFromAccountSettings()
    Timber.d("DashboardScreen: Profile update flag = $profileUpdated")
    
    if (profileUpdated) {
        Timber.d("DashboardScreen: Profile was updated from AccountSettings, refreshing profile...")
        // Clear flag first to prevent multiple refreshes
        userStateManager.setProfileUpdatedFromAccountSettings(false)
        // Refresh user profile from API to get updated name
        viewModel.refreshUserProfile()
    } else {
        Timber.d("DashboardScreen: Profile update flag is false, skipping refresh")
    }
}

/**
 * Home Header Component with Dynamic Sizing
 * Adapts to different screen sizes for optimal readability
 */
data class HomeUiState(
    val userProfile: UserProfile?
)

@Composable
fun HomeHeader(uiState: HomeUiState) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    // 1. Dynamic Sizing Logic
    val titleSize = when {
        screenWidth < 360.dp -> 22.sp // Small devices (very tight)
        screenWidth < 400.dp -> 24.sp // Standard small/medium phones
        else -> 28.sp                 // Large phones (Pixel 7 Pro, S23 Ultra)
    }
    
    val subtitleSize = if (screenWidth < 360.dp) 14.sp else 16.sp

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Greeting
        Text(
            text = "Hey there, ${uiState.userProfile?.firstName ?: "there"}",
            style = TextStyle(
                fontSize = subtitleSize,
                fontWeight = FontWeight.Medium,
                color = Color.Gray,
                letterSpacing = 0.5.sp // Adds slight breathability
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis // Prevents name from breaking layout
        )

        Spacer(Modifier.height(4.dp)) // Slightly reduced for tighter cohesion

        // Headline
        Text(
            text = "Schedule your next booking",
            style = TextStyle(
                fontSize = titleSize,
                fontWeight = FontWeight.Bold,
                color = LimoOrange,
                // 2. Critical: Set LineHeight to handle wrapping gracefully
                lineHeight = titleSize * 1.3f 
            )
        )
    }
}

/**
 * Shimmer for Dashboard Card
 * Mimics the exact layout of UserBookingCard for smooth loading
 */
@Composable
private fun DashboardCardShimmer() {
    Card(
        modifier = Modifier
            .width(340.dp) // Match the fixed width of the real card
            .height(300.dp), // Approx height of a booking card
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .dashboardShimmerEffect()
            )
            // Summary Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(Color(0xFFF5F5F5))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(50.dp, 12.dp).clip(RoundedCornerShape(4.dp)).dashboardShimmerEffect())
                    Box(modifier = Modifier.size(60.dp, 12.dp).clip(RoundedCornerShape(4.dp)).dashboardShimmerEffect())
                    Box(modifier = Modifier.size(50.dp, 12.dp).clip(RoundedCornerShape(4.dp)).dashboardShimmerEffect())
                }
            }
            // Route
            Column(modifier = Modifier.padding(16.dp)) {
                Box(modifier = Modifier.fillMaxWidth(0.9f).height(14.dp).clip(RoundedCornerShape(4.dp)).dashboardShimmerEffect())
                Spacer(Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth(0.7f).height(14.dp).clip(RoundedCornerShape(4.dp)).dashboardShimmerEffect())
            }
            // Driver
            Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).dashboardShimmerEffect())
                Spacer(Modifier.width(12.dp))
                Column {
                    Box(modifier = Modifier.width(120.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).dashboardShimmerEffect())
                    Spacer(Modifier.height(8.dp))
                    Box(modifier = Modifier.width(80.dp).height(10.dp).clip(RoundedCornerShape(4.dp)).dashboardShimmerEffect())
                }
            }
            Spacer(Modifier.weight(1f))
            // Buttons
            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(8.dp)).dashboardShimmerEffect())
                Box(modifier = Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(8.dp)).dashboardShimmerEffect())
            }
        }
    }
}

// Private extension for dashboard shimmer to avoid conflicts
private fun Modifier.dashboardShimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "DashShimmer")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(animation = tween(1000)),
        label = "Offset"
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFEEEEEE),
                Color(0xFFFAFAFA),
                Color(0xFFEEEEEE),
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    ).onGloballyPositioned { size = it.size }
}