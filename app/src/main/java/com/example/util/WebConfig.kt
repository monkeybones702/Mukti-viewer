package com.example.util

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

object WebConfig {

    @SuppressLint("SetJavaScriptEnabled")
    fun configureWebView(
        webView: WebView,
        onPageStarted: (String) -> Unit = {},
        onPageFinished: (String) -> Unit = {},
        onPageError: (String) -> Unit = {}
    ) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            loadsImagesAutomatically = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (url != null) onPageStarted(url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                CookieManager.getInstance().flush()
                if (url != null) onPageFinished(url)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    val desc = error?.description?.toString() ?: "Stream connection dropped"
                    onPageError(desc)
                }
            }
        }
    }

    fun applyWindowZoom(
        webView: WebView,
        zoomCols: Int,
        zoomRows: Int,
        fitEntirePhone: Boolean,
        proportionalZoom: Boolean
    ) {
        val cols = zoomCols.coerceIn(1, 5)
        val rows = zoomRows.coerceIn(1, 10)
        val script = AutoScrollScripts.buildWindowZoomScript(
            zoomCols = cols,
            zoomRows = rows,
            fitEntirePhone = fitEntirePhone,
            proportionalZoom = proportionalZoom
        )
        webView.evaluateJavascript(script, null)
    }

    fun applyAudioMute(webView: WebView, isMuted: Boolean) {
        val script = AutoScrollScripts.buildAudioMuteScript(isMuted)
        webView.evaluateJavascript(script, null)
    }

    fun performSoftRefresh(webView: WebView, currentUrl: String) {
        val script = AutoScrollScripts.buildSoftRefreshRecoveryScript()
        webView.evaluateJavascript(script) {
            if (webView.url.isNullOrBlank()) {
                webView.loadUrl(currentUrl)
            } else {
                webView.reload()
            }
        }
    }
}
