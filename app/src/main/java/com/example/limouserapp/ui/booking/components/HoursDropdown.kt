package com.example.limouserapp.ui.booking.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.limouserapp.ui.theme.GoogleSansFamily

/**
 * Hours dropdown component for Hourly ride type
 * Shows dropdown with hours options: 2 hours minimum, 3, 4, 5, 6, 8, 10, 12 hours
 */
@Composable
fun HoursDropdown(
    selectedHours: String,
    onHoursSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val hoursOptions = listOf(
        "2 hours minimum",
        "3 hours",
        "4 hours",
        "5 hours",
        "6 hours",
        "8 hours",
        "10 hours",
        "12 hours"
    )
    
    var expanded by remember { mutableStateOf(false) }
    val borderColor = Color(0xFF252C3E)
    
    Box(modifier) {
        OutlinedButton(
            onClick = { expanded = !expanded },
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
                    selectedHours,
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
            onDismissRequest = { expanded = false }
        ) {
            hoursOptions.forEach { hours ->
                DropdownMenuItem(
                    text = { Text(hours) },
                    onClick = {
                        onHoursSelected(hours)
                        expanded = false
                    }
                )
            }
        }
    }
}

