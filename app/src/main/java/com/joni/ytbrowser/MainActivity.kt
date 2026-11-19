package com.joni.ytbrowser

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.browser.customtabs.CustomTabsIntent

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private lateinit var root: FrameLayout
    private var fullscreenView: View? = null
    private var fullscreenCallback: WebChromeClient.CustomViewCallback? = null
    private var isVideoFullscreen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        val page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(6, 4, 6, 4)
            setBackgroundColor(Color.rgb(245, 245, 245))
        }
        webView = WebView(this)
        fun addTool(label: String, action: () -> Unit) {
            toolbar.addView(TextView(this).apply {
                text = label; textSize = 14f; setTextColor(Color.BLACK)
                setPadding(12, 12, 12, 12); setOnClickListener { action() }
            })
        }
        addTool("Back") { if (webView.canGoBack()) webView.goBack() }
        addTool("Forward") { if (webView.canGoForward()) webView.goForward() }
        addTool("Reload") { webView.reload() }
        addTool("Login") { openChromeAuthPage() }
        page.addView(toolbar)
        page.addView(webView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        root.addView(page)
        setContentView(root)
        configureWebView()
        if (savedInstanceState == null) webView.loadUrl("https://www.youtube.com/")
        else webView.restoreState(savedInstanceState)
    }

    @Suppress("SetJavaScriptEnabled")
    private fun configureWebView() {
        val s = webView.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.databaseEnabled = true
        s.mediaPlaybackRequiresUserGesture = true
        s.useWideViewPort = true
        s.loadWithOverviewMode = true
        s.javaScriptCanOpenWindowsAutomatically = true
        s.setSupportMultipleWindows(false)
        s.cacheMode = WebSettings.LOAD_DEFAULT
        val ua = s.userAgentString
        s.userAgentString = ua.replace(" (Linux; Android", " (X11; Linux")
            .replace(Regex("; wv\\)"), ")")
            .replace(Regex(" Version/[0-9.]+"), "")
            .replace(" Mobile", "")
        s.setSupportZoom(true)
        s.builtInZoomControls = true
        s.displayZoomControls = false
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?, request: android.webkit.WebResourceRequest?
            ): Boolean {
                val uri = request?.url ?: return false
                val host = uri.host.orEmpty()
                val internal = listOf("youtube.com", "youtu.be", "ytimg.com", "googlevideo.com")
                    .any { host == it || host.endsWith(".$it") }
                if (internal) return false
                return try {
                    startActivity(Intent(Intent.ACTION_VIEW, uri)); true
                } catch (_: Exception) { false }
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (view == null || fullscreenView != null) {
                    callback?.onCustomViewHidden(); return
                }
                fullscreenView = view
                fullscreenCallback = callback
                isVideoFullscreen = true
                webView.visibility = View.GONE
                root.addView(view, FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                ))
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
            override fun onHideCustomView() { exitFullscreen() }
        }
    }

    private fun openChromeAuthPage() {
        try {
            CustomTabsIntent.Builder().build()
                .launchUrl(this, Uri.parse("https://accounts.google.com/"))
        } catch (_: Exception) {
            Toast.makeText(this, "No compatible browser found", Toast.LENGTH_LONG).show()
        }
    }

    private fun exitFullscreen() {
        fullscreenView?.let { root.removeView(it) }
        fullscreenView = null
        fullscreenCallback?.onCustomViewHidden()
        fullscreenCallback = null
        webView.visibility = View.VISIBLE
        isVideoFullscreen = false
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isVideoFullscreen && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPictureInPictureMode(PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9)).build())
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean, newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        fullscreenView?.systemUiVisibility = if (isInPictureInPictureMode)
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        else View.SYSTEM_UI_FLAG_VISIBLE
    }

    override fun onBackPressed() {
        when {
            fullscreenView != null -> exitFullscreen()
            webView.canGoBack() -> webView.goBack()
            else -> super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState); super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        webView.stopLoading()
        webView.webChromeClient = null
        webView.webViewClient = null
        webView.destroy()
        super.onDestroy()
    }
}
