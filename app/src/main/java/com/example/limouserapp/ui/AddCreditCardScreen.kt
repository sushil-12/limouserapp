package com.example.limouserapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.text.TextRange
import com.example.limouserapp.R
import com.example.limouserapp.ui.theme.LimoOrange
import com.example.limouserapp.ui.theme.GoogleSansFamily
import com.example.limouserapp.ui.components.LocationAutocomplete
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import timber.log.Timber

@Composable
fun AddCreditCardScreen(
    onNext: () -> Unit,
    onBack: (() -> Unit)? = null,
    uiState: com.example.limouserapp.ui.state.CreditCardUiState? = null,
    onEvent: ((com.example.limouserapp.ui.state.CreditCardUiEvent) -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    // Local state for form fields with TextFieldValue for cursor preservation
    var localCardNumber by remember { mutableStateOf(TextFieldValue("")) }
    var localExpiryDate by remember { mutableStateOf(TextFieldValue("")) }
    var localCvv by remember { mutableStateOf("") }
    var localNameOnCard by remember { mutableStateOf("") }
    var localLocation by remember { mutableStateOf("") }
    var localPostalCode by remember { mutableStateOf("") }
    var localIsLocationSelected by remember { mutableStateOf(false) }

    // Error states for validation
    var cardNumberError by remember { mutableStateOf<String?>(null) }
    var expiryDateError by remember { mutableStateOf<String?>(null) }
    var cvvError by remember { mutableStateOf<String?>(null) }
    var nameOnCardError by remember { mutableStateOf<String?>(null) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var postalCodeError by remember { mutableStateOf<String?>(null) }

    // Focus requesters
    val cardNumberFocus = remember { FocusRequester() }
    val expiryDateFocus = remember { FocusRequester() }
    val cvvFocus = remember { FocusRequester() }
    val nameOnCardFocus = remember { FocusRequester() }
    val locationFocus = remember { FocusRequester() }
    val postalCodeFocus = remember { FocusRequester() }

    // Use ViewModel state if provided, else local
    val cardNumber = uiState?.cardNumber ?: localCardNumber.text
    val expiryMonth = uiState?.expiryMonth ?: if (localExpiryDate.text.length >= 2) localExpiryDate.text.take(2) else ""
    val expiryYear = uiState?.expiryYear ?: if (localExpiryDate.text.length > 2) localExpiryDate.text.drop(2) else ""
    val cvv = uiState?.cvv ?: localCvv
    val nameOnCard = uiState?.cardHolderName ?: localNameOnCard
    val locationDisplay = uiState?.locationDisplay ?: localLocation // Use locationDisplay for input field
    val location = uiState?.location ?: "" // Full address (used in API request)
    val city = uiState?.city ?: ""
    val state = uiState?.state ?: ""
    val postalCode = uiState?.zipCode ?: localPostalCode
    val isLocationSelected = uiState?.isLocationSelected ?: localIsLocationSelected
    
    // Auto-focus postal code field when location is selected and postal code is populated
    LaunchedEffect(isLocationSelected, postalCode) {
        if (isLocationSelected && postalCode.isNotEmpty()) {
            delay(200) // Delay to ensure UI state is updated
            postalCodeFocus.requestFocus()
        }
    }

    // Format card number for display
    val displayCardNumber = cardNumber.replace(" ", "").replace("-", "").chunked(4).joinToString(" ")

    // Format expiry date for display
    val displayExpiryDate = when {
        expiryMonth.isNotEmpty() && expiryYear.isNotEmpty() -> "$expiryMonth/$expiryYear"
        expiryMonth.isNotEmpty() -> expiryMonth
        else -> ""
    }

    // Helper function to preserve cursor position during formatting
    fun formatCardNumberWithCursor(oldValue: TextFieldValue, newValue: TextFieldValue): TextFieldValue {
    // Remove all non-digit characters
    val digits = newValue.text.filter { it.isDigit() }.take(16)

    // Format digits into groups of 4
    val formatted = digits.chunked(4).joinToString(" ")

    // Figure out how many digits were before the cursor
    val digitCursorPosition = newValue.selection.start.let { cursor ->
        newValue.text.take(cursor).count { it.isDigit() }
    }

    // Convert digit position to formatted position
    var newCursor = 0
    var digitsSeen = 0
    for (i in formatted.indices) {
        if (formatted[i].isDigit()) digitsSeen++
        if (digitsSeen == digitCursorPosition + 1) {
            newCursor = i + 1
            break
        }
        }
        if (newCursor == 0) newCursor = formatted.length

        return TextFieldValue(
            text = formatted,
            selection = TextRange(newCursor.coerceIn(0, formatted.length))
        )
    }

    
    fun formatExpiryDateWithCursor(oldValue: TextFieldValue, newValue: TextFieldValue): TextFieldValue {
    // Keep only digits, max 4 (MMYY)
    val digits = newValue.text.filter { it.isDigit() }.take(4)

    // Insert slash after 2 digits if needed
    val formatted = when {
        digits.length <= 2 -> digits
        else -> digits.substring(0, 2) + "/" + digits.substring(2)
    }

    // Figure out how many digits were before the cursor
    val digitCursorPosition = newValue.selection.start.let { cursor ->
        newValue.text.take(cursor).count { it.isDigit() }
    }

    // Convert digit position to formatted position
        var newCursor = 0
        var digitsSeen = 0
        for (i in formatted.indices) {
            if (formatted[i].isDigit()) digitsSeen++
            if (digitsSeen == digitCursorPosition + 1) {
                newCursor = i + 1
                break
            }
        }
        if (newCursor == 0) newCursor = formatted.length

        return TextFieldValue(
            text = formatted,
            selection = TextRange(newCursor.coerceIn(0, formatted.length))
        )
    }


    // Validation functions
    fun isValidCardNumber(number: String): Boolean {
        val cleanNumber = number.replace(" ", "").replace("-", "")
        if (cleanNumber.length !in 13..19) return false
        // Luhn algorithm
        val digits = cleanNumber.map { it.digitToInt() }
        var sum = 0
        var isEven = false
        for (i in digits.size - 1 downTo 0) {
            var digit = digits[i]
            if (isEven) {
                digit *= 2
                if (digit > 9) digit -= 9
            }
            sum += digit
            isEven = !isEven
        }
        return sum % 10 == 0
    }

    fun isValidExpiryDate(month: String, year: String): Boolean {
        if (month.isEmpty() || year.isEmpty()) return false
        val monthInt = month.toIntOrNull() ?: return false
        if (monthInt !in 1..12) return false
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR) % 100 // Get last 2 digits
        val yearInt = year.toIntOrNull() ?: return false
        val currentMonth = calendar.get(Calendar.MONTH) + 1 // Calendar months are 0-based
        return when {
            yearInt > currentYear -> true
            yearInt == currentYear -> monthInt >= currentMonth
            else -> false
        }
    }

    fun isValidCvv(cvv: String, cardNumber: String): Boolean {
        val cleanNumber = cardNumber.replace(" ", "").replace("-", "")
        val isAmex = cleanNumber.startsWith("34") || cleanNumber.startsWith("37")
        return cvv.length == if (isAmex) 4 else 3
    }

    fun isValidName(name: String): Boolean {
        return name.length >= 2 && name.matches(Regex("[a-zA-Z\\s-]{2,100}"))
    }

    fun isValidPostalCode(postalCode: String): Boolean {
        return postalCode.isNotEmpty() && postalCode.matches(Regex("[a-zA-Z0-9\\s-]{3,10}"))
    }

    // Validate form for button enablement
    val isFormValid = if (uiState != null) {
        uiState.isFormValid
    } else {
        isValidCardNumber(cardNumber) &&
                isValidExpiryDate(expiryMonth, expiryYear) &&
                isValidCvv(cvv, cardNumber) &&
                isValidName(nameOnCard) &&
                isLocationSelected &&
                isValidPostalCode(postalCode)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Back button
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            IconButton(
                onClick = {
                    Timber.d("AddCreditCardScreen: Back button clicked")
                    Timber.d("AddCreditCardScreen: onBack callback is ${if (onBack != null) "not null" else "null"}")
                    try {
                        onBack?.invoke()
                        Timber.d("AddCreditCardScreen: onBack callback invoked successfully")
                    } catch (e: Exception) {
                        Timber.e(e, "AddCreditCardScreen: Error invoking onBack callback: ${e.message}")
                    }
                },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF3F3F3))
                    .align(Alignment.TopStart)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.backarrow),
                    contentDescription = "Back",
                    modifier = Modifier.size(12.dp),
                    tint = Color.Unspecified
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Header text
        val responsiveFontSize = when {
            screenWidth < 360.dp -> 24.sp
            screenWidth < 400.dp -> 26.sp
            else -> 28.sp
        }
        Text(
            text = "Add Credit Card",
            style = TextStyle(
                fontFamily = GoogleSansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = responsiveFontSize,
                lineHeight = (responsiveFontSize * 1.5f),
                color = Color(0xFF121212)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(32.dp))

        // Card Number
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "CARD NUMBER",
                style = TextStyle(
                    fontFamily = GoogleSansFamily,
                    fontWeight = FontWeight(510),
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = Color(0xFF121212).copy(alpha = 0.8f)
                )
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = localCardNumber,
                onValueChange = { newValue ->
                    localCardNumber = formatCardNumberWithCursor(localCardNumber, newValue)
                    val cleanNumber = localCardNumber.text.replace(" ", "")
                    cardNumberError = if (!isValidCardNumber(cleanNumber)) "Invalid card number" else null

                    if (onEvent != null) {
                        coroutineScope.launch {
                            onEvent(
                                com.example.limouserapp.ui.state.CreditCardUiEvent.CardNumberChanged(
                                    cleanNumber
                                )
                            )
                        }
                    }
                },
                placeholder = {
                    Text(
                        text = "0123 4567 8910 1112",
                        style = TextStyle(
                            fontFamily = GoogleSansFamily,
                            fontWeight = FontWeight(400),
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                            letterSpacing = 0.sp,
                            color = Color(0xFF121212).copy(alpha = 0.4f)
                        )
                    )
                },
                isError = cardNumberError != null,
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .focusRequester(cardNumberFocus),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        if (localCardNumber.text.replace(" ", "").length >= 16) {
                            expiryDateFocus.requestFocus()
                        }
                    }
                ),
                textStyle = TextStyle(
                    fontFamily = GoogleSansFamily,
                    fontWeight = FontWeight(400),
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    letterSpacing = 0.sp,
                    color = Color(0xFF121212)
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (cardNumberError != null) Color.Red else Color(0xFF121212).copy(alpha = 0.1f),
                    unfocusedBorderColor = if (cardNumberError != null) Color.Red else Color(0xFF121212).copy(alpha = 0.1f),
                    focusedContainerColor = Color(0xFFF5F5F5),
                    errorContainerColor = Color(0xFFF5F5F5),
                    unfocusedContainerColor = Color(0xFFF5F5F5),
                    cursorColor = Color(0xFF121212),
                    errorBorderColor = Color.Red
                )
            )

            if (cardNumberError != null) {
                Text(
                    text = cardNumberError!!,
                    style = TextStyle(
                        fontFamily = GoogleSansFamily,
                        fontWeight = FontWeight(400),
                        fontSize = 12.sp,
                        color = Color.Red
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Expiry Date and CVV Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Expiry Date
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "EXPIRY DATE",
                    style = TextStyle(
                        fontFamily = GoogleSansFamily,
                        fontWeight = FontWeight(510),
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        color = Color(0xFF121212).copy(alpha = 0.8f)
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = localExpiryDate,
                    onValueChange = { newValue ->
                        localExpiryDate = formatExpiryDateWithCursor(localExpiryDate, newValue)

                        val cleanInput = localExpiryDate.text.replace("/", "")
                        val month = if (cleanInput.length >= 2) cleanInput.take(2) else cleanInput
                        val year = if (cleanInput.length > 2) cleanInput.drop(2) else ""

                        expiryDateError = if (!isValidExpiryDate(month, year)) "Invalid or expired date" else null

                        if (onEvent != null) {
                            coroutineScope.launch {
                                onEvent(com.example.limouserapp.ui.state.CreditCardUiEvent.ExpiryMonthChanged(month))
                                onEvent(com.example.limouserapp.ui.state.CreditCardUiEvent.ExpiryYearChanged(year))
                            }
                        }
                    },
                    placeholder = {
                        Text(
                            text = "12/24",
                            style = TextStyle(
                                fontFamily = GoogleSansFamily,
                                fontWeight = FontWeight(400),
                                fontSize = 16.sp,
                                lineHeight = 20.sp,
                                letterSpacing = 0.sp,
                                color = Color(0xFF121212).copy(alpha = 0.4f)
                            )
                        )
                    },
                    isError = expiryDateError != null,
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .focusRequester(expiryDateFocus),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    ),
                    textStyle = TextStyle(
                        fontFamily = GoogleSansFamily,
                        fontWeight = FontWeight(400),
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        letterSpacing = 0.sp,
                        color = Color(0xFF121212)
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (expiryDateError != null) Color.Red else Color(0xFF121212).copy(alpha = 0.1f),
                        unfocusedBorderColor = if (expiryDateError != null) Color.Red else Color(0xFF121212).copy(alpha = 0.1f),
                        focusedContainerColor = Color(0xFFF5F5F5),
                        errorContainerColor = Color(0xFFF5F5F5),
                        unfocusedContainerColor = Color(0xFFF5F5F5),
                        cursorColor = Color(0xFF121212),
                        errorBorderColor = Color.Red
                    )
                )
                if (expiryDateError != null) {
                    Text(
                        text = expiryDateError!!,
                        style = TextStyle(
                            fontFamily = GoogleSansFamily,
                            fontWeight = FontWeight(400),
                            fontSize = 12.sp,
                            color = Color.Red
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // CVV
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "CVV/CVC",
                    style = TextStyle(
                        fontFamily = GoogleSansFamily,
                        fontWeight = FontWeight(510),
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        color = Color(0xFF121212).copy(alpha = 0.8f)
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = cvv,
                    onValueChange = {
                        val cleanCvv = it.take(4).filter { char -> char.isDigit() }
                        if (onEvent != null) {
                            onEvent(com.example.limouserapp.ui.state.CreditCardUiEvent.CvvChanged(cleanCvv))
                            cvvError = if (!isValidCvv(cleanCvv, cardNumber)) "Invalid CVV" else null
                        } else {
                            localCvv = cleanCvv
                            cvvError = if (!isValidCvv(cleanCvv, cardNumber)) "Invalid CVV" else null
                        }
                    },
                    placeholder = {
                        Text(
                            text = "***",
                            style = TextStyle(
                                fontFamily = GoogleSansFamily,
                                fontWeight = FontWeight(400),
                                fontSize = 16.sp,
                                lineHeight = 20.sp,
                                letterSpacing = 0.sp,
                                color = Color(0xFF121212).copy(alpha = 0.4f)
                            )
                        )
                    },
                    isError = cvvError != null,
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .focusRequester(cvvFocus),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(
                        fontFamily = GoogleSansFamily,
                        fontWeight = FontWeight(400),
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        letterSpacing = 0.sp,
                        color = Color(0xFF121212)
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (cvvError != null) Color.Red else Color(0xFF121212).copy(alpha = 0.1f),
                        unfocusedBorderColor = if (cvvError != null) Color.Red else Color(0xFF121212).copy(alpha = 0.1f),
                        focusedContainerColor = Color(0xFFF5F5F5),
                        errorContainerColor = Color(0xFFF5F5F5),
                        unfocusedContainerColor = Color(0xFFF5F5F5),
                        cursorColor = Color(0xFF121212),
                        errorBorderColor = Color.Red
                    )
                )
                if (cvvError != null) {
                    Text(
                        text = cvvError!!,
                        style = TextStyle(
                            fontFamily = GoogleSansFamily,
                            fontWeight = FontWeight(400),
                            fontSize = 12.sp,
                            color = Color.Red
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }


        Spacer(Modifier.height(24.dp))

        // Name on Card
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "NAME ON CARD",
                style = TextStyle(
                    fontFamily = GoogleSansFamily,
                    fontWeight = FontWeight(510),
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = Color(0xFF121212).copy(alpha = 0.8f)
                )
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = nameOnCard,
                onValueChange = {
                    val cleanName = it.take(100)
                    if (onEvent != null) {
                        onEvent(com.example.limouserapp.ui.state.CreditCardUiEvent.CardHolderNameChanged(cleanName))
                        nameOnCardError = if (!isValidName(cleanName)) "Enter a valid name" else null
                    } else {
                        localNameOnCard = cleanName
                        nameOnCardError = if (!isValidName(cleanName)) "Enter a valid name" else null
                    }
                },
                placeholder = {
                    Text(
                        text = "Alexa Smith",
                        style = TextStyle(
                            fontFamily = GoogleSansFamily,
                            fontWeight = FontWeight(400),
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                            letterSpacing = 0.sp,
                            color = Color(0xFF121212).copy(alpha = 0.4f)
                        )
                    )
                },
                isError = nameOnCardError != null,
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .focusRequester(nameOnCardFocus),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                textStyle = TextStyle(
                    fontFamily = GoogleSansFamily,
                    fontWeight = FontWeight(400),
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    letterSpacing = 0.sp,
                    color = Color(0xFF121212)
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (nameOnCardError != null) Color.Red else Color(0xFF121212).copy(alpha = 0.1f),
                    unfocusedBorderColor = if (nameOnCardError != null) Color.Red else Color(0xFF121212).copy(alpha = 0.1f),
                    focusedContainerColor = Color(0xFFF5F5F5),
                    errorContainerColor = Color(0xFFF5F5F5),
                    unfocusedContainerColor = Color(0xFFF5F5F5),
                    cursorColor = Color(0xFF121212),
                    errorBorderColor = Color.Red
                )
            )
            if (nameOnCardError != null) {
                Text(
                    text = nameOnCardError!!,
                    style = TextStyle(
                        fontFamily = GoogleSansFamily,
                        fontWeight = FontWeight(400),
                        fontSize = 12.sp,
                        color = Color.Red
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Location Autocomplete
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "LOCATION",
                style = TextStyle(
                    fontFamily = GoogleSansFamily,
                    fontWeight = FontWeight(510),
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = Color(0xFF121212).copy(alpha = 0.8f)
                )
            )
            Spacer(Modifier.height(8.dp))
            LocationAutocomplete(
                value = locationDisplay,
                onValueChange = { newValue ->
                    if (onEvent != null) {
                        onEvent(com.example.limouserapp.ui.state.CreditCardUiEvent.LocationChanged(newValue))
                    } else {
                        localLocation = newValue
                        localIsLocationSelected = false // Reset when typing
                    }
                },
                onLocationSelected = { fullAddress, selectedCity, selectedState, selectedZipCode, displayText, latitude, longitude, country ->
                    if (onEvent != null) {
                        onEvent(com.example.limouserapp.ui.state.CreditCardUiEvent.LocationSelected(
                            fullAddress = fullAddress,
                            city = selectedCity,
                            state = selectedState,
                            zipCode = selectedZipCode,
                            locationDisplay = displayText
                        ))
                    } else {
                        localLocation = displayText
                        localPostalCode = selectedZipCode
                        localIsLocationSelected = true
                    }
                    locationError = null
                    // Move focus to postal code field after location selection
                    coroutineScope.launch {
                        delay(100) // Small delay to ensure UI is updated
                        postalCodeFocus.requestFocus()
                    }
                },
                placeholder = "Search for your address",
                modifier = Modifier.fillMaxWidth()
            )
            // Show error if location is entered but not selected from dropdown
            val shouldShowLocationError = locationDisplay.isNotEmpty() && !isLocationSelected
            if (shouldShowLocationError || locationError != null) {
                Text(
                    text = locationError ?: "Please select an address from the suggestions",
                    style = TextStyle(
                        fontFamily = GoogleSansFamily,
                        fontWeight = FontWeight(400),
                        fontSize = 12.sp,
                        color = Color.Red
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Postal Code
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "POSTAL CODE",
                style = TextStyle(
                    fontFamily = GoogleSansFamily,
                    fontWeight = FontWeight(510),
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = Color(0xFF121212).copy(alpha = 0.8f)
                )
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = postalCode,
                onValueChange = {
                    val cleanPostal = it.take(10)
                    if (onEvent != null) {
                        onEvent(com.example.limouserapp.ui.state.CreditCardUiEvent.ZipCodeChanged(cleanPostal))
                        postalCodeError = if (!isValidPostalCode(cleanPostal)) "Invalid postal code" else null
                    } else {
                        localPostalCode = cleanPostal
                        postalCodeError = if (!isValidPostalCode(cleanPostal)) "Invalid postal code" else null
                    }
                },
                placeholder = {
                    Text(
                        text = "91343",
                        style = TextStyle(
                            fontFamily = GoogleSansFamily,
                            fontWeight = FontWeight(400),
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                            letterSpacing = 0.sp,
                            color = Color(0xFF121212).copy(alpha = 0.4f)
                        )
                    )
                },
                isError = postalCodeError != null,
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .focusRequester(postalCodeFocus),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                textStyle = TextStyle(
                    fontFamily = GoogleSansFamily,
                    fontWeight = FontWeight(400),
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    letterSpacing = 0.sp,
                    color = Color(0xFF121212)
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (postalCodeError != null) Color.Red else Color(0xFF121212).copy(alpha = 0.1f),
                    unfocusedBorderColor = if (postalCodeError != null) Color.Red else Color(0xFF121212).copy(alpha = 0.1f),
                    focusedContainerColor = Color(0xFFF5F5F5),
                    errorContainerColor = Color(0xFFF5F5F5),
                    unfocusedContainerColor = Color(0xFFF5F5F5),
                    cursorColor = Color(0xFF121212),
                    errorBorderColor = Color.Red
                )
            )
            if (postalCodeError != null) {
                Text(
                    text = postalCodeError!!,
                    style = TextStyle(
                        fontFamily = GoogleSansFamily,
                        fontWeight = FontWeight(400),
                        fontSize = 12.sp,
                        color = Color.Red
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // SMS Opt-In Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = uiState?.smsOptIn ?: true,
                onCheckedChange = { isChecked ->
                    if (onEvent != null) {
                        onEvent(com.example.limouserapp.ui.state.CreditCardUiEvent.SmsOptInChanged(isChecked))
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = LimoOrange,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFE0E0E0)
                ),
                modifier = Modifier.size(48.dp, 28.dp)
            )
            
            Spacer(Modifier.width(12.dp))
            
            Text(
                text = "I agree to receive SMS notifications about my rides and account updates",
                style = TextStyle(
                    fontFamily = GoogleSansFamily,
                    fontWeight = FontWeight(400),
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    color = Color(0xFF121212).copy(alpha = 0.8f)
                ),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.weight(1f))

        // Terms and Conditions
        Text(
            text = "By adding a new card, you agree to our Credit cards terms & conditions.",
            style = TextStyle(
                fontFamily = GoogleSansFamily,
                fontWeight = FontWeight(400),
                fontSize = 14.sp,
                lineHeight = 18.sp,
                color = Color(0xFF121212).copy(alpha = 0.6f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        // Error message from ViewModel
        if (uiState?.error != null) {
            Text(
                text = uiState.error,
                style = TextStyle(
                    fontFamily = GoogleSansFamily,
                    fontWeight = FontWeight(400),
                    fontSize = 14.sp,
                    color = Color.Red
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
        }

        // Add a Card & Sign Up button
        Button(
            onClick = { onNext() },
            colors = ButtonDefaults.buttonColors(
                containerColor = LimoOrange
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = isFormValid && !(uiState?.isLoading ?: false)
        ) {
            Text(
                text = "Add a Card & Sign Up",
                style = TextStyle(
                    fontFamily = GoogleSansFamily,
                    fontWeight = FontWeight(400),
                    fontSize = 16.sp,
                    color = Color.White
                )
            )
        }
    }

    // Loading overlay
    if (uiState?.isLoading == true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = LimoOrange)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddCreditCardScreenPreview() {
    AddCreditCardScreen(
        onNext = { /* Preview action */ },
        onBack = { /* Preview action */ }
    )
}
