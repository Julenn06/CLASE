package com.julen.clases.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.julen.clases.AppViewModel
import com.julen.clases.UiState
import com.julen.clases.ui.screens.AttendanceScreen
import com.julen.clases.ui.screens.HomeScreen
import com.julen.clases.ui.screens.ScheduleScreen
import com.julen.clases.ui.screens.SettingsScreen
import com.julen.clases.ui.screens.TasksScreen
import kotlinx.coroutines.launch

enum class Destination(val label: String, val icon: ImageVector) {
    HOME("Inicio", Icons.Filled.Home),
    SCHEDULE("Horario", Icons.Filled.CalendarMonth),
    ATTENDANCE("Asistencia", Icons.Filled.FactCheck),
    TASKS("Tareas", Icons.Filled.EditNote),
    SETTINGS("Ajustes", Icons.Filled.Settings),
}

/** Muestra un snackbar con acción opcional (normalmente "Deshacer"). */
typealias ShowMessage = (message: String, actionLabel: String?, onAction: (() -> Unit)?) -> Unit

@Composable
fun ClasesApp(state: UiState, viewModel: AppViewModel) {
    var destination by rememberSaveable { mutableStateOf(Destination.HOME) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val showMessage: ShowMessage = { message, actionLabel, onAction ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) onAction?.invoke()
        }
    }

    BackHandler(enabled = destination != Destination.HOME) { destination = Destination.HOME }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                Destination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = item == destination,
                        onClick = { destination = item },
                        icon = {
                            val alert = item == Destination.ATTENDANCE && state.subjectsAtRisk.isNotEmpty()
                            val pending = item == Destination.TASKS && state.pendingTasks.isNotEmpty()
                            BadgedBox(
                                badge = {
                                    when {
                                        alert -> Badge()
                                        pending -> Badge(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                        ) { Text(state.pendingTasks.size.coerceAtMost(99).toString()) }
                                    }
                                },
                            ) {
                                Icon(item.icon, contentDescription = null)
                            }
                        },
                        label = { Text(item.label, maxLines = 1) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            Crossfade(targetState = destination, label = "navigation") { current ->
                when (current) {
                    Destination.HOME -> HomeScreen(
                        state = state,
                        viewModel = viewModel,
                        onNavigate = { destination = it },
                        showMessage = showMessage,
                    )
                    Destination.SCHEDULE -> ScheduleScreen(state = state)
                    Destination.ATTENDANCE -> AttendanceScreen(
                        state = state,
                        viewModel = viewModel,
                        showMessage = showMessage,
                    )
                    Destination.TASKS -> TasksScreen(
                        state = state,
                        viewModel = viewModel,
                        showMessage = showMessage,
                    )
                    Destination.SETTINGS -> SettingsScreen(
                        state = state,
                        viewModel = viewModel,
                        showMessage = showMessage,
                    )
                }
            }
        }
    }
}
