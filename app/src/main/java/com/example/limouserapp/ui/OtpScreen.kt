package com.example.limouserapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.limouserapp.R
import com.example.limouserapp.ui.components.ErrorAlertDialog
import com.example.limouserapp.ui.state.OtpUiEvent
import com.example.limouserapp.ui.state.OtpUiState
import com.example.limouserapp.ui.theme.LimoOrange
import com.example.limouserapp.ui.theme.GoogleSansFamily
import com.example.limouserapp.ui.theme.AppDimensions
import com.example.limouserapp.ui.viewmodel.OtpViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val RESEND_INTERVAL_SECONDS = 30

@Composable
fun OtpScreen(
    tempUserId: String,
    phoneNumber: String,
    onNext: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: OtpViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    // OTP
    var otpValue by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    // Error dialog
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorDialogTitle by remember { mutableStateOf("") }
    var errorDialogMessage by remember { mutableStateOf("") }

    // 🔹 RESEND TIMER STATE
    var resendCooldown by remember { mutableIntStateOf(RESEND_INTERVAL_SECONDS) }
    var canResend by remember { mutableStateOf(false) }

    // 🔹 Start resend timer immediately on screen load
    LaunchedEffect(Unit) {
        resendCooldown = RESEND_INTERVAL_SECONDS
        canResend = false
        while (resendCooldown > 0) {
            delay(1_000)
            resendCooldown--
        }
        canResend = true
    }

    LaunchedEffect(tempUserId, phoneNumber) {
        viewModel.setInitialData(tempUserId, phoneNumber)
        delay(300)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    // Auto-navigate when OTP verification succeeds
    LaunchedEffect(uiState.success, uiState.nextAction) {
        if (uiState.success && uiState.nextAction != null) {
            // Small delay to ensure UI updates are complete before navigation
            delay(100)
            onNext(uiState.nextAction!!)
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            errorDialogTitle = "Error"
            errorDialogMessage = error
            showErrorDialog = true
            otpValue = ""
            focusRequester.requestFocus()
        }
    }

    // Auto-submit OTP when 6 digits are entered
    LaunchedEffect(otpValue) {
        if (otpValue.length == 6) {
            // Clear focus and hide keyboard immediately for better UX
            focusManager.clearFocus()
            keyboardController?.hide()
            
            // Update OTP value and trigger verification
            viewModel.onEvent(OtpUiEvent.OtpChanged(otpValue))
            // Small delay to ensure state is updated before verification
            delay(50)
            viewModel.onEvent(OtpUiEvent.VerifyOtp)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(50.dp))

        Text(
            text = buildAnnotatedString {
                append("Enter the 6-digit code sent via SMS at ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(phoneNumber)
                }
                append(".")
            },
            style = TextStyle(
                fontFamily = GoogleSansFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 24.sp,
                color = Color.Black,
                lineHeight = 32.sp
            )
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Change your mobile number?",
            style = TextStyle(
                fontFamily = GoogleSansFamily,
                fontSize = 16.sp,
                color = Color.Black.copy(alpha = 0.6f),
                textDecoration = TextDecoration.Underline
            ),
            modifier = Modifier.clickable {
                otpValue = ""
                // Clear stored data when changing phone number
                viewModel.onEvent(OtpUiEvent.ClearError)
                viewModel.onEvent(OtpUiEvent.ClearSuccess)
                onBack?.invoke()
            }
        )

        Spacer(Modifier.height(40.dp))

        // OTP Input
        Box(Modifier.fillMaxWidth()) {
            BasicTextField(
                value = otpValue,
                onValueChange = {
                    if (it.length <= 6 && it.all(Char::isDigit)) {
                        otpValue = it
                    }
                },
                modifier = Modifier
                    .matchParentSize()
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                cursorBrush = SolidColor(Color.Transparent),
                textStyle = TextStyle(color = Color.Transparent),
                decorationBox = { it() }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(6) { index ->
                    val char = otpValue.getOrNull(index)?.toString() ?: ""
                    OtpDigitVisual(
                        char = char,
                        isFocused = index == otpValue.length
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        val timerText = if (!canResend) {
            "(0:${String.format("%02d", resendCooldown)})"
        } else ""

        // 🔹 RESEND BUTTON
        Surface(
            onClick = {
                if (canResend) {
                    viewModel.onEvent(OtpUiEvent.ResendOtp)

                    resendCooldown = RESEND_INTERVAL_SECONDS
                    canResend = false

                    scope.launch {
                        while (resendCooldown > 0) {
                            delay(1_000)
                            resendCooldown--
                        }
                        canResend = true
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF3F4F6),
            enabled = canResend
        ) {
            Text(
                text = "Resend code via SMS $timerText",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                style = TextStyle(
                    fontSize = 16.sp,
                    color = if (canResend) Color.Black else Color.Gray,
                    fontWeight = FontWeight.Medium,
                    fontFamily = GoogleSansFamily
                )
            )
        }

    }

    ErrorAlertDialog(
        isVisible = showErrorDialog,
        onDismiss = { showErrorDialog = false },
        title = errorDialogTitle,
        message = errorDialogMessage
    )
}

@Composable
private fun OtpDigitVisual(char: String, isFocused: Boolean) {
    val borderColor = if (isFocused) Color.Black else Color(0xFFE5E7EB)
    val containerColor = if (isFocused) Color.White else Color(0xFFF9FAFB)

    Box(
        modifier = Modifier
            .size(46.dp)
            .background(containerColor, RoundedCornerShape(12.dp))
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = char,
            style = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                fontFamily = GoogleSansFamily,
                textAlign = TextAlign.Center
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun OtpScreenPreview() {
    OtpScreen(
        tempUserId = "test123",
        phoneNumber = "+1 5551234567",
        onNext = {},
        onBack = {}
    )
}
