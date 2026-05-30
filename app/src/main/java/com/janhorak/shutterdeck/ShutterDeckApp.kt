package com.janhorak.shutterdeck

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration
import java.io.File

@HiltAndroidApp
class ShutterDeckApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val configuration = Configuration.getInstance()
        val basePath = File(cacheDir, "osmdroid")
        val tileCache = File(basePath, "tiles")
        basePath.mkdirs()
        tileCache.mkdirs()

        configuration.userAgentValue = packageName
        configuration.osmdroidBasePath = basePath
        configuration.osmdroidTileCache = tileCache
    }
}