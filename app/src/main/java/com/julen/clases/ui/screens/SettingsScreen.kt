package com.julen.clases.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.julen.clases.AppViewModel
import com.julen.clases.UiState
import com.julen.clases.data.AttendanceMath
import com.julen.clases.data.ThemeMode
import com.julen.clases.ui.ShowMessage
import com.julen.clases.ui.components.AppCard
import com.julen.clases.ui.components.DetailTopBar
import com.julen.clases.ui.components.IconBubble
import com.julen.clases.ui.components.PeriodPickerDialog
import com.julen.clases.ui.formatDateUtc
import com.julen.clases.ui.formatLocal
import com.julen.clases.ui.plural
import com.julen.clases.ui.theme.AppColors

private enum class ResetKind { ATTENDANCE, ALL }

@Composable
fun SettingsScreen(state: UiState, viewModel: AppViewModel, onBack: () -> Unit, showMessage: ShowMessage) {
    val context = LocalContext.current
    var limit by remember(state.absenceLimit) { mutableFloatStateOf(state.absenceLimit) }
    var name by rememberSaveable(state.userName) { mutableStateOf(state.userName) }
    var showPeriodPicker by remember { mutableStateOf(false) }
    var showHolidayDialog by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf<ResetKind?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            showMessage(if (viewModel.exportBackup(uri)) "Copia de seguridad guardada" else "No se pudo guardar la copia", null, null)
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            showMessage(if (viewModel.importBackup(uri)) "Datos restaurados" else "El archivo no es una copia válida", null, null)
        }
    }
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "—"
    }

    Column(Modifier.fillMaxSize()) {
        DetailTopBar(title = "Ajustes", onBack = onBack)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                SettingsCard(Icons.Filled.Person, "Perfil", MaterialTheme.colorScheme.primary) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(30) },
                        label = { Text("Tu nombre") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (name.trim() != state.userName) {
                                TextButton(onClick = {
                                    viewModel.setUserName(name)
                                    showMessage("Nombre guardado", null, null)
                                }) { Text("Guardar") }
                            }
                        },
                    )
                }
            }

            item {
                SettingsCard(Icons.Filled.Tune, "Límite de faltas", AppColors.Warning) {
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
                        "${AttendanceMath.TARDIES_PER_ABSENCE} retrasos cuentan como 1 falta. Aviso amarillo a partir de 2/3 del límite.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                SettingsCard(Icons.Filled.DateRange, "Periodo de evaluación", MaterialTheme.colorScheme.primary) {
                    val period = state.period
                    Text(
                        if (period != null) "${formatDateUtc(period.startUtc)} – ${formatDateUtc(period.endUtc)}" else "Sin configurar",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.size(10.dp))
                    OutlinedButton(onClick = { showPeriodPicker = true }, shape = RoundedCornerShape(14.dp)) {
                        Text(if (period == null) "Elegir fechas" else "Cambiar fechas")
                    }
                }
            }

            item {
                SettingsCard(Icons.Filled.BeachAccess, "Días no lectivos", AppColors.Safe) {
                    Text(
                        "Festivos, puentes y vacaciones. No cuentan como horas de clase al calcular las faltas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    state.period?.let { period ->
                        val lost = AttendanceMath.schoolDaysLost(period, state.holidays)
                        if (lost > 0) {
                            Text(
                                "${plural(lost, "día lectivo", "días lectivos")} descontados en esta evaluación",
                                style = MaterialTheme.typography.labelMedium,
                                color = AppColors.Safe,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                    Spacer(Modifier.size(6.dp))
                    state.holidays.forEach { holiday ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(holiday.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (holiday.startUtc == holiday.endUtc) {
                                        formatDateUtc(holiday.startUtc, "EEEE d MMM yyyy")
                                    } else {
                                        "${formatDateUtc(holiday.startUtc, "d MMM")} – ${formatDateUtc(holiday.endUtc, "d MMM yyyy")}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = {
                                viewModel.deleteHoliday(holiday.id)
                                showMessage("Eliminado: ${holiday.name}", "Deshacer") { viewModel.restoreHoliday(holiday) }
                            }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    TextButton(onClick = { showHolidayDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Añadir días no lectivos")
                    }
                }
            }

            item {
                SettingsCard(Icons.Filled.Palette, "Apariencia", Color(0xFFDB2777)) {
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
                SettingsCard(Icons.Filled.Download, "Copia de seguridad", Color(0xFF0891B2)) {
                    Text(
                        "Guarda todos tus datos en un archivo para pasarlos a otro móvil o recuperarlos más tarde.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.size(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                exportLauncher.launch("horario-clases-${formatLocal(System.currentTimeMillis(), "yyyy-MM-dd")}.json")
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Exportar")
                        }
                        OutlinedButton(
                            onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Importar")
                        }
                    }
                    Text(
                        "Importar sustituye todos los datos actuales.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            item {
                SettingsCard(Icons.Filled.DeleteForever, "Datos", AppColors.Danger) {
                    DangerAction(
                        icon = Icons.Filled.RestartAlt,
                        title = "Reiniciar asistencia",
                        description = "Pone a cero faltas, retrasos e historial. Mantiene el resto.",
                        onClick = { confirmReset = ResetKind.ATTENDANCE },
                    )
                    Spacer(Modifier.size(8.dp))
                    DangerAction(
                        icon = Icons.Filled.DeleteForever,
                        title = "Borrar todos los datos",
                        description = "Elimina todo lo guardado en el dispositivo.",
                        onClick = { confirmReset = ResetKind.ALL },
                    )
                }
            }

            item {
                SettingsCard(Icons.Filled.Info, "Acerca de", MaterialTheme.colorScheme.onSurfaceVariant) {
                    Text("Horario de Clases · versión $versionName", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Tus datos se guardan solo en este dispositivo. Tipografía Plus Jakarta Sans (SIL Open Font License).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item { Spacer(Modifier.navigationBarsPadding()) }
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

    if (showHolidayDialog) {
        HolidayDialog(
            onDismiss = { showHolidayDialog = false },
            onConfirm = { holidayName, start, end ->
                viewModel.addHoliday(holidayName, start, end)
                showHolidayDialog = false
                showMessage("Día no lectivo añadido", null, null)
            },
        )
    }

    confirmReset?.let { kind ->
        AlertDialog(
            onDismissRequest = { confirmReset = null },
            icon = { IconBubble(Icons.Filled.DeleteForever, AppColors.Danger) },
            title = { Text(if (kind == ResetKind.ALL) "¿Borrar todos los datos?" else "¿Reiniciar asistencia?") },
            text = {
                Text(
                    if (kind == ResetKind.ALL) {
                        "Se eliminarán faltas, notas, tareas, apuntes, festivos y ajustes. Exporta una copia antes si quieres conservarlos."
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
private fun SettingsCard(
    icon: ImageVector,
    title: String,
    accent: Color,
    content: @Composable () -> Unit,
) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
            IconBubble(icon, accent, size = 36.dp)
            Spacer(Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        content()
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
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HolidayDialog(onDismiss: () -> Unit, onConfirm: (name: String, startUtc: Long, endUtc: Long) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var start by rememberSaveable { mutableStateOf<Long?>(null) }
    var end by rememberSaveable { mutableStateOf<Long?>(null) }
    var showPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { IconBubble(Icons.Filled.BeachAccess, AppColors.Safe) },
        title = { Text("Días no lectivos") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    label = { Text("Nombre") },
                    placeholder = { Text("Puente de diciembre, Navidad…") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(onClick = { showPicker = true }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    val s = start
                    val e = end
                    Text(
                        when {
                            s == null || e == null -> "Elegir fechas"
                            s == e -> formatDateUtc(s, "d MMM yyyy")
                            else -> "${formatDateUtc(s, "d MMM")} – ${formatDateUtc(e, "d MMM yyyy")}"
                        }
                    )
                }
                Text(
                    "Para un solo día, toca la misma fecha dos veces.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            val s = start
            val e = end
            TextButton(
                enabled = name.isNotBlank() && s != null && e != null,
                onClick = { if (s != null && e != null) onConfirm(name, s, e) },
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )

    if (showPicker) {
        PeriodPickerDialog(
            initial = null,
            title = "Días no lectivos",
            onDismiss = { showPicker = false },
            onConfirm = { s, e ->
                start = s
                end = e
                showPicker = false
            },
        )
    }
}
