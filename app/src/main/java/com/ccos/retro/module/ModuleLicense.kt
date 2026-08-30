package com.ccos.retro.module

import android.content.Context

/**
 * CCOS is a single paid app. Every surface is unlocked.
 * Local flags from the old module kit are ignored.
 */
class ModuleLicense(context: Context) {
    @Suppress("UNUSED_PARAMETER")
    private val unused = context

    fun isUnlocked(spec: ModuleSpec): Boolean = true

    fun grant(spec: ModuleSpec) {}

    fun revokeForTesting(spec: ModuleSpec) {}
}
