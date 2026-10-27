package com.ccos.retro.pro

import com.ccos.retro.data.LaunchSnapshot

class MemProStore : ProStore {
    val map = HashMap<String, Any>()
    override fun getString(key: String): String? = map[key] as? String
    override fun putString(key: String, value: String) { map[key] = value }
    override fun getBoolean(key: String, default: Boolean): Boolean = map[key] as? Boolean ?: default
    override fun putBoolean(key: String, value: Boolean) { map[key] = value }
}

const val NOW = 1793085951000L // 2026-10-27T07:25:51Z
const val MIN = 60_000L
const val HOUR = 3_600_000L

fun snap(
    id: String = "ll2-real-1",
    net: Long = NOW,
    abbrev: String = "Success",
    status: String = "Launch Successful",
    provider: String = "SpaceX",
    rocket: String = "Falcon 9 Block 5",
    mission: String = "Starlink Group 15-16",
    name: String = "$rocket | $mission",
    webcastLive: Boolean = false
) = LaunchSnapshot(
    id = id, name = name, statusName = status, statusAbbrev = abbrev,
    netMs = net, windowStartMs = net, windowEndMs = net,
    provider = provider, rocketName = rocket, missionName = mission,
    pad = "Pad", location = "Somewhere", webcastLive = webcastLive
)
