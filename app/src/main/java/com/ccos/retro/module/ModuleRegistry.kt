package com.ccos.retro.module

object ModuleRegistry {
    private val modules = linkedMapOf<String, Module>()
    @Volatile private var activeId: String? = null

    fun register(module: Module) {
        modules[module.id] = module
        if (activeId == null) activeId = module.id
    }

    fun getActive(): Module? = activeId?.let { modules[it] }

    fun setActive(id: String) {
        if (modules.containsKey(id)) activeId = id
    }

    fun all(): List<Module> = modules.values.toList()
    fun get(id: String): Module? = modules[id]
}
