package com.julen.clases.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.julen.clases.AppViewModel
import com.julen.clases.UiState
import com.julen.clases.data.Block
import com.julen.clases.data.NowStatus
import com.julen.clases.data.Subjects
import com.julen.clases.data.Task
import com.julen.clases.data.Timetable
import com.julen.clases.data.formatMinutes
import com.julen.clases.ui.Destination
import com.julen.clases.ui.ShowMessage
import com.julen.clases.ui.capitalized
import com.julen.clases.ui.components.AppCard
import com.julen.clases.ui.components.Dot
import com.julen.clases.ui.components.PeriodPickerDialog
import com.julen.clases.ui.components.Pill
import com.julen.clases.ui.components.ProgressTrack
import com.julen.clases.ui.components.SectionLabel
import com.julen.clases.ui.components.SubjectBadge
import com.julen.clases.ui.durationText
import com.julen.clases.ui.formatDateUtc
import com.julen.clases.ui.formatLocal
import com.julen.clases.ui.greeting
import com.julen.clases.ui.plural
import com.julen.clases.ui.relativeDay
import com.julen.clases.ui.theme.AppColors
import com.julen.clases.ui.theme.color
import com.julen.clases.ui.theme.subjectColor
import java.util.Calendar

@Composable
fun HomeScreen(
    state: UiState,
    viewModel: AppViewModel,
    onNavigate: (Destination) -> Unit,
    showMessage: ShowMessage,
) {
    var showPeriodPicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(top = 16.dp, bottom = 2.dp)) {
                Text(
                    text = formatLocal(state.nowMillis, "EEEE, d 'de' MMMM").capitalized(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = greeting(state.nowMillis),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }

        item {
            NowCard(
                status = state.nowStatus,
                today = state.today,
                onAbsence = { registerAbsence(viewModel, it, showMessage) },
                onTardy = { registerTardy(viewModel, it, showMessage) },
            )
        }

        if (state.today != null) {
            item { TodayTimeline(state = state, onOpenSchedule = { onNavigate(Destination.SCHEDULE) }) }
        }

        item {
            PeriodSummary(
                state = state,
                onConfigure = { showPeriodPicker = true },
                onOpenAttendance = { onNavigate(Destination.ATTENDANCE) },
            )
        }

        item {
            UpcomingTasks(
                state = state,
                onToggle = { viewModel.toggleTask(it.id) },
                onOpenTasks = { onNavigate(Destination.TASKS) },
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
            },
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Tarjeta "Ahora"
// ---------------------------------------------------------------------------------------------

@Composable
private fun NowCard(
    status: NowStatus,
    today: Int?,
    onAbsence: (String) -> Unit,
    onTardy: (String) -> Unit,
) {
    val white = Color.White
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(AppColors.HeroGradient))
            .padding(22.dp),
    ) {
        when (status) {
            is NowStatus.InClass -> {
                val code = status.block.subject.orEmpty()
                HeroLabel("Ahora en clase")
                HeroSubject(code)
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(status.block.label, color = white.copy(alpha = 0.75f), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Filled.Timer, null, tint = white, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "quedan ${durationText(status.minutesLeft)}",
                        color = white,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.height(8.dp))
                ProgressTrack(
                    fraction = status.progress,
                    color = white,
                    trackColor = white.copy(alpha = 0.18f),
                )
                Spacer(Modifier.height(12.dp))
                Text(nextText(status.next), color = white.copy(alpha = 0.75f), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HeroButton("Retraso", Icons.Filled.Timer, Modifier.weight(1f)) { onTardy(code) }
                    HeroButton("Falta", Icons.Filled.Add, Modifier.weight(1f)) { onAbsence(code) }
                }
            }

            is NowStatus.OnBreak -> {
                HeroLabel("Recreo")
                Text(
                    "Vuelves en ${durationText(status.minutesLeft)}",
                    color = white,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Spacer(Modifier.height(10.dp))
                Text(nextText(status.next), color = white.copy(alpha = 0.75f), style = MaterialTheme.typography.bodyMedium)
            }

            is NowStatus.BeforeClasses -> {
                HeroLabel("Hoy empiezas con")
                HeroSubject(status.first.subject.orEmpty())
                Spacer(Modifier.height(12.dp))
                Text(
                    "A las ${formatMinutes(status.first.start)} · en ${durationText(status.minutesUntil)}",
                    color = white.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            is NowStatus.NoClasses -> {
                val weekend = status.reason == NowStatus.Reason.WEEKEND
                HeroLabel(if (weekend) "Fin de semana" else "Clases terminadas")
                Text(
                    if (weekend) "Disfruta del descanso" else "¡Buen trabajo hoy!",
                    color = white,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
                val first = status.firstBlock
                if (first != null && first.subject != null) {
                    val whenText = when {
                        weekend || (today == 4 && status.nextDay == 0) -> "El lunes"
                        else -> "Mañana"
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "$whenText empiezas con ${first.subject} a las ${formatMinutes(first.start)}",
                        color = white.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = Color.White.copy(alpha = 0.65f),
        style = MaterialTheme.typography.labelSmall,
    )
}

@Composable
private fun HeroSubject(code: String) {
    Text(
        text = code,
        color = Color.White,
        style = MaterialTheme.typography.displaySmall,
        modifier = Modifier.padding(top = 2.dp),
    )
    Text(
        text = Subjects.nameOf(code),
        color = Color.White.copy(alpha = 0.85f),
        style = MaterialTheme.typography.titleMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun HeroButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.16f),
            contentColor = Color.White,
        ),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, fontWeight = FontWeight.Bold)
    }
}

private fun nextText(next: Block?): String = when {
    next == null -> "Es la última clase del día"
    next.isBreak -> "Después: recreo a las ${formatMinutes(next.start)}"
    else -> "Después: ${next.subject} a las ${formatMinutes(next.start)}"
}

// ---------------------------------------------------------------------------------------------
// Horario de hoy
// ---------------------------------------------------------------------------------------------

@Composable
private fun TodayTimeline(state: UiState, onOpenSchedule: () -> Unit) {
    val day = state.today ?: return
    val minute = remember(state.nowMillis) {
        Calendar.getInstance().apply { timeInMillis = state.nowMillis }.let {
            it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE)
        }
    }

    AppCard {
        SectionLabel("Hoy · ${Timetable.dayNames[day]}") {
            TextButton(onClick = onOpenSchedule) { Text("Semana") }
        }
        Timetable.blocksFor(day).forEach { block ->
            TimelineRow(block = block, minuteOfDay = minute)
        }
    }
}

@Composable
internal fun TimelineRow(block: Block, minuteOfDay: Int?) {
    val isNow = minuteOfDay != null && minuteOfDay >= block.start && minuteOfDay < block.end
    val isPast = minuteOfDay != null && minuteOfDay >= block.end
    val color = subjectColor(block.subject)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isPast) 0.45f else 1f)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isNow) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = if (block.isBreak) 6.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatMinutes(block.start),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(48.dp),
        )
        if (block.isBreak) {
            Text(
                "Recreo · ${durationText(block.durationMinutes)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        } else {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    block.subject.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "${Subjects.nameOf(block.subject.orEmpty())} · ${durationText(block.durationMinutes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (isNow) {
            Spacer(Modifier.width(8.dp))
            Pill("Ahora", MaterialTheme.colorScheme.primary)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Resumen de la evaluación
// ---------------------------------------------------------------------------------------------

@Composable
private fun PeriodSummary(state: UiState, onConfigure: () -> Unit, onOpenAttendance: () -> Unit) {
    val period = state.period
    val progress = state.periodProgress
    if (period == null || progress == null) {
        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.DateRange, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text("Configura tu evaluación", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "Elige las fechas de inicio y fin para calcular el porcentaje de faltas y cuántas te quedan en cada asignatura.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
            )
            Button(onClick = onConfigure, shape = RoundedCornerShape(14.dp)) { Text("Elegir fechas") }
        }
        return
    }

    AppCard(onClick = onOpenAttendance) {
        SectionLabel("Evaluación") {
            Text(
                "${formatDateUtc(period.startUtc, "d MMM")} – ${formatDateUtc(period.endUtc, "d MMM")}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                progress.remainingDays.toString(),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (progress.remainingDays == 1) "día restante" else "días restantes",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${(progress.fraction * 100).toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        ProgressTrack(fraction = progress.fraction, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Faltas", state.totalAbsences.toString(), Modifier.weight(1f))
            StatTile("Retrasos", state.totalTardies.toString(), Modifier.weight(1f))
            StatTile(
                "En riesgo",
                state.subjectsAtRisk.size.toString(),
                Modifier.weight(1f),
                valueColor = if (state.subjectsAtRisk.isEmpty()) AppColors.Safe else AppColors.Danger,
            )
        }
        state.subjectsAtRisk.takeIf { it.isNotEmpty() }?.let { risky ->
            Spacer(Modifier.height(14.dp))
            risky.sortedByDescending { it.percentage }.forEach { subject ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Warning, null, tint = subject.risk.color(), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        subject.code,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.width(56.dp),
                    )
                    Text(
                        if (subject.remainingAbsences >= 0) {
                            "${"%.1f".format(subject.percentage)}% · te quedan ${subject.remainingAbsences}"
                        } else {
                            "${"%.1f".format(subject.percentage)}% · límite superado"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = subject.risk.color(),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.headlineSmall, color = valueColor, fontWeight = FontWeight.Black)
    }
}

// ---------------------------------------------------------------------------------------------
// Próximas tareas
// ---------------------------------------------------------------------------------------------

@Composable
private fun UpcomingTasks(state: UiState, onToggle: (Task) -> Unit, onOpenTasks: () -> Unit) {
    val upcoming = state.pendingTasks.sortedWith(taskOrder).take(3)
    AppCard {
        SectionLabel("Próximas tareas") {
            TextButton(onClick = onOpenTasks) { Text(if (upcoming.isEmpty()) "Añadir" else "Ver todas") }
        }
        if (upcoming.isEmpty()) {
            Text(
                "No tienes tareas pendientes. ¡Bien hecho!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        } else {
            upcoming.forEach { task ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = task.done, onCheckedChange = { onToggle(task) })
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            task.title,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Dot(subjectColor(task.subject), size = 6.dp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                task.subject,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            task.dueUtc?.let { due ->
                                val overdue = due < state.todayUtc
                                Text(
                                    " · ${relativeDay(due, state.todayUtc)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (overdue) AppColors.Danger else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (overdue) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                }
            }
            val more = state.pendingTasks.size - upcoming.size
            if (more > 0) {
                Text(
                    "y ${plural(more, "tarea más", "tareas más")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp, top = 4.dp),
                    fontSize = 12.sp,
                )
            }
        }
    }
}

/** Pendientes primero, después por fecha límite (sin fecha al final) y por antigüedad. */
internal val taskOrder: Comparator<Task> =
    compareBy<Task> { it.done }
        .thenBy { it.dueUtc ?: Long.MAX_VALUE }
        .thenBy { it.id }
