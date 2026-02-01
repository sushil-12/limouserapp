package com.example.limouserapp.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.limouserapp.data.model.dashboard.TutorialContent
import com.example.limouserapp.ui.components.CommonHeader
import com.example.limouserapp.ui.components.ShimmerBox
import com.example.limouserapp.ui.theme.LimoOrange
import com.example.limouserapp.ui.theme.LimoWhite
import com.example.limouserapp.ui.viewmodel.TutorialsUiState
import com.example.limouserapp.ui.viewmodel.TutorialsViewModel
import timber.log.Timber

/**
 * Tutorials Screen
 * Displays user tutorials organized by category (same design as limodriverapp).
 */
@Composable
fun TutorialsScreen(
    onNavigateBack: () -> Unit,
    viewModel: TutorialsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CommonHeader(
                title = "Tutorials",
                onBackClick = onNavigateBack
            )
        },
        containerColor = LimoWhite
    ) { scaffoldPadding ->

        when (uiState) {
            is TutorialsUiState.Loading -> {
                LoadingState(modifier = Modifier.padding(scaffoldPadding))
            }
            is TutorialsUiState.Success -> {
                val groupedTutorials = (uiState as TutorialsUiState.Success).groupedByCategory

                TutorialsList(
                    groupedTutorials = groupedTutorials,
                    topPadding = scaffoldPadding.calculateTopPadding(),
                    onTutorialClick = { tutorial ->
                        openVideoExternal(context, tutorial.link)
                    }
                )
            }
            is TutorialsUiState.Error -> {
                ErrorState(
                    message = (uiState as TutorialsUiState.Error).message,
                    onRetry = { viewModel.retry() },
                    modifier = Modifier.padding(scaffoldPadding)
                )
            }
        }
    }
}

@Composable
private fun TutorialsList(
    groupedTutorials: Map<String, List<TutorialContent>>,
    topPadding: androidx.compose.ui.unit.Dp,
    onTutorialClick: (TutorialContent) -> Unit
) {
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = topPadding + 16.dp,
            start = 16.dp,
            end = 16.dp,
            bottom = navBarHeight + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        groupedTutorials.forEach { (category, tutorials) ->
            item {
                Text(
                    text = category,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = LimoOrange,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items(tutorials) { tutorial ->
                TutorialCard(
                    tutorial = tutorial,
                    onClick = { onTutorialClick(tutorial) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun TutorialCard(
    tutorial: TutorialContent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val imageUrl = remember(tutorial.image) {
        if (tutorial.image.startsWith("assets/")) {
            "file:///android_asset/" + tutorial.image.removePrefix("assets/")
        } else {
            tutorial.image
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .fillMaxHeight()
                    .background(Color(0xFFE0E0E0))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = tutorial.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f))
                )

                Icon(
                    imageVector = Icons.Default.PlayCircleOutline,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.Center)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = tutorial.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF121212),
                    maxLines = 2,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tutorial.content,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF666666),
                    lineHeight = 16.sp,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        repeat(6) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = message,
                fontSize = 16.sp,
                color = Color(0xFF666666),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = LimoOrange)
            ) {
                Text("Retry")
            }
        }
    }
}

private fun openVideoExternal(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Timber.e(e, "Could not open video URL: $url")
    }
}
