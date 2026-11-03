package com.julen.clases.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.julen.clases.AppViewModel
import com.julen.clases.UiState
import com.julen.clases.data.AttendanceMath
import com.julen.clases.data.ThemeMode
import com.julen.clases.ui.ShowMessage
import com.julen.clases.ui.components.AppCard
import com.julen.clases.ui.components.PeriodPickerDialog
import com.julen.clases.ui.components.ScreenHeader
import com.julen.clases.ui.components.SectionLabel
import com.julen.clases.ui.formatDateUtc
import com.julen.clases.ui.theme.AppColors

private enum class ResetKind { ATTENDANCE, ALL }

@Composable
fun SettingsScreen(state: UiState, viewModel: AppViewModel, showMessage: ShowMessage) {
    var limit by remember(state.absenceLimit) { mutableFloatStateOf(state.absenceLimit) }
    var showPeriodPicker by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf<ResetKind?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { ScreenHeader(title = "Ajustes", subtitle = "Personaliza la app") }

        item {
            AppCard {
                SectionLabel("Límite de faltas")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Porcentaje máximo de horas que puedes faltar por asignatura",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "${limit.toInt()}%",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Slider(
                    value = limit,
                    onValueChange = { limit = it },
                    onValueChangeFinished = { viewModel.setAbsenceLimit(limit) },
                    valueRange = 5f..30f,
                    steps = 24,
                )
                Text(
                    "${AttendanceMath.TARDIES_PER_ABSENCE} retrasos cuentan como 1 falta. " +
                        "Aviso amarillo a partir de 2/3 del límite.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            AppCard {
                SectionLabel("Periodo de evaluación")
                val period = state.period
                Text(
                    if (period != null) {
                        "${formatDateUtc(period.startUtc)} – ${formatDateUtc(period.endUtc)}"
                    } else {
                        "Sin configurar"
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.size(10.dp))
                OutlinedButton(onClick = { showPeriodPicker = true }, shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (period == null) "Elegir fechas" else "Cambiar fechas")
                }
            }
        }

        item {
            AppCard {
                SectionLabel("Apariencia")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = state.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            label = {
                                Text(
                                    when (mode) {
                                        ThemeMode.SYSTEM -> "Sistema"
                                        ThemeMode.LIGHT -> "Claro"
                                        ThemeMode.DARK -> "Oscuro"
                                    }
                                )
                            },
                        )
                    }
                }
            }
        }

        item {
            AppCard {
                SectionLabel("Datos")
                DangerAction(
                    icon = Icons.Filled.RestartAlt,
                    title = "Reiniciar asistencia",
                    description = "Pone a cero faltas, retrasos e historial. Mantiene tareas, apuntes y ajustes.",
                    onClick = { confirmReset = ResetKind.ATTENDANCE },
                )
                Spacer(Modifier.size(8.dp))
                DangerAction(
                    icon = Icons.Filled.DeleteForever,
                    title = "Borrar todos los datos",
                    description = "Elimina todo lo guardado en el dispositivo. No se puede deshacer.",
                    onClick = { confirmReset = ResetKind.ALL },
                )
            }
        }

        item {
            Text(
                "Horario de Clases · los datos se guardan solo en este dispositivo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }
    }

    if (showPeriodPicker) {
        PeriodPickerDialog(
            initial = state.period,
            onDismiss = { showPeriodPicker = false },
            onConfirm = { start, end ->
                viewModel.savePeriod(start, end)
                showPeriodPicker = false
                showMessage("Periodo de evaluación guardado", null, null)
            },
        )
    }

    confirmReset?.let { kind ->
        AlertDialog(
            onDismissRequest = { confirmReset = null },
            title = { Text(if (kind == ResetKind.ALL) "¿Borrar todos los datos?" else "¿Reiniciar asistencia?") },
            text = {
                Text(
                    if (kind == ResetKind.ALL) {
                        "Se eliminarán faltas, retrasos, historial, tareas, apuntes, fechas y ajustes. Esta acción no se puede deshacer."
                    } else {
                        "Se pondrán a cero las faltas y retrasos de todas las asignaturas y se borrará el historial."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (kind == ResetKind.ALL) viewModel.resetAll() else viewModel.resetAttendance()
                    confirmReset = null
                    showMessage(if (kind == ResetKind.ALL) "Datos borrados" else "Asistencia reiniciada", null, null)
                }) {
                    Text("Borrar", color = AppColors.Danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmReset = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun DangerAction(icon: ImageVector, title: String, description: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = AppColors.Danger.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = AppColors.Danger)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = AppColors.Danger)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
