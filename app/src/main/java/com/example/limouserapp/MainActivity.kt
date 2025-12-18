package com.example.limouserapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import android.os.Build
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.limouserapp.ui.OnboardingScreen
import com.example.limouserapp.ui.SplashScreen
import com.example.limouserapp.ui.PhoneEntryScreen
import com.example.limouserapp.ui.OtpScreen
import com.example.limouserapp.ui.AddBasicDetailsScreen
import com.example.limouserapp.ui.AddCreditCardScreen
import com.example.limouserapp.ui.DashboardScreen
import com.example.limouserapp.ui.MyBookingsScreen
import com.example.limouserapp.ui.MyInvoicesScreen
import com.example.limouserapp.ui.InvoiceWebViewScreen
import com.example.limouserapp.ui.WebViewScreen
import com.example.limouserapp.ui.MyCardsScreen
import com.example.limouserapp.ui.AccountSettingsScreen
import com.example.limouserapp.ui.liveride.LiveRideInProgressScreen
import com.example.limouserapp.ui.chat.ChatScreen
import com.example.limouserapp.ui.components.NotificationContainer
import com.example.limouserapp.ui.components.LoadingOverlay
import com.example.limouserapp.data.socket.SocketService
import com.example.limouserapp.ui.theme.LimouserappTheme
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import com.example.limouserapp.di.LoadingStateManagerEntryPoint
import com.example.limouserapp.di.UserStateManagerEntryPoint
import com.example.limouserapp.di.LogoutUseCaseEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.example.limouserapp.ui.NotificationScreen
import com.example.limouserapp.ui.theme.LimoOrange
import com.example.limouserapp.ui.theme.LimoBlack
import com.example.limouserapp.ui.viewmodel.SplashViewModel
import com.example.limouserapp.ui.viewmodel.PhoneEntryViewModel
import com.example.limouserapp.ui.viewmodel.OtpViewModel
import com.example.limouserapp.ui.viewmodel.BasicDetailsViewModel
import com.example.limouserapp.ui.viewmodel.CreditCardViewModel
import com.example.limouserapp.ui.state.PhoneEntryUiEvent
import timber.log.Timber
import com.example.limouserapp.ui.state.BasicDetailsUiEvent
import com.example.limouserapp.ui.state.CreditCardUiEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.example.limouserapp.ui.navigation.NavEvent
import com.example.limouserapp.ui.navigation.NavigationEventBus

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var navigationEventBus: NavigationEventBus
    
    // Permission request launchers
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        // Update permission state
        _hasLocationPermission.value = fineLocationGranted || coarseLocationGranted
    }
    
    // Notification permission launcher (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.util.Log.d("MainActivity", "✅ Notification permission granted")
        } else {
            android.util.Log.w("MainActivity", "⚠️ Notification permission denied")
        }
    }
    
    // Permission state
    private val _hasLocationPermission = mutableStateOf(false)
    val hasLocationPermission: State<Boolean> = _hasLocationPermission
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle deep-links from notification taps (system tray).
        handleIntentForNavigation(intent)
        
        // Check initial permission state
        _hasLocationPermission.value = hasLocationPermissions()
        
        // Request notification permission for Android 13+ (API 33+)
        requestNotificationPermissionIfNeeded()
        
        // Performance optimization: reduce initial load time
        setContent {
            LimouserappTheme {
                val navController = rememberNavController()
                AppNavHost(navController, this, navigationEventBus)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIntentForNavigation(intent)
    }

    private fun handleIntentForNavigation(intent: android.content.Intent?) {
        if (intent == null) return
        val destination = intent.getStringExtra("destination")?.trim().orEmpty()
        val bookingIdStr = intent.getStringExtra("booking_id")?.trim()
        Timber.d("Handling intent - destination: $destination, bookingId: $bookingIdStr")
        when (destination) {
            "live_ride" -> {
                Timber.d("Navigating to live ride with bookingId: $bookingIdStr")
                navigationEventBus.tryEmit(NavEvent.ToLiveRide(bookingId = bookingIdStr))
            }
            "chat" -> bookingIdStr?.toIntOrNull()?.let {
                Timber.d("Navigating to chat with bookingId: $it")
                navigationEventBus.tryEmit(NavEvent.ToChat(it))
            }
        }
    }
    
    /**
     * Request notification permission for Android 13+ (API 33+)
     * For Android 12 and below, notifications work by default
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            
            if (!hasPermission) {
                android.util.Log.d("MainActivity", "Requesting notification permission...")
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                android.util.Log.d("MainActivity", "Notification permission already granted")
            }
        }
    }
    
    /**
     * Check if location permissions are granted
     */
    private fun hasLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Request location permissions
     */
    fun requestLocationPermissions() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
}

private object Routes {
    const val Splash = "splash"
    const val Onboarding = "onboarding"
    const val Phone = "phone"
    const val Otp = "otp"
    const val BasicDetails = "basic_details"
    const val CreditCard = "credit_card"
    const val Dashboard = "dashboard"
    const val MyBookings = "my_bookings"
    const val Invoices = "invoices"
    const val InvoiceWebView = "invoice_webview"
    const val MyCards = "my_cards"
    const val AccountSettings = "account_settings"
    const val LiveRide = "live_ride/{bookingId}"
    const val Chat = "chat"
    const val Notifications = "notifications"
    const val Success = "success"
    const val TermsOfUse = "terms_of_use"
    const val PrivacyPolicy = "privacy_policy"
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    mainActivity: MainActivity,
    navigationEventBus: NavigationEventBus
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Get LoadingStateManager from application context via Hilt EntryPoint
    val loadingStateManager = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            LoadingStateManagerEntryPoint::class.java
        ).loadingStateManager()
    }
    
    // Get LogoutUseCase from application context via Hilt EntryPoint
    val logoutUseCase = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            LogoutUseCaseEntryPoint::class.java
        ).logoutUseCase()
    }
    
    // Observe loading state
    val isLoading by loadingStateManager.isLoading.collectAsStateWithLifecycle()
    
    // Get current destination to conditionally show loading overlay
    val currentDestination = navController.currentDestination?.route
    
    val socketService: SocketService? = try {
        val app = context.applicationContext as? com.example.limouserapp.LimoApplication
        app?.socketService
    } catch (e: Exception) {
        null
    }

    // Bugfix (3): react to in-process navigation events (Socket/FCM) without app restart.
    LaunchedEffect(navigationEventBus, navController) {
        navigationEventBus.events.collect { event ->
            Timber.d("Received navigation event: $event")
            when (event) {
                is NavEvent.ToLiveRide -> {
                    val route = if (event.bookingId != null) {
                        "live_ride/${event.bookingId}"
                    } else {
                        // Fallback: if no bookingId, navigate to a default or handle error
                        Timber.w("NavEvent.ToLiveRide without bookingId")
                        return@collect
                    }
                    // Don't navigate if already on LiveRide or Chat (related screens)
                    val currentRoute = navController.currentDestination?.route
                    if (currentRoute != Routes.LiveRide && currentRoute?.startsWith("${Routes.Chat}/") != true) {
                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                    }
                }
                is NavEvent.ToChat -> {
                    navController.navigate("${Routes.Chat}/${event.bookingId}") {
                        launchSingleTop = true
                    }
                }
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = Routes.Splash) {
            composable(Routes.Splash) {
                SplashScreenWithViewModel(navController = navController)
            }
            composable(Routes.Onboarding) {
                OnboardingScreen(onGetStarted = { navController.navigate(Routes.Phone) })
            }
            composable(Routes.Phone) {
                PhoneEntryScreenWithViewModel(navController = navController)
            }
            composable(Routes.Otp) {
                OtpScreenWithViewModel(navController = navController)
            }
            composable(Routes.BasicDetails) {
                BasicDetailsScreenWithViewModel(navController = navController)
            }
            composable(Routes.TermsOfUse) {
                WebViewScreen(
                    url = "https://1800limoco.infodevbox.com/client-terms-condition",
                    title = "Terms of Use",
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(Routes.PrivacyPolicy) {
                WebViewScreen(
                    url = "https://1800limoco.infodevbox.com/privacy-policy",
                    title = "Privacy Policy",
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(Routes.CreditCard) {
                CreditCardScreenWithViewModel(navController = navController)
            }
            // Dashboard route with optional editBookingId, repeatBookingId, isReturnFlow, and openDrawer parameters
            composable(
                route = "${Routes.Dashboard}?editBookingId={editBookingId}&repeatBookingId={repeatBookingId}&isReturnFlow={isReturnFlow}&openDrawer={openDrawer}",
                arguments = listOf(
                    navArgument("editBookingId") {
                        type = NavType.IntType
                        defaultValue = -1
                    },
                    navArgument("repeatBookingId") {
                        type = NavType.IntType
                        defaultValue = -1
                    },
                    navArgument("isReturnFlow") {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                    navArgument("openDrawer") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) { backStackEntry ->
                val editBookingIdArg = backStackEntry.arguments?.getInt("editBookingId") ?: -1
                val initialEditBookingId = if (editBookingIdArg > 0) editBookingIdArg else null
                val repeatBookingIdArg = backStackEntry.arguments?.getInt("repeatBookingId") ?: -1
                val initialRepeatBookingId = if (repeatBookingIdArg > 0) repeatBookingIdArg else null
                val initialIsReturnFlow = backStackEntry.arguments?.getBoolean("isReturnFlow") ?: false
                val shouldOpenDrawer = backStackEntry.arguments?.getBoolean("openDrawer") ?: false
                
                DashboardScreenWithPermissions(
                    navController = navController,
                    onNavigateToProfile = { /* TODO: Implement profile navigation */ },
                    onNavigateToBookings = { navController.navigate(Routes.MyBookings) },
                    onNavigateToSettings = { navController.navigate(Routes.AccountSettings) },
                    onNavigateToCreateBooking = { /* TODO: Implement create booking navigation */ },
                    onNavigateToMyCards = { navController.navigate(Routes.MyCards) },
                    onNavigateToInvoices = { navController.navigate(Routes.Invoices) },
                    onNavigateToEditBooking = { bookingId ->
                        // Edit booking is handled within DashboardScreen
                        // This callback is kept for consistency but not used
                    },
                    initialEditBookingId = initialEditBookingId,
                    initialRepeatBookingId = initialRepeatBookingId,
                    initialIsReturnFlow = initialIsReturnFlow,
                    shouldOpenDrawer = shouldOpenDrawer,
                    onLogout = {
                        coroutineScope.launch {
                            // Execute logout use case to clear all user data and services
                            logoutUseCase()
                            
                            // Navigate directly to login screen (phone entry) and clear entire back stack
                            navController.navigate(Routes.Phone) {
                                // Clear entire navigation back stack by popping up to the graph root
                                popUpTo(navController.graph.id) { inclusive = true }
                                // Prevent multiple instances of the same screen
                                launchSingleTop = true
                            }
                        }
                    },
                    onNavigateToLiveRide = { bookingId ->
                        val route = if (bookingId != null) {
                            "live_ride/$bookingId"
                        } else {
                            "live_ride/default" // Fallback
                        }
                        navController.navigate(route)
                    },
                    onNavigateToNotifications = { navController.navigate(Routes.Notifications) }
                )
            }
            composable(Routes.AccountSettings) {
                AccountSettingsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(Routes.MyBookings) {
                MyBookingsScreen(
                    onBackClick = { 
                        // Navigate back to Dashboard and open drawer
                        navController.navigate("${Routes.Dashboard}?editBookingId=-1&repeatBookingId=-1&isReturnFlow=false&openDrawer=true") {
                            popUpTo(Routes.Dashboard) { inclusive = false }
                        }
                    },
                    onEditBooking = { bookingId ->
                        // Navigate to dashboard with booking ID to trigger edit mode
                        android.util.Log.d("MainActivity", "🔄 Navigating to dashboard with edit booking ID: $bookingId")
                        navController.navigate("${Routes.Dashboard}?editBookingId=$bookingId")
                    },
                    onRepeatBooking = { bookingId ->
                        // Navigate to dashboard with booking ID to trigger repeat mode
                        android.util.Log.d("MainActivity", "🔄 Navigating to dashboard with repeat booking ID: $bookingId")
                        navController.navigate("${Routes.Dashboard}?repeatBookingId=$bookingId&isReturnFlow=false")
                    },
                    onReturnBooking = { bookingId ->
                        // Navigate to dashboard with booking ID to trigger return flow
                        android.util.Log.d("MainActivity", "🔄 Navigating to dashboard with return booking ID: $bookingId")
                        navController.navigate("${Routes.Dashboard}?repeatBookingId=$bookingId&isReturnFlow=true")
                    }
                )
            }
            composable(Routes.Invoices) {
                MyInvoicesScreen(
                    onBackClick = { navController.popBackStack() },
                    onViewInvoiceSummary = { invoiceNumber ->
                        navController.navigate("${Routes.InvoiceWebView}/$invoiceNumber")
                    }
                )
            }
            composable(
                route = "${Routes.InvoiceWebView}/{invoiceNumber}",
                arguments = listOf(
                    navArgument("invoiceNumber") {
                        type = NavType.IntType
                    }
                )
            ) { backStackEntry ->
                val invoiceNumber = backStackEntry.arguments?.getInt("invoiceNumber") ?: 0
                InvoiceWebViewScreen(
                    invoiceNumber = invoiceNumber,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(Routes.MyCards) {
                MyCardsScreen(
                    onBackClick = { 
                        // Navigate back to Dashboard and open drawer
                        navController.navigate("${Routes.Dashboard}?editBookingId=-1&repeatBookingId=-1&isReturnFlow=false&openDrawer=true") {
                            popUpTo(Routes.Dashboard) { inclusive = false }
                        }
                    }
                )
            }
            composable(Routes.Notifications) {
                NotificationScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(
                route = Routes.LiveRide,
                arguments = listOf(
                    navArgument("bookingId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString("bookingId")
                LiveRideInProgressScreen(
                    bookingId = bookingId,
                    onBackClick = { navController.popBackStack() },
                    onNavigateToChat = { chatBookingId ->
                        navController.navigate("${Routes.Chat}/$chatBookingId")
                    }
                )
            }
            composable(
                route = "${Routes.Chat}/{bookingId}",
                arguments = listOf(
                    navArgument("bookingId") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getInt("bookingId") ?: 0
                val service = socketService
                if (service == null || bookingId <= 0) {
                    // Simple fallback UI if launched too early.
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Chat unavailable")
                    }
                } else {
                    ChatScreen(
                        bookingId = bookingId,
                        socketService = service,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            composable(Routes.Success) {
                SuccessScreen(navController = navController)
            }
            composable(
                route = "mapLocationPicker?initialLat={initialLat}&initialLong={initialLong}&initialAddress={initialAddress}",
                arguments = listOf(
                    navArgument("initialLat") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("initialLong") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("initialAddress") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) { backStackEntry ->
                val initialLat = backStackEntry.arguments?.getString("initialLat")?.toDoubleOrNull()
                val initialLong = backStackEntry.arguments?.getString("initialLong")?.toDoubleOrNull()
                val initialAddress = backStackEntry.arguments?.getString("initialAddress")
                com.example.limouserapp.ui.booking.MapLocationPickerScreen(
                    navController = navController,
                    initialLat = initialLat,
                    initialLong = initialLong,
                    initialAddress = initialAddress
                )
            }
        }
        
        // Display notifications on top of all content
        socketService?.let { service ->
            NotificationContainer(
                socketService = service,
                modifier = Modifier.align(Alignment.TopStart)
            )
        }
        
        // Global loading overlay - shows for all API calls except on Dashboard, My Bookings, My Cards, and Account Settings
        // These screens have their own shimmer loaders or loading indicators
        val shouldHideLoader = currentDestination?.let { route ->
            route.startsWith(Routes.Dashboard) ||
            route == Routes.MyBookings ||
            route == Routes.MyCards ||
            route == Routes.Invoices ||
            route == Routes.Notifications ||
            route == Routes.AccountSettings
        } ?: false
        LoadingOverlay(isLoading = isLoading && !shouldHideLoader)
    }
}

@Composable
private fun SplashScreen(onFinished: () -> Unit) {
    var scale by remember { mutableStateOf(0.95f) }
    var alpha by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        // Fade in
        alpha = 1f
        kotlinx.coroutines.delay(200)
        
        // Zoom in
        scale = 1.0f
        kotlinx.coroutines.delay(600)
        
        // Hold
        kotlinx.coroutines.delay(300)
        
        // Fade out
        alpha = 0f
        kotlinx.coroutines.delay(150)
        
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
                .fillMaxWidth(0.85f)
                .wrapContentHeight()
                .wrapContentWidth(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun OnboardingScreen(onGetStarted: () -> Unit) {
    // Placeholder; will use provided image and gradient later
    Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            Text("Sit back & go", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(8.dp))
            Text("wherever you want", style = MaterialTheme.typography.headlineLarge, color = LimoOrange)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onGetStarted, colors = ButtonDefaults.buttonColors(containerColor = LimoOrange)) {
                Text("Let's get started")
            }
        }
    }
}

@Composable
private fun SplashScreenWithViewModel(navController: NavHostController) {
    val viewModel: SplashViewModel = hiltViewModel()
    
    LaunchedEffect(Unit) {
        // Wait for splash animation (1.25 seconds) + auth check
        kotlinx.coroutines.delay(1500) // Total wait time
        
        val nextDestination = viewModel.getNextDestination()
        navController.navigate(nextDestination) {
            popUpTo(Routes.Splash) { inclusive = true }
        }
    }
    
    SplashScreen(onFinished = { /* Handled by LaunchedEffect */ })
}

@Composable
private fun PhoneEntryScreenWithViewModel(navController: NavHostController) {
    val viewModel: PhoneEntryViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            // Navigate to OTP screen with temp user ID and phone number
            val tempUserId = uiState.tempUserId
            val phoneNumber = uiState.phoneNumberWithCountryCode
            navController.navigate(Routes.Otp)
        }
    }
    
    PhoneEntryScreen(
        onNext = { viewModel.onEvent(PhoneEntryUiEvent.SendVerificationCode) },
        onBack = { navController.popBackStack() },
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}

@Composable
private fun OtpScreenWithViewModel(navController: NavHostController) {
    val viewModel: OtpViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Initialize OTP screen with proper data
    
    LaunchedEffect(uiState.success, uiState.nextAction) {
        if (uiState.success && uiState.nextAction != null) {
            val destination = when (uiState.nextAction) {
                "dashboard" -> Routes.Dashboard
                "basic_details" -> Routes.BasicDetails
                "credit_card" -> Routes.CreditCard
                "success" -> Routes.Success
                else -> Routes.BasicDetails // fallback
            }
            navController.navigate(destination)
        }
    }
    
    OtpScreen(
        onNext = { /* Handled by LaunchedEffect */ },
        onBack = { navController.popBackStack() },
        uiState = uiState,
        onEvent = viewModel::onEvent,
        phoneNumber = uiState.phoneNumber.ifEmpty { "+1 9876543210" }
    )
}

@Composable
private fun BasicDetailsScreenWithViewModel(navController: NavHostController) {
    val viewModel: BasicDetailsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val userStateManager = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            UserStateManagerEntryPoint::class.java
        ).userStateManager()
    }

    // Log state when screen is displayed
    LaunchedEffect(Unit) {
        Timber.d("MainActivity: BasicDetailsScreen displayed - name='${uiState.name}', email='${uiState.email}', success=${uiState.success}")
    }

    // Log state changes
    LaunchedEffect(uiState.name, uiState.email) {
        Timber.d("MainActivity: BasicDetailsScreen state updated - name='${uiState.name}', email='${uiState.email}'")
    }

    // Track previous success state to only navigate when it transitions from false to true
    var previousSuccess by remember { mutableStateOf(false) }

    // Navigate when success becomes true and we haven't navigated yet
    LaunchedEffect(uiState.success) {
        val currentDestination = navController.currentDestination?.route
        Timber.d("MainActivity: BasicDetailsScreen - success=${uiState.success}, previousSuccess=$previousSuccess, currentDestination=$currentDestination")

        // Only navigate when success transitions from false to true
        // AND we're currently on the basic_details screen (not already navigated away)
        if (uiState.success && !previousSuccess && currentDestination == Routes.BasicDetails) {
            previousSuccess = true
            Timber.d("MainActivity: BasicDetailsScreen - Success flag changed to true, navigating forward")
            // Use next_step from stored value to determine navigation
            val nextStep = userStateManager.getNextStep()
            Timber.d("MainActivity: BasicDetailsScreen - nextStep from userStateManager: $nextStep")
            val destination = when (nextStep) {
                "credit_card" -> Routes.CreditCard
                "basic_details" -> Routes.BasicDetails
                "dashboard" -> Routes.Dashboard
                else -> Routes.CreditCard // Default fallback
            }
            Timber.d("MainActivity: BasicDetailsScreen - navigating to destination: $destination")
            navController.navigate(destination)
        } else if (!uiState.success) {
            // Reset the flag when success becomes false
            previousSuccess = false
            Timber.d("MainActivity: BasicDetailsScreen - success became false, resetting previousSuccess")
        } else if (uiState.success && currentDestination != Routes.BasicDetails) {
            // If success is true but we're not on basic_details, we've already navigated
            // Update previousSuccess to prevent re-navigation
            previousSuccess = true
            Timber.d("MainActivity: BasicDetailsScreen - Already navigated away, not navigating again")
        } else {
            Timber.d("MainActivity: BasicDetailsScreen - Navigation conditions not met: success=${uiState.success}, !previousSuccess=${!previousSuccess}, currentDestination==BasicDetails=${currentDestination == Routes.BasicDetails}")
        }
    }
    
    AddBasicDetailsScreen(
        onNext = { viewModel.onEvent(BasicDetailsUiEvent.SubmitDetails) },
        onBack = { navController.popBackStack() },
        onNavigateToTerms = { navController.navigate(Routes.TermsOfUse) },
        onNavigateToPrivacy = { navController.navigate(Routes.PrivacyPolicy) },
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}

@Composable
private fun CreditCardScreenWithViewModel(navController: NavHostController) {
    val viewModel: CreditCardViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val userStateManager = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            UserStateManagerEntryPoint::class.java
        ).userStateManager()
    }
    
    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            // Check if profile is completed - if so, go directly to Dashboard
            val isProfileCompleted = userStateManager.isProfileCompleted()
            if (isProfileCompleted) {
                navController.navigate(Routes.Dashboard)
            } else {
                // Use next_step from stored value to determine navigation
                val nextStep = userStateManager.getNextStep()
                val destination = when (nextStep) {
                    "dashboard" -> Routes.Dashboard
                    "profile_complete" -> Routes.Dashboard
                    "success" -> Routes.Success
                    else -> Routes.Success // Default fallback
                }
                navController.navigate(destination)
            }
        }
    }
    
    AddCreditCardScreen(
        onNext = { viewModel.onEvent(CreditCardUiEvent.SubmitCard) },
        onBack = {
            Timber.d("MainActivity: CreditCardScreen - Back button clicked, attempting to navigate back")
            Timber.d("MainActivity: Current destination before pop: ${navController.currentDestination?.route}")

            // Set flag to indicate we're navigating back from credit card screen
            userStateManager.setJustNavigatedBackFromCreditCard(true)
            Timber.d("MainActivity: Set justNavigatedBackFromCreditCard flag to true")

            try {
                val popped = navController.popBackStack()
                Timber.d("MainActivity: popBackStack() returned: $popped")
                if (!popped) {
                    Timber.w("MainActivity: popBackStack() returned false - no destination to pop to")
                    // Try navigating explicitly to BasicDetails as fallback
                    Timber.d("MainActivity: Attempting explicit navigate to BasicDetails")
                    navController.navigate(Routes.BasicDetails) {
                        popUpTo(Routes.CreditCard) { inclusive = true }
                    }
                } else {
                    Timber.d("MainActivity: Successfully navigated back to: ${navController.currentDestination?.route}")
                }
            } catch (e: Exception) {
                Timber.e(e, "MainActivity: Error during popBackStack(): ${e.message}")
            }
        },
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}

@Composable
private fun SuccessScreen(navController: NavHostController) {
    // Simple success screen - can be enhanced later
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "🎉",
                style = MaterialTheme.typography.displayLarge
            )
            Text(
                text = "Welcome to 1800Limo!",
                style = MaterialTheme.typography.headlineLarge,
                color = LimoOrange
            )
            Text(
                text = "Your account has been created successfully.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF121212)
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { navController.navigate(Routes.Dashboard) },
                colors = ButtonDefaults.buttonColors(containerColor = LimoOrange)
            ) {
                Text("Get Started")
            }
        }
    }
}

// TEMPORARY: Simple wrapper to pass SocketService for test button
@Composable  
private fun DashboardScreenWithPermissions(
    navController: NavHostController,
    onNavigateToProfile: () -> Unit,
    onNavigateToBookings: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCreateBooking: () -> Unit,
    onNavigateToMyCards: () -> Unit,
    onNavigateToInvoices: () -> Unit,
    onNavigateToEditBooking: (Int) -> Unit = {},
    onLogout: () -> Unit,
    onNavigateToLiveRide: (bookingId: String?) -> Unit,
    onNavigateToNotifications: () -> Unit,
    initialEditBookingId: Int? = null,
    initialRepeatBookingId: Int? = null,
    initialIsReturnFlow: Boolean = false,
    shouldOpenDrawer: Boolean = false
) {
    val context = LocalContext.current
    val activity = context as MainActivity
    
    // TEMPORARY: Create a simple composable that injects SocketService
    TestDashboardWrapper(
        navController = navController,
        onNavigateToProfile = onNavigateToProfile,
        onNavigateToBookings = onNavigateToBookings,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToCreateBooking = onNavigateToCreateBooking,
        onNavigateToMyCards = onNavigateToMyCards,
        onNavigateToInvoices = onNavigateToInvoices,
        onLogout = onLogout,
        activity = activity,
        onNavigateToLiveRide = onNavigateToLiveRide,
        onNavigateToNotifications = onNavigateToNotifications,
        initialEditBookingId = initialEditBookingId,
        initialRepeatBookingId = initialRepeatBookingId,
        initialIsReturnFlow = initialIsReturnFlow,
        shouldOpenDrawer = shouldOpenDrawer,
    )
}

@Composable
private fun TestDashboardWrapper(
    navController: NavHostController,
    onNavigateToProfile: () -> Unit,
    onNavigateToBookings: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCreateBooking: () -> Unit,
    onNavigateToMyCards: () -> Unit,
    onNavigateToInvoices: () -> Unit,
    onNavigateToEditBooking: (Int) -> Unit = {},
    onLogout: () -> Unit,
    activity: MainActivity,
    onNavigateToLiveRide: (bookingId: String?) -> Unit,
    onNavigateToNotifications: () -> Unit,
    initialEditBookingId: Int? = null,
    initialRepeatBookingId: Int? = null,
    initialIsReturnFlow: Boolean = false,
    shouldOpenDrawer: Boolean = false
) {
    // For now, just pass null - test button won't show
    DashboardScreen(
        onNavigateToProfile = onNavigateToProfile,
        onNavigateToBookings = onNavigateToBookings,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToCreateBooking = onNavigateToCreateBooking,
        onNavigateToMyCards = onNavigateToMyCards,
        onNavigateToInvoices = onNavigateToInvoices,
        onNavigateToEditBooking = onNavigateToEditBooking,
        onLogout = onLogout,
        hasLocationPermission = activity.hasLocationPermission.value,
        onRequestLocationPermission = { activity.requestLocationPermissions() },
        onNavigateToLiveRide = onNavigateToLiveRide,
        onNavigateToNotifications = onNavigateToNotifications,
        initialEditBookingId = initialEditBookingId,
        initialRepeatBookingId = initialRepeatBookingId,
        initialIsReturnFlow = initialIsReturnFlow,
        shouldOpenDrawer = shouldOpenDrawer,
        navController = navController
    )
}

@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreview() {
    LimouserappTheme {
        OnboardingScreen(onGetStarted = {})
    }
}