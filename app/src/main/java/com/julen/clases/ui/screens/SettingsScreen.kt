package com.julen.clases.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.julen.clases.AppViewModel
import com.julen.clases.UiState
import com.julen.clases.data.AccentColor
import com.julen.clases.data.AttendanceMath
import com.julen.clases.data.ThemeMode
import com.julen.clases.ui.ShowMessage
import com.julen.clases.ui.components.AppCard
import com.julen.clases.ui.components.AppScreen
import com.julen.clases.ui.components.PeriodPickerDialog
import com.julen.clases.ui.components.SectionTitle
import com.julen.clases.ui.components.screenPadding
import com.julen.clases.ui.formatDateUtc
import com.julen.clases.ui.formatLocal
import com.julen.clases.ui.plural
import com.julen.clases.ui.theme.AppColors
import com.julen.clases.ui.theme.swatch

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
    // Qué aviso quería activarse cuando se pidió el permiso de notificaciones.
    var pendingReminder by remember { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            when (pendingReminder) {
                "class" -> viewModel.setClassReminder(true)
                "summary" -> viewModel.setDailySummary(true)
            }
        } else {
            showMessage("Sin permiso no se pueden mostrar avisos", null, null)
        }
        pendingReminder = null
    }
    fun enableReminder(kind: String, enabled: Boolean) {
        val needsPermission = enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            pendingReminder = kind
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (kind == "class") {
            viewModel.setClassReminder(enabled)
        } else {
            viewModel.setDailySummary(enabled)
        }
    }

    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "—"
    }

    AppScreen(title = "Ajustes", onBack = onBack, edgeToEdgeBottom = true) { inner ->
        LazyColumn(
            modifier = Modifier.imePadding(),
            contentPadding = screenPadding(inner, extraBottom = 32.dp),
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
                SettingsCard(Icons.Filled.Notifications, "Avisos", Color(0xFF7C3AED)) {
                    ToggleRow(
                        title = "Antes de cada clase",
                        subtitle = "Te avisa de qué toca y a qué hora",
                        checked = state.reminders.classReminder,
                        onCheckedChange = { enableReminder("class", it) },
                    )
                    if (state.reminders.classReminder) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) {
                            listOf(2, 5, 10, 15).forEach { minutes ->
                                FilterChip(
                                    selected = state.reminders.leadMinutes == minutes,
                                    onClick = { viewModel.setReminderLead(minutes) },
                                    label = { Text("$minutes min") },
                                )
                            }
                        }
                    }
                    ToggleRow(
                        title = "Resumen de la noche",
                        subtitle = "Exámenes, entregas y primera clase del día siguiente",
                        checked = state.reminders.dailySummary,
                        onCheckedChange = { enableReminder("summary", it) },
                    )
                    if (state.reminders.dailySummary) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                            listOf(18, 20, 21, 22).forEach { hour ->
                                FilterChip(
                                    selected = state.reminders.summaryHour == hour,
                                    onClick = { viewModel.setSummaryHour(hour) },
                                    label = { Text("$hour:00") },
                                )
                            }
                        }
                    }
                    Text(
                        "Con el ahorro de batería activo, Android puede retrasar los avisos unos minutos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
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
                    Text("Color de acento", style = MaterialTheme.typography.labelLarge)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
                    ) {
                        AccentColor.entries.forEach { accent ->
                            val selected = state.accent == accent
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(CircleShape)
                                    .background(accent.swatch().first())
                                    .border(if (selected) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    .clickable { viewModel.setAccent(accent) },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (selected) Icon(Icons.Filled.Check, contentDescription = accent.label, tint = Color.White)
                            }
                        }
                    }
                    Text(
                        state.accent.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    Text("Tema", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 6.dp))
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
            holidays = state.holidays,
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
            icon = { Icon(Icons.Outlined.DeleteForever, contentDescription = null, tint = AppColors.Danger) },
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
    // Título de sección y contenido en una superficie tonal; [icon] y [accent] se conservan
    // en la firma para identificar cada bloque en el código.
    SectionTitle(title)
    AppCard { content() }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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
        icon = { Icon(Icons.Outlined.BeachAccess, contentDescription = null) },
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
            showPresets = false,
            onDismiss = { showPicker = false },
            onConfirm = { s, e ->
                start = s
                end = e
                showPicker = false
            },
        )
    }
}
