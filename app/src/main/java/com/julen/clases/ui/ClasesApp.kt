package com.julen.clases.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.School
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.julen.clases.AppViewModel
import com.julen.clases.UiState
import com.julen.clases.ui.screens.AgendaScreen
import com.julen.clases.ui.screens.AttendanceScreen
import com.julen.clases.ui.screens.GradesScreen
import com.julen.clases.ui.screens.HomeScreen
import com.julen.clases.ui.screens.OnboardingScreen
import com.julen.clases.ui.screens.ScheduleScreen
import com.julen.clases.ui.screens.SettingsScreen
import com.julen.clases.ui.screens.SubjectDetailScreen
import kotlinx.coroutines.launch

enum class Destination(val label: String, val icon: ImageVector, val outlinedIcon: ImageVector) {
    HOME("Inicio", Icons.Filled.Home, Icons.Outlined.Home),
    SCHEDULE("Horario", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    ATTENDANCE("Asistencia", Icons.Filled.FactCheck, Icons.Outlined.FactCheck),
    AGENDA("Agenda", Icons.Filled.EventNote, Icons.Outlined.EventNote),
    GRADES("Notas", Icons.Filled.School, Icons.Outlined.School),
}

/** Muestra un snackbar con acción opcional (normalmente "Deshacer"). */
typealias ShowMessage = (message: String, actionLabel: String?, onAction: (() -> Unit)?) -> Unit

/** Acciones de navegación disponibles para las pantallas. */
class Navigator(
    val openTab: (Destination) -> Unit,
    val openSubject: (String) -> Unit,
    val openSettings: () -> Unit,
)

private const val OVERLAY_SETTINGS = "settings"
private const val OVERLAY_SUBJECT = "subject:"

@Composable
fun ClasesApp(state: UiState, viewModel: AppViewModel) {
    if (!state.onboarded) {
        OnboardingScreen(state = state, viewModel = viewModel)
        return
    }

    var destination by rememberSaveable { mutableStateOf(Destination.HOME) }
    var overlay by rememberSaveable { mutableStateOf<String?>(null) }
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
    val navigator = remember {
        Navigator(
            openTab = { destination = it; overlay = null },
            openSubject = { overlay = OVERLAY_SUBJECT + it },
            openSettings = { overlay = OVERLAY_SETTINGS },
        )
    }

    BackHandler(enabled = overlay != null || destination != Destination.HOME) {
        if (overlay != null) overlay = null else destination = Destination.HOME
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            // Cada pestaña dibuja su propia barra superior, que ya gestiona la barra de estado.
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                AppNavigationBar(
                    selected = destination,
                    onSelect = { destination = it },
                    badges = mapOf(
                        Destination.ATTENDANCE to state.subjectsAtRisk.size,
                        Destination.AGENDA to state.pendingTasks.size,
                    ),
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
            ) {
                AnimatedContent(
                    targetState = destination,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tabs",
                ) { current ->
                    when (current) {
                        Destination.HOME -> HomeScreen(state, viewModel, navigator, showMessage)
                        Destination.SCHEDULE -> ScheduleScreen(state, viewModel, navigator, showMessage)
                        Destination.ATTENDANCE -> AttendanceScreen(state, viewModel, navigator, showMessage)
                        Destination.AGENDA -> AgendaScreen(state, viewModel, showMessage)
                        Destination.GRADES -> GradesScreen(state, viewModel, navigator, showMessage)
                    }
                }
            }
        }

        // Pantallas secundarias que se deslizan por encima de las pestañas.
        AnimatedContent(
            targetState = overlay,
            transitionSpec = {
                if (targetState != null) {
                    slideInHorizontally { it } + fadeIn() togetherWith fadeOut()
                } else {
                    fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                }
            },
            label = "overlay",
            modifier = Modifier.fillMaxSize(),
        ) { current ->
            when {
                current == null -> Spacer(Modifier)
                current == OVERLAY_SETTINGS -> OverlaySurface {
                    SettingsScreen(state, viewModel, onBack = { overlay = null }, showMessage = showMessage)
                }
                current.startsWith(OVERLAY_SUBJECT) -> OverlaySurface {
                    SubjectDetailScreen(
                        code = current.removePrefix(OVERLAY_SUBJECT),
                        state = state,
                        viewModel = viewModel,
                        onBack = { overlay = null },
                        showMessage = showMessage,
                    )
                }
                else -> Spacer(Modifier)
            }
        }

        // Los snackbars de las pantallas superpuestas también deben verse.
        if (overlay != null) {
            SnackbarHost(
                snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
            )
        }
    }
}

@Composable
private fun OverlaySurface(content: @Composable () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { content() }
}

/** Barra de navegación estándar de Material 3 (3–5 destinos en pantallas compactas). */
@Composable
private fun AppNavigationBar(
    selected: Destination,
    onSelect: (Destination) -> Unit,
    badges: Map<Destination, Int>,
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Destination.entries.forEach { item ->
            val count = badges[item] ?: 0
            NavigationBarItem(
                selected = item == selected,
                onClick = { onSelect(item) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (count > 0) {
                                Badge(
                                    containerColor = if (item == Destination.ATTENDANCE) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                    contentColor = if (item == Destination.ATTENDANCE) {
                                        MaterialTheme.colorScheme.onError
                                    } else {
                                        MaterialTheme.colorScheme.onPrimary
                                    },
                                ) { Text(count.coerceAtMost(99).toString()) }
                            }
                        },
                    ) {
                        Icon(if (item == selected) item.icon else item.outlinedIcon, contentDescription = null)
                    }
                },
                label = { Text(item.label, maxLines = 1) },
            )
        }
    }
}
