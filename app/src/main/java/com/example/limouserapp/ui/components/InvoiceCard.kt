package com.example.limouserapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.limouserapp.data.model.dashboard.Invoice
import com.example.limouserapp.ui.theme.AppColors
import com.example.limouserapp.ui.theme.LimoOrange

/**
 * Invoice Card Component
 * Matches iOS InvoiceCard design with 5 sections:
 * 1. Top Header (Black) - Date and Vehicle Type
 * 2. Invoice Summary (Light Grey #E6E6E6) - Invoice #, Status, Total
 * 3. Route Details (White) - Visual route indicator with addresses
 * 4. Driver Information (Light Grey) - Driver name, payment method, phone
 * 5. Action Button (White) - Orange "View Invoice Summary" button
 */
@Composable
fun InvoiceCard(
    invoice: Invoice,
    onViewInvoiceSummary: (() -> Unit)? = null,
    onDriverPhoneClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.Gray.copy(alpha = 0.2f)
        )
    ) {
        Column {
            // 1. Top Header (Black)
            TopHeaderView(invoice)
            
            // 2. Invoice Summary (Light Grey #E6E6E6)
            InvoiceSummaryView(invoice)
            
            // 3. Route Details (White)
            RouteDetailsView(invoice)
            
            // 4. Driver Information (Light Grey)
            DriverInfoView(invoice, onDriverPhoneClick)
            
            // 5. Action Button (White) - Orange button
            ActionButtonView(invoice, onViewInvoiceSummary)
        }
    }
}

@Composable
private fun TopHeaderView(invoice: Invoice) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side - Invoice icon and date
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = LimoOrange,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = invoice.formattedDate,
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            )
        }
        
        // Right side - Vehicle type button
        Text(
            text = invoice.vehicleType,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            ),
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(6.dp))
                .border(
                    1.dp,
                    Color.Black,
                    RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun InvoiceSummaryView(invoice: Invoice) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE6E6E6))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Invoice Number
        Text(
            text = "#${invoice.invoiceNumber}",
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Payment Status
        Text(
            text = invoice.status.replaceFirstChar { 
                if (it.isLowerCase()) it.titlecase() else it.toString() 
            },
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = getStatusColor(invoice.status)
            )
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Total Amount
        Text(
            text = "Total ${invoice.formattedTotal}",
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
        )
    }
}

@Composable
private fun RouteDetailsView(invoice: Invoice) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Left side - Visual route indicator
        Column(
            modifier = Modifier.padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pickup circle
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(LimoOrange)
            )
            
            // Connecting line - conditional height based on text length
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(if (shouldUseTallLine(invoice)) 40.dp else 24.dp)
                    .background(Color.Gray.copy(alpha = 0.6f))
            )
            
            // Dropoff square
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(LimoOrange)
            )
        }
        
        // Right side - Location text
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pickup location
            Text(
                text = invoice.pickupAddress,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                ),
                maxLines = 2
            )
            
            // Dropoff location
            Text(
                text = invoice.dropoffAddress,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                ),
                maxLines = 2
            )
        }
    }
}

@Composable
private fun DriverInfoView(
    invoice: Invoice,
    onDriverPhoneClick: ((String) -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Gray.copy(alpha = 0.1f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Driver label
        Text(
            text = "DRIVER",
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )
        )
        
        // Driver name
        Text(
            text = invoice.driverName,
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
        )
        
        // Separator
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(20.dp)
                .background(Color.Gray.copy(alpha = 0.4f))
        )
        
        // Payment method
        Text(
            text = invoice.paymentMethodDisplay,
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Driver phone - tappable
        TextButton(
            onClick = {
                onDriverPhoneClick?.invoke(invoice.driverPhone)
            }
        ) {
            Text(
                text = invoice.driverPhone,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Blue
                )
            )
        }
    }
}

@Composable
private fun ActionButtonView(
    invoice: Invoice,
    onViewInvoiceSummary: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // View Invoice Summary button - Orange color
        Button(
            onClick = { onViewInvoiceSummary?.invoke() },
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LimoOrange)
        ) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "View Invoice Summary",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

// MARK: - Helper Methods

@Composable
private fun getStatusColor(status: String): Color {
    return when (status.lowercase()) {
        "paid" -> Color(0xFF4CAF50) // Green
        "paid_cash" -> Color.Blue
        "pending" -> LimoOrange
        "failed" -> Color.Red
        else -> Color.Gray
    }
}

private fun shouldUseTallLine(invoice: Invoice): Boolean {
    return invoice.pickupAddress.length > 30 || invoice.dropoffAddress.length > 30
}

