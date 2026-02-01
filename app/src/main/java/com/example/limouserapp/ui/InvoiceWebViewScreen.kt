package com.example.limouserapp.ui

import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.limouserapp.data.network.NetworkConfig
import com.example.limouserapp.di.TokenManagerEntryPoint
import dagger.hilt.android.EntryPointAccessors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceWebViewScreen(
    invoiceNumber: Int,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val appContext = context.applicationContext

    // 1. REVERTED: Removed 'remember' to ensure we get the fresh token every time
    // just like your original code.
    val tokenManager = EntryPointAccessors.fromApplication(
        appContext,
        TokenManagerEntryPoint::class.java
    ).tokenManager()

    val token = tokenManager.getAccessToken()
    
    val baseUrl = NetworkConfig.BASE_URL.trimEnd('/')
    val url = "$baseUrl/api/mobile/v1/user/invoice/$invoiceNumber"

    // 2. Ensure headers are rebuilt if token changes
    val headers = if (token != null) {
        mapOf(NetworkConfig.AUTHORIZATION to "${NetworkConfig.BEARER_PREFIX}$token")
    } else {
        emptyMap()
    }

    var isLoading by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Invoice #${invoiceNumber.coerceAtLeast(0)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            // Mixed content allows HTTP images on HTTPS sites (common source of blank screens)
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                            }

                            // 3. DEBUGGING: Log errors to Logcat if the page fails
                            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                Log.e("InvoiceWebView", "Error loading: ${error?.description}")
                                isLoading = false 
                            }
                        }
                        
                        // Load initially
                        loadUrl(url, headers)
                    }
                },
                // 4. CRITICAL FIX: If recomposition happens (e.g. token arrives late),
                // ensure we load the URL if the WebView is currently blank.
                update = { webView ->
                    if (webView.url == null) {
                        webView.loadUrl(url, headers)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}