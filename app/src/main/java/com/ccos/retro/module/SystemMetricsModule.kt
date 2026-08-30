package com.ccos.retro.module

class SystemMetricsModule : Module {
    override val id = "system_metrics"
    override val displayName = "System Metrics"
    override val isFree = true
    override val buttonLabels = arrayOf("CMD", "BAT", "CPU", "RAM", "DIS", "NET", "SEN", "HOME")
    var activePage: Int = 0
    override fun onModuleButton(index: Int): Boolean {
        activePage = index.coerceIn(0, 7)
        return true
    }
    override fun slidersFor(activeIndex: Int): List<SliderDef> = emptyList()
}
