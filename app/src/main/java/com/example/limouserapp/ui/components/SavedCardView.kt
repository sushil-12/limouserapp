package com.example.limouserapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.limouserapp.data.model.dashboard.CardData
import com.example.limouserapp.ui.theme.GoogleSansFamily

/**
 * Saved card view component matching the dark gradient card design
 * Displays a credit card with dark charcoal grey background and wave-like gradient patterns
 */
@Composable
fun SavedCardView(
    card: CardData,
    modifier: Modifier = Modifier
) {
    var cardSize by remember { mutableStateOf(Size.Zero) }
    
    Box(
        modifier = modifier
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .onSizeChanged { size ->
                cardSize = Size(size.width.toFloat(), size.height.toFloat())
            }
    ) {
        // Base dark charcoal grey background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1F1F1F))
        )
        
        // Top-right corner gradient (wave pattern)
        if (cardSize.width > 0 && cardSize.height > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF3A3A3A).copy(alpha = 0.6f),
                                Color(0xFF2A2A2A).copy(alpha = 0.4f),
                                Color.Transparent
                            ),
                            center = Offset(x = cardSize.width * 1.2f, y = -cardSize.height * 0.2f),
                            radius = cardSize.width * 1.5f
                        )
                    )
            )
            
            // Bottom-right corner gradient (wave pattern)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF3A3A3A).copy(alpha = 0.5f),
                                Color(0xFF2A2A2A).copy(alpha = 0.3f),
                                Color.Transparent
                            ),
                            center = Offset(x = cardSize.width * 1.2f, y = cardSize.height * 1.2f),
                            radius = cardSize.width * 1.3f
                        )
                    )
            )
        }
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top row: "Credit" and Brand
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Credit",
                    fontSize = 14.sp,
                    fontFamily = GoogleSansFamily,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                
                Text(
                    text = card.brandUppercase,
                    fontSize = 20.sp,
                    fontFamily = GoogleSansFamily,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Bottom section: Card Holder Name and Card Number
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = card.name,
                    fontSize = 16.sp,
                    fontFamily = GoogleSansFamily,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                
                // Card number with proper formatting (XXXX - XXXX - XXXX - XXXX)
                Text(
                    text = formatCardNumberForDisplay(card.last4),
                    fontSize = 18.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

/**
 * Format card number for display: XXXX - XXXX - XXXX - XXXX
 */
private fun formatCardNumberForDisplay(last4: String): String {
    return if (last4.startsWith("XXXX-XXXX-XXXX-")) {
        last4
    } else {
        "XXXX - XXXX - XXXX - $last4"
    }
}

