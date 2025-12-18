package com.example.limouserapp.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.limouserapp.data.model.dashboard.CardData
import com.example.limouserapp.ui.components.SavedCardView
import com.example.limouserapp.ui.theme.GoogleSansFamily
import com.example.limouserapp.ui.theme.LimoOrange
import com.example.limouserapp.ui.viewmodel.MyCardsViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCardsScreen(
    onBackClick: () -> Unit = {},
    viewModel: MyCardsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Alert Handling
    if (uiState.showSuccessAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSuccessAlert() },
            title = { Text("Success", fontFamily = GoogleSansFamily) },
            text = { Text(uiState.successMessage, fontFamily = GoogleSansFamily) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissSuccessAlert() }) {
                    Text("OK", fontFamily = GoogleSansFamily)
                }
            }
        )
    }

    if (uiState.showErrorAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissErrorAlert() },
            title = { Text("Error", fontFamily = GoogleSansFamily) },
            text = { Text(uiState.errorMessage, fontFamily = GoogleSansFamily) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissErrorAlert() }) {
                    Text("OK", fontFamily = GoogleSansFamily)
                }
            }
        )
    }

    // Main Scaffold with fixed bottom bar for action buttons
    // CRITICAL: Set contentWindowInsets to WindowInsets(0) to prevent double padding
    // Scaffold's bottomBar automatically handles IME insets, so we don't need manual padding
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White,
        contentWindowInsets = WindowInsets(0), // Disable default inset consumption to prevent conflicts
        topBar = {
            // Header matching MyBookingsScreen style
            LocalCommonHeader(
                title = "My Cards",
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            // Fixed Action Buttons at bottom
            // Scaffold automatically applies IME padding to bottomBar, so no manual padding needed
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))

                    // Reset Button
                    Button(
                        onClick = { viewModel.resetForm() },
                        enabled = !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF5F5F5),
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .width(94.dp)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Text(
                            text = "Reset",
                            fontSize = 16.sp,
                            fontFamily = GoogleSansFamily,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Save Button
                    Button(
                        onClick = { viewModel.saveCard() },
                        enabled = !uiState.isLoading && uiState.isFormValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LimoOrange,
                            disabledContainerColor = LimoOrange.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .width(94.dp)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Save",
                                fontSize = 16.sp,
                                fontFamily = GoogleSansFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        val scrollState = rememberScrollState()
        
        // Scrollable Column (only content, buttons are in bottomBar)
        // Apply safeDrawing padding from innerPadding and IME padding for keyboard
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding() // Adds padding when keyboard is visible - this prevents content from being hidden
                .verticalScroll(scrollState)
        ) {
            // 1. Saved Cards Section
            SavedCardsSection(
                savedCards = uiState.savedCards,
                isLoading = uiState.isLoading,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
            )

            // 2. Separator
            SeparatorView(
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            // 3. Add Another Card Form
            AddCardSection(
                uiState = uiState,
                onCardHolderNameChange = { viewModel.updateCardHolderName(it) },
                onCardNumberChange = { viewModel.updateCardNumber(it) },
                onExpiryMonthChange = { viewModel.updateExpiryMonth(it) },
                onExpiryYearChange = { viewModel.updateExpiryYear(it) },
                onCvvChange = { viewModel.updateCVV(it) },
                onIsPrimaryToggle = { viewModel.toggleIsPrimary() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
            )

            // Bottom spacer to ensure content isn't hidden behind bottom bar
            // This accounts for the bottom bar height (buttons + padding)
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

// --- Components ---

/**
 * Local Header Component matching MyBookingsScreen style
 */
@Composable
private fun LocalCommonHeader(
    title: String,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            // CRITICAL FIX: Adds padding matching the system status bar height
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
            }

            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = GoogleSansFamily,
                color = Color.Black
            )

            // Dummy spacer to center the title perfectly
            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}

@Composable
private fun SavedCardsSection(
    savedCards: List<CardData>,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { savedCards.size })

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Saved Cards",
            fontSize = 18.sp,
            fontFamily = GoogleSansFamily,
            fontWeight = FontWeight.SemiBold,
            color = LimoOrange
        )

        if (isLoading && savedCards.isEmpty()) {
            // Loading Shimmer
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                CardShimmer()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    repeat(3) {
                        Box(modifier = Modifier.padding(4.dp).size(8.dp).clip(CircleShape).shimmerEffect())
                    }
                }
            }
        } else if (savedCards.isNotEmpty()) {
            // Slider
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        pageSpacing = 16.dp,
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) { page ->
                        SavedCardView(card = savedCards[page], modifier = Modifier.fillMaxWidth())
                    }
                }
                // Indicators
                if (savedCards.size > 1) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        repeat(savedCards.size) { index ->
                            val isSelected = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(if (isSelected) 10.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) LimoOrange else Color.Gray.copy(0.3f))
                            )
                        }
                    }
                }
            }
        } else {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color(0xFFF9F9F9), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "💳",
                        fontSize = 32.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "No cards saved yet",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        fontFamily = GoogleSansFamily
                    )
                }
            }
        }
    }
}

@Composable
private fun SeparatorView(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFEEEEEE))
        Text(
            text = "Or",
            modifier = Modifier.padding(horizontal = 16.dp),
            fontSize = 14.sp,
            fontFamily = GoogleSansFamily,
            color = Color.Gray
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFEEEEEE))
    }
}

@Composable
private fun AddCardSection(
    uiState: com.example.limouserapp.ui.viewmodel.MyCardsUiState,
    onCardHolderNameChange: (String) -> Unit,
    onCardNumberChange: (String) -> Unit,
    onExpiryMonthChange: (String) -> Unit,
    onExpiryYearChange: (String) -> Unit,
    onCvvChange: (String) -> Unit,
    onIsPrimaryToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Card Number Formatting Logic with cursor position preservation
    var localCardNumber by remember { mutableStateOf(TextFieldValue(uiState.cardNumber)) }
    LaunchedEffect(uiState.cardNumber) {
        if (localCardNumber.text != uiState.cardNumber) {
            // Only update if the text actually changed (not from our own formatting)
            val digits = uiState.cardNumber.filter { it.isDigit() }
            var formatted = ""
            for (i in digits.indices) {
                formatted += digits[i]
                if ((i + 1) % 4 == 0 && i != digits.length - 1) formatted += " "
            }
            localCardNumber = TextFieldValue(formatted, TextRange(formatted.length))
        }
    }
    
    fun formatCardNumber(newValue: TextFieldValue): TextFieldValue {
        val cursorPosition = newValue.selection.start
        val textBeforeCursor = newValue.text.substring(0, cursorPosition)
        
        // Remove all spaces and get digits
        val digits = newValue.text.filter { it.isDigit() }.take(16)
        
        // Count digits before cursor position (excluding spaces)
        val digitsBeforeCursor = textBeforeCursor.filter { it.isDigit() }.length
        
        // Build formatted string
        var formatted = ""
        for (i in digits.indices) {
            formatted += digits[i]
            if ((i + 1) % 4 == 0 && i != digits.length - 1) formatted += " "
        }
        
        // Calculate new cursor position
        // We need to find where the cursor should be after formatting
        // Count digits and spaces until we reach the same number of digits
        var newCursorPosition = 0
        var digitCount = 0
        
        for (i in formatted.indices) {
            if (formatted[i].isDigit()) {
                digitCount++
                // If we've reached the number of digits that were before cursor, place cursor here
                if (digitCount == digitsBeforeCursor) {
                    newCursorPosition = i + 1
                    break
                }
            } else if (formatted[i] == ' ') {
                // If cursor was right after a space, we might need to place it after this space
                if (digitCount == digitsBeforeCursor) {
                    newCursorPosition = i + 1
                    break
                }
            }
        }
        
        // If we didn't find a position (cursor at end), place at end
        if (newCursorPosition == 0 && digitsBeforeCursor >= digits.length) {
            newCursorPosition = formatted.length
        }
        
        // Ensure cursor is within bounds
        newCursorPosition = newCursorPosition.coerceIn(0, formatted.length)
        
        return TextFieldValue(formatted, TextRange(newCursorPosition))
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(
            text = "Add another card",
            fontSize = 18.sp,
            fontFamily = GoogleSansFamily,
            fontWeight = FontWeight.SemiBold,
            color = LimoOrange
        )

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            FormField(
                label = "FIRST & LAST NAME *",
                value = uiState.cardHolderName,
                onValueChange = onCardHolderNameChange,
                placeholder = "Enter name",
                keyboardType = KeyboardType.Text
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LabelText("CARD NO. *")
                OutlinedTextField(
                    value = localCardNumber,
                    onValueChange = {
                        val formatted = formatCardNumber(it)
                        localCardNumber = formatted
                        onCardNumberChange(formatted.text)
                    },
                    placeholder = { Text("0000 0000 0000 0000", color = Color.Gray, fontFamily = GoogleSansFamily) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = inputFieldColors(),
                    textStyle = TextStyle(
                        fontFamily = GoogleSansFamily,
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabelText("MONTH *")
                    MonthYearDropdown(
                        selectedValue = uiState.expiryMonth.ifEmpty { "MM" },
                        isMonth = true,
                        onValueChange = onExpiryMonthChange
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabelText("YEAR *")
                    MonthYearDropdown(
                        selectedValue = uiState.expiryYear.ifEmpty { "YY" },
                        isMonth = false,
                        onValueChange = onExpiryYearChange
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabelText("CVV *")
                    OutlinedTextField(
                        value = uiState.cvv,
                        onValueChange = onCvvChange,
                        placeholder = { Text("123", color = Color.Gray, fontFamily = GoogleSansFamily) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = inputFieldColors(),
                        textStyle = TextStyle(fontFamily = GoogleSansFamily, fontSize = 16.sp, color = Color.Black)
                    )
                }
            }
        }
    }
}

// --- Helpers ---

@Composable
fun LabelText(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = GoogleSansFamily,
        color = Color.Gray
    )
}

@Composable
fun inputFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color(0xFFFAFAFA),
    unfocusedContainerColor = Color(0xFFFAFAFA),
    focusedBorderColor = LimoOrange,
    unfocusedBorderColor = Color(0xFFEEEEEE),
    cursorColor = LimoOrange
)

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LabelText(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color.Gray, fontFamily = GoogleSansFamily) },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = inputFieldColors(),
            textStyle = TextStyle(fontFamily = GoogleSansFamily, fontSize = 16.sp, color = Color.Black)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthYearDropdown(
    selectedValue: String,
    isMonth: Boolean,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val items = if (isMonth) (1..12).map { String.format("%02d", it) } else {
        val current = Calendar.getInstance().get(Calendar.YEAR)
        (current..current + 10).map { it.toString() }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = inputFieldColors(),
            textStyle = TextStyle(fontFamily = GoogleSansFamily, fontSize = 16.sp, color = Color.Black)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item, fontFamily = GoogleSansFamily) },
                    onClick = { onValueChange(item); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun CardShimmer() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1F1F1F))
    ) {
        Column(modifier = Modifier.padding(20.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.size(60.dp, 14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                Box(modifier = Modifier.size(50.dp, 30.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.fillMaxWidth(0.6f).height(16.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                Box(modifier = Modifier.fillMaxWidth(0.9f).height(18.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
            }
        }
    }
}

fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "Shimmer")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000)
        ),
        label = "ShimmerOffset"
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF2B2B2B),
                Color(0xFF3D3D3D),
                Color(0xFF2B2B2B),
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    ).onGloballyPositioned {
        size = it.size
    }
}