package com.example.limouserapp.ui.booking

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.limouserapp.data.model.booking.RideData
import com.example.limouserapp.data.model.booking.Vehicle
import com.example.limouserapp.ui.theme.LimoBlack
import com.example.limouserapp.ui.theme.LimoOrange
import com.example.limouserapp.ui.theme.LimoWhite
import com.example.limouserapp.ui.theme.AppDimensions
import com.example.limouserapp.ui.viewmodel.VehicleListingViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.example.limouserapp.ui.utils.DebugTags
import com.example.limouserapp.ui.components.ShimmerCard
import com.example.limouserapp.ui.components.ShimmerBox
import com.example.limouserapp.ui.components.ShimmerText
import com.example.limouserapp.ui.theme.LimoGreen
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.res.painterResource
import com.example.limouserapp.R
import com.example.limouserapp.data.model.booking.FilterData
import com.example.limouserapp.ui.booking.components.FilterDialog
import com.example.limouserapp.ui.booking.components.FilterSelectionState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

/**
 * Vehicle Listing Screen
 * Shows actual vehicles from vehicle listing API with filters applied
 * Matches iOS BookingDetailsView
 */
@Composable
fun VehicleListingScreen(
    rideData: RideData,
    selectedMasterVehicleId: Int,
    initialFilterSelection: com.example.limouserapp.ui.booking.components.FilterSelectionState = com.example.limouserapp.ui.booking.components.FilterSelectionState(),
    onDismiss: () -> Unit,
    onVehicleSelected: (Vehicle) -> Unit,
    onVehicleDetails: (Vehicle) -> Unit = {},
    onBookNow: (Vehicle) -> Unit = {}
) {
    val viewModel: VehicleListingViewModel = hiltViewModel()
    val vehicles by viewModel.vehicles.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    var selectedVehicle by remember { mutableStateOf<Vehicle?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var filterSelection by remember { mutableStateOf(initialFilterSelection) }
    var filterData by remember { mutableStateOf<FilterData?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    // Fetch filter data when dialog is shown
    LaunchedEffect(showFilterDialog) {
        if (showFilterDialog && filterData == null) {
            coroutineScope.launch {
                filterData = viewModel.filterService.fetchFilters()
            }
        }
    }

    LaunchedEffect(rideData, selectedMasterVehicleId, filterSelection) {
        Log.d(DebugTags.BookingProcess, "Open VehicleListing. Fetching vehicles for master vehicle ID: $selectedMasterVehicleId")
        viewModel.loadVehicles(rideData, selectedMasterVehicleId, filterSelection)
    }
    
    // Auto-select first vehicle when vehicles are loaded
    LaunchedEffect(vehicles) {
        if (selectedVehicle == null && vehicles.isNotEmpty()) {
            selectedVehicle = vehicles.first()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LimoWhite)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = LimoBlack)
            }
            Text(
                "Select the Vehicle",
                style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = LimoBlack)
            )
            // Invisible IconButton to center title
            IconButton(onClick = {}) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.Transparent)
            }
        }
        
        // Filter Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = { showFilterDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Gray.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.filter_icon),
                        contentDescription = "Filter",
                        modifier = Modifier.size(14.dp),
                        tint = LimoBlack
                    )
                    Text(
                        "Filter By",
                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = LimoBlack)
                    )
                }
            }
        }
        
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            color = Color.Gray.copy(alpha = 0.3f)
        )
        
        // Vehicle List
        if (loading) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .imePadding(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(6) {
                    VehicleCardShimmer()
                }
            }
        } else if (error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "⚠️",
                        fontSize = 40.sp
                    )
                    Text(
                        "Error loading vehicles",
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = LimoBlack)
                    )
                    Text(
                        error ?: "An error occurred",
                        style = TextStyle(fontSize = 14.sp, color = Color.Gray),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else if (vehicles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No vehicles available for your selection.",
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .imePadding(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(vehicles) { vehicle ->
                    VehicleListingCard(
                        vehicle = vehicle,
                        isSelected = selectedVehicle?.id == vehicle.id,
                        rideData = rideData,
                        onTap = { selectedVehicle = vehicle },
                        onDetailsClick = { onVehicleDetails(vehicle) },
                        onBookNowClick = { onBookNow(vehicle) }
                    )
                }
            }
        }
    }
    
    // Filter Dialog
    if (showFilterDialog) {
        FilterDialog(
            filterData = filterData,
            currentSelection = filterSelection,
            onDismiss = { showFilterDialog = false },
            onApply = { updatedSelection ->
                filterSelection = updatedSelection
                showFilterDialog = false
                // Filters will be applied via LaunchedEffect when filterSelection changes
            },
            onClear = {
                filterSelection = FilterSelectionState()
            },
            isLoading = filterData == null && showFilterDialog,
            errorMessage = null
        )
    }
}

// Define colors based on the screenshot design
private val BadgeGray = Color(0xFFF2F2F2)
private val PriceGold = Color(0xFFD99030) // Matches the price background
private val CapacityBeige = Color(0xFFFFF5E9) // Matches the passenger/luggage bg
private val CapacityIconColor = Color(0xFFD99030) // Matches the icon tint inside beige box

@Composable
private fun VehicleListingCard(
    vehicle: Vehicle,
    isSelected: Boolean,
    rideData: RideData,
    onTap: () -> Unit,
    onDetailsClick: () -> Unit,
    onBookNowClick: () -> Unit
) {
    Card(
        onClick = onTap,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp), // Spacing between cards
        border = null // No border needed since action buttons are present
    ) {
        Column {
            // --- TOP SECTION: Image & Details ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min) // Ensure height matches content
                    .padding(12.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Vehicle Image (Left - increased width)
                Box(
                    modifier = Modifier
                        .weight(0.45f) // Increased from 0.35f to 0.45f
                        .height(100.dp) // Fixed height for image area
                        .clip(RoundedCornerShape(8.dp)), // Clip image to prevent overflow
                    contentAlignment = Alignment.Center
                ) {
                    val vehicleImageUrl = vehicle.vehicleImages?.firstOrNull() ?: vehicle.image
                    if (!vehicleImageUrl.isNullOrEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(vehicleImageUrl),
                            contentDescription = vehicle.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp)), // Ensure image is clipped
                            contentScale = ContentScale.Fit // Fit ensures the whole car is seen
                        )
                    } else {
                        // Fallback placeholder
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🚗", fontSize = 24.sp)
                        }
                    }
                }
                
                // Spacer between image and details
                Spacer(modifier = Modifier.width(12.dp))
                
                // 2. Vehicle Details (Right - adjusted for larger image)
                Column(
                    modifier = Modifier
                        .weight(0.55f) // Adjusted from 0.65f to 0.55f
                        .fillMaxWidth(), // Ensure column takes full available width
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Title
                    Text(
                        vehicle.name,
                        style = TextStyle(
                            fontSize = 18.sp, // Increased from 16.sp
                            fontWeight = FontWeight.Bold,
                            color = LimoBlack
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )

                    // Price & Service Type Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Service Type Pill (Gray)
                        Surface(
                            color = BadgeGray,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = getServiceTypeDisplayName(rideData.serviceType), // e.g., "One way"
                                style = TextStyle(
                                    fontSize = 14.sp, // Increased from 12.sp
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Black
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        
                        // Price Pill (Gold/Orange)
                        vehicle.getPrice(rideData.serviceType)?.let { price ->
                            Surface(
                                color = PriceGold,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "$ ${"%.2f".format(price)}",
                                    style = TextStyle(
                                        fontSize = 15.sp, // Increased from 13.sp
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    
                    // Gratuity Text
                    Text(
                        "All INC Rate include GRATUITY*",
                        style = TextStyle(
                            fontSize = 12.sp, // Increased from 10.sp
                            fontWeight = FontWeight.Normal,
                            color = Color.Gray
                        ),
                        modifier = Modifier.padding(
                            top = 4.dp,
                        )
                    )
                    
                    // Specs Row (Make, Model, Year)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        vehicle.vehicleDetails?.make?.let { SpecBadge(it) }
                        vehicle.vehicleDetails?.model?.let { SpecBadge(it) }
                        vehicle.vehicleDetails?.year?.let { SpecBadge(it) }
                    }
                }
            }
            
            // --- DIVIDER ---
            Divider(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFF0F0F0),
                thickness = 1.dp
            )
            
            // --- BOTTOM SECTION: Capacity & Buttons ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Capacity Indicators (Beige Backgrounds)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CapacityBadge(
                        iconRes = R.drawable.passenger,
                        count = String.format("%02d", vehicle.getCapacity())
                    )
                    
                    CapacityBadge(
                        iconRes = R.drawable.luggage,
                        count = String.format("%02d", vehicle.luggage ?: 0)
                    )
                }
                
                // Right: Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Booking Details (Black)
                    Button(
                        onClick = { onDetailsClick() },
                        colors = ButtonDefaults.buttonColors(containerColor = LimoBlack),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp), // Compact height
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            "Booking Details", // Updated text per screenshot
                            style = TextStyle(
                                fontSize = 13.sp, // Increased from 11.sp
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        )
                    }
                    
                    // Book Now (Green)
                    Button(
                        onClick = { onBookNowClick() },
                        colors = ButtonDefaults.buttonColors(containerColor = LimoGreen),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            "Book Now",
                            style = TextStyle(
                                fontSize = 13.sp, // Increased from 11.sp
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VehicleCardShimmer() {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f))
    ) {
        Column {
            // Top Section: Image + Details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                // Vehicle Image shimmer (1/4 width)
                ShimmerBox(
                    modifier = Modifier
                        .weight(0.25f)
                        .fillMaxHeight()
                        .padding(16.dp),
                    shape = RoundedCornerShape(8.dp)
                )
                
                // Vertical Divider
                Divider(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight(),
                    color = Color.Gray.copy(alpha = 0.3f)
                )
                
                // Vehicle Details shimmer (3/4 width)
                Column(
                    modifier = Modifier
                        .weight(0.75f)
                        .fillMaxHeight()
                        .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ShimmerText(modifier = Modifier.fillMaxWidth(0.7f), height = 18.dp)
                    ShimmerText(modifier = Modifier.fillMaxWidth(0.5f), height = 14.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ShimmerText(modifier = Modifier.width(60.dp), height = 12.dp)
                        ShimmerText(modifier = Modifier.width(60.dp), height = 12.dp)
                        ShimmerText(modifier = Modifier.width(60.dp), height = 12.dp)
                    }
                }
            }
            
            // Bottom Section: Price and Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Price shimmer
                Column {
                    ShimmerText(modifier = Modifier.width(80.dp), height = 20.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    ShimmerText(modifier = Modifier.width(100.dp), height = 14.dp)
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Buttons shimmer
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShimmerBox(
                        modifier = Modifier
                            .width(70.dp)
                            .height(32.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                    ShimmerBox(
                        modifier = Modifier
                            .width(80.dp)
                            .height(32.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }
    }
}

private fun getServiceTypeDisplayName(serviceType: String): String {
    return when (serviceType.uppercase()) {
        "ONE_WAY", "one_way" -> "One way"
        "ROUND_TRIP", "round_trip" -> "Round Trip"
        "CHARTERT_TOUR", "charter_tour" -> "Charter Tour"
        else -> "One way"
    }
}

// --- Local Helper Components for Styling ---

@Composable
private fun SpecBadge(text: String) {
    if (text.isNotEmpty()) {
        Surface(
            color = BadgeGray,
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = text,
                style = TextStyle(
                    fontSize = 12.sp, // Increased from 10.sp
                    fontWeight = FontWeight.Medium,
                    color = Color.Black.copy(alpha = 0.7f)
                ),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun CapacityBadge(iconRes: Int, count: String) {
    Surface(
        color = CapacityBeige,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.width(60.dp) // Fixed width for uniform look
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = CapacityIconColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = count,
                style = TextStyle(
                    fontSize = 14.sp, // Increased from 12.sp
                    fontWeight = FontWeight.Bold,
                    color = LimoBlack
                )
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun VehicleListingScreenPreview() {
    MaterialTheme {
        // Sample ride data
        val sampleRideData = RideData(
            serviceType = "one_way",
            bookingHour = "2",
            pickupType = "address",
            dropoffType = "address",
            pickupDate = "2024-01-15",
            pickupTime = "10:00:00",
            pickupLocation = "123 Main St, New York, NY",
            destinationLocation = "456 Park Ave, New York, NY",
            selectedPickupAirport = "",
            selectedDestinationAirport = "",
            noOfPassenger = 2,
            noOfLuggage = 1,
            noOfVehicles = 1,
            pickupLat = 40.7128,
            pickupLong = -74.0060,
            destinationLat = 40.7580,
            destinationLong = -73.9855
        )
        
        // Sample vehicles
        val sampleVehicles = listOf(
            Vehicle(
                id = 1,
                name = "Luxury Sedan",
                image = null,
                vehicleImages = listOf("https://example.com/sedan.jpg"),
                capacity = 4,
                passenger = 4,
                luggage = 2,
                price = 125.50,
                rateBreakdownOneWay = null,
                rateBreakdownRoundTrip = null,
                rateBreakdownCharterTour = null
            ),
            Vehicle(
                id = 2,
                name = "Premium SUV",
                image = null,
                vehicleImages = listOf("https://example.com/suv.jpg"),
                capacity = 6,
                passenger = 6,
                luggage = 4,
                price = 185.75,
                rateBreakdownOneWay = null,
                rateBreakdownRoundTrip = null,
                rateBreakdownCharterTour = null
            ),
            Vehicle(
                id = 3,
                name = "Executive Limousine",
                image = null,
                vehicleImages = listOf("https://example.com/limo.jpg"),
                capacity = 8,
                passenger = 8,
                luggage = 6,
                price = 250.00,
                rateBreakdownOneWay = null,
                rateBreakdownRoundTrip = null,
                rateBreakdownCharterTour = null
            )
        )
        
        // Preview the screen structure with sample data
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LimoWhite)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = LimoBlack)
                }
                Text(
                    "Select the Vehicle",
                    style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = LimoBlack)
                )
                IconButton(onClick = {}) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.Transparent)
                }
            }
            
            // Filter Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Gray.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Filter",
                            modifier = Modifier.size(14.dp),
                            tint = LimoBlack
                        )
                        Text(
                            "Filter By",
                            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = LimoBlack)
                        )
                    }
                }
            }
            
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = Color.Gray.copy(alpha = 0.3f)
            )
            
            // Vehicle List
            LazyColumn(
                modifier = Modifier
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sampleVehicles) { vehicle ->
                    VehicleListingCard(
                        vehicle = vehicle,
                        isSelected = vehicle.id == 1, // First vehicle selected
                        rideData = sampleRideData,
                        onTap = { },
                        onDetailsClick = { },
                        onBookNowClick = { }
                    )
                }
            }
        }
    }
}

