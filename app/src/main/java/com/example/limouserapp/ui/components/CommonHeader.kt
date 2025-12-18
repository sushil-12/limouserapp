package com.example.limouserapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.limouserapp.ui.theme.LimoWhite

/**
 * Common Header Component
 * Matches Figma design with light gray background
 * Back button opens navigation drawer
 *
 * @param title The header title text
 * @param onBackClick Callback when back button is clicked (should open navigation drawer)
 * @param onActionClick Optional callback for action button (search, edit, etc.)
 * @param actionIcon Optional icon for action button (defaults to Search)
 * @param showAction Whether to show the action button
 * @param modifier Optional modifier
 */
@Composable
fun CommonHeader(
    title: String,
    onBackClick: () -> Unit,
    onActionClick: (() -> Unit)? = null,
    actionIcon: ImageVector? = null,
    showAction: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(LimoWhite) // iOS systemGray6 equivalent - light gray background
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button - opens navigation drawer
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black
                )
            }

            // Title - centered
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            // Action button (search, edit, etc.) or invisible spacer
            if (showAction && onActionClick != null && actionIcon != null) {
                IconButton(onClick = onActionClick) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = "Action",
                        tint = Color.Black
                    )
                }
            } else {
                // Invisible spacer to balance layout (matching iOS)
                Spacer(modifier = Modifier.size(48.dp))
            }
        }

        // Bottom border/shadow (subtle separator)
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(0.5.dp)
//                .background(Color.Gray.copy(alpha = 0.2f))
//        )
    }
}

/**
 * Common Header with Search functionality
 * Convenience composable for screens with search
 */
@Composable
fun CommonHeaderWithSearch(
    title: String,
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit,
    isSearching: Boolean,
    modifier: Modifier = Modifier
) {
    CommonHeader(
        title = title,
        onBackClick = onBackClick,
        onActionClick = onSearchClick,
        actionIcon = if (isSearching) Icons.Default.Close else Icons.Default.Search,
        showAction = true,
        modifier = modifier
    )
}

/**
 * Common Header with Edit functionality
 * Convenience composable for screens with edit mode
 */
@Composable
fun CommonHeaderWithEdit(
    title: String,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    isEditing: Boolean,
    modifier: Modifier = Modifier
) {
    CommonHeader(
        title = title,
        onBackClick = onBackClick,
        onActionClick = onEditClick,
        actionIcon = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
        showAction = true,
        modifier = modifier
    )
}

