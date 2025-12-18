package com.example.limouserapp.ui.booking.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.limouserapp.R
import com.example.limouserapp.data.model.booking.BookingType
import com.example.limouserapp.ui.theme.LimoBlack
import com.example.limouserapp.ui.theme.GoogleSansFamily

/**
 * Booking type dropdowns for pickup and destination
 */
@Composable
fun BookingTypeSelection(
    selectedBookingType: BookingType,
    selectedDestinationType: BookingType,
    onBookingTypeSelected: (BookingType) -> Unit,
    onDestinationTypeSelected: (BookingType) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPickupDropdown by remember { mutableStateOf(false) }
    var showDestinationDropdown by remember { mutableStateOf(false) }
    val borderColor = Color(0xFF252C3E)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pickup booking type dropdown
        BookingTypeDropdown(
            selectedType = selectedBookingType,
            onTypeSelected = {
                onBookingTypeSelected(it)
                showPickupDropdown = false
            },
            expanded = showPickupDropdown,
            onExpandedChange = { showPickupDropdown = it },
            modifier = Modifier.weight(1f)
        )

        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = LimoBlack)

        // Destination booking type dropdown
        BookingTypeDropdown(
            selectedType = selectedDestinationType,
            onTypeSelected = {
                onDestinationTypeSelected(it)
                showDestinationDropdown = false
            },
            expanded = showDestinationDropdown,
            onExpandedChange = { showDestinationDropdown = it },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BookingTypeDropdown(
    selectedType: BookingType,
    onTypeSelected: (BookingType) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = Color(0xFF252C3E)
    
    Box(modifier) {
        OutlinedButton(
            onClick = { onExpandedChange(!expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, borderColor),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = borderColor
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    selectedType.displayName,
                    fontFamily = GoogleSansFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 22.sp,
                    color = borderColor,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Icon(
                    painter = painterResource(id = R.drawable.dropdown_arrow),
                    contentDescription = null,
                    modifier = Modifier.size(8.dp),
                    tint = borderColor
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            BookingType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.displayName) },
                    onClick = {
                        onTypeSelected(type)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

