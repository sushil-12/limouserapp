package com.example.limouserapp.ui.booking.comprehensivebooking.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.limouserapp.data.model.booking.RideData
import com.example.limouserapp.data.model.booking.Vehicle
import com.example.limouserapp.ui.booking.comprehensivebooking.*
import com.example.limouserapp.ui.theme.LimoBlack

/**
 * Booking Details Section
 * Matches iOS bookingDetailsSection - all fields editable
 */
@Composable
fun BookingDetailsSection(
    rideData: RideData,
    vehicle: Vehicle,
    pickupDate: String,
    pickupTime: String,
    selectedServiceType: String,
    selectedTransferType: String,
    selectedHours: String,
    numberOfVehicles: Int,
    serviceTypes: List<String>,
    transferTypes: List<String>,
    hoursOptions: List<String>,
    showServiceTypeDropdown: Boolean,
    showTransferTypeDropdown: Boolean,
    showHoursDropdown: Boolean,
    onServiceTypeSelected: (String) -> Unit,
    onTransferTypeSelected: (String) -> Unit,
    onHoursSelected: (String) -> Unit,
    onNumberOfVehiclesChange: (Int) -> Unit,
    onServiceTypeDropdownChange: (Boolean) -> Unit,
    onTransferTypeDropdownChange: (Boolean) -> Unit,
    onHoursDropdownChange: (Boolean) -> Unit,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
    isEditMode: Boolean,
    passengerCount: String,
    luggageCount: String,
    onPassengerCountChange: (Int) -> Unit,
    onLuggageCountChange: (Int) -> Unit,
    // Meet & Greet fields (matches web app)
    selectedMeetAndGreet: String,
    meetAndGreetOptions: List<String>,
    showMeetAndGreetDropdown: Boolean,
    onMeetAndGreetChange: (String) -> Unit,
    onMeetAndGreetDropdownChange: (Boolean) -> Unit,
    // Error states
    serviceTypeError: Boolean = false,
    transferTypeError: Boolean = false,
    pickupDateTimeError: Boolean = false,
    charterHoursError: Boolean = false,
    // Error messages
    serviceTypeErrorMessage: String? = null,
    transferTypeErrorMessage: String? = null,
    pickupDateTimeErrorMessage: String? = null,
    charterHoursErrorMessage: String? = null
) {
    SectionHeader("Booking Details")
    
    StyledDropdown(
        label = "SERVICE TYPE",
        value = selectedServiceType,
        options = serviceTypes,
        expanded = showServiceTypeDropdown,
        onExpand = onServiceTypeDropdownChange,
        onSelect = onServiceTypeSelected,
        isError = serviceTypeError,
        errorMessage = serviceTypeErrorMessage
    )
    Spacer(Modifier.height(12.dp))
    
    StyledDropdown(
        label = "TRANSFER TYPE",
        value = selectedTransferType,
        options = transferTypes,
        expanded = showTransferTypeDropdown,
        onExpand = onTransferTypeDropdownChange,
        onSelect = onTransferTypeSelected,
        isError = transferTypeError,
        errorMessage = transferTypeErrorMessage
    )
    Spacer(Modifier.height(12.dp))
    
    // Meet & Greet Dropdown (matches web app - positioned after transfer type)
    StyledDropdown(
        label = "MEET & GREET",
        value = selectedMeetAndGreet,
        options = meetAndGreetOptions,
        expanded = showMeetAndGreetDropdown,
        onExpand = onMeetAndGreetDropdownChange,
        onSelect = onMeetAndGreetChange
    )
    Spacer(Modifier.height(12.dp))

    StyledInput(
        label = "NO. OF VEHICLES",
        value = numberOfVehicles.toString(),
        onValueChange = { newValue ->
            val intValue = newValue.toIntOrNull()
            if (intValue != null && intValue > 0) {
                onNumberOfVehiclesChange(intValue)
            }
        },
        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
    )
    Spacer(Modifier.height(12.dp))

    // Passengers (editable text field)
    StyledInput(
        label = "PASSENGERS",
        value = passengerCount.toString(),
        onValueChange = { newValue ->
            try {
                val count = newValue.toIntOrNull() ?: 1
                if (count >= 1) {
                    onPassengerCountChange(count)
                } else if (newValue.isEmpty()) {
                    onPassengerCountChange(1)
                }
            } catch (e: Exception) {
                // Ignore invalid input
            }
        },
        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
    )
    Spacer(Modifier.height(12.dp))

    // Luggage (editable text field)
    StyledInput(
        label = "LUGGAGE",
        value = luggageCount.toString(),
        onValueChange = { newValue ->
            try {
                val count = newValue.toIntOrNull() ?: 0
                if (count >= 0) {
                    onLuggageCountChange(count)
                } else if (newValue.isEmpty()) {
                    onLuggageCountChange(0)
                }
            } catch (e: Exception) {
                // Ignore invalid input
            }
        },
        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
    )

    // Hours (editable dropdown - only for Charter Tour - matches iOS)
    if (selectedServiceType == "Charter Tour") {
        Spacer(Modifier.height(12.dp))
        StyledDropdown(
            label = "HOURS",
            value = selectedHours,
            options = hoursOptions,
            expanded = showHoursDropdown,
            onExpand = onHoursDropdownChange,
            onSelect = onHoursSelected,
            isError = charterHoursError,
            errorMessage = charterHoursErrorMessage
        )
    }
}

/**
 * Special Instructions Section
 */
@Composable
fun SpecialInstructionsSection(
    specialInstructions: String,
    onInstructionsChange: (String) -> Unit
) {
    Text(
        "SPECIAL INSTRUCTIONS / EXACT BUILDING NAME",
        style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = com.example.limouserapp.ui.theme.LimoOrange),
        modifier = Modifier.padding(bottom = 8.dp)
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = specialInstructions,
        onValueChange = onInstructionsChange,
        modifier = Modifier
            .fillMaxWidth()
            .background(androidx.compose.ui.graphics.Color(0xFFF5F5F5), androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
            focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent
        ),
        textStyle = TextStyle(fontSize = 13.sp, color = LimoBlack),
        minLines = 4
    )
}

