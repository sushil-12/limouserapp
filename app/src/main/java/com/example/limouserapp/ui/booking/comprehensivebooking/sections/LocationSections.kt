package com.example.limouserapp.ui.booking.comprehensivebooking.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    onCruisePortChange: (String) -> Unit
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
                modifier = Modifier.weight(1f)
            )

            EditableField(
                label = "PICKUP TIME",
                value = formatTime(pickupTime),
                onClick = onTimeClick,
                icon = Icons.Default.AccessTime,
                modifier = Modifier.weight(1f)
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
                        placeholder = "Enter pickup address"
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
                        onClick = onAirportClick
                    )
                    
                    // Airline selection (matches iOS)
                    EditableField(
                        label = "SELECT AIRLINE",
                        value = selectedPickupAirline?.displayName ?: "Select Airline",
                        onClick = onAirlineClick
                    )
                    
                    // Flight number
                    EditableTextField(
                        label = "FLIGHT / TAIL #",
                        value = pickupFlightNumber,
                        onValueChange = onFlightNumberChange
                    )
                    
                    // Origin Airport / City (matches iOS)
                    EditableTextField(
                        label = "ORIGIN AIRPORT / CITY",
                        value = originAirportCity,
                        onValueChange = onOriginCityChange
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
                        onValueChange = onCruisePortChange
                    )
                    
                    EditableTextField(
                        label = "CRUISE SHIP NAME",
                        value = cruiseShipName,
                        onValueChange = onCruiseShipChange
                    )
                    
                    EditableTextField(
                        label = "SHIP ARRIVAL TIME",
                        value = shipArrivalTime,
                        onValueChange = onShipArrivalChange
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
    onCruisePortChange: (String) -> Unit
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
                        placeholder = "Enter drop-off address"
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
                        onClick = onAirportClick
                    )
                    
                    // Airline selection (matches iOS)
                    EditableField(
                        label = "SELECT AIRLINE",
                        value = selectedDropoffAirline?.displayName ?: "Select Airline",
                        onClick = onAirlineClick
                    )
                    
                    // Flight number
                    EditableTextField(
                        label = "FLIGHT / TAIL #",
                        value = dropoffFlightNumber,
                        onValueChange = onFlightNumberChange
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
                        onValueChange = onCruisePortChange
                    )
                    
                    EditableTextField(
                        label = "CRUISE SHIP NAME",
                        value = cruiseShipName,
                        onValueChange = onCruiseShipChange
                    )
                    
                    EditableTextField(
                        label = "SHIP ARRIVAL TIME",
                        value = shipArrivalTime,
                        onValueChange = onShipArrivalChange
                    )
                }
            }
        }
    }
}

