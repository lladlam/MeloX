package com.lladlam.melox.ui.account

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lladlam.melox.core.account.NeteaseSessionStore
import kotlinx.coroutines.delay

private const val NETEASE_LOGIN_URL = "https://music.163.com/#"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NeteaseLoginScreen(
    session: NeteaseSessionStore,
    onDismiss: () -> Unit,
    onLoggedIn: () -> Unit,
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var pageLoading by remember { mutableStateOf(true) }
    var verifying by remember { mutableStateOf(false) }
    var verificationError by remember { mutableStateOf<String?>(null) }
    var handledCookie by remember { mutableStateOf<String?>(null) }

    BackHandler {
        val view = webView
        if (view?.canGoBack() == true) view.goBack() else onDismiss()
    }

    LaunchedEffect(webView) {
        while (true) {
            val candidate = collectNeteaseCookieHeader()
            if (
                candidate.isNotBlank() &&
                NeteaseSessionStore.containsMusicU(candidate) &&
                candidate != handledCookie &&
                !verifying
            ) {
                handledCookie = candidate
                verifying = true
                verificationError = null
                val result = session.acceptAuthenticatedCookie(candidate)
                verifying = false
                result.onSuccess {
                    CookieManager.getInstance().flush()
                    onLoggedIn()
                    return@LaunchedEffect
                }.onFailure { error ->
                    verificationError = error.message ?: "登录状态验证失败，请稍后重试"
                    handledCookie = null
                }
            }
            delay(500)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "取消",
                modifier = Modifier.padding(8.dp),
                color = Color(0xFFFF3147),
                fontSize = 16.sp,
            )
            Text(
                text = "登录网易云音乐",
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "取消",
                modifier = Modifier.padding(8.dp),
                color = Color.Transparent,
                fontSize = 16.sp,
            )
        }

        if (pageLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Box(modifier = Modifier.weight(1f)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        webView = this
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)

                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.userAgentString =
                            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                "Chrome/124.0.0.0 Safari/537.36"

                        webChromeClient = WebChromeClient()
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                pageLoading = false
                                super.onPageFinished(view, url)
                            }
                        }
                        loadUrl(NETEASE_LOGIN_URL)
                    }
                },
            )

            if (verifying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(
                            text = "正在验证登录状态…",
                            modifier = Modifier.padding(top = 12.dp),
                            color = Color.White,
                        )
                    }
                }
            }

            verificationError?.let { message ->
                Text(
                    text = message,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .background(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.medium,
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

private fun collectNeteaseCookieHeader(): String {
    val manager = CookieManager.getInstance()
    val values = linkedMapOf<String, String>()
    val urls = listOf(
        "https://music.163.com/",
        "https://interface.music.163.com/",
    )

    urls.forEach { url ->
        manager.getCookie(url)
            ?.split(';')
            ?.forEach { item ->
                val parts = item.trim().split('=', limit = 2)
                if (parts.size == 2 && parts[0].isNotBlank()) {
                    values[parts[0].trim()] = parts[1].trim()
                }
            }
    }

    return values.toSortedMap().entries.joinToString("; ") { (key, value) -> "$key=$value" }
}
