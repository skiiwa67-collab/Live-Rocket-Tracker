package com.ccos.retro.skin

import android.graphics.Color
import com.ccos.retro.data.LaunchSnapshot

/**
 * Agency-aware skin tokens for the Live Rocket Telemetry module.
 * Selecting a launch reskins the whole command surface.
 */
object TelemetrySkin {

    enum class ButtonStyle { SPACEX, NASA, CASC, ROSCOSMOS, ESA, ISRO, GENERIC }

    enum class MapStyle { SPACEX, NASA_PETAL, ROSCOSMOS, CASC, ESA, ROCKETLAB, ISRO, JAXA, BLUE, GENERIC }

    data class Tokens(
        val bg: Int,
        val panel: Int,
        val accent: Int,
        val accentDim: Int,
        val text: Int,
        val muted: Int,
        val go: Int,
        val hold: Int,
        val danger: Int,
        val grid: Int,
        val label: String,
        val btnStyle: ButtonStyle,
        val mapStyle: MapStyle,
        // Button-specific
        val btnIdleFill: Int,
        val btnActiveFill: Int,
        val btnIdleStroke: Int,
        val btnActiveStroke: Int,
        val btnTextIdle: Int,
        val btnTextActive: Int,
        val btnLampOn: Int,
        val btnLampOff: Int
    )

    val spacex = Tokens(
        bg = Color.parseColor("#050505"),
        panel = Color.parseColor("#CC0A0A0A"),
        accent = Color.parseColor("#FFFFFF"),
        accentDim = Color.parseColor("#005288"),
        text = Color.parseColor("#F0F0F0"),
        muted = Color.parseColor("#5A5A5A"),
        go = Color.parseColor("#00D26A"),
        hold = Color.parseColor("#FFB020"),
        danger = Color.parseColor("#FF3B30"),
        grid = Color.parseColor("#0A1520"),
        label = "SPACEX",
        btnStyle = ButtonStyle.SPACEX,
        mapStyle = MapStyle.SPACEX,
        btnIdleFill = Color.parseColor("#0C0C0C"),
        btnActiveFill = Color.parseColor("#1A1A1A"),
        btnIdleStroke = Color.parseColor("#3A3A3A"),
        btnActiveStroke = Color.parseColor("#FFFFFF"),
        btnTextIdle = Color.parseColor("#8A8A8A"),
        btnTextActive = Color.parseColor("#FFFFFF"),
        btnLampOn = Color.parseColor("#00D26A"),
        btnLampOff = Color.parseColor("#222222")
    )

    val nasa = Tokens(
        bg = Color.parseColor("#0A0E18"),
        panel = Color.parseColor("#CC0C1220"),
        accent = Color.parseColor("#0032A0"),
        accentDim = Color.parseColor("#4A6FA5"),
        text = Color.parseColor("#E8EEF8"),
        muted = Color.parseColor("#6A7A90"),
        go = Color.parseColor("#2ECC71"),
        hold = Color.parseColor("#F39C12"),
        danger = Color.parseColor("#C8102E"),
        grid = Color.parseColor("#141C28"),
        label = "NASA",
        btnStyle = ButtonStyle.NASA,
        mapStyle = MapStyle.NASA_PETAL,
        btnIdleFill = Color.parseColor("#101828"),
        btnActiveFill = Color.parseColor("#1A2840"),
        btnIdleStroke = Color.parseColor("#2A3A55"),
        btnActiveStroke = Color.parseColor("#0032A0"),
        btnTextIdle = Color.parseColor("#7A8AA0"),
        btnTextActive = Color.parseColor("#E8EEF8"),
        btnLampOn = Color.parseColor("#C8102E"),
        btnLampOff = Color.parseColor("#1A2230")
    )

    val chinese = Tokens(
        bg = Color.parseColor("#0C0808"),
        panel = Color.parseColor("#CC140C0C"),
        accent = Color.parseColor("#DE2910"),
        accentDim = Color.parseColor("#8B1A10"),
        text = Color.parseColor("#F5E6C8"),
        muted = Color.parseColor("#7A6A50"),
        go = Color.parseColor("#2ECC71"),
        hold = Color.parseColor("#E67E22"),
        danger = Color.parseColor("#C0392B"),
        grid = Color.parseColor("#1C1010"),
        label = "CASC",
        btnStyle = ButtonStyle.CASC,
        mapStyle = MapStyle.CASC,
        btnIdleFill = Color.parseColor("#140A0A"),
        btnActiveFill = Color.parseColor("#2A1010"),
        btnIdleStroke = Color.parseColor("#5A2020"),
        btnActiveStroke = Color.parseColor("#DE2910"),
        btnTextIdle = Color.parseColor("#A08060"),
        btnTextActive = Color.parseColor("#F5E6C8"),
        btnLampOn = Color.parseColor("#FFD700"),
        btnLampOff = Color.parseColor("#2A1810")
    )

    val roscosmos = Tokens(
        bg = Color.parseColor("#080A0C"),
        panel = Color.parseColor("#CC0C1014"),
        accent = Color.parseColor("#E8C547"),          // Soviet gold
        accentDim = Color.parseColor("#8A7030"),
        text = Color.parseColor("#E8E0D0"),
        muted = Color.parseColor("#6A6558"),
        go = Color.parseColor("#3DCC7A"),
        hold = Color.parseColor("#E07020"),
        danger = Color.parseColor("#C41E3A"),          // Russian red
        grid = Color.parseColor("#14181C"),
        label = "РОСКОСМОС",
        btnStyle = ButtonStyle.ROSCOSMOS,
        mapStyle = MapStyle.ROSCOSMOS,
        btnIdleFill = Color.parseColor("#101418"),
        btnActiveFill = Color.parseColor("#1C242C"),
        btnIdleStroke = Color.parseColor("#3A4048"),
        btnActiveStroke = Color.parseColor("#E8C547"),
        btnTextIdle = Color.parseColor("#8A8580"),
        btnTextActive = Color.parseColor("#F0E8D0"),
        btnLampOn = Color.parseColor("#C41E3A"),
        btnLampOff = Color.parseColor("#1A1E22")
    )

    val blueOrigin = Tokens(
        bg = Color.parseColor("#061018"),
        panel = Color.parseColor("#CC081420"),
        accent = Color.parseColor("#3D9BE9"),
        accentDim = Color.parseColor("#1A5A8A"),
        text = Color.parseColor("#E8F4FF"),
        muted = Color.parseColor("#6A889C"),
        go = Color.parseColor("#3DCC7A"),
        hold = Color.parseColor("#E8B020"),
        danger = Color.parseColor("#E04040"),
        grid = Color.parseColor("#102028"),
        label = "BLUE ORIGIN",
        btnStyle = ButtonStyle.NASA,
        mapStyle = MapStyle.BLUE,
        btnIdleFill = Color.parseColor("#0A1824"),
        btnActiveFill = Color.parseColor("#123044"),
        btnIdleStroke = Color.parseColor("#2A5068"),
        btnActiveStroke = Color.parseColor("#3D9BE9"),
        btnTextIdle = Color.parseColor("#7A98A8"),
        btnTextActive = Color.parseColor("#E8F4FF"),
        btnLampOn = Color.parseColor("#C4A35A"),
        btnLampOff = Color.parseColor("#1A2830")
    )

    val esa = Tokens(
        bg = Color.parseColor("#070B18"),
        panel = Color.parseColor("#CC0A1024"),
        accent = Color.parseColor("#FFD100"),
        accentDim = Color.parseColor("#8A7010"),
        text = Color.parseColor("#F0F4FF"),
        muted = Color.parseColor("#6A78A0"),
        go = Color.parseColor("#3DCC7A"),
        hold = Color.parseColor("#FFD100"),
        danger = Color.parseColor("#E04040"),
        grid = Color.parseColor("#101830"),
        label = "ESA / ARIANESPACE",
        btnStyle = ButtonStyle.ESA,
        mapStyle = MapStyle.ESA,
        btnIdleFill = Color.parseColor("#0C1428"),
        btnActiveFill = Color.parseColor("#183060"),
        btnIdleStroke = Color.parseColor("#003399"),
        btnActiveStroke = Color.parseColor("#FFD100"),
        btnTextIdle = Color.parseColor("#8A98C0"),
        btnTextActive = Color.parseColor("#FFFFFF"),
        btnLampOn = Color.parseColor("#FFD100"),
        btnLampOff = Color.parseColor("#1A2038")
    )

    val rocketLab = Tokens(
        bg = Color.parseColor("#080808"),
        panel = Color.parseColor("#CC101010"),
        accent = Color.parseColor("#FF5A1F"),
        accentDim = Color.parseColor("#8A3010"),
        text = Color.parseColor("#F2F2F2"),
        muted = Color.parseColor("#6A6A6A"),
        go = Color.parseColor("#00D26A"),
        hold = Color.parseColor("#FFB020"),
        danger = Color.parseColor("#FF3B30"),
        grid = Color.parseColor("#1A1A1A"),
        label = "ROCKET LAB",
        btnStyle = ButtonStyle.SPACEX,
        mapStyle = MapStyle.ROCKETLAB,
        btnIdleFill = Color.parseColor("#101010"),
        btnActiveFill = Color.parseColor("#1C1C1C"),
        btnIdleStroke = Color.parseColor("#3A3A3A"),
        btnActiveStroke = Color.parseColor("#FF5A1F"),
        btnTextIdle = Color.parseColor("#8A8A8A"),
        btnTextActive = Color.parseColor("#FFFFFF"),
        btnLampOn = Color.parseColor("#FF5A1F"),
        btnLampOff = Color.parseColor("#222222")
    )

    val isro = Tokens(
        bg = Color.parseColor("#0A0806"),
        panel = Color.parseColor("#CC14100C"),
        accent = Color.parseColor("#FF9933"),
        accentDim = Color.parseColor("#8A5010"),
        text = Color.parseColor("#F5F0E6"),
        muted = Color.parseColor("#7A7060"),
        go = Color.parseColor("#138808"),
        hold = Color.parseColor("#FF9933"),
        danger = Color.parseColor("#C8102E"),
        grid = Color.parseColor("#1A1410"),
        label = "इसरो",
        btnStyle = ButtonStyle.ISRO,
        mapStyle = MapStyle.ISRO,
        btnIdleFill = Color.parseColor("#14100C"),
        btnActiveFill = Color.parseColor("#241810"),
        btnIdleStroke = Color.parseColor("#4A3820"),
        btnActiveStroke = Color.parseColor("#FF9933"),
        btnTextIdle = Color.parseColor("#A09070"),
        btnTextActive = Color.parseColor("#F5F0E6"),
        btnLampOn = Color.parseColor("#138808"),
        btnLampOff = Color.parseColor("#1A1810")
    )

    val jaxa = Tokens(
        bg = Color.parseColor("#0C0808"),
        panel = Color.parseColor("#CC180C0C"),
        accent = Color.parseColor("#BC002D"),
        accentDim = Color.parseColor("#6A1020"),
        text = Color.parseColor("#F8F0F0"),
        muted = Color.parseColor("#807070"),
        go = Color.parseColor("#3DCC7A"),
        hold = Color.parseColor("#E8B020"),
        danger = Color.parseColor("#BC002D"),
        grid = Color.parseColor("#1C1010"),
        label = "JAXA",
        btnStyle = ButtonStyle.NASA,
        mapStyle = MapStyle.JAXA,
        btnIdleFill = Color.parseColor("#140A0A"),
        btnActiveFill = Color.parseColor("#281010"),
        btnIdleStroke = Color.parseColor("#5A2028"),
        btnActiveStroke = Color.parseColor("#BC002D"),
        btnTextIdle = Color.parseColor("#A08080"),
        btnTextActive = Color.parseColor("#F8F0F0"),
        btnLampOn = Color.parseColor("#BC002D"),
        btnLampOff = Color.parseColor("#221010")
    )

    val generic = Tokens(
        bg = Color.parseColor("#06080C"),
        panel = Color.parseColor("#CC0A1018"),
        accent = Color.parseColor("#00C8FF"),
        accentDim = Color.parseColor("#007A9A"),
        text = Color.parseColor("#D0E0F0"),
        muted = Color.parseColor("#5A7080"),
        go = Color.parseColor("#3DCC7A"),
        hold = Color.parseColor("#E8A020"),
        danger = Color.parseColor("#E04040"),
        grid = Color.parseColor("#121820"),
        label = "MISSION",
        btnStyle = ButtonStyle.GENERIC,
        mapStyle = MapStyle.GENERIC,
        btnIdleFill = Color.parseColor("#0A1018"),
        btnActiveFill = Color.parseColor("#122030"),
        btnIdleStroke = Color.parseColor("#2A3A4A"),
        btnActiveStroke = Color.parseColor("#00C8FF"),
        btnTextIdle = Color.parseColor("#6A8090"),
        btnTextActive = Color.parseColor("#D0E0F0"),
        btnLampOn = Color.parseColor("#00C8FF"),
        btnLampOff = Color.parseColor("#1A2830")
    )

    fun forLaunch(launch: LaunchSnapshot?): Tokens = when {
        launch == null -> generic
        launch.isSpaceX() -> spacex
        launch.isBlueOrigin() -> blueOrigin
        launch.isNasa() -> nasa
        launch.isChinese() -> chinese
        launch.isRussian() -> roscosmos
        launch.isEsa() -> esa
        launch.isRocketLab() -> rocketLab
        launch.isIsro() -> isro
        launch.isJaxa() -> jaxa
        else -> generic
    }
}

