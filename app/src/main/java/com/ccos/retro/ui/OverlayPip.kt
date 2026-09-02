package com.ccos.retro.ui

import android.content.Context
import android.content.Intent

/**
 * One HUD overlay PiP. Small player window. Wallpaper VID and MCC minimize land here.
 * Never a second MCC YouTube pane. Never a full-screen touch sink.
 */
object OverlayPip {
    fun switch(context: Context, url: String, title: String) {
        val i = Intent(context, OverlayPipActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            )
            putExtra(OverlayPipActivity.EXTRA_URL, url)
            putExtra(OverlayPipActivity.EXTRA_TITLE, title)
        }
        context.startActivity(i)
    }
}
