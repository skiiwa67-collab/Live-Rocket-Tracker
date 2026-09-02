package com.ccos.retro.ui

import android.content.Context
import android.content.Intent

/**
 * One overlay PiP. Hot links switch the same window. Never a second MCC YouTube pane.
 * Wallpaper and CMD VID both land here.
 */
object OverlayPip {
    fun switch(context: Context, url: String, title: String) {
        val i = Intent(context, OverlayPipActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            )
            putExtra(OverlayPipActivity.EXTRA_URL, url)
            putExtra(OverlayPipActivity.EXTRA_TITLE, title)
        }
        context.startActivity(i)
    }
}
