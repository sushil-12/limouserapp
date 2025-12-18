package com.example.limouserapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.limouserapp.data.model.dashboard.UserBooking
import com.example.limouserapp.ui.theme.LimoBlack
import com.example.limouserapp.ui.theme.LimoGreen
import com.example.limouserapp.ui.theme.LimoGrey
import com.example.limouserapp.ui.theme.LimoOrange
import com.example.limouserapp.ui.theme.LimoRed
import com.example.limouserapp.ui.theme.LimoWhite
import com.example.limouserapp.ui.utils.BookingFormatter

// Define specific neutral colors to avoid the "Pink/Purple" Material 3 tint
private val CardBackgroundWhite = Color.White
private val SectionBackgroundGray = Color(0xFFF5F5F5) // Light Gray for Summary
private val DriverBackgroundGray = Color(0xFFFAFAFA)  // Very Light Gray for Driver
private val TimelineColor = Color.Black
private val TimelineLineColor = Color.LightGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserBookingCard(
    booking: UserBooking,
    onEditBooking: (() -> Unit)? = null,
    onCancelBooking: (() -> Unit)? = null,
    onRepeatBooking: (() -> Unit)? = null,
    onReturnBooking: (() -> Unit)? = null,
    onDriverPhoneClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cardStyle = remember(booking.bookingStatus) {
        getCardStyleForStatus(booking.bookingStatus)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackgroundWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // 1. Top Header
            TopHeaderView(booking, cardStyle)

            // 2. Booking Summary
            BookingSummaryView(booking, cardStyle)

            // 3. Route Details (Fixed Timeline)
            RouteDetailsView(booking)

            // 4. Driver Information
            DriverInfoView(booking, onDriverPhoneClick)

            // 5. Action Buttons
            ActionButtonsView(
                booking = booking,
                onEditBooking = onEditBooking,
                onCancelBooking = onCancelBooking,
                onRepeatBooking = onRepeatBooking,
                onReturnBooking = onReturnBooking
            )
        }
    }
}

@Composable
private fun TopHeaderView(booking: UserBooking, style: BookingCardStyle) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(style.headerBackgroundColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = style.accentColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = BookingFormatter.formatBookingTime(booking.pickupDate, booking.pickupTime),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }

        // Service Badge
        Surface(
            color = CardBackgroundWhite,
            shape = RoundedCornerShape(4.dp),
        ) {
            Text(
                text = "${BookingFormatter.formatServiceType(booking.serviceType)} / ${BookingFormatter.formatTransferType(booking.transferType ?: "")}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = LimoBlack
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun BookingSummaryView(booking: UserBooking, style: BookingCardStyle) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // FIX: Use explicit Light Gray, not SurfaceVariant
            .background(SectionBackgroundGray)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#${booking.bookingId}",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
        )

        Text(
            text = booking.statusDisplayText,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = BookingFormatter.getBookingStatusColor(booking.bookingStatus)
            )
        )

        Text(
            text = "Total: ${booking.formattedTotal}",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
private fun RouteDetailsView(booking: UserBooking) {
    // FIX: IntrinsicSize.Min is crucial here.
    // It forces the Row to be exactly as tall as the text, allowing the line
    // to connect the dots perfectly from top to bottom.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackgroundWhite)
            .height(IntrinsicSize.Min)
            .padding(all = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Timeline Visual ---
        Column(
            modifier = Modifier.width(16.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 1. Pickup Circle
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(TimelineColor)
            )

            // 2. Connecting Line (Fills remaining space)
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .weight(1f) // Stretches to fill space between dots
                    .background(TimelineLineColor)
            )

            // 3. Dropoff Square
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(2.dp)) // Square with slight radius
                    .background(TimelineColor)
            )
        }

        // --- Addresses ---
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Pickup Address (Top aligned with Circle)
            Text(
                text = booking.pickupAddress,
                style = MaterialTheme.typography.bodyMedium,
                color = LimoBlack,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 12.dp) // Gap between addresses
            )

            // Dropoff Address (Bottom aligned with Square)
            Text(
                text = booking.dropoffAddress,
                style = MaterialTheme.typography.bodyMedium,
                color = LimoBlack,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DriverInfoView(
    booking: UserBooking,
    onDriverPhoneClick: ((String) -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // FIX: Very light gray background to separate from Route but keep it clean
            .background(DriverBackgroundGray)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar Circle
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, Color.LightGray, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = "Driver",
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = booking.fullDriverName.ifEmpty { "Pending Assignment" },
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = LimoBlack
            )

            if (booking.formattedDriverPhone.isNotEmpty()) {
                Text(
                    text = booking.formattedDriverPhone,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onDriverPhoneClick?.invoke(booking.formattedDriverPhone) }
                        .padding(top = 2.dp)
                )
            }
        }

        // Optional Affiliate Badge (if needed)
//        if (booking.companyName.isNotEmpty()) {
            booking.companyName?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
//        }
    }
}

@Composable
private fun ActionButtonsView(
    booking: UserBooking,
    onEditBooking: (() -> Unit)? = null,
    onCancelBooking: (() -> Unit)? = null,
    onRepeatBooking: (() -> Unit)? = null,
    onReturnBooking: (() -> Unit)? = null
) {
    val paymentStatus = booking.paymentStatus.lowercase()
    val isPaid = paymentStatus == "paid" || paymentStatus == "paid_cash"

    // White background for actions
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackgroundWhite)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (!isPaid) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Edit Button
                OutlinedButton(
                    onClick = { onEditBooking?.invoke() },
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, LimoBlack),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Black,
                        contentColor = LimoWhite
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Edit/Change",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                // Cancel Button
                Button(
                    onClick = { onCancelBooking?.invoke() },
                    modifier = Modifier.weight(1f).height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LimoRed),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Cancel Ride",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                }

                // More Button
                // More Button (Replaced IconButton with Box for precise sizing)
                var expanded by remember { mutableStateOf(false) }

                Box(
                    // 1. Controls the location of the dropdown
                    modifier = Modifier.wrapContentSize(Alignment.TopStart)
                ) {
                    // 2. The Custom Button
                    Box(
                        modifier = Modifier
                            .size(40.dp) // Matches the height of "Edit" and "Cancel" buttons exactly
                            .clip(RoundedCornerShape(8.dp)) // Clip ripple to shape
                            .background(Color(0xFFEEEEEE))
                            .clickable { expanded = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            modifier = Modifier.size(24.dp), // Standard icon size
                            tint = LimoBlack
                        )
                    }

                    // 3. The Dropdown Menu
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color.White) // Force White background
                    ) {
                        DropdownMenuItem(
                            text = { Text("Repeat Booking") },
                            onClick = {
                                expanded = false
                                onRepeatBooking?.invoke()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Return Ride") },
                            onClick = {
                                expanded = false
                                onReturnBooking?.invoke()
                            }
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp), // Gap between Invoice and More
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. View Invoice Button
                // FIX: Use .weight(1f) so it shares space with the More button
                Button(
                    onClick = { /* Handle invoice click */ },
                    colors = ButtonDefaults.buttonColors(containerColor = LimoGreen),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f) // <--- CRITICAL FIX: Fills available width
                        .height(40.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "View Invoice",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                }

                // 2. More Button (Fixed Size)
                var expanded by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier.wrapContentSize(Alignment.TopStart)
                ) {
                    // The Button itself
                    Box(
                        modifier = Modifier
                            .size(40.dp) // Fixed square size
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEEEEEE))
                            .clickable { expanded = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            modifier = Modifier.size(24.dp),
                            tint = LimoBlack
                        )
                    }

                    // The Menu
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Repeat Booking") },
                            onClick = {
                                expanded = false
                                onRepeatBooking?.invoke()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Return Ride") },
                            onClick = {
                                expanded = false
                                onReturnBooking?.invoke()
                            }
                        )
                    }
                }
            }
        }
    }
}

// --- Helper Data Class ---

private data class BookingCardStyle(
    val headerBackgroundColor: Color,
    val accentColor: Color
)

private fun getCardStyleForStatus(status: String): BookingCardStyle {
    return when (status.lowercase()) {
        "completed", "finished" -> BookingCardStyle(
            headerBackgroundColor = Color(0xFF2E7D32), // Green
            accentColor = Color.White
        )
        "cancelled" -> BookingCardStyle(
            headerBackgroundColor = LimoRed,
            accentColor = Color.White
        )
        else -> BookingCardStyle(
            headerBackgroundColor = LimoBlack,
            accentColor = LimoOrange
        )
    }
}

// ... (Paste this at the bottom of the file) ...

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun UserBookingCardPreview() {
    // Mock Data for "Confirmed/Pending"
    val activeBooking = UserBooking(
        bookingId = 2023,
        accountType = "individual", subAccountType = null, loseAffiliate = null,
        pickupDate = "2025-11-30", pickupTime = "12:00:00",
        serviceType = "oneway", paymentStatus = "pending", paymentMethod = "card",
        bookingStatus = "confirmed", status = true, affiliateId = 0, isTransferred = 0,
        passengerName = "Robert Wimberly", passengerEmail = "", passengerCellIsd = "", passengerCellCountry = "", passengerCell = "",
        pickupAddress = "ORD - Chicago Ohare Intl, Chicago, United States",
        dropoffAddress = "4217 Oak Ave, Brookfield, IL 60513, USA",
        vehicleCategoryName = "Sedan", affiliateType = "", companyName = "1800Limo",
        affiliateDispatchIsd = null, affiliateDispatchNumber = null, dispatchEmail = null,
        gigCellIsd = "", gigCellMobile = "", gigEmail = "",
        driverFirstName = "Robert", driverLastName = "Wimberly", driverCellIsd = "+1", driverCellNumber = "7734853506",
        looseAffiliateName = null, looseAffiliatePhoneIsd = null, looseAffiliatePhone = null, looseAffiliateEmail = null, looseAffDriverName = null,
        reservationType = null, grandTotal = 62.50, currency = "USD", farmoutAffiliate = null,
        pickupDay = "Sun", paxTel = null, paxIsd = null, currencySymbol = "$", driverName = null, agentType = "user", driverTel = null, transferType = "Airport to city"
    )

    Box(modifier = Modifier.padding(16.dp)) {
        UserBookingCard(
            booking = activeBooking,
            onEditBooking = {},
            onCancelBooking = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun UserBookingCardCompletedPreview() {
    // Mock Data for "Completed/Paid"
    val completedBooking = UserBooking(
        bookingId = 2021,
        accountType = "individual", subAccountType = null, loseAffiliate = null,
        pickupDate = "2025-11-30", pickupTime = "16:03:00",
        serviceType = "oneway", paymentStatus = "paid", paymentMethod = "card",
        bookingStatus = "completed", status = true, affiliateId = 0, isTransferred = 0,
        passengerName = "Robert Wimberly", passengerEmail = "", passengerCellIsd = "", passengerCellCountry = "", passengerCell = "",
        pickupAddress = "4217 Oak Ave, Brookfield, IL 60513, USA",
        dropoffAddress = "ORD - Chicago Ohare Intl, Chicago, United States",
        vehicleCategoryName = "Sedan", affiliateType = "", companyName = "Black Car Limo",
        affiliateDispatchIsd = null, affiliateDispatchNumber = null, dispatchEmail = null,
        gigCellIsd = "", gigCellMobile = "", gigEmail = "",
        driverFirstName = "James", driverLastName = "Driver", driverCellIsd = "+1", driverCellNumber = "7085512369",
        looseAffiliateName = null, looseAffiliatePhoneIsd = null, looseAffiliatePhone = null, looseAffiliateEmail = null, looseAffDriverName = null,
        reservationType = null, grandTotal = 73.79, currency = "USD", farmoutAffiliate = null,
        pickupDay = "Sun", paxTel = null, paxIsd = null, currencySymbol = "$", driverName = null, agentType = "user", driverTel = null, transferType = "City to airport"
    )

    Box(modifier = Modifier.padding(16.dp)) {
        UserBookingCard(
            booking = completedBooking,
            onEditBooking = {},
            onCancelBooking = {}
        )
    }
}