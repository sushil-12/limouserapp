package com.example.limouserapp.ui.booking.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.limouserapp.ui.theme.AppTextStyles
import com.example.limouserapp.ui.theme.LimoBlack

/**
 * Header component for Schedule Ride Bottom Sheet
 */
@Composable
fun ScheduleRideHeader(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = LimoBlack)
            }
            Text(
                text = "Schedule Ride",
                style = AppTextStyles.buttonLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.size(16.dp))
        }
        Text(
            text = "Free Prearranged Quotes in Over 40 Countries.",
            style = AppTextStyles.captionSmall.copy(fontSize = 14.sp),
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Divider(color = Color(0xFFEAEAEA), thickness = 1.dp)
    }
}

