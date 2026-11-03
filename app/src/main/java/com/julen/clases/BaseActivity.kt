package com.julen.clases

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        optimizeRefreshRate()
    }

    private fun optimizeRefreshRate() {
        // En dispositivos modernos, solicita el máximo refresco disponible para fluidez total
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val modes = display?.supportedModes
            if (!modes.isNullOrEmpty()) {
                // Buscamos el modo con mayor tasa de refresco (FPS)
                val maxRefreshRateMode = modes.maxByOrNull { it.refreshRate }
                if (maxRefreshRateMode != null) {
                    val attrs = window.attributes
                    attrs.preferredDisplayModeId = maxRefreshRateMode.modeId
                    window.attributes = attrs
                }
            }
        }
    }
}
