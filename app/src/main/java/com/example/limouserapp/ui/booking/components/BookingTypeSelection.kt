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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.toSize

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
    // 1. Measure the trigger size to match the menu width exactly
    var rowSize by remember { mutableStateOf(Size.Zero) }

    // 2. Icon rotation state
    val iconRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "Arrow Rotation"
    )

    // Design Constants
    val borderColor = Color(0xFF252C3E)
    val menuBorderColor = Color(0xFFE0E0E0)
    val containerShape = RoundedCornerShape(4.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        // --- OUTER TRIGGER (Visual match to original, but responsive) ---
        Surface(
            shape = containerShape,
            border = BorderStroke(1.dp, borderColor),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { layoutCoordinates ->
                    rowSize = layoutCoordinates.size.toSize()
                }
                .clickable { onExpandedChange(!expanded) }
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 10.dp), // Dynamic height
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedType.displayName,
                    fontFamily = GoogleSansFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = borderColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    painter = painterResource(id = R.drawable.dropdown_arrow),
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = borderColor,
                    modifier = Modifier
                        .size(10.dp)
                        .rotate(iconRotation)
                )
            }
        }

        // --- INNER DROPDOWN (Redesigned Style) ---
        MaterialTheme(
            shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(8.dp))
        ) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                offset = DpOffset(0.dp, 6.dp), // Floating effect
                modifier = Modifier
                    .width(with(LocalDensity.current) { rowSize.width.toDp() }) // Match Trigger width
                    .background(Color.White)
                    .border(BorderStroke(1.dp, menuBorderColor), RoundedCornerShape(8.dp))
            ) {
                BookingType.entries.forEach { type ->
                    val isSelected = type == selectedType

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = type.displayName,
                                fontFamily = GoogleSansFamily,
                                fontSize = 14.sp,
                                // Bold if selected, Medium otherwise
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) borderColor else Color.DarkGray
                            )
                        },
                        onClick = {
                            onTypeSelected(type)
                            onExpandedChange(false)
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = borderColor,
                            leadingIconColor = borderColor
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

