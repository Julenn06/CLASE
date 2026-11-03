package com.julen.clases.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.julen.clases.AppViewModel
import com.julen.clases.UiState
import com.julen.clases.data.AttendanceMath
import com.julen.clases.data.Block
import com.julen.clases.data.CalendarMath
import com.julen.clases.data.NowStatus
import com.julen.clases.data.Subjects
import com.julen.clases.data.TaskKind
import com.julen.clases.data.Timetable
import com.julen.clases.data.formatMinutes
import com.julen.clases.ui.Destination
import com.julen.clases.ui.Navigator
import com.julen.clases.ui.ShowMessage
import com.julen.clases.ui.capitalized
import com.julen.clases.ui.components.AppScreen
import com.julen.clases.ui.components.Dot
import com.julen.clases.ui.components.GroupDivider
import com.julen.clases.ui.components.GroupItem
import com.julen.clases.ui.components.ListGroup
import com.julen.clases.ui.components.PeriodPickerDialog
import com.julen.clases.ui.components.ProgressTrack
import com.julen.clases.ui.components.RowIcon
import com.julen.clases.ui.components.SectionTitle
import com.julen.clases.ui.components.screenPadding
import com.julen.clases.ui.daysUntil
import com.julen.clases.ui.durationText
import com.julen.clases.ui.formatDateUtc
import com.julen.clases.ui.formatLocal
import com.julen.clases.ui.formatPercent
import com.julen.clases.ui.formatScore
import com.julen.clases.ui.greeting
import com.julen.clases.ui.plural
import com.julen.clases.ui.relativeDay
import com.julen.clases.ui.theme.AppColors
import com.julen.clases.ui.theme.color
import com.julen.clases.ui.theme.subjectColor

@Composable
fun HomeScreen(
    state: UiState,
    viewModel: AppViewModel,
    navigator: Navigator,
    showMessage: ShowMessage,
) {
    var showPeriodPicker by remember { mutableStateOf(false) }
    val title = greeting(state.nowMillis) + if (state.userName.isNotBlank()) ", ${state.userName}" else ""

    AppScreen(
        title = title,
        actions = {
            IconButton(onClick = navigator.openSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = "Ajustes")
            }
        },
    ) { inner ->
        LazyColumn(contentPadding = screenPadding(inner)) {
            item {
                Text(
                    formatLocal(state.nowMillis, "EEEE, d 'de' MMMM").capitalized(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
                )
            }

            item {
                NowSection(
                    state = state,
                    onAbsence = { registerAbsence(viewModel, it, showMessage) },
                    onTardy = { registerTardy(viewModel, it, showMessage) },
                    onOpenSubject = navigator.openSubject,
                )
            }

            item { SectionTitle("Esta semana") }
            item { WeekStrip(state, onClick = { navigator.openTab(Destination.AGENDA) }) }

            val day = state.today
            if (day != null && Timetable.blocksFor(day).isNotEmpty()) {
                item {
                    SectionTitle(
                        "Hoy, ${Timetable.dayNames[day].lowercase()}",
                        actionLabel = "Semana",
                        onAction = { navigator.openTab(Destination.SCHEDULE) },
                    )
                }
                item {
                    ListGroup {
                        val blocks = Timetable.blocksFor(day)
                        blocks.forEachIndexed { index, block ->
                            if (index > 0) GroupDivider(inset = 72.dp)
                            TimelineRow(block, state.minuteOfDay, onOpenSubject = navigator.openSubject)
                        }
                    }
                }
            }

            item { SectionTitle("Resumen") }
            item { SummaryGroup(state, navigator, onConfigurePeriod = { showPeriodPicker = true }) }

            item {
                SectionTitle(
                    "Próximas tareas",
                    actionLabel = if (state.pendingTasks.isEmpty()) "Añadir" else "Ver todas",
                    onAction = { navigator.openTab(Destination.AGENDA) },
                )
            }
            item { UpcomingTasks(state, onToggle = { viewModel.toggleTask(it) }) }
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
}

// ---------------------------------------------------------------------------------------------
// Ahora: tipografía sobre el fondo, sin contenedor
// ---------------------------------------------------------------------------------------------

@Composable
private fun NowSection(
    state: UiState,
    onAbsence: (String) -> Unit,
    onTardy: (String) -> Unit,
    onOpenSubject: (String) -> Unit,
) {
    val status = state.nowStatus
    val holiday = state.isHolidayToday
    val haptic = LocalHapticFeedback.current
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
        when {
            holiday != null -> {
                NowLabel("Día no lectivo", MaterialTheme.colorScheme.primary)
                NowTitle(holiday.name, MaterialTheme.colorScheme.onBackground)
                NowLine("Hoy no cuenta para las faltas.", muted)
            }

            status is NowStatus.InClass -> {
                val code = status.block.subject.orEmpty()
                val color = subjectColor(code)
                NowLabel("Ahora · ${status.block.label}", color)
                NowTitle(code, color)
                Text(
                    Subjects.nameOf(code),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(16.dp))
                ProgressTrack(fraction = status.progress, color = color, height = 4.dp)
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        "Quedan ${durationText(status.minutesLeft)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                    Text(nextText(status.next), style = MaterialTheme.typography.bodyMedium, color = muted)
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onTardy(code)
                    }) { Text("Retraso") }
                    FilledTonalButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAbsence(code)
                    }) { Text("Falta") }
                    TextButton(onClick = { onOpenSubject(code) }) { Text("Ver asignatura") }
                }
            }

            status is NowStatus.OnBreak -> {
                NowLabel(if (status.block.isFree) "Hora libre" else "Recreo", MaterialTheme.colorScheme.primary)
                NowTitle("${status.minutesLeft} min", MaterialTheme.colorScheme.onBackground)
                NowLine(nextText(status.next), muted)
            }

            status is NowStatus.BeforeClasses -> {
                val code = status.first.subject.orEmpty()
                NowLabel("Hoy empiezas con", MaterialTheme.colorScheme.primary)
                NowTitle(code, subjectColor(code))
                NowLine(
                    "${Subjects.nameOf(code)} · a las ${formatMinutes(status.first.start)}, dentro de ${durationText(status.minutesUntil)}",
                    muted,
                )
            }

            status is NowStatus.NoClasses -> {
                val weekend = status.reason == NowStatus.Reason.WEEKEND
                NowLabel(if (weekend) "Fin de semana" else "Clases terminadas", MaterialTheme.colorScheme.primary)
                val first = status.firstBlock
                if (first != null && first.subject != null) {
                    val whenText = if (weekend || status.nextDay == 0) "El lunes" else "Mañana"
                    NowTitle(first.subject, subjectColor(first.subject))
                    NowLine("$whenText a las ${formatMinutes(first.start)} · ${Subjects.nameOf(first.subject)}", muted)
                } else {
                    NowTitle("Sin clases", MaterialTheme.colorScheme.onBackground)
                }
            }
        }
    }
}

@Composable
private fun NowLabel(text: String, color: Color) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = color)
}

@Composable
private fun NowTitle(text: String, color: Color) {
    Text(
        text,
        style = MaterialTheme.typography.displayMedium,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun NowLine(text: String, color: Color) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = color, modifier = Modifier.padding(top = 4.dp))
}

private fun nextText(next: Block?): String = when {
    next == null -> "Última clase del día"
    next.isBreak -> "Después, recreo"
    next.isFree -> "Después, hora libre"
    else -> "Después ${next.subject} · ${formatMinutes(next.start)}"
}

// ---------------------------------------------------------------------------------------------
// Semana
// ---------------------------------------------------------------------------------------------

/** La semana lectiva de un vistazo: fecha, horas de clase, festivos y entregas de cada día. */
@Composable
private fun WeekStrip(state: UiState, onClick: () -> Unit) {
    val today = state.todayUtc
    val weekday = AttendanceMath.weekdayOfUtc(today)
    val monday = today - weekday * AttendanceMath.DAY_MS
    // En fin de semana se muestra la semana siguiente.
    val start = if (weekday >= 5) monday + 7 * AttendanceMath.DAY_MS else monday
    val scheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (offset in 0 until 5) {
            val day = start + offset * AttendanceMath.DAY_MS
            val isToday = day == today
            val holiday = state.holidays.any { day in it }
            val hours = if (holiday) 0 else Timetable.blocksFor(offset).filter { it.subject != null && !it.isBreak }.sumOf { it.durationMinutes } / 60
            val due = state.tasks.filter { !it.done && it.dueUtc == day }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (day < today) 0.45f else 1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isToday) scheme.primaryContainer else Color.Transparent)
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val fg = if (isToday) scheme.onPrimaryContainer else scheme.onBackground
                Text(Timetable.dayShortNames[offset], style = MaterialTheme.typography.labelMedium, color = fg.copy(alpha = 0.7f))
                Text(CalendarMath.dayOf(day).toString(), style = MaterialTheme.typography.titleLarge, color = fg)
                Text(
                    if (holiday) "Festivo" else "$hours h",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (holiday) AppColors.Safe else fg.copy(alpha = 0.7f),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .height(6.dp),
                ) {
                    due.take(3).forEach { task -> Dot(task.kind.color(), size = 6.dp) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Resumen
// ---------------------------------------------------------------------------------------------

@Composable
private fun SummaryGroup(state: UiState, navigator: Navigator, onConfigurePeriod: () -> Unit) {
    val chevron: @Composable () -> Unit = {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    ListGroup {
        val period = state.period
        val progress = state.periodProgress
        if (period == null || progress == null) {
            GroupItem(
                headline = "Configura tu evaluación",
                supporting = "Elige las fechas para calcular cuántas faltas te quedan",
                leading = { RowIcon(Icons.Outlined.DateRange, MaterialTheme.colorScheme.primary) },
                trailing = chevron,
                onClick = onConfigurePeriod,
            )
        } else {
            GroupItem(
                headline = "Quedan ${plural(progress.remainingDays, "día", "días")} de evaluación",
                supporting = "${formatDateUtc(period.startUtc, "d MMM")} – ${formatDateUtc(period.endUtc, "d MMM")} · ${(progress.fraction * 100).toInt()} % completado",
                leading = { RowIcon(Icons.Outlined.DateRange) },
                trailing = chevron,
                onClick = { navigator.openTab(Destination.ATTENDANCE) },
            )
            GroupDivider(inset = 56.dp)
            val attendance = state.overallAttendance
            GroupItem(
                headline = "Asistencia ${attendance?.let { formatPercent(it) } ?: "—"}",
                supporting = buildString {
                    append(plural(state.totalAbsences, "falta"))
                    append(" · ").append(plural(state.totalTardies, "retraso"))
                    state.streak?.let { append(" · ").append(plural(it, "día", "días")).append(" sin faltar") }
                },
                leading = { RowIcon(Icons.Outlined.FactCheck) },
                trailing = chevron,
                onClick = { navigator.openTab(Destination.ATTENDANCE) },
            )
        }

        state.subjectsAtRisk.sortedByDescending { it.percentage }.forEach { subject ->
            GroupDivider(inset = 56.dp)
            GroupItem(
                headline = "${subject.code} en riesgo",
                supporting = if (subject.remainingAbsences >= 0) {
                    "${formatPercent(subject.percentage)} de faltas · te quedan ${subject.remainingAbsences}"
                } else {
                    "${formatPercent(subject.percentage)} de faltas · límite superado"
                },
                supportingColor = subject.risk.color(),
                leading = { RowIcon(Icons.Outlined.ErrorOutline, subject.risk.color()) },
                trailing = chevron,
                onClick = { navigator.openSubject(subject.code) },
            )
        }

        GroupDivider(inset = 56.dp)
        val average = state.overallAverage
        val passed = state.gradeSummaries.count { it.passed == true }
        val failed = state.gradeSummaries.count { it.passed == false }
        GroupItem(
            headline = if (average != null) "Media ${formatScore((average * 10).toInt() / 10f)}" else "Sin notas todavía",
            supporting = if (average != null) "$passed aprobadas · $failed por debajo de 5" else "Añade calificaciones para ver tu media",
            leading = { RowIcon(Icons.Outlined.School) },
            trailing = chevron,
            onClick = { navigator.openTab(Destination.GRADES) },
        )

        state.nextExam?.let { exam ->
            val due = exam.dueUtc ?: return@let
            val days = daysUntil(due, state.todayUtc)
            GroupDivider(inset = 56.dp)
            GroupItem(
                headline = "${exam.kind.label} de ${exam.subject} ${if (days == 0) "hoy" else if (days == 1) "mañana" else "en $days días"}",
                supporting = "${exam.title} · ${formatDateUtc(due, "EEEE d MMM").capitalized()}",
                leading = { RowIcon(Icons.Outlined.Quiz, exam.kind.color()) },
                trailing = chevron,
                onClick = { navigator.openTab(Destination.AGENDA) },
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Horario del día
// ---------------------------------------------------------------------------------------------

/** Fila del horario: hora, color de la asignatura y nombre; la clase actual va resaltada. */
@Composable
internal fun TimelineRow(block: Block, minuteOfDay: Int?, onOpenSubject: ((String) -> Unit)? = null) {
    val isNow = minuteOfDay != null && minuteOfDay >= block.start && minuteOfDay < block.end
    val isPast = minuteOfDay != null && minuteOfDay >= block.end
    val code = block.subject
    val color = subjectColor(code)
    val quiet = block.isBreak || block.isFree

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isPast) 0.45f else 1f)
            .background(if (isNow) color.copy(alpha = 0.10f) else Color.Transparent)
            .then(if (code != null && onOpenSubject != null) Modifier.clickable { onOpenSubject(code) } else Modifier)
            .padding(horizontal = 16.dp, vertical = if (quiet) 10.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            formatMinutes(block.start),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(44.dp),
        )
        Dot(if (quiet) MaterialTheme.colorScheme.outlineVariant else color, size = 10.dp)
        Spacer(Modifier.width(14.dp))
        if (quiet) {
            Text(
                "${if (block.isBreak) "Recreo" else "Hora libre"} · ${durationText(block.durationMinutes)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        } else {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    Subjects.nameOf(code.orEmpty()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "$code · ${block.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isNow) {
                Text("Ahora", style = MaterialTheme.typography.labelLarge, color = color, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Próximas tareas
// ---------------------------------------------------------------------------------------------

@Composable
private fun UpcomingTasks(state: UiState, onToggle: (Long) -> Unit) {
    val upcoming = state.pendingTasks.sortedWith(taskOrder).take(4)
    ListGroup {
        if (upcoming.isEmpty()) {
            GroupItem(
                headline = "Nada pendiente",
                supporting = "Las tareas, exámenes y entregas aparecerán aquí",
                leading = { RowIcon(Icons.Outlined.TaskAlt, AppColors.Safe) },
            )
        }
        upcoming.forEachIndexed { index, task ->
            if (index > 0) GroupDivider(inset = 56.dp)
            val overdue = task.dueUtc != null && task.dueUtc < state.todayUtc
            GroupItem(
                headline = task.title,
                supporting = buildString {
                    append(task.subject)
                    if (task.kind != TaskKind.TASK) append(" · ").append(task.kind.label)
                    task.dueUtc?.let { append(" · ").append(relativeDay(it, state.todayUtc)) }
                },
                supportingColor = if (overdue) AppColors.Danger else null,
                leading = {
                    Checkbox(
                        checked = task.done,
                        onCheckedChange = { onToggle(task.id) },
                        modifier = Modifier.size(24.dp),
                    )
                },
            )
        }
    }
}
