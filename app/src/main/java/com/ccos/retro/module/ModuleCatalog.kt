package com.ccos.retro.module

import com.ccos.retro.model.AppPrefs

/**
 * Single list of CCOS modules. Wallpaper is free; modules plug in here.
 *
 * Free today: Vector Rocket (build game), System Metrics.
 * Paid today: Live Rocket Telemetry (Play product id below).
 * Add a new module by appending a [ModuleSpec] and registering its
 * [Module] instance in the wallpaper engine — do not hard-code names
 * in Settings.
 */
data class ModuleSpec(
    val id: String,
    val displayName: String,
    val isFree: Boolean,
    /** Google Play product id when [isFree] is false. */
    val playProductId: String? = null
)

object ModuleCatalog {
    const val PRODUCT_LIVE_TELEMETRY = "ccos_module_live_telemetry"

    val rocket = ModuleSpec(
        id = AppPrefs.MODULE_ROCKET,
        displayName = "Vector Rocket (Mercury)",
        isFree = true
    )
    val system = ModuleSpec(
        id = AppPrefs.MODULE_SYSTEM,
        displayName = "System Metrics",
        isFree = true
    )
    val telemetry = ModuleSpec(
        id = AppPrefs.MODULE_TELEMETRY,
        displayName = "Live Rocket Telemetry",
        isFree = false,
        playProductId = PRODUCT_LIVE_TELEMETRY
    )

    fun all(): List<ModuleSpec> = listOf(rocket, system, telemetry)

    fun get(id: String): ModuleSpec? = all().firstOrNull { it.id == id }

    fun labelForSpinner(spec: ModuleSpec): String {
        val tag = if (spec.isFree) "FREE" else "PAID"
        return "$tag  ·  ${spec.displayName}"
    }
}
