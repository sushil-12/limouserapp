package com.example.limouserapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.limouserapp.R
import com.example.limouserapp.ui.state.OtpUiEvent
import com.example.limouserapp.ui.state.OtpUiState
import com.example.limouserapp.ui.theme.LimoOrange
import com.example.limouserapp.ui.theme.GoogleSansFamily
import com.example.limouserapp.ui.theme.AppDimensions

@Composable
fun OtpScreen(
    onNext: () -> Unit,
    onBack: (() -> Unit)? = null,
    uiState: OtpUiState? = null,
    onEvent: (OtpUiEvent) -> Unit = {},
    phoneNumber: String = "+1 9876543210"
) {
    var otpDigits by remember { mutableStateOf(List(6) { "" }) }
    val focusRequesters = remember { List(6) { FocusRequester() } }
    val focusManager = LocalFocusManager.current
    var localMessage by remember { mutableStateOf<String?>(null) }

    // Update ViewModel when OTP changes
    LaunchedEffect(otpDigits) {
        onEvent(OtpUiEvent.OtpChanged(otpDigits.joinToString("")))
    }

    val isOtpComplete = otpDigits.all { it.isNotEmpty() }
    val isOtpValid = isOtpComplete && otpDigits.all { it.firstOrNull()?.isDigit() == true }

    fun verifyOtp(otp: String) {
        if (otp.length == 6 && otp.all { it.isDigit() }) {
            onEvent(OtpUiEvent.VerifyOtp)
        } else {
            localMessage = "Please enter a valid 6-digit code"
        }
    }

    LaunchedEffect(uiState?.success) {
        if (uiState?.success == true) onNext()
    }

    LaunchedEffect(uiState?.error, uiState?.message) {
        localMessage = uiState?.error ?: uiState?.message
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .padding(horizontal = 24.dp)
    ) {
        // Back button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            IconButton(
                onClick = { onBack?.invoke() },
                modifier = Modifier
                    .size(40.dp)
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

        val screenWidth = LocalConfiguration.current.screenWidthDp.dp
        val responsiveFontSize = when {
            screenWidth < 360.dp -> 24.sp
            screenWidth < 400.dp -> 26.sp
            else -> 28.sp
        }

        Text(
            text = "Enter the verification code\nsent to you",
            style = TextStyle(
                fontFamily = GoogleSansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = responsiveFontSize,
                lineHeight = (responsiveFontSize * 1.5f),
                color = Color(0xFF121212)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "We have sent you a 6-digit code on your $phoneNumber.",
            style = TextStyle(
                fontFamily = GoogleSansFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = Color(0xFF121212)
            )
        )

        Spacer(Modifier.height(32.dp))

        // OTP input boxes
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(6) { index ->
                OutlinedTextField(
                    value = otpDigits[index],
                    onValueChange = { newValue ->
                        val char = newValue.takeLast(1)
                        if (char.isEmpty()) {
                            val temp = otpDigits.toMutableList()
                            temp[index] = ""
                            otpDigits = temp
                            if (index > 0) focusRequesters[index - 1].requestFocus()
                        } else if (char.all { it.isDigit() }) {
                            val temp = otpDigits.toMutableList()
                            temp[index] = char
                            otpDigits = temp
                            if (index < 5) focusRequesters[index + 1].requestFocus()
                            else focusManager.clearFocus()
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .width(48.dp)
                        .height(48.dp)
                        .focusRequester(focusRequesters[index]),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(
                        fontFamily = GoogleSansFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF121212)
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LimoOrange,
                        unfocusedBorderColor = Color(0xFF121212).copy(alpha = 0.3f),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        cursorColor = LimoOrange,
                        errorBorderColor = Color(0xFF121212).copy(alpha = 0.3f) // override red
                    )
                )
            }
        }

        LaunchedEffect(Unit) { focusRequesters[0].requestFocus() }

        // Neutral message text
        localMessage?.let { msg ->
            Text(
                text = msg,
                color = Color(0xFF121212).copy(alpha = 0.6f),
                style = TextStyle(
                    fontFamily = GoogleSansFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )
        }

        if (uiState?.isLoading == true) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = LimoOrange,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Verifying...",
                    style = TextStyle(
                        fontFamily = GoogleSansFamily,
                        fontSize = 14.sp,
                        color = Color(0xFF121212)
                    )
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Bottom Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { onEvent(OtpUiEvent.ResendOtp) },
                shape = RoundedCornerShape(17.dp),
                border = BorderStroke(0.dp, Color.Transparent),
                modifier = Modifier
                    .width(200.dp)
                    .height(34.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFFF3F3F3), // light gray background
                    contentColor = Color(0xFF121212).copy(alpha = 0.5f) // soft gray text
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp) // tight capsule look
            ) {
                Text(
                    text = "Resend code via SMS (0:09)", // timer text
                    style = TextStyle(
                        fontFamily = GoogleSansFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        color = Color(0xFF121212).copy(alpha = 0.5f)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    textAlign = TextAlign.Center
                )
            }


            Button(
                onClick = {
                    val otpString = otpDigits.joinToString("")
                    if (isOtpValid) verifyOtp(otpString)
                    else localMessage = "Please enter all 6 digits"
                },
                enabled = isOtpComplete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isOtpValid)
                        LimoOrange else LimoOrange.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .width(94.dp)
                    .height(48.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Next",
                        style = TextStyle(
                            fontFamily = GoogleSansFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.right_arrow),
                        contentDescription = "Arrow",
                        tint = Color.White,
                        modifier = Modifier.size(AppDimensions.iconSize)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OtpScreenPreview() {
    OtpScreen(
        onNext = {},
        onBack = {},
        phoneNumber = "+1 5551234567"
    )
}
