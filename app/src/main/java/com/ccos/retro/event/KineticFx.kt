package com.ccos.retro.event

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.ccos.retro.R
import com.ccos.retro.data.LaunchSnapshot

/**
 * Generic sampled kinetic layer. Same rumbles for every vehicle.
 * Not a SpaceX recording. Ignition hits the chassis. Burn is short, not a loop forever.
 */
class KineticFx(context: Context) {

    private val app = context.applicationContext
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val ignite = pool.load(app, R.raw.fx_ignite, 1)
    private val burn = pool.load(app, R.raw.fx_burn, 1)
    private val cutoff = pool.load(app, R.raw.fx_cutoff, 1)
    private val deploy = pool.load(app, R.raw.fx_deploy, 1)
    private val reentry = pool.load(app, R.raw.fx_reentry, 1)
    private val tick = pool.load(app, R.raw.fx_tick, 1)

    private val audio = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusReq: AudioFocusRequest? = null
    private var holding = false
    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            // Book (or anything else) took the speaker. That is his decision.
            holding = false
            focusReq = null
        }
    }

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        (app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun play(event: FlightEvent) {
        holdLiveFocus()
        val title = event.title.uppercase()
        val detail = event.detail.uppercase()
        val blob = "$title $detail"
        when {
            event.severity == EventSeverity.FAIL || "RUD" in blob || "ANOMALY" in blob || "DESTROYED" in blob -> {
                slam(longArrayOf(0, 40, 30, 80, 30, 200, 40, 320), floatArrayOf(0f, 1f, 0f, 0.8f, 0f, 1f, 0f, 1f))
                pool.play(cutoff, 1f, 1f, 1, 0, 0.75f)
                pool.play(ignite, 0.7f, 0.7f, 1, 0, 0.7f)
            }
            "LIFTOFF" in blob || "HOT-STAGE" in blob || "HOT STAGE" in blob || "SES" in blob || "IGNITION" in blob -> {
                slam(longArrayOf(0, 90, 35, 160, 40, 280), floatArrayOf(0f, 1f, 0f, 0.85f, 0f, 1f))
                pool.play(ignite, 1f, 1f, 1, 0, 1f)
                pool.play(burn, 0.55f, 0.55f, 0, 0, 1f)
            }
            "SEP" in blob && "SRB" !in blob -> {
                slam(longArrayOf(0, 50, 25, 90), floatArrayOf(0f, 0.85f, 0f, 0.6f))
                pool.play(cutoff, 0.6f, 0.6f, 1, 0, 1.15f)
                pool.play(tick, 0.7f, 0.7f, 0, 0, 1f)
            }
            "BOOSTBACK" in blob || "LANDING BURN" in blob || "RELIGHT" in blob || "FLIP" in blob -> {
                slam(longArrayOf(0, 70, 30, 140, 30, 220), floatArrayOf(0f, 0.9f, 0f, 1f, 0f, 0.8f))
                pool.play(ignite, 0.9f, 0.9f, 1, 0, 1.05f)
            }
            "SPLASH" in blob || "TOUCHDOWN" in blob || "CATCH" in blob -> {
                slam(longArrayOf(0, 30, 20, 80, 30, 180), floatArrayOf(0f, 1f, 0f, 0.7f, 0f, 1f))
                pool.play(ignite, 0.75f, 0.75f, 1, 0, 0.9f)
                pool.play(cutoff, 0.8f, 0.8f, 1, 0, 0.85f)
            }
            "DEPLOY" in blob || "DOOR" in blob || "STARLINK" in blob || "FAIRING" in blob -> {
                pulse(55)
                pool.play(deploy, 0.85f, 0.85f, 1, 0, 1f)
            }
            "REENTRY" in blob || "ENTRY" in blob || "PLASMA" in blob -> {
                slam(longArrayOf(0, 40, 20, 60, 20, 90, 20, 140), floatArrayOf(0f, 0.6f, 0f, 0.7f, 0f, 0.85f, 0f, 1f))
                pool.play(reentry, 0.9f, 0.9f, 1, 0, 1f)
            }
            "MECO" in blob || "CUTOFF" in blob || "SECO" in blob -> {
                pulse(35)
                pool.play(cutoff, 0.7f, 0.7f, 1, 0, 1f)
            }
            else -> {
                pulse(18)
                pool.play(tick, 0.45f, 0.45f, 0, 0, 1f)
            }
        }
    }

    /** Wallpaper: only wake the phone for a live stack, not sim theater. */
    fun shouldAlertOnWallpaper(launch: LaunchSnapshot?, tSec: Float, sim: Float?): Boolean {
        if (sim != null) return false
        if (launch == null || launch.id.startsWith("demo-")) return false
        if (launch.isReplayable()) return false
        return tSec >= -15f && tSec < 90f * 60f
    }

    fun release() {
        releaseLiveFocus()
        try { pool.release() } catch (_: Exception) {}
    }

    /** LIVE watch: take the speaker and keep it. Do not give it back after one cue. */
    fun holdLiveFocus() {
        if (holding) return
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener(focusListener)
                    .build()
                focusReq = req
                val ok = audio.requestAudioFocus(req)
                holding = ok == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } else {
                @Suppress("DEPRECATION")
                val ok = audio.requestAudioFocus(
                    focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN
                )
                holding = ok == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            }
        } catch (_: Exception) {}
    }

    /** Unpin, catalog/historic/sim, or engine teardown. Book can play again. */
    fun releaseLiveFocus() {
        if (!holding && focusReq == null) return
        holding = false
        abandonFocus()
    }

    private fun abandonFocus() {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                focusReq?.let { audio.abandonAudioFocusRequest(it) }
                focusReq = null
            } else {
                @Suppress("DEPRECATION")
                audio.abandonAudioFocus(null)
            }
        } catch (_: Exception) {}
    }

    private fun pulse(ms: Int) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                v.vibrate(VibrationEffect.createOneShot(ms.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(ms.toLong())
            }
        } catch (_: Exception) {}
    }

    private fun slam(timings: LongArray, amps: FloatArray) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                val a = IntArray(amps.size) { (amps[it] * 255).toInt().coerceIn(1, 255) }
                v.vibrate(VibrationEffect.createWaveform(timings, a, -1))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(timings.sum())
            }
        } catch (_: Exception) {}
    }
}

object EventClock {
    fun span(sec: Float): String {
        val a = kotlin.math.abs(sec).toInt()
        val hh = a / 3600
        val mm = (a % 3600) / 60
        val ss = a % 60
        return if (hh > 0) String.format("%02d:%02d:%02d", hh, mm, ss)
        else String.format("%02d:%02d", mm, ss)
    }

    fun fmt(tSec: Float): String {
        val sign = if (tSec >= 0f) "T+" else "T-"
        return sign + span(tSec)
    }

    fun remain(from: Float, to: Float): String = "IN " + span((to - from).coerceAtLeast(0f))

    fun lastNext(launch: LaunchSnapshot?, tSec: Float): Pair<FlightEvent?, FlightEvent?> {
        val ev = FlightEventCatalog.timeline(launch)
        val last = ev.lastOrNull { it.tSec <= tSec + 0.2f }
        val next = ev.firstOrNull { it.tSec > tSec + 0.2f }
        return last to next
    }

    fun line(launch: LaunchSnapshot?, tSec: Float): String {
        val (last, next) = lastNext(launch, tSec)
        val a = last?.let { "LAST  ${it.title.take(16)}  ${fmt(it.tSec)}" } ?: "LAST  —"
        val b = next?.let {
            val eta = (it.tSec - tSec).coerceAtLeast(0f)
            "NEXT  ${it.title.take(16)}  ${fmt(it.tSec)}  (${eta.toInt()}s)"
        } ?: "NEXT  —"
        return "$a   ·   $b"
    }
}
