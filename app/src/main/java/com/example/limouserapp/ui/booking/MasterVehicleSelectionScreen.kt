package com.example.limouserapp.ui.booking

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.limouserapp.R
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.draw.shadow
import com.example.limouserapp.data.model.booking.RideData
import com.example.limouserapp.data.model.booking.Vehicle
import com.example.limouserapp.data.model.booking.FilterData
import com.example.limouserapp.ui.theme.LimoBlack
import com.example.limouserapp.ui.theme.LimoOrange
import com.example.limouserapp.ui.theme.LimoWhite
import com.example.limouserapp.ui.viewmodel.MasterVehicleViewModel
import com.example.limouserapp.ui.utils.DebugTags
import com.example.limouserapp.ui.booking.components.FilterDialog
import com.example.limouserapp.ui.booking.components.FilterSelectionState
import com.example.limouserapp.ui.components.ShimmerBox
import com.example.limouserapp.ui.components.ShimmerText

/**
 * Master Vehicle Selection Screen
 * Shows vehicle categories from master vehicle listing API
 * Matches iOS VehicleSelectionView
 */
@Composable
fun MasterVehicleSelectionScreen(
    rideData: RideData,
    onDismiss: () -> Unit,
    onMasterVehicleSelected: (Vehicle) -> Unit,
    initialFilterSelection: FilterSelectionState = FilterSelectionState()
) {
    val viewModel: MasterVehicleViewModel = hiltViewModel()
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

    LaunchedEffect(rideData) {
        Log.d(DebugTags.BookingProcess, "Open MasterVehicleSelection. Loading master vehicles for ride: $rideData")
        viewModel.loadMasterVehicles(rideData)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LimoWhite)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
    ) {
        // Header with proper centering
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp)

        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = LimoBlack)
            }
            Text(
                "Select the vehicle",
                style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = LimoBlack),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Sub-header
        Text(
            "Use Filter / Sort to customize your ride.",
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, color = Color.Gray, textAlign = TextAlign.Center),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 3.dp)
        )

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            color = Color.Gray.copy(alpha = 0.3f) // This creates a subtle line
        )

        // Filter Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
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
                        painter = androidx.compose.ui.res.painterResource(com.example.limouserapp.R.drawable.filter_icon),
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
            color = Color.Gray.copy(alpha = 0.1f)
        )

        // Vehicle Options
        if (loading) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .imePadding(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(6) {
                    VehicleCategoryCardShimmer()
                }
            }
        } else if (error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Error loading vehicles",
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = LimoBlack)
                    )
                    Text(
                        error ?: "An error occurred",
                        style = TextStyle(fontSize = 14.sp, color = Color.Gray)
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(vehicles) { vehicle ->
                    MasterVehicleOptionCard(
                        vehicle = vehicle,
                        isSelected = selectedVehicle?.id == vehicle.id,
                        onTap = { 
                            selectedVehicle = vehicle
                            // Navigate immediately when vehicle card is tapped
                            onMasterVehicleSelected(vehicle)
                        }
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
                // Automatically navigate to VehicleListingScreen after applying filters (matches iOS behavior)
                // iOS uses: selectedVehicle?.id ?? 1, so we use selected vehicle or first available
                val vehicleToNavigate = selectedVehicle ?: vehicles.firstOrNull()
                if (vehicleToNavigate != null) {
                    // If no vehicle was selected before, select it now
                    if (selectedVehicle == null) {
                        selectedVehicle = vehicleToNavigate
                    }
                    onMasterVehicleSelected(vehicleToNavigate)
                }
                // If no vehicles are available, don't navigate (user should select a vehicle first)
            },
            onClear = {
                filterSelection = FilterSelectionState()
            },
            isLoading = filterData == null && showFilterDialog,
            errorMessage = null
        )
    }
}

@Composable
private fun MasterVehicleOptionCard(
    vehicle: Vehicle,
    isSelected: Boolean,
    onTap: () -> Unit
) {
    // Custom color for the tag background (Light Cream/Orange) extracted from image

    Card(
        onClick = onTap,
        shape = RoundedCornerShape(12.dp), // Slightly softer corners to match image
        colors = CardDefaults.cardColors(containerColor = LimoWhite),
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp), // Adjusted height slightly to fit the vertical stack comfortably
        border = if (isSelected) {
            BorderStroke(3.dp, LimoBlack) // Thick black border as shown in image
        } else {
            BorderStroke(1.dp, Color(0xFFE0E0E0)) // Light gray when not selected
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Section: Content Stack
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Vehicle Name
                Text(
                    text = vehicle.name,
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = LimoBlack
                    ),
                    maxLines = 1
                )
                
                Spacer(modifier = Modifier.height(4.dp))

//                // 2. FROM Label
//                Text(
//                    text = "FROM",
//                    style = TextStyle(
//                        fontSize = 12.sp,
//                        fontWeight = FontWeight.Normal,
//                        color = Color.Gray
//                    )
//                )

                Spacer(modifier = Modifier.height(4.dp))

                // 3. Price Box (Orange)
                Box(
                    modifier = Modifier
                        .background(color = LimoOrange, shape = RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "FROM $ ${"%.2f".format(vehicle.price ?: 0.0)}",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // 4. Capacity Tags Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Passenger Tag
                    VehicleSpecTag(
                        iconResId = R.drawable.passenger,
                        label = "Passengers",
                        count = String.format("%02d", vehicle.capacity ?: 0)
                    )
                    
                    // Luggage Tag
                    VehicleSpecTag(
                        iconResId = R.drawable.luggage,
                        label = "Luggage",
                        count = String.format("%02d", vehicle.luggage ?: 0)
                    )
                }
            }

            // Right Section: Vehicle Image
            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .width(120.dp) // Slightly wider to accommodate car length
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (!vehicle.image.isNullOrEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(vehicle.image),
                        contentDescription = vehicle.name,
                        modifier = Modifier.fillMaxWidth(),
                        // IMPORTANT: changed to Fit so the car isn't cropped like a square
                        contentScale = ContentScale.Fit 
                    )
                } else {
                    // Placeholder
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun VehicleSpecTag(
    iconResId: Int,
    label: String,
    count: String
) {
    Row(
        modifier = Modifier
            .background(
                LimoOrange.copy(alpha = 0.1f),
                RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .height(22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = label,
            tint = LimoOrange,
            modifier = Modifier.size(12.dp)
        )
        
        // Text: Label + Count (both same weight per Figma)
        Text(
            text = "$label " ,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium, // 510 = Medium weight
                lineHeight = 18.sp, // 150% of 12sp = 18sp
                color = LimoBlack // #121212
            )
        )
        Text(
            text = "$count ",
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 18.sp,
                color = LimoBlack
            )
        )
    }
}

@Composable
private fun VehicleCategoryCardShimmer() {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = LimoWhite),
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp), // Match the main card height
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 15.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top section shimmer
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Vehicle Name shimmer
                    ShimmerText(modifier = Modifier.fillMaxWidth(0.6f), height = 18.dp)

                    // FROM label and Price Box shimmer
                    Column(
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        ShimmerText(modifier = Modifier.width(40.dp), height = 12.dp)
                        ShimmerBox(
                            modifier = Modifier
                                .width(80.dp)
                                .height(22.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                    }
                }

                // Bottom section: Capacity Tags shimmer
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShimmerBox(
                        modifier = Modifier
                            .width(100.dp)
                            .height(22.dp),
                        shape = RoundedCornerShape(4.dp)
                    )
                    ShimmerBox(
                        modifier = Modifier
                            .width(90.dp)
                            .height(22.dp),
                        shape = RoundedCornerShape(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Vehicle Image shimmer
            ShimmerBox(
                modifier = Modifier
                    .size(99.dp, 74.dp), // Match the main card image size
                shape = RoundedCornerShape(4.dp)
            )
        }
    }
}