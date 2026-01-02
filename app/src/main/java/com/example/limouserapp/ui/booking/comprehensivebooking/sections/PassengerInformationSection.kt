package com.example.limouserapp.ui.booking.comprehensivebooking.sections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.limouserapp.ui.booking.comprehensivebooking.StyledInput
import com.example.limouserapp.ui.theme.LimoBlack

/**
 * Passenger Information Section - editable fields for name, email, and mobile
 * Matches web app passenger information section
 */
@Composable
fun PassengerInformationSection(
    passengerName: String,
    passengerEmail: String,
    passengerMobile: String,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onMobileChange: (String) -> Unit,
    nameError: Boolean = false,
    emailError: Boolean = false,
    mobileError: Boolean = false,
    nameErrorMessage: String? = null,
    emailErrorMessage: String? = null,
    mobileErrorMessage: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section Header
            Text(
                text = "Passenger Information",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = LimoBlack
                )
            )
            
            // Passenger Name Field
            StyledInput(
                label = "Passenger Name",
                value = passengerName,
                onValueChange = onNameChange,
                placeholder = "Enter passenger name",
                isRequired = true,
                isError = nameError,
                errorMessage = nameErrorMessage
            )
            
            // Passenger Email Field
            StyledInput(
                label = "Passenger Email",
                value = passengerEmail,
                onValueChange = onEmailChange,
                placeholder = "Enter email address",
                isRequired = true,
                isError = emailError,
                errorMessage = emailErrorMessage
            )
            
            // Passenger Mobile Field
            StyledInput(
                label = "Passenger Cell",
                value = passengerMobile,
                onValueChange = onMobileChange,
                placeholder = "1234567890",
                isRequired = true,
                isError = mobileError,
                errorMessage = mobileErrorMessage
            )
        }
    }
}

