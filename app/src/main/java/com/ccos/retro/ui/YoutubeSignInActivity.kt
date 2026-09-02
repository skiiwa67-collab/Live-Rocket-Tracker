package com.ccos.retro.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

/**
 * One-shot fullscreen Google / YouTube sign-in. Cookies persist on CookieManager
 * for the HUD and MCC WebViews. Never load accounts.google.com inside the 280×168 hole.
 */
@SuppressLint("SetJavaScriptEnabled")
class YoutubeSignInActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(android.R.color.black)
        val cookies = CookieManager.getInstance()
        cookies.setAcceptCookie(true)
        val web = WebView(this).apply {
            setBackgroundColor(Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.setSupportMultipleWindows(false)
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            settings.userAgentString = chromeMobileUa(settings.userAgentString)
            cookies.setAcceptThirdPartyCookies(this, true)
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    return false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    cookies.flush()
                    if (signedIn(url)) {
                        generation += 1
                        cookies.flush()
                        finish()
                    }
                }
            }
        }
        setContentView(FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(web, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))
        })
        web.loadUrl(LOGIN_URL)
    }

    override fun onDestroy() {
        CookieManager.getInstance().flush()
        super.onDestroy()
    }

    companion object {
        val LOGIN_URL =
            "https://accounts.google.com/ServiceLogin?service=youtube&continue=" +
                Uri.encode("https://m.youtube.com")

        @Volatile
        var generation: Int = 0

        fun start(context: Context) {
            val i = Intent(context, YoutubeSignInActivity::class.java)
            if (context !is android.app.Activity) {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(i)
        }

        fun isAuthUrl(url: String?): Boolean {
            val u = url.orEmpty().lowercase()
            if (u.isBlank()) return false
            return u.contains("accounts.google") ||
                u.contains("accounts.youtube") ||
                (u.contains("youtube.com") && u.contains("/signin"))
        }

        fun signedIn(url: String?): Boolean {
            val u = url.orEmpty().lowercase()
            return u.contains("youtube.com") &&
                !u.contains("accounts.google") &&
                !u.contains("accounts.youtube") &&
                !u.contains("/signin")
        }

        private fun chromeMobileUa(current: String?): String {
            val raw = current.orEmpty()
            val stripped = raw.replace("; wv", "").replace(" Version/4.0", "")
            return if (stripped.contains("Chrome/")) stripped
            else "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
        }
    }
}
