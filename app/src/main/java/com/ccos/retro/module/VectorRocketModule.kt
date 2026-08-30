package com.ccos.retro.module

import com.ccos.retro.model.BuildState

class VectorRocketModule(private val state: BuildState) : Module {
    override val id = "vector_rocket"
    override val displayName = "Vector Rocket"
    override val isFree = true
    override val buttonLabels = arrayOf("CMD", "ENG", "RKT", "PAD", "TEL", "SUB", "PER", "BUD")

    override fun onModuleButton(index: Int): Boolean {
        state.activeModule = index
        state.registerInteraction()
        return true
    }

    override fun slidersFor(activeIndex: Int): List<SliderDef> = when (activeIndex) {
        0 -> listOf(SliderDef("ENGAGEMENT", state.engagement / 1.6f) { state.engagement = it * 1.6f })
        1 -> listOf(
            SliderDef("ENGINE R&D", state.engineRndAlloc) { state.engineRndAlloc = it },
            SliderDef("ENGINE CREW", state.personnelEngine) { state.personnelEngine = it }
        )
        2 -> listOf(
            SliderDef("BODY R&D", state.bodyRndAlloc) { state.bodyRndAlloc = it },
            SliderDef("BODY CREW", state.personnelBody) { state.personnelBody = it }
        )
        3 -> listOf(
            SliderDef("PAD R&D", state.padRndAlloc) { state.padRndAlloc = it },
            SliderDef("PAD CREW", state.personnelPad) { state.personnelPad = it }
        )
        5 -> listOf(
            SliderDef("SUB R&D", state.subRndAlloc) { state.subRndAlloc = it },
            SliderDef("SUB CREW", state.personnelSub) { state.personnelSub = it }
        )
        6 -> listOf(
            SliderDef("ENGINE CREW", state.personnelEngine) { state.personnelEngine = it },
            SliderDef("PAD CREW", state.personnelPad) { state.personnelPad = it },
            SliderDef("BODY CREW", state.personnelBody) { state.personnelBody = it },
            SliderDef("SUB CREW", state.personnelSub) { state.personnelSub = it }
        )
        7 -> listOf(SliderDef("WORK RATE", state.engagement / 1.6f) { state.engagement = it * 1.6f })
        else -> emptyList()
    }
}
