package com.example.limouserapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.limouserapp.R
import com.example.limouserapp.ui.theme.LimoOrange
import com.example.limouserapp.ui.theme.GoogleSansFamily

@Composable
fun AddBasicDetailsScreen(
    onNext: () -> Unit,
    onBack: (() -> Unit)? = null,
    onNavigateToTerms: (() -> Unit)? = null,
    onNavigateToPrivacy: (() -> Unit)? = null,
    uiState: com.example.limouserapp.ui.state.BasicDetailsUiState? = null,
    onEvent: ((com.example.limouserapp.ui.state.BasicDetailsUiEvent) -> Unit)? = null
) {
    var localName by remember { mutableStateOf("") }
    var localEmail by remember { mutableStateOf("") }

    val name = uiState?.name ?: localName
    val email = uiState?.email ?: localEmail
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    val isLocalFormValid = remember(name, email) {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()
        trimmedName.isNotEmpty() &&
                trimmedName.length >= 2 &&
                trimmedEmail.isNotEmpty() &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()
    }

    val isFormValid = uiState?.isFormValid ?: isLocalFormValid
    val isLoading = uiState?.isLoading ?: false

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Back Button Placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            // Uncomment your back button logic here when needed
        }

        Spacer(Modifier.height(16.dp))

        // Header
        val responsiveFontSize = when {
            screenWidth < 360.dp -> 24.sp
            screenWidth < 400.dp -> 26.sp
            else -> 28.sp
        }

        Text(
            text = "Add Basic Details",
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

        // --- Name Input ---
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Enter Your Name",
                style = TextStyle(
                    fontFamily = GoogleSansFamily,
                    fontWeight = FontWeight(510),
                    fontSize = 14.sp,
                    color = Color(0xFF121212).copy(alpha = 0.8f)
                )
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = {
                    if (onEvent != null) onEvent(com.example.limouserapp.ui.state.BasicDetailsUiEvent.NameChanged(it))
                    else localName = it
                },
                placeholder = {
                    Text("eg. Alexa Smith", color = Color(0xFF121212).copy(alpha = 0.4f))
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF121212).copy(alpha = 0.1f),
                    unfocusedBorderColor = Color(0xFF121212).copy(alpha = 0.1f),
                    focusedContainerColor = Color(0xFFF5F5F5),
                    unfocusedContainerColor = Color(0xFFF5F5F5),
                    cursorColor = Color(0xFF121212)
                )
            )
        }

        Spacer(Modifier.height(24.dp))

        // --- Email Input ---
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Enter Your Email Id",
                style = TextStyle(
                    fontFamily = GoogleSansFamily,
                    fontWeight = FontWeight(510),
                    fontSize = 14.sp,
                    color = Color(0xFF121212).copy(alpha = 0.8f)
                )
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = email,
                onValueChange = {
                    if (onEvent != null) onEvent(com.example.limouserapp.ui.state.BasicDetailsUiEvent.EmailChanged(it))
                    else localEmail = it
                },
                placeholder = {
                    Text("eg. alexasmith@gmail.com", color = Color(0xFF121212).copy(alpha = 0.4f))
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF121212).copy(alpha = 0.1f),
                    unfocusedBorderColor = Color(0xFF121212).copy(alpha = 0.1f),
                    focusedContainerColor = Color(0xFFF5F5F5),
                    unfocusedContainerColor = Color(0xFFF5F5F5),
                    cursorColor = Color(0xFF121212)
                )
            )
        }

        // Pushes content to bottom
        Spacer(Modifier.weight(1f))

        if (uiState?.error != null) {
            Text(
                text = uiState.error,
                color = Color.Red,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // --- BOTTOM ROW: Terms (Left) + Next Button (Right) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp), // Bottom padding for screen edge
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Terms Text
            TermsAndPrivacyText(
                onTermsClick = { onNavigateToTerms?.invoke() },
                onPrivacyClick = { onNavigateToPrivacy?.invoke() },
                modifier = Modifier
                    .weight(1f) // Takes up available space
                    .padding(end = 12.dp) // Avoid touching the button
            )

            // Next Button
            Button(
                onClick = { onNext() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFormValid) LimoOrange else LimoOrange.copy(alpha = 0.5f),
                    disabledContainerColor = LimoOrange.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 16.dp), // Reduced padding
                modifier = Modifier
                    .width(94.dp)
                    .height(48.dp),
                enabled = isFormValid && !isLoading
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.wrapContentSize()
                ) {
                    Text(
                        text = "Next",
                        style = TextStyle(
                            fontFamily = GoogleSansFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.right_arrow),
                        contentDescription = "Arrow",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    if (uiState?.isLoading == true) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = LimoOrange)
        }
    }
}

@Composable
private fun TermsAndPrivacyText(
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    modifier: Modifier = Modifier // Added Modifier parameter
) {
    val fullText = "By continuing, I agree to the Terms of use & Privacy policy"
    val termsText = "Terms of use"
    val privacyText = "Privacy policy"

    val annotatedString = buildAnnotatedString {
        append(fullText)

        addStyle(
            style = SpanStyle(
                color = Color(0xFF121212).copy(alpha = 0.6f),
                fontFamily = GoogleSansFamily,
                fontWeight = FontWeight(400)
            ),
            start = 0,
            end = fullText.length
        )

        val termsStart = fullText.indexOf(termsText)
        if (termsStart != -1) {
            addStyle(
                style = SpanStyle(
                    color = LimoOrange,
                    textDecoration = TextDecoration.Underline,
                    fontFamily = GoogleSansFamily,
                    fontWeight = FontWeight(400)
                ),
                start = termsStart,
                end = termsStart + termsText.length
            )
            addStringAnnotation("TERMS", "", termsStart, termsStart + termsText.length)
        }

        val privacyStart = fullText.indexOf(privacyText)
        if (privacyStart != -1) {
            addStyle(
                style = SpanStyle(
                    color = LimoOrange,
                    textDecoration = TextDecoration.Underline,
                    fontFamily = GoogleSansFamily,
                    fontWeight = FontWeight(400)
                ),
                start = privacyStart,
                end = privacyStart + privacyText.length
            )
            addStringAnnotation("PRIVACY", "", privacyStart, privacyStart + privacyText.length)
        }
    }

    ClickableText(
        text = annotatedString,
        modifier = modifier, // Applied Modifier here
        onClick = { offset ->
            annotatedString.getStringAnnotations("TERMS", offset, offset).firstOrNull()?.let { onTermsClick() }
            annotatedString.getStringAnnotations("PRIVACY", offset, offset).firstOrNull()?.let { onPrivacyClick() }
        },
        style = TextStyle(
            fontFamily = GoogleSansFamily,
            fontWeight = FontWeight(400),
            fontSize = 14.sp,
            lineHeight = 18.sp
        )
    )
}

@Preview(showBackground = true)
@Composable
fun AddBasicDetailsScreenPreview() {
    AddBasicDetailsScreen(
        onNext = {},
        onBack = {}
    )
}