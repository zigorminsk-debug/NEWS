package com.techpulse.app.ui.web

import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.techpulse.app.ui.components.GradientDivider
import com.techpulse.app.ui.formatHost
import com.techpulse.app.ui.openUrl
import com.techpulse.app.ui.theme.AccentCyan
import com.techpulse.app.ui.theme.Background
import com.techpulse.app.ui.theme.TextPrimary
import com.techpulse.app.ui.theme.TextSecondary
import java.net.URLEncoder

/**
 * Встроенный просмотр сайта внутри приложения.
 *
 * Если [offerTranslation] = true (англоязычный ресурс), страница грузится
 * через прокси Google Translate — сайт автоматически показывается на русском
 * с сохранением вёрстки. Переключатель «перевод» в шапке позволяет вернуться
 * к оригиналу.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    title: String,
    url: String,
    offerTranslation: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var progress by remember { mutableStateOf(0) }
    var pageTitle by remember { mutableStateOf(title) }
    var translated by remember { mutableStateOf(offerTranslation) }

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.builtInZoomControls = true
            settings.displayZoomControls = false

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    val uri = request.url
                    return if (uri.scheme == "http" || uri.scheme == "https") {
                        false // навигация остаётся внутри приложения
                    } else {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        } catch (_: Exception) {
                        }
                        true
                    }
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    view.title?.takeIf { it.isNotBlank() }?.let { pageTitle = it }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    progress = newProgress
                }
            }
        }
    }

    val goBackOrClose: () -> Unit = {
        if (webView.canGoBack()) webView.goBack() else onBack()
    }
    BackHandler(onBack = goBackOrClose)

    LaunchedEffect(translated) {
        webView.loadUrl(if (translated) googleTranslateUrl(url) else url)
    }

    DisposableEffect(Unit) {
        onDispose { webView.destroy() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = goBackOrClose) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = TextPrimary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pageTitle,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (translated) "авто-перевод RU · ${formatHost(url)}" else formatHost(url),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = if (translated) AccentCyan else TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (offerTranslation) {
                IconButton(onClick = { translated = !translated }) {
                    Icon(
                        Icons.Filled.Translate,
                        contentDescription = "Перевести сайт / показать оригинал",
                        tint = if (translated) AccentCyan else TextSecondary
                    )
                }
            }
            IconButton(onClick = { openUrl(context, url) }) {
                Icon(
                    Icons.Filled.OpenInBrowser,
                    contentDescription = "Открыть в браузере",
                    tint = TextSecondary
                )
            }
        }

        if (progress in 1..99) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = AccentCyan,
                trackColor = Color.Transparent
            )
        }
        GradientDivider()

        AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/** URL прокси Google Translate: отдаёт сайт, переведённый на русский. */
private fun googleTranslateUrl(url: String): String =
    "https://translate.google.com/translate?sl=auto&tl=ru&u=" +
        URLEncoder.encode(url, "UTF-8")
