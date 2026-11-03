package com.julen.clases

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.julen.clases.ui.ClasesApp
import com.julen.clases.ui.theme.ClasesTheme
import com.julen.clases.ui.theme.isAppInDarkTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestMaxRefreshRate()

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val darkTheme = isAppInDarkTheme(state.themeMode)

            // Iconos de las barras del sistema acordes al tema elegido en la app.
            DisposableEffect(darkTheme) {
                val transparent = android.graphics.Color.TRANSPARENT
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(transparent, transparent) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(transparent, transparent) { darkTheme },
                )
                onDispose {}
            }

            ClasesTheme(darkTheme = darkTheme) {
                ClasesApp(state = state, viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Por si ha cambiado el día o la hora mientras la app estaba en segundo plano.
        viewModel.refresh()
    }

    /** Pide la tasa de refresco más alta disponible para animaciones fluidas. */
    private fun requestMaxRefreshRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val mode = display?.supportedModes?.maxByOrNull { it.refreshRate } ?: return
            window.attributes = window.attributes.apply { preferredDisplayModeId = mode.modeId }
        }
    }
}
