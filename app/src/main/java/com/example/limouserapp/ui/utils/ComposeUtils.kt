package com.example.limouserapp.ui.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * Makes a composable clickable without any visual ripple/indication.
 * Use for areas where ripple is not desired (matches limodriverapp behavior).
 * Theme-level ripple is already disabled; this is for explicit modifier use.
 */
@Composable
fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = this.clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = null,
    onClick = onClick
)
