package com.example.limouserapp.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.limouserapp.data.model.dashboard.FaqItem
import com.example.limouserapp.ui.components.CommonHeader
import com.example.limouserapp.ui.components.ShimmerBox
import com.example.limouserapp.ui.theme.LimoOrange
import com.example.limouserapp.ui.viewmodel.FaqUiState
import com.example.limouserapp.ui.viewmodel.FaqViewModel

@Composable
fun FaqScreen(
    onNavigateBack: () -> Unit,
    viewModel: FaqViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CommonHeader(
                title = "FAQs",
                onBackClick = onNavigateBack
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (uiState) {
                is FaqUiState.Loading -> {
                    LoadingState()
                }
                is FaqUiState.Success -> {
                    val context = LocalContext.current
                    val faqData = (uiState as FaqUiState.Success).faqData
                    val allItems = remember(faqData) {
                        faqData.sections.flatMap { it.items }
                    }

                    FaqList(
                        items = allItems,
                        onContactSupportClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse(
                                    "mailto:info@1800limo.com" +
                                        "?subject=${Uri.encode("FAQ / Question - 1800 Limo User App")}"
                                )
                            }
                            context.startActivity(
                                Intent.createChooser(intent, "Send email")
                            )
                        }
                    )
                }
                is FaqUiState.Error -> {
                    ErrorState(
                        message = (uiState as FaqUiState.Error).message,
                        onRetry = { viewModel.retry() }
                    )
                }
            }
        }
    }
}

@Composable
private fun FaqList(
    items: List<FaqItem>,
    onContactSupportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = navBarHeight + 32.dp
        ),
        flingBehavior = ScrollableDefaults.flingBehavior()
    ) {
        itemsIndexed(items) { index, item ->
            FaqItemRow(item = item)
            if (index < items.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = Color(0xFFF0F0F0),
                    thickness = 1.dp
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
            ContactSupportFooter(onClick = onContactSupportClick)
        }
    }
}

@Composable
private fun ContactSupportFooter(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Still have questions?",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color.Gray,
                fontSize = 14.sp
            )
        )
        Spacer(modifier = Modifier.height(8.dp))

        val annotatedString = buildAnnotatedString {
            append("Did not find this useful? ")
            pushStringAnnotation(tag = "CONTACT", annotation = "contact")
            withStyle(
                style = SpanStyle(
                    color = LimoOrange,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append("Write us here")
            }
            pop()
        }

        Text(
            text = annotatedString,
            modifier = Modifier.clickable { onClick() },
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 15.sp,
                color = Color(0xFF121212)
            )
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun FaqItemRow(item: FaqItem) {
    var isExpanded by remember { mutableStateOf(false) }

    val rotationState by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "ArrowRotation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = item.question,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF121212),
                lineHeight = 24.sp,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            )

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Expand",
                tint = Color(0xFF757575),
                modifier = Modifier
                    .size(24.dp)
                    .rotate(rotationState)
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Text(
                text = item.answer,
                fontSize = 15.sp,
                color = Color(0xFF555555),
                lineHeight = 22.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(8) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Unable to load FAQs",
            style = MaterialTheme.typography.titleMedium,
            color = Color.Black
        )
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Try Again")
        }
    }
}
