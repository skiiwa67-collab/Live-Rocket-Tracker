package com.ccos.retro.pro

import com.ccos.retro.data.LaunchSnapshot

/**
 * v1.1 picker tag (Da Vinci + erratum): included rows read "[INCLUDED] ", every other historic row reads "[PRO] ",
 * for Pro owners too. Returns null when the row keeps its normal time/status tag (HOLD, LIVE, PAST, T+, h, d).
 */
object PickerTag {
    const val INCLUDED = "[INCLUDED] "
    const val PRO = "[PRO] "

    fun proTag(l: LaunchSnapshot, historic: Boolean, now: Long): String? = when {
        DemoSet.isFreeDemo(l) -> INCLUDED
        l.id.startsWith("demo-") -> PRO
        !historic -> null
        l.isHold() -> null
        l.isActiveWatch(now) && l.secondsToNet(now) <= 0 -> null
        // vc159 spec call: a launch still inside its live window (NET+60 min) is never a Pro row.
        !l.isReplayable(now) -> null
        ProGate.isStillLive(l) -> null // B1: In Flight / streaming past NET+60 is still live, not a Pro row
        else -> PRO
    }
}
