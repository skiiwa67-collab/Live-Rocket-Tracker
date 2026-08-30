package com.ccos.retro.module

interface Module {
    val id: String
    val displayName: String
    val isFree: Boolean
    /** Non-null for paid add-ons. Must match [ModuleCatalog] product ids. */
    val playProductId: String? get() = null
    val buttonLabels: Array<String>
    fun onModuleButton(index: Int): Boolean
    fun slidersFor(activeIndex: Int): List<SliderDef>
}

data class SliderDef(
    val label: String,
    val value: Float,
    val onChange: (Float) -> Unit
)
