package com.ccos.retro.ui

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.ccos.retro.R
import com.ccos.retro.model.AppPrefs
import com.ccos.retro.wallpaper.RetroCommandWallpaperService

/**
 * First tap of CCOS. Yes applies the live wallpaper, then the one + page line.
 */
class SetupActivity : AppCompatActivity() {

    private lateinit var prefs: AppPrefs
    private var waitingForWallpaper = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = AppPrefs(this)
        prefs.ensurePaidAppDefaults()
        if (prefs.setupComplete) {
            goSettings()
            return
        }

        setContentView(R.layout.activity_setup)

        val ask = findViewById<View>(R.id.panel_ask)
        val guide = findViewById<View>(R.id.panel_guide)

        findViewById<Button>(R.id.btn_yes).setOnClickListener {
            prefs.hudEveryScreen = false
            prefs.commandPageIndex = 2
            if (prefs.launcherPageCount < 3) prefs.launcherPageCount = 3
            prefs.telemetryListMode = "current"
            prefs.telemetryAuto = true
            prefs.telemetryLaunchId = ""
            prefs.telemetryHoldUntilMs = 0L
            ask.visibility = View.GONE
            if (isCcosWallpaper()) {
                guide.visibility = View.VISIBLE
            } else {
                waitingForWallpaper = true
                applyLiveWallpaper()
            }
        }

        findViewById<Button>(R.id.btn_done).setOnClickListener {
            prefs.setupComplete = true
            goHome()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!waitingForWallpaper) return
        waitingForWallpaper = false
        findViewById<View>(R.id.panel_ask).visibility = View.GONE
        findViewById<View>(R.id.panel_guide).visibility = View.VISIBLE
    }

    private fun isCcosWallpaper(): Boolean {
        val info = WallpaperManager.getInstance(this).wallpaperInfo ?: return false
        return info.component.packageName == packageName
    }

    private fun applyLiveWallpaper() {
        val component = ComponentName(this, RetroCommandWallpaperService::class.java)
        val direct = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component)
        }
        try {
            startActivity(direct)
        } catch (_: Exception) {
            startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
        }
    }

    private fun goSettings() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        finish()
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }
}
