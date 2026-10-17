package com.ccos.retro

import android.app.Application
import android.webkit.CookieManager

/**
 * YouTube / Google cookies live in this process. Flush on start so a
 * previous MCC SignIN is still there the next time VID opens.
 */
class LiveRocketApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val cookies = CookieManager.getInstance()
        cookies.setAcceptCookie(true)
        cookies.flush()
    }
}
