plugins {
    id("com.android.application") version "8.7.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    // Throwaway feasibility branch only: headless layout screenshots (not for main).
    // 1.3.5's layoutlib has no API-36 android.os.Build inner classes (NoSuchElementException
    // in Renderer.configureBuildProperties). 2.0.0-alpha05 ships layoutlib 16.2.1 + compileSdk 36.
    id("app.cash.paparazzi") version "2.0.0-alpha05" apply false
}
