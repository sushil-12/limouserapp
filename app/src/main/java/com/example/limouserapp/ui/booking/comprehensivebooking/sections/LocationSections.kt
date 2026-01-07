package com.example.limouserapp.ui.booking.comprehensivebooking.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.limouserapp.data.model.booking.Airline
import com.example.limouserapp.data.model.booking.Airport
import com.example.limouserapp.ui.components.LocationAutocomplete
import com.example.limouserapp.ui.booking.comprehensivebooking.*
import com.example.limouserapp.ui.theme.LimoBlack

/**
 * Pick-up Section
 * Matches iOS pickupSection - fields change based on transfer type
 */
@Composable
fun PickupSection(
    selectedTransferType: String,
    pickupLocation: String,
    pickupDate: String,
    pickupTime: String,
    pickupFlightNumber: String,
    originAirportCity: String,
    cruiseShipName: String,
    shipArrivalTime: String,
    cruisePort: String,
    selectedPickupAirport: Airport?,
    selectedPickupAirline: Airline?,
    onLocationSelected: (String, String, String, String, String, Double?, Double?, String?) -> Unit, // (fullAddress, city, state, zipCode, locationDisplay, latitude, longitude, country)
    onLocationChange: (String) -> Unit,
    onAirportClick: () -> Unit,
    onAirlineClick: () -> Unit,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
    onFlightNumberChange: (String) -> Unit,
    onOriginCityChange: (String) -> Unit,
    onCruiseShipChange: (String) -> Unit,
    onShipArrivalChange: (String) -> Unit,
    onShipArrivalClick: () -> Unit,
    onCruisePortChange: (String) -> Unit,
    // Error states
    pickupLocationError: Boolean = false,
    pickupCoordinatesError: Boolean = false,
    pickupAddressValidationError: String? = null, // Address validation error message from Directions API
    airportPickupError: Boolean = false,
    cruisePickupError: Boolean = false,
    cruisePickupPortError: Boolean = false,
    cruisePickupShipError: Boolean = false,
    pickupDateTimeError: Boolean = false,
    // Specific airport field errors
    pickupAirportError: Boolean = false,
    pickupAirlineError: Boolean = false,
    pickupFlightError: Boolean = false,
    originCityError: Boolean = false
) {
    SectionHeader("Pick-up")
    
    Column {
        // Travel Date & Pickup Time (side by side - each taking half width)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EditableField(
                label = "TRAVEL DATE",
                value = formatDate(pickupDate),
                onClick = onDateClick,
                icon = Icons.Default.CalendarToday,
                modifier = Modifier.weight(1f),
                isError = pickupDateTimeError
            )

            EditableField(
                label = "PICKUP TIME",
                value = formatTime(pickupTime),
                onClick = onTimeClick,
                icon = Icons.Default.AccessTime,
                modifier = Modifier.weight(1f),
                isError = pickupDateTimeError
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dynamic pickup fields based on transfer type (matches iOS logic exactly)
        when {
            // City pickup - show address field with Google Places autocomplete
            selectedTransferType == "City to City" || 
            selectedTransferType == "City to Airport" || 
            selectedTransferType == "City to Cruise Port" -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "PICKUP ADDRESS",
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = LimoBlack)
                    )
                    LocationAutocomplete(
                        value = pickupLocation,
                        onValueChange = onLocationChange,
                        onLocationSelected = onLocationSelected,
                        placeholder = "Enter pickup address",
                        error = pickupAddressValidationError ?: if (pickupLocationError) "Pickup location required" else if (pickupCoordinatesError) "Select location with coordinates" else null
                    )
                }
            }
            // Airport pickup - show airport, airline, flight number, origin city (matches iOS)
            selectedTransferType == "Airport to City" || 
            selectedTransferType == "Airport to Airport" || 
            selectedTransferType == "Airport to Cruise Port" -> {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Airport selection
                    EditableField(
                        label = "SELECT AIRPORT",
                        value = selectedPickupAirport?.displayName ?: "Search airports...",
                        onClick = onAirportClick,
                        isError = pickupAirportError,
                        errorMessage = if (pickupAirportError) "Airport is required" else null
                    )

                    // Airline selection (matches iOS) - required for pickup airport
                    EditableField(
                        label = "SELECT AIRLINE",
                        value = selectedPickupAirline?.displayName ?: "Select Airline",
                        onClick = onAirlineClick,
                        isError = pickupAirlineError,
                        errorMessage = if (pickupAirlineError) "Airline is required" else null
                    )
                    
                    // Flight number - required for pickup airport
                    EditableTextField(
                        label = "FLIGHT / TAIL #",
                        value = pickupFlightNumber,
                        onValueChange = onFlightNumberChange,
                        error = if (pickupFlightError) "Flight number is required" else null
                    )

                    // Origin Airport / City (matches iOS)
                    EditableTextField(
                        label = "ORIGIN AIRPORT / CITY",
                        value = originAirportCity,
                        onValueChange = onOriginCityChange,
                        error = if (originCityError) "Origin airport city is required" else null
                    )
                }
            }
            // Cruise Port pickup - show address + cruise ship details (matches iOS)
            selectedTransferType == "Cruise Port to City" ||
            selectedTransferType == "Cruise Port to Airport" -> {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "PICKUP ADDRESS",
                            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = LimoBlack)
                        )
                        LocationAutocomplete(
                            value = pickupLocation,
                            onValueChange = onLocationChange,
                            onLocationSelected = onLocationSelected,
                            placeholder = "Enter pickup address"
                        )
                    }

                    EditableTextField(
                        label = "CRUISE PORT",
                        value = cruisePort,
                        onValueChange = onCruisePortChange,
                        error = if (cruisePickupPortError) "Cruise port is required" else null
                    )

                    EditableTextField(
                        label = "CRUISE SHIP NAME",
                        value = cruiseShipName,
                        onValueChange = onCruiseShipChange,
                        error = if (cruisePickupShipError) "Cruise ship name is required" else null
                    )
                    
                    EditableField(
                        label = "SHIP ARRIVAL TIME",
                        value = shipArrivalTime.ifEmpty { "Select time" },
                        onClick = onShipArrivalClick,
                        icon = Icons.Default.AccessTime,
                        isError = false
                    )
                }
            }
        }
    }
}

/**
 * Drop-off Section
 * Matches iOS dropoffSection - fields change based on transfer type
 */
@Composable
fun DropoffSection(
    selectedTransferType: String,
    dropoffLocation: String,
    dropoffFlightNumber: String,
    cruiseShipName: String,
    shipArrivalTime: String,
    cruisePort: String,
    selectedDropoffAirport: Airport?,
    selectedDropoffAirline: Airline?,
    onLocationSelected: (String, String, String, String, String, Double?, Double?, String?) -> Unit, // (fullAddress, city, state, zipCode, locationDisplay, latitude, longitude, country)
    onLocationChange: (String) -> Unit,
    onAirportClick: () -> Unit,
    onAirlineClick: () -> Unit,
    onFlightNumberChange: (String) -> Unit,
    onCruiseShipChange: (String) -> Unit,
    onShipArrivalChange: (String) -> Unit,
    onShipArrivalClick: () -> Unit,
    onCruisePortChange: (String) -> Unit,
    // Error states
    dropoffLocationError: Boolean = false,
    dropoffCoordinatesError: Boolean = false,
    dropoffAddressValidationError: String? = null, // Address validation error message from Directions API
    airportDropoffError: Boolean = false,
    cruiseDropoffError: Boolean = false,
    cruiseDropoffPortError: Boolean = false,
    cruiseDropoffShipError: Boolean = false,
    // Specific airport field errors
    dropoffAirportError: Boolean = false,
    dropoffAirlineError: Boolean = false
    // Note: dropoff flight is NOT required (matches web app)
) {
    SectionHeader("Drop-off")
    
    Column {
        // Dynamic dropoff fields based on transfer type (matches iOS logic exactly)
        when {
            // City dropoff - show address field with Google Places autocomplete
            selectedTransferType == "City to City" || 
            selectedTransferType == "Airport to City" || 
            selectedTransferType == "Cruise Port to City" -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "DROP-OFF ADDRESS",
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = LimoBlack)
                    )
                    LocationAutocomplete(
                        value = dropoffLocation,
                        onValueChange = onLocationChange,
                        onLocationSelected = onLocationSelected,
                        placeholder = "Enter drop-off address",
                        error = dropoffAddressValidationError ?: if (dropoffLocationError) "Dropoff location required" else if (dropoffCoordinatesError) "Select location with coordinates" else null
                    )
                }
            }
            // Airport dropoff - show airport, airline, flight number (matches iOS)
            selectedTransferType == "City to Airport" ||
            selectedTransferType == "Airport to Airport" ||
            selectedTransferType == "Cruise Port to Airport" -> {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Airport selection
                    EditableField(
                        label = "SELECT AIRPORT",
                        value = selectedDropoffAirport?.displayName ?: "Search airports...",
                        onClick = onAirportClick,
                        isError = dropoffAirportError,
                        errorMessage = if (dropoffAirportError) "Airport is required" else null
                    )

                    // Airline selection (matches iOS) - required for dropoff airport, flight is NOT required
                    EditableField(
                        label = "SELECT AIRLINE",
                        value = selectedDropoffAirline?.displayName ?: "Select Airline",
                        onClick = onAirlineClick,
                        isError = dropoffAirlineError,
                        errorMessage = if (dropoffAirlineError) "Airline is required" else null
                    )

                    // Flight number - NOT required for dropoff airport (matches web app)
                    EditableTextField(
                        label = "FLIGHT / TAIL #",
                        value = dropoffFlightNumber,
                        onValueChange = onFlightNumberChange,
                        error = null // Flight number is NOT required for dropoff airport
                    )
                }
            }
            // Cruise Port dropoff - show address + cruise ship details (matches iOS)
            selectedTransferType == "City to Cruise Port" ||
            selectedTransferType == "Airport to Cruise Port" -> {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "DROP-OFF ADDRESS",
                            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = LimoBlack)
                        )
                        LocationAutocomplete(
                            value = dropoffLocation,
                            onValueChange = onLocationChange,
                            onLocationSelected = onLocationSelected,
                            placeholder = "Enter drop-off address"
                        )
                    }

                    EditableTextField(
                        label = "CRUISE PORT",
                        value = cruisePort,
                        onValueChange = onCruisePortChange,
                        error = if (cruiseDropoffPortError) "Cruise port is required" else null
                    )

                    EditableTextField(
                        label = "CRUISE SHIP NAME",
                        value = cruiseShipName,
                        onValueChange = onCruiseShipChange,
                        error = if (cruiseDropoffShipError) "Cruise ship name is required" else null
                    )
                    
                    EditableField(
                        label = "SHIP Departure TIME",
                        value = shipArrivalTime.ifEmpty { "Select time" },
                        onClick = onShipArrivalClick,
                        icon = Icons.Default.AccessTime,
                        isError = false
                    )
                }
            }
        }
    }
}

