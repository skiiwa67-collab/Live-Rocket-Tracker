package com.ccos.retro.screenshot

import android.view.View
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.ccos.retro.R
import org.junit.Rule
import org.junit.Test

/**
 * Throwaway-branch Paparazzi captures for the cloud-VM feasibility run.
 * Inflates existing XML (no Activity lifecycle, no network).
 */
class HeadlessScreenshotsTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6,
        theme = "Theme.RetroCommand"
    )

    @Test
    fun settingsMainActivity() {
        paparazzi.snapshot(paparazzi.inflate<View>(R.layout.activity_main), "settings")
    }

    @Test
    fun commandCenterLaunchView() {
        paparazzi.snapshot(paparazzi.inflate<View>(R.layout.activity_command_center), "command_center")
    }

    @Test
    fun proSheet() {
        paparazzi.snapshot(paparazzi.inflate<View>(R.layout.sheet_pro_upsell), "pro_sheet")
    }
}
