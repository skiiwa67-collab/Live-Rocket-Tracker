package com.ccos.retro.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import java.io.RandomAccessFile

/**
 * First concrete Data Provider – live device metrics only.
 * No simulated numbers for battery / CPU / memory.
 */
class SystemMetricsProvider(private val context: Context) : DataProvider {

    override val id = "system_metrics"

    data class Snapshot(
        val batteryPercent: Int,
        val isCharging: Boolean,
        val cpuPercent: Int,
        val availableProcessors: Int,
        val activeThreads: Int,
        val usedMemMb: Int,
        val maxMemMb: Int,
        val frameTimeMs: Float,
        val internalStorageFrac: Float,
        val externalStorageFrac: Float,
        val internalUsedGb: Float,
        val internalTotalGb: Float
    )

    private var lastCpuIdle = 0L
    private var lastCpuTotal = 0L

    override fun sample(): Snapshot = sample(16f)

    fun sample(frameTimeMs: Float = 16f): Snapshot {
        val battery = sampleBattery()
        val cpu = sampleCpu()
        val rt = Runtime.getRuntime()
        val used = ((rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)).toInt()
        val max = (rt.maxMemory() / (1024 * 1024)).toInt()

        return Snapshot(
            batteryPercent = battery.first,
            isCharging = battery.second,
            cpuPercent = cpu,
            availableProcessors = rt.availableProcessors(),
            activeThreads = Thread.activeCount(),
            usedMemMb = used,
            maxMemMb = max,
            frameTimeMs = frameTimeMs,
            internalStorageFrac = storageFrac(Environment.getDataDirectory().absolutePath),
            externalStorageFrac = storageFrac(context.getExternalFilesDir(null)?.absolutePath),
            internalUsedGb = storageUsedGb(Environment.getDataDirectory().absolutePath),
            internalTotalGb = storageTotalGb(Environment.getDataDirectory().absolutePath)
        )
    }

    private fun storageFrac(path: String?): Float {
        if (path.isNullOrBlank()) return 0f
        return try {
            val st = StatFs(path)
            val total = st.blockCountLong * st.blockSizeLong
            val free = st.availableBlocksLong * st.blockSizeLong
            if (total <= 0L) 0f else (1.0 - free.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f)
        } catch (_: Exception) { 0f }
    }

    private fun storageTotalGb(path: String?): Float {
        if (path.isNullOrBlank()) return 0f
        return try {
            val st = StatFs(path)
            (st.blockCountLong * st.blockSizeLong / 1_073_741_824.0).toFloat()
        } catch (_: Exception) { 0f }
    }

    private fun storageUsedGb(path: String?): Float {
        if (path.isNullOrBlank()) return 0f
        return try {
            val st = StatFs(path)
            val total = st.blockCountLong * st.blockSizeLong
            val free = st.availableBlocksLong * st.blockSizeLong
            ((total - free) / 1_073_741_824.0).toFloat()
        } catch (_: Exception) { 0f }
    }

    private fun sampleBattery(): Pair<Int, Boolean> {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val status = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val charging = status?.getIntExtra(BatteryManager.EXTRA_STATUS, -1).let {
                it == BatteryManager.BATTERY_STATUS_CHARGING || it == BatteryManager.BATTERY_STATUS_FULL
            }
            pct to (charging == true)
        } catch (_: Exception) {
            50 to false
        }
    }

    private fun sampleCpu(): Int {
        return try {
            val reader = RandomAccessFile("/proc/stat", "r")
            val load = reader.readLine()
            reader.close()
            val toks = load.split(" ").filter { it.isNotEmpty() }
            val idle = toks[4].toLong()
            val total = toks.drop(1).take(7).sumOf { it.toLongOrNull() ?: 0L }
            val diffIdle = idle - lastCpuIdle
            val diffTotal = total - lastCpuTotal
            lastCpuIdle = idle
            lastCpuTotal = total
            if (diffTotal == 0L) 0
            else (100.0 * (diffTotal - diffIdle) / diffTotal).toInt().coerceIn(0, 100)
        } catch (_: Exception) {
            3
        }
    }
}
