package com.ccos.retro.pro

import com.ccos.retro.model.AppPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProContrastTest {
    private val eps = 0.01

    @Test fun luminanceEndpoints() {
        assertEquals(0.0, ProContrast.luminance(0xFF000000.toInt()), 1e-9)
        assertEquals(1.0, ProContrast.luminance(0xFFFFFFFF.toInt()), 1e-9)
        // sRGB linear segment boundary and a mid grey (WCAG reference values).
        assertEquals(0.2159, ProContrast.luminance(0xFF808080.toInt()), 1e-3)
    }

    @Test fun ratioMatchesWcagReference() {
        assertEquals(21.0, ProContrast.ratio(0xFF000000.toInt(), 0xFFFFFFFF.toInt()), 1e-9)
        assertEquals(1.0, ProContrast.ratio(0xFF336699.toInt(), 0xFF336699.toInt()), 1e-9)
        // Symmetric.
        assertEquals(ProContrast.ratio(0xFFFFB000.toInt(), 0xFF0F1620.toInt()), ProContrast.ratio(0xFF0F1620.toInt(), 0xFFFFB000.toInt()), 1e-12)
        // #777777 on white is the classic 4.48:1 just-fail case.
        assertEquals(4.48, ProContrast.ratio(0xFF777777.toInt(), 0xFFFFFFFF.toInt()), eps)
    }

    @Test fun passingThemeUsesAccent() {
        val b = ProContrast.buyButtonForSkin(AppPrefs.CONSOLE_SKIN_MCC)
        assertTrue(b.usesAccent)
        assertEquals(0xFFFFB000.toInt(), b.fill)
        assertEquals(ProContrast.ACCENT_LABEL, b.label)
        assertEquals(10.49, b.labelRatio, eps)
        assertEquals(9.92, b.fillRatio, eps)
    }

    @Test fun lowContrastThemesFallBack() {
        for (skin in listOf(AppPrefs.CONSOLE_SKIN_JAXA, AppPrefs.CONSOLE_SKIN_CNSA)) {
            val b = ProContrast.buyButtonForSkin(skin)
            assertFalse(skin, b.usesAccent)
            assertEquals(0xFF90E0FF.toInt(), b.fill)
            assertEquals(0xFF1A1A22.toInt(), b.label)
        }
        assertEquals(3.87, ProContrast.ratio(ProContrast.ACCENT_LABEL, ProContrast.accentFor(AppPrefs.CONSOLE_SKIN_JAXA)), eps)
    }

    @Test fun bothThresholdsAreRequired() {
        // Label passes but fill-vs-sheet fails: a near-black accent on the dark sheet.
        val white = 0xFFFFFFFF.toInt()
        val darkFill = 0xFF1C2630.toInt()
        assertTrue(ProContrast.ratio(white, darkFill) >= 4.5)
        assertTrue(ProContrast.ratio(darkFill, ProContrast.SHEET_BG) < 3.0)
        assertFalse(ProContrast.buyButton(darkFill, accentLabel = white).usesAccent)
        // Fill passes but label fails: white label on amber.
        val amber = 0xFFFFB000.toInt()
        assertTrue(ProContrast.ratio(amber, ProContrast.SHEET_BG) >= 3.0)
        assertTrue(ProContrast.ratio(white, amber) < 4.5)
        assertFalse(ProContrast.buyButton(amber, accentLabel = white).usesAccent)
    }

    @Test fun everyThemeEndsUpAccessible() {
        for (skin in AppPrefs.CONSOLE_SKIN_IDS) {
            val b = ProContrast.buyButtonForSkin(skin)
            assertTrue("$skin label ${b.labelRatio}", ProContrast.ratio(b.label, b.fill) >= 4.5)
            assertTrue("$skin fill ${b.fillRatio}", ProContrast.ratio(b.fill, ProContrast.SHEET_BG) >= 3.0)
        }
    }
}
