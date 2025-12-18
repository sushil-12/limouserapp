package com.example.limouserapp.ui.booking.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.limouserapp.data.model.booking.RecentLocation
import com.example.limouserapp.ui.theme.GoogleSansFamily
import com.example.limouserapp.ui.theme.LimoOrange

/**
 * Recent locations list component
 * Displays recent pickup or dropoff locations
 */
@Composable
fun RecentLocationsList(
    locations: List<RecentLocation>,
    onLocationSelected: (RecentLocation) -> Unit,
    modifier: Modifier = Modifier
) {
    if (locations.isEmpty()) {
        return
    }
    
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(locations) { location ->
            RecentLocationItem(
                location = location,
                onClick = { onLocationSelected(location) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun RecentLocationItem(
    location: RecentLocation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Location icon - use airport icon if it's an airport, otherwise use place icon
        Icon(
            imageVector = if (location.isAirport) Icons.Default.FlightTakeoff else Icons.Default.Place,
            contentDescription = "Location",
            tint = if (location.isAirport) LimoOrange.copy(alpha = 0.9f) else LimoOrange.copy(alpha = 0.8f),
            modifier = Modifier
                .padding(end = 12.dp)
                .size(20.dp)
        )
        
        // Location address
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = location.address,
                fontFamily = GoogleSansFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            if (location.isAirport && location.airportCode != null) {
                Text(
                    text = location.airportCode,
                    fontFamily = GoogleSansFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.Gray
                )
            }
        }
    }
    
    Divider(color = Color(0xFFEAEAEA), thickness = 1.dp)
}

