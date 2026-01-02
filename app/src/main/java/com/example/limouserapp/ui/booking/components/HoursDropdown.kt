package com.example.limouserapp.ui.booking.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.example.limouserapp.R
import com.example.limouserapp.ui.theme.GoogleSansFamily

@Composable
fun HoursDropdown(
    selectedHours: String,
    onHoursSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val hoursOptions = remember {
        listOf(
            "2 hours minimum", "3 hours", "4 hours", "5 hours",
            "6 hours", "8 hours", "10 hours", "12 hours"
        )
    }

    var expanded by remember { mutableStateOf(false) }
    var rowSize by remember { mutableStateOf(Size.Zero) }

    // Animation for the arrow icon
    val iconRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "Arrow Rotation"
    )

    val borderColor = Color(0xFF252C3E)
    val containerShape = RoundedCornerShape(4.dp)
    // Lighter border for the popup menu to make it subtle
    val menuBorderColor = Color(0xFFE0E0E0)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        // --- OUTER TRIGGER (Kept consistent) ---
        Surface(
            shape = containerShape,
            border = BorderStroke(1.dp, borderColor),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { layoutCoordinates ->
                    rowSize = layoutCoordinates.size.toSize()
                }
                .clickable { expanded = !expanded }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedHours,
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
                    contentDescription = "Expand",
                    tint = borderColor,
                    modifier = Modifier
                        .size(10.dp)
                        .rotate(iconRotation)
                )
            }
        }

        // --- INNER DROPDOWN REDESIGN ---
        MaterialTheme(
            shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(8.dp))
        ) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                offset = DpOffset(0.dp, 6.dp), // Adds a small gap between button and menu
                modifier = Modifier
                    .width(with(LocalDensity.current) { rowSize.width.toDp() }) // Matches width
                    .background(Color.White)
                    .border(BorderStroke(1.dp, menuBorderColor), RoundedCornerShape(8.dp)) // Nice border
            ) {
                hoursOptions.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                fontFamily = GoogleSansFamily,
                                fontSize = 14.sp,
                                // Highlight the currently selected item visually
                                fontWeight = if (option == selectedHours) FontWeight.Bold else FontWeight.Medium,
                                color = if (option == selectedHours) borderColor else Color.DarkGray
                            )
                        },
                        onClick = {
                            onHoursSelected(option)
                            expanded = false
                        },
                        // Custom colors for standard states
                        colors = MenuDefaults.itemColors(
                            textColor = borderColor,
                            leadingIconColor = borderColor
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    )

                    // Optional: Add a subtle divider between items, except the last one
                    // if (index < hoursOptions.lastIndex) {
                    //    HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFF0F0F0))
                    // }
                }
            }
        }
    }
}