plugins {
    id("com.android.application") version "8.7.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    // Throwaway feasibility branch only: headless layout screenshots (not for main).
    // 1.3.5 layoutlib cannot host compileSdk 36 (missing android.os.Build inner classes).
    // 2.0.0-alpha05 broke AGP 8.7.2 (GradleVersion.parse binary incompatibility).
    id("app.cash.paparazzi") version "1.3.5" apply false
}
