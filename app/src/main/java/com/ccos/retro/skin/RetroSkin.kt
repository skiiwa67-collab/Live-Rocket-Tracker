package com.ccos.retro.skin

import android.graphics.Color

/**
 * Mercury-program era skin for the Vector Rocket module.
 * Project Mercury instrument panels: charcoal, aluminum, NASA red, amber needles.
 */
object RetroSkin {
    val bg          = Color.parseColor("#060708")
    // Issue 11: HUD grid a notch brighter so a dark webcast/PiP reads
    // against wallpaper. Plates / clock / chrome stay on their own paints.
    val grid        = Color.parseColor("#4E5C6C")
    val cyan        = Color.parseColor("#D8E0E8")   // stencil silver
    val orange      = Color.parseColor("#E8A020")   // amber needle
    val green       = Color.parseColor("#3DCC7A")   // GO lamp
    val red         = Color.parseColor("#C8102E")   // NASA safety red
    val steel       = Color.parseColor("#9AA3AD")
    val dim         = Color.parseColor("#1C2228")
    val labelMuted  = Color.parseColor("#6A7380")
    val panelFill   = Color.parseColor("#CC060708")
    val particle    = Color.parseColor("#A0C8E8")
    val btnActiveFill   = Color.parseColor("#44C8102E")
    val btnIdleFill     = Color.parseColor("#181C2228")
    val btnActiveStroke = red
    val btnIdleStroke   = Color.parseColor("#8A95A0")
}
