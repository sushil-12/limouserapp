package com.example.limouserapp.ui.booking.comprehensivebooking.sections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
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
import com.example.limouserapp.ui.theme.LimoOrange

/**
 * Return Journey Section
 * Matches iOS returnJourneySection - complete return trip configuration
 */
@Composable
fun ReturnJourneySection(
    selectedReturnServiceType: String?,
    selectedReturnTransferType: String?,
    selectedReturnMeetAndGreet: String?,
    returnPickupDate: String,
    returnPickupTime: String,
    returnPickupLocation: String,
    returnDropoffLocation: String,
    returnPickupFlightNumber: String,
    returnDropoffFlightNumber: String,
    returnOriginAirportCity: String,
    returnCruisePort: String,
    returnCruiseShipName: String,
    returnShipArrivalTime: String,
    returnSpecialInstructions: String,
    returnNumberOfVehicles: Int,
    selectedReturnHours: String,
    selectedReturnPickupAirport: Airport?,
    selectedReturnDropoffAirport: Airport?,
    selectedReturnPickupAirline: Airline?,
    selectedReturnDropoffAirline: Airline?,
    serviceTypes: List<String>,
    transferTypes: List<String>,
    hoursOptions: List<String>,
    meetAndGreetOptions: List<String>,
    showReturnServiceTypeDropdown: Boolean,
    showReturnTransferTypeDropdown: Boolean,
    showReturnMeetAndGreetDropdown: Boolean,
    showReturnDatePicker: Boolean,
    showReturnTimePicker: Boolean,
    onReturnServiceTypeSelected: (String) -> Unit,
    onReturnTransferTypeSelected: (String) -> Unit,
    onReturnMeetAndGreetSelected: (String) -> Unit,
    onReturnDateClick: () -> Unit,
    onReturnTimeClick: () -> Unit,
    onReturnPickupLocationSelected: (String, String, String, String, String, Double?, Double?, String?) -> Unit, // (fullAddress, city, state, zipCode, locationDisplay, latitude, longitude, country)
    onReturnPickupLocationChange: (String) -> Unit,
    onReturnDropoffLocationSelected: (String, String, String, String, String, Double?, Double?, String?) -> Unit, // (fullAddress, city, state, zipCode, locationDisplay, latitude, longitude, country)
    onReturnDropoffLocationChange: (String) -> Unit,
    onReturnPickupAirportClick: () -> Unit,
    onReturnDropoffAirportClick: () -> Unit,
    onReturnPickupAirlineClick: () -> Unit,
    onReturnDropoffAirlineClick: () -> Unit,
    onReturnFlightNumberChange: (String) -> Unit,
    onReturnDropoffFlightNumberChange: (String) -> Unit,
    onReturnOriginCityChange: (String) -> Unit,
    onReturnCruisePortChange: (String) -> Unit,
    onReturnCruiseShipChange: (String) -> Unit,
    onReturnShipArrivalChange: (String) -> Unit,
    onReturnSpecialInstructionsChange: (String) -> Unit,
    onReturnServiceTypeDropdownChange: (Boolean) -> Unit,
    onReturnTransferTypeDropdownChange: (Boolean) -> Unit,
    onReturnMeetAndGreetDropdownChange: (Boolean) -> Unit,
    // Return Extra Stops parameters
    returnExtraStops: List<com.example.limouserapp.ui.booking.comprehensivebooking.ExtraStop>,
    onReturnExtraStopsAdd: () -> Unit,
    onReturnExtraStopsRemove: (com.example.limouserapp.ui.booking.comprehensivebooking.ExtraStop) -> Unit,
    onReturnExtraStopsLocationSelected: (com.example.limouserapp.ui.booking.comprehensivebooking.ExtraStop, String, String, String, String, String, Double?, Double?, String?) -> Unit,
    onReturnExtraStopsLocationChange: (com.example.limouserapp.ui.booking.comprehensivebooking.ExtraStop, String) -> Unit,
    onReturnExtraStopsInstructionsChange: (com.example.limouserapp.ui.booking.comprehensivebooking.ExtraStop, String) -> Unit,
    // Return Distance parameters
    returnDistance: Pair<String, String>?,
    returnDistanceLoading: Boolean,
    // Error states
    returnPickupDateTimeError: Boolean = false,
    returnPickupLocationError: Boolean = false,
    returnPickupCoordinatesError: Boolean = false,
    returnPickupAirportError: Boolean = false,
    returnPickupAirlineError: Boolean = false,
    returnPickupFlightError: Boolean = false,
    returnOriginCityError: Boolean = false,
    returnDropoffLocationError: Boolean = false,
    returnDropoffCoordinatesError: Boolean = false,
    returnDropoffAirportError: Boolean = false,
    returnDropoffAirlineError: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        // Return Transfer Type, Meet & Greet (Service Type removed - it's the same as main service type)
        BookingSection(
            title = "Return Booking Details",
            titleColor = LimoOrange,
            titleSize = 18.sp
        ) {
            
            StyledDropdown(
                label = "TRANSFER TYPE",
                value = selectedReturnTransferType ?: "Select Transfer Type",
                options = transferTypes,
                expanded = showReturnTransferTypeDropdown,
                onExpand = onReturnTransferTypeDropdownChange,
                onSelect = onReturnTransferTypeSelected
            )
            Spacer(Modifier.height(4.dp))
            
            StyledDropdown(
                label = "MEET & GREET",
                value = selectedReturnMeetAndGreet ?: "Select Meet & Greet Option",
                options = meetAndGreetOptions,
                expanded = showReturnMeetAndGreetDropdown,
                onExpand = onReturnMeetAndGreetDropdownChange,
                onSelect = onReturnMeetAndGreetSelected
            )
            
            Spacer(Modifier.height(4.dp))
            
            // Return Travel Date & Time (side by side - matches iOS)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                EditableField(
                    label = "RETURN DATE",
                    value = if (returnPickupDate.isNotEmpty()) formatDate(returnPickupDate) else "Select date",
                    onClick = onReturnDateClick,
                    icon = Icons.Default.CalendarToday,
                    modifier = Modifier.weight(1f),
                    isError = returnPickupDateTimeError,
                    errorMessage = if (returnPickupDateTimeError) "Return date is required" else null
                )
                
                EditableField(
                    label = "RETURN TIME",
                    value = if (returnPickupTime.isNotEmpty()) formatTime(returnPickupTime) else "Select time",
                    onClick = onReturnTimeClick,
                    icon = Icons.Default.AccessTime,
                    modifier = Modifier.weight(1f),
                    isError = returnPickupDateTimeError,
                    errorMessage = if (returnPickupDateTimeError) "Return time is required" else null
                )
            }
        }
        
        // Return Pickup Section
        if (selectedReturnTransferType != null) {
            ReturnPickupSection(
                selectedReturnTransferType = selectedReturnTransferType,
                returnPickupLocation = returnPickupLocation,
                returnPickupFlightNumber = returnPickupFlightNumber,
                returnOriginAirportCity = returnOriginAirportCity,
                returnCruisePort = returnCruisePort,
                returnCruiseShipName = returnCruiseShipName,
                returnShipArrivalTime = returnShipArrivalTime,
                selectedReturnPickupAirport = selectedReturnPickupAirport,
                selectedReturnPickupAirline = selectedReturnPickupAirline,
                onLocationSelected = onReturnPickupLocationSelected,
                onLocationChange = onReturnPickupLocationChange,
                onAirportClick = onReturnPickupAirportClick,
                onAirlineClick = onReturnPickupAirlineClick,
                onFlightNumberChange = onReturnFlightNumberChange,
                onOriginCityChange = onReturnOriginCityChange,
                onCruisePortChange = onReturnCruisePortChange,
                onCruiseShipChange = onReturnCruiseShipChange,
                onShipArrivalChange = onReturnShipArrivalChange,
                returnPickupLocationError = returnPickupLocationError,
                returnPickupCoordinatesError = returnPickupCoordinatesError,
                returnPickupAirportError = returnPickupAirportError,
                returnPickupAirlineError = returnPickupAirlineError,
                returnPickupFlightError = returnPickupFlightError,
                returnOriginCityError = returnOriginCityError,
                returnPickupDateTimeError = returnPickupDateTimeError
            )
        }
        
        // Return Dropoff Section
        if (selectedReturnTransferType != null) {
            ReturnDropoffSection(
                selectedReturnTransferType = selectedReturnTransferType,
                returnDropoffLocation = returnDropoffLocation,
                returnDropoffFlightNumber = returnDropoffFlightNumber,
                returnCruisePort = returnCruisePort,
                returnCruiseShipName = returnCruiseShipName,
                returnShipArrivalTime = returnShipArrivalTime,
                selectedReturnDropoffAirport = selectedReturnDropoffAirport,
                selectedReturnDropoffAirline = selectedReturnDropoffAirline,
                onLocationSelected = onReturnDropoffLocationSelected,
                onLocationChange = onReturnDropoffLocationChange,
                onAirportClick = onReturnDropoffAirportClick,
                onAirlineClick = onReturnDropoffAirlineClick,
                onFlightNumberChange = onReturnDropoffFlightNumberChange,
                onCruisePortChange = onReturnCruisePortChange,
                onCruiseShipChange = onReturnCruiseShipChange,
                onShipArrivalChange = onReturnShipArrivalChange,
                returnDropoffLocationError = returnDropoffLocationError,
                returnDropoffCoordinatesError = returnDropoffCoordinatesError,
                returnDropoffAirportError = returnDropoffAirportError,
                returnDropoffAirlineError = returnDropoffAirlineError
            )
        }
        
        // Return Distance Information (positioned right after Return Dropoff Section)
        // This ensures proper ordering: Return Booking Details → Return Pickup → Return Drop-off → Return estimated distance & time
        Spacer(Modifier.height(24.dp))
        ReturnDistanceInformationSection(
            returnDistance = returnDistance ?: Pair("", ""),
            isLoading = returnDistanceLoading
        )
        
        // Return Extra Stops Section (positioned right after Return Distance Information)
        if (selectedReturnTransferType != null) {
            Spacer(Modifier.height(16.dp))
            ReturnExtraStopsSection(
                returnExtraStops = returnExtraStops,
                onAddStop = onReturnExtraStopsAdd,
                onRemoveStop = onReturnExtraStopsRemove,
                onLocationSelected = onReturnExtraStopsLocationSelected,
                onLocationChange = onReturnExtraStopsLocationChange,
                onInstructionsChange = onReturnExtraStopsInstructionsChange,
            )
        }
        
        // Return Special Instructions
        BookingSection(
            title = "Return Special Instructions",
            titleColor = LimoOrange,
            titleSize = 18.sp
        ) {
            OutlinedTextField(
                value = returnSpecialInstructions,
                onValueChange = onReturnSpecialInstructionsChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp),
                placeholder = { Text("Enter return trip special instructions...") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Gray.copy(alpha = 0.15f),
                    unfocusedContainerColor = Color.Gray.copy(alpha = 0.15f),
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = LimoBlack
                ),
                textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = LimoBlack),
                shape = RoundedCornerShape(8.dp),
                minLines = 3
            )
        }
    }
}

/**
 * Return Pickup Section
 * Matches iOS returnPickupSection - dynamic fields based on return transfer type
 */
@Composable
fun ReturnPickupSection(
    selectedReturnTransferType: String,
    returnPickupLocation: String,
    returnPickupFlightNumber: String,
    returnOriginAirportCity: String,
    returnCruisePort: String,
    returnCruiseShipName: String,
    returnShipArrivalTime: String,
    selectedReturnPickupAirport: Airport?,
    selectedReturnPickupAirline: Airline?,
    onLocationSelected: (String, String, String, String, String, Double?, Double?, String?) -> Unit, // (fullAddress, city, state, zipCode, locationDisplay, latitude, longitude, country)
    onLocationChange: (String) -> Unit,
    onAirportClick: () -> Unit,
    onAirlineClick: () -> Unit,
    onFlightNumberChange: (String) -> Unit,
    onOriginCityChange: (String) -> Unit,
    onCruisePortChange: (String) -> Unit,
    onCruiseShipChange: (String) -> Unit,
    onShipArrivalChange: (String) -> Unit,
    // Error states
    returnPickupLocationError: Boolean = false,
    returnPickupCoordinatesError: Boolean = false,
    returnPickupAirportError: Boolean = false,
    returnPickupAirlineError: Boolean = false,
    returnPickupFlightError: Boolean = false,
    returnOriginCityError: Boolean = false,
    returnPickupDateTimeError: Boolean = false
) {
    BookingSection(
        title = "Return Pick-up",
        titleColor = LimoOrange,
        titleSize = 18.sp
    ) {
        // Dynamic return pickup fields based on return transfer type (matches iOS logic)
        when {
            // City pickup - show address field with Google Places autocomplete
            selectedReturnTransferType == "City to City" || 
            selectedReturnTransferType == "City to Airport" || 
            selectedReturnTransferType == "City to Cruise Port" -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "RETURN PICKUP ADDRESS",
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = LimoBlack)
                    )
                    LocationAutocomplete(
                        value = returnPickupLocation,
                        onValueChange = onLocationChange,
                        onLocationSelected = onLocationSelected,
                        placeholder = "Enter return pickup address",
                        error = if (returnPickupLocationError) "Return pickup location is required" else if (returnPickupCoordinatesError) "Select location with coordinates" else null
                    )
                }
            }
            // Airport pickup - show airport, airline, flight number, origin city (matches iOS)
            selectedReturnTransferType == "Airport to City" || 
            selectedReturnTransferType == "Airport to Airport" || 
            selectedReturnTransferType == "Airport to Cruise Port" -> {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    EditableField(
                        label = "SELECT AIRPORT",
                        value = selectedReturnPickupAirport?.displayName ?: "Search airports...",
                        onClick = onAirportClick,
                        isError = returnPickupAirportError,
                        errorMessage = if (returnPickupAirportError) "Return pickup airport is required" else null
                    )
                    
                    EditableField(
                        label = "SELECT AIRLINE",
                        value = selectedReturnPickupAirline?.displayName ?: "Select Airline",
                        onClick = onAirlineClick,
                        isError = returnPickupAirlineError,
                        errorMessage = if (returnPickupAirlineError) "Return pickup airline is required" else null
                    )
                    
                    EditableTextField(
                        label = "FLIGHT / TAIL #",
                        value = returnPickupFlightNumber,
                        onValueChange = onFlightNumberChange,
                        error = if (returnPickupFlightError) "Return pickup flight number is required" else null
                    )
                    
                    EditableTextField(
                        label = "ORIGIN AIRPORT / CITY",
                        value = returnOriginAirportCity,
                        onValueChange = onOriginCityChange,
                        error = if (returnOriginCityError) "Return origin airport city is required" else null
                    )
                }
            }
            // Cruise Port pickup - show address + cruise ship details (matches iOS)
            selectedReturnTransferType == "Cruise Port to City" || 
            selectedReturnTransferType == "Cruise Port to Airport" ||
            selectedReturnTransferType == "Cruise Port to Cruise Port" -> {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "RETURN PICKUP ADDRESS",
                            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = LimoBlack)
                        )
                        LocationAutocomplete(
                            value = returnPickupLocation,
                            onValueChange = onLocationChange,
                            onLocationSelected = onLocationSelected,
                            placeholder = "Enter return pickup address",
                            error = if (returnPickupLocationError) "Return pickup location is required" else if (returnPickupCoordinatesError) "Select location with coordinates" else null
                        )
                    }
                    
                    EditableTextField(
                        label = "CRUISE PORT",
                        value = returnCruisePort,
                        onValueChange = onCruisePortChange,
                        error = if (returnPickupLocationError) "Cruise port is required" else null
                    )
                    
                    EditableTextField(
                        label = "CRUISE SHIP NAME",
                        value = returnCruiseShipName,
                        onValueChange = onCruiseShipChange,
                        error = if (returnPickupLocationError) "Cruise ship name is required" else null
                    )
                    
                    EditableField(
                        label = "SHIP ARRIVAL TIME",
                        value = if (returnShipArrivalTime.isNotEmpty()) returnShipArrivalTime else "Select time",
                        onClick = { /* TODO: Add time picker */ },
                        icon = Icons.Default.AccessTime
                    )
                }
            }
        }
    }
}

/**
 * Return Dropoff Section
 * Matches iOS returnDropoffSection - dynamic fields based on return transfer type
 */
@Composable
fun ReturnDropoffSection(
    selectedReturnTransferType: String,
    returnDropoffLocation: String,
    returnDropoffFlightNumber: String,
    returnCruisePort: String,
    returnCruiseShipName: String,
    returnShipArrivalTime: String,
    selectedReturnDropoffAirport: Airport?,
    selectedReturnDropoffAirline: Airline?,
    onLocationSelected: (String, String, String, String, String, Double?, Double?, String?) -> Unit, // (fullAddress, city, state, zipCode, locationDisplay, latitude, longitude, country)
    onLocationChange: (String) -> Unit,
    onAirportClick: () -> Unit,
    onAirlineClick: () -> Unit,
    onFlightNumberChange: (String) -> Unit,
    onCruisePortChange: (String) -> Unit,
    onCruiseShipChange: (String) -> Unit,
    onShipArrivalChange: (String) -> Unit,
    // Error states
    returnDropoffLocationError: Boolean = false,
    returnDropoffCoordinatesError: Boolean = false,
    returnDropoffAirportError: Boolean = false,
    returnDropoffAirlineError: Boolean = false
) {
    BookingSection(
        title = "Return Drop-off",
        titleColor = LimoOrange,
        titleSize = 18.sp
    ) {
        // Dynamic return dropoff fields based on return transfer type (matches iOS logic)
        when {
            // City dropoff - show address field with Google Places autocomplete
            selectedReturnTransferType == "City to City" || 
            selectedReturnTransferType == "Airport to City" || 
            selectedReturnTransferType == "Cruise Port to City" -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "RETURN DROPOFF ADDRESS",
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = LimoBlack)
                    )
                    LocationAutocomplete(
                        value = returnDropoffLocation,
                        onValueChange = onLocationChange,
                        onLocationSelected = onLocationSelected,
                        placeholder = "Enter return dropoff address",
                        error = if (returnDropoffLocationError) "Return dropoff location is required" else if (returnDropoffCoordinatesError) "Select location with coordinates" else null
                    )
                }
            }
            // Airport dropoff - show airport, airline, flight number (matches iOS)
            selectedReturnTransferType == "City to Airport" || 
            selectedReturnTransferType == "Airport to Airport" || 
            selectedReturnTransferType == "Cruise Port to Airport" -> {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    EditableField(
                        label = "SELECT AIRPORT",
                        value = selectedReturnDropoffAirport?.displayName ?: "Search airports...",
                        onClick = onAirportClick,
                        isError = returnDropoffAirportError,
                        errorMessage = if (returnDropoffAirportError) "Return dropoff airport is required" else null
                    )
                    
                    EditableField(
                        label = "SELECT AIRLINE",
                        value = selectedReturnDropoffAirline?.displayName ?: "Select Airline",
                        onClick = onAirlineClick,
                        isError = returnDropoffAirlineError,
                        errorMessage = if (returnDropoffAirlineError) "Return dropoff airline is required" else null
                    )
                    
                    EditableTextField(
                        label = "FLIGHT / TAIL #",
                        value = returnDropoffFlightNumber,
                        onValueChange = onFlightNumberChange
                    )
                }
            }
            // Cruise Port dropoff - show address + cruise ship details (matches iOS)
            selectedReturnTransferType == "City to Cruise Port" || 
            selectedReturnTransferType == "Airport to Cruise Port" ||
            selectedReturnTransferType == "Cruise Port to Cruise Port" -> {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "RETURN DROPOFF ADDRESS",
                            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = LimoBlack)
                        )
                        LocationAutocomplete(
                            value = returnDropoffLocation,
                            onValueChange = onLocationChange,
                            onLocationSelected = onLocationSelected,
                            placeholder = "Enter return dropoff address",
                            error = if (returnDropoffLocationError) "Return dropoff location is required" else if (returnDropoffCoordinatesError) "Select location with coordinates" else null
                        )
                    }
                    
                    EditableTextField(
                        label = "CRUISE PORT",
                        value = returnCruisePort,
                        onValueChange = onCruisePortChange,
                        error = if (returnDropoffLocationError) "Cruise port is required" else null
                    )
                    
                    EditableTextField(
                        label = "CRUISE SHIP NAME",
                        value = returnCruiseShipName,
                        onValueChange = onCruiseShipChange,
                        error = if (returnDropoffLocationError) "Cruise ship name is required" else null
                    )
                    
                    EditableField(
                        label = "SHIP ARRIVAL TIME",
                        value = if (returnShipArrivalTime.isNotEmpty()) returnShipArrivalTime else "Select time",
                        onClick = { /* TODO: Add time picker */ },
                        icon = Icons.Default.AccessTime
                    )
                }
            }
        }
    }
}

