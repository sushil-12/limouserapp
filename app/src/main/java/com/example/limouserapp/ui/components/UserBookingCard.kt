package com.example.limouserapp.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.limouserapp.data.model.dashboard.UserBooking
import com.example.limouserapp.ui.theme.LimoBlack
import com.example.limouserapp.ui.theme.LimoGreen
import com.example.limouserapp.ui.theme.LimoOrange
import com.example.limouserapp.ui.theme.LimoRed
import com.example.limouserapp.ui.theme.LimoWhite
import com.example.limouserapp.ui.utils.BookingFormatter
import kotlinx.coroutines.launch

// Match driver DynamicBookingCard colors and layout
private val IOSBlack = Color(0xFF000000)
private val IOSLightGray = Color(0xFFE6E6E6)
private val IOSPassengerBg = Color(0xFFF2F2F2)
private val CardBackgroundWhite = Color.White
private val Blue = Color(0xFF0474CF)

// Bottom sheet design (same as driver)
private val NavyBlue = Color(0xFF0F2E53)
private val AlertRed = Color(0xFF8B3A3A)
private val TextBlack = Color(0xFF1A1A1A)
private val SurfaceBg = Color(0xFFF9F9F9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserBookingCard(
    booking: UserBooking,
    onClick: (() -> Unit)? = null,
    onEditBooking: (() -> Unit)? = null,
    onCancelBooking: (() -> Unit)? = null,
    onRepeatBooking: (() -> Unit)? = null,
    onReturnBooking: (() -> Unit)? = null,
    onDriverPhoneClick: ((String) -> Unit)? = null,
    onChatClick: ((Int) -> Unit)? = null,
    onViewInvoice: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackgroundWhite),
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Top Header (same as driver: black, date/time left, service badge right)
            TopHeaderView(booking)

            // 2. Booking Summary (same as driver: gray, #id, status center, Total + orange pill)
            BookingSummaryView(booking)

            // 3. Route Details (same as driver: timeline + addresses)
            RouteDetailsView(booking)

            // 4. Driver row (same as driver PAX row: tap opens bottom sheet)
            DriverInfoView(
                booking = booking,
                onDriverPhoneClick = onDriverPhoneClick,
                onOpenBottomSheet = { showBottomSheet = true }
            )

            // 5. Action Buttons (unchanged)
            ActionButtonsView(
                booking = booking,
                onEditBooking = onEditBooking,
                onCancelBooking = onCancelBooking,
                onRepeatBooking = onRepeatBooking,
                onReturnBooking = onReturnBooking,
                onViewInvoice = onViewInvoice
            )
        }
    }

    if (showBottomSheet) {
        BookingActionBottomSheet(
            booking = booking,
            sheetState = sheetState,
            onDismiss = {
                scope.launch {
                    sheetState.hide()
                    showBottomSheet = false
                }
            },
            onChatClick = onChatClick
        )
    }
}

@Composable
private fun TopHeaderView(booking: UserBooking) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(IOSBlack)
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
                tint = LimoOrange,
                modifier = Modifier.size(16.dp)
            )
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = BookingFormatter.formatDate(booking.pickupDate),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = BookingFormatter.formatTime(booking.pickupTime),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }

        Surface(
            color = LimoWhite,
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = "${BookingFormatter.formatServiceType(booking.serviceType)} / ${BookingFormatter.formatTransferType(booking.transferType ?: "")}",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun BookingSummaryView(booking: UserBooking) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(IOSLightGray)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#${booking.bookingId}",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black,
            maxLines = 1
        )

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = booking.statusDisplayText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = BookingFormatter.getBookingStatusColor(booking.bookingStatus),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Total",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
            Surface(color = LimoOrange, shape = RoundedCornerShape(8.dp)) {
                Text(
                    text = booking.formattedTotal,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LimoWhite,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun RouteDetailsView(booking: UserBooking) {
    var pickupLineCount by remember { mutableStateOf(1) }
    val connectorHeight = if (pickupLineCount > 1) 40.dp else 20.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackgroundWhite)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(IOSBlack)
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(connectorHeight)
                    .background(IOSBlack)
            )
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(IOSBlack)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = booking.pickupAddress,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { textLayoutResult: TextLayoutResult ->
                    pickupLineCount = textLayoutResult.lineCount
                }
            )
            Text(
                text = booking.dropoffAddress,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DriverInfoView(
    booking: UserBooking,
    onDriverPhoneClick: ((String) -> Unit)?,
    onOpenBottomSheet: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(IOSPassengerBg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Driver",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )
        Surface(
            color = CardBackgroundWhite,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onOpenBottomSheet() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = booking.fullDriverName.ifEmpty { "Pending Assignment" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(18.dp)
                        .background(Color.Gray.copy(alpha = 0.3f))
                )
                Text(
                    text = booking.formattedDriverPhone.ifEmpty { "—" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Blue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ActionButtonsView(
    booking: UserBooking,
    onEditBooking: (() -> Unit)? = null,
    onCancelBooking: (() -> Unit)? = null,
    onRepeatBooking: (() -> Unit)? = null,
    onReturnBooking: (() -> Unit)? = null,
    onViewInvoice: ((Int) -> Unit)? = null
) {
    val paymentStatus = booking.paymentStatus.lowercase()
    // View Invoice only for paid or paid_cash
    val showViewInvoice = paymentStatus == "paid" || paymentStatus == "paid_cash"
    val isPaid = showViewInvoice

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
        } else if (showViewInvoice) {
            // Paid/paid_cash only: View Invoice + More
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. View Invoice Button (only for paid or paid_cash)
                Button(
                    onClick = { onViewInvoice?.invoke(booking.bookingId) },
                    colors = ButtonDefaults.buttonColors(containerColor = LimoGreen),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
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

// --- Booking Action Bottom Sheet (same design as driver DynamicBookingCard) ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookingActionBottomSheet(
    booking: UserBooking,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onChatClick: ((Int) -> Unit)? = null
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = SurfaceBg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.Gray.copy(alpha = 0.3f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp)
        ) {
            // Header: Booking #id, driver name, Report Issue
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Booking #${booking.bookingId}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyBlue
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = booking.fullDriverName.ifEmpty { "Pending Assignment" },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextBlack
                    )
                    Text(
                        text = "Driver",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:")
                            putExtra(Intent.EXTRA_EMAIL, arrayOf("info@1800limo.com"))
                            putExtra(Intent.EXTRA_SUBJECT, "Issue - Booking #${booking.bookingId}")
                        }
                        context.startActivity(Intent.createChooser(intent, "Send email"))
                    },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, AlertRed),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = AlertRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Report Issue",
                        fontSize = 12.sp,
                        color = AlertRed,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Section: Driver Communication
            BookingSectionLabel(text = "Driver Communication")
            Spacer(modifier = Modifier.height(16.dp))

            val driverPhone = booking.formattedDriverPhone

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BookingPrimaryActionButton(
                    text = "Call Driver",
                    icon = Icons.Outlined.Call,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (driverPhone.isNotEmpty()) {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${driverPhone.filter { it.isDigit() || it == '+' }}")
                            }
                            context.startActivity(intent)
                        }
                    }
                )

                BookingPrimaryActionButton(
                    text = "Chat Driver",
                    icon = Icons.Outlined.Chat,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onChatClick?.invoke(booking.bookingId)
                            ?: run {
                                // Fallback: open SMS to driver if no chat handler
                                if (driverPhone.isNotEmpty()) {
                                    context.startActivity(
                                        Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$driverPhone"))
                                    )
                                }
                            }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section: Admin Support
            BookingSectionLabel(text = "Admin Support")
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BookingPrimaryActionButton(
                    text = "Call Admin",
                    icon = Icons.Outlined.Call,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:17082056607"))
                        )
                    }
                )
                BookingPrimaryActionButton(
                    text = "Text Admin",
                    icon = Icons.Outlined.Sms,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:17082056607")))
                    }
                )
                BookingPrimaryActionButton(
                    text = "Email Admin",
                    icon = Icons.Outlined.Email,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:info@1800limo.com"))
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun BookingPrimaryActionButton(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = NavyBlue,
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun BookingSectionLabel(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Normal
        )
        Spacer(modifier = Modifier.width(12.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = Color.Gray.copy(alpha = 0.2f)
        )
    }
}

// --- Previews ---

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