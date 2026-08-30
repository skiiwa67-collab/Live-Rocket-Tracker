package com.ccos.retro.data

/**
 * Data Provider Layer.
 * All real-world or external data enters the system through this contract.
 * System metrics is the first concrete provider.
 * Future providers (flight APIs, weather, calendar, etc.) implement the same shape.
 */
interface DataProvider {
    val id: String
    fun sample(): Any?          // typed snapshots per provider
}
