package com.example.limouserapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.limouserapp.ui.theme.LimoOrange
import com.example.limouserapp.ui.theme.LimoWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Generic searchable bottom sheet component
 * Matches iOS SearchableBottomSheet functionality
 * Uses ModalBottomSheet for proper Android bottom sheet behavior
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SearchableBottomSheet(
    title: String,
    items: List<T>,
    selectedItemId: Int? = null,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onItemSelected: (T) -> Unit,
    onSearchChanged: ((String) -> Unit)? = null,
    getItemId: (T) -> Int,
    getDisplayText: (T) -> String,
    getSubtitle: ((T) -> String?)? = null,
    modifier: Modifier = Modifier
) {
    var searchText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var lastProcessedQuery by remember { mutableStateOf<String?>(null) }
    var debounceJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    
    // Sheet state with medium and large detents (matches iOS)
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )
    
    // Debounce search text with proper cancellation support
    LaunchedEffect(searchText) {
        val currentQuery = searchText
        
        // Cancel previous debounce job if user continues typing
        debounceJob?.cancel()
        
        // Only search if query is at least 2 characters
        if (currentQuery.length < 2) {
            // Clear results for short queries
            if (currentQuery.isEmpty()) {
                lastProcessedQuery = null
            }
            return@LaunchedEffect
        }
        
        // Create new debounce job
        debounceJob = scope.launch {
            delay(500) // Debounce delay to reduce API calls
            
            // Only invoke if searchText hasn't changed during delay (still matches current query)
            // and it's different from last processed query
            if (searchText == currentQuery && searchText != lastProcessedQuery) {
                lastProcessedQuery = searchText
                onSearchChanged?.invoke(searchText)
            }
        }
    }
    
    // Reset state when bottom sheet closes
    LaunchedEffect(isVisible) {
        if (!isVisible) {
            // Cancel any pending debounce job
            debounceJob?.cancel()
            debounceJob = null
            // Reset search text and last processed query
            searchText = ""
            lastProcessedQuery = null
        }
    }
    
    // Show/hide bottom sheet based on isVisible
    LaunchedEffect(isVisible) {
        if (isVisible) {
            sheetState.show()
        } else {
            sheetState.hide()
        }
    }
    
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    onDismiss()
                }
            },
            sheetState = sheetState,
            modifier = modifier,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            containerColor = LimoWhite,
            dragHandle = {
                // Handle bar at top
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(
                            Color.Gray.copy(alpha = 0.5f),
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    color = Color.Black
                )
                
                // Search Bar
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("Search...") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.Gray
                        )
                    },
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            IconButton(onClick = { searchText = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = Color.Gray
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Gray.copy(alpha = 0.1f),
                        unfocusedContainerColor = Color.Gray.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                
                // Items List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp), // Limit height for better UX
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(items) { item ->
                        val isSelected = selectedItemId != null && getItemId(item) == selectedItemId
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    onItemSelected(item)
                                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                                        onDismiss()
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = getDisplayText(item),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.Black
                                )
                                val subtitle = getSubtitle?.invoke(item)
                                if (subtitle != null && subtitle.isNotEmpty()) {
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Gray
                                    )
                                }
                            }
                            
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = LimoOrange,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                        
                        if (item != items.lastOrNull()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 12.dp),
                                color = Color.Gray.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }
    }
}

