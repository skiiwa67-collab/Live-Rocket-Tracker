package com.ccos.retro.pro

import android.content.SharedPreferences

/**
 * Pro lockout (patch/pro-lockout): tiny key/value seam so ProAccess and ThemeAccess are
 * plain-JVM testable. Production wraps the app SharedPreferences; tests use an in-memory map.
 */
interface ProStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun getBoolean(key: String, default: Boolean = false): Boolean
    fun putBoolean(key: String, value: Boolean)
}

class SharedPrefsProStore(private val prefs: SharedPreferences) : ProStore {
    override fun getString(key: String): String? = prefs.getString(key, null)
    override fun putString(key: String, value: String) { prefs.edit().putString(key, value).apply() }
    override fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)
    override fun putBoolean(key: String, value: Boolean) { prefs.edit().putBoolean(key, value).apply() }
}
