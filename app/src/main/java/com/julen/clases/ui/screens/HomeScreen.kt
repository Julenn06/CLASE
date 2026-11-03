package com.julen.clases.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.julen.clases.AppViewModel
import com.julen.clases.UiState
import com.julen.clases.data.Block
import com.julen.clases.data.NowStatus
import com.julen.clases.data.Subjects
import com.julen.clases.data.Task
import com.julen.clases.data.TaskKind
import com.julen.clases.data.Timetable
import com.julen.clases.data.formatMinutes
import com.julen.clases.ui.Destination
import com.julen.clases.ui.Navigator
import com.julen.clases.ui.ShowMessage
import com.julen.clases.ui.capitalized
import com.julen.clases.ui.components.AppCard
import com.julen.clases.ui.components.Dot
import com.julen.clases.ui.components.GradientCard
import com.julen.clases.ui.components.IconBubble
import com.julen.clases.ui.components.PeriodPickerDialog
import com.julen.clases.ui.components.Pill
import com.julen.clases.ui.components.ProgressTrack
import com.julen.clases.ui.components.RingProgress
import com.julen.clases.ui.components.SectionHeader
import com.julen.clases.ui.components.StatTile
import com.julen.clases.ui.components.SubjectBadge
import com.julen.clases.ui.daysUntil
import com.julen.clases.ui.durationText
import com.julen.clases.ui.formatDateUtc
import com.julen.clases.ui.formatLocal
import com.julen.clases.ui.formatPercent
import com.julen.clases.ui.formatScore
import com.julen.clases.ui.greeting
import com.julen.clases.ui.initialOf
import com.julen.clases.ui.plural
import com.julen.clases.ui.relativeDay
import com.julen.clases.ui.theme.AppColors
import com.julen.clases.ui.theme.color
import com.julen.clases.ui.theme.gradeColor
import com.julen.clases.ui.theme.subjectColor

@Composable
fun HomeScreen(
    state: UiState,
    viewModel: AppViewModel,
    navigator: Navigator,
    showMessage: ShowMessage,
) {
    var showPeriodPicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { HomeHeader(state, onSettings = navigator.openSettings) }

        item {
            NowCard(
                state = state,
                onAbsence = { registerAbsence(viewModel, it, showMessage) },
                onTardy = { registerTardy(viewModel, it, showMessage) },
                onOpenSubject = navigator.openSubject,
            )
        }

        state.nextExam?.let { exam ->
            item { NextExamCard(exam, state.todayUtc, onClick = { navigator.openTab(Destination.AGENDA) }) }
        }

        item { StatsRow(state, navigator) }

        if (state.period == null) {
            item { ConfigurePeriodCard(onConfigure = { showPeriodPicker = true }) }
        } else {
            item { PeriodCard(state, onClick = { navigator.openTab(Destination.ATTENDANCE) }) }
        }

        if (state.subjectsAtRisk.isNotEmpty()) {
            item { RiskCard(state, onOpenSubject = navigator.openSubject) }
        }

        val day = state.today
        if (day != null) {
            item {
                SectionHeader(
                    "Hoy · ${Timetable.dayNames[day]}",
                    actionLabel = "Semana",
                    onAction = { navigator.openTab(Destination.SCHEDULE) },
                )
            }
            item {
                AppCard(contentPadding = 14.dp) {
                    Timetable.blocksFor(day).forEach { block ->
                        TimelineRow(block, state.minuteOfDay, onOpenSubject = navigator.openSubject)
                    }
                }
            }
        }

        item {
            SectionHeader(
                "Próximas tareas",
                actionLabel = if (state.pendingTasks.isEmpty()) "Añadir" else "Ver todas",
                onAction = { navigator.openTab(Destination.AGENDA) },
            )
        }
        item { UpcomingTasks(state, onToggle = { viewModel.toggleTask(it.id) }) }
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
}

// ---------------------------------------------------------------------------------------------

@Composable
private fun HomeHeader(state: UiState, onSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(AppColors.HeroGradient)),
            contentAlignment = Alignment.Center,
        ) {
            Text(initialOf(state.userName), color = Color.White, style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                formatLocal(state.nowMillis, "EEEE, d 'de' MMMM").capitalized(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                greeting(state.nowMillis) + if (state.userName.isNotBlank()) ", ${state.userName}" else "",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        FilledTonalIconButton(onClick = onSettings) {
            Icon(Icons.Filled.Settings, contentDescription = "Ajustes")
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Tarjeta "Ahora"
// ---------------------------------------------------------------------------------------------

@Composable
private fun NowCard(
    state: UiState,
    onAbsence: (String) -> Unit,
    onTardy: (String) -> Unit,
    onOpenSubject: (String) -> Unit,
) {
    val white = Color.White
    val holiday = state.isHolidayToday
    val status = state.nowStatus

    GradientCard(colors = AppColors.HeroGradient) {
        when {
            holiday != null -> {
                HeroLabel("Día no lectivo", Icons.Filled.BeachAccess)
                HeroTitle(holiday.name)
                HeroSubtitle("Hoy no cuenta para las faltas. ¡Aprovecha!")
            }

            status is NowStatus.InClass -> {
                val code = status.block.subject.orEmpty()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        HeroLabel("Ahora en clase", Icons.Filled.Timer)
                        HeroTitle(code)
                        HeroSubtitle(Subjects.nameOf(code))
                    }
                    RingProgress(
                        progress = status.progress,
                        color = white,
                        trackColor = white.copy(alpha = 0.18f),
                        size = 86.dp,
                        strokeWidth = 9.dp,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(status.minutesLeft.toString(), color = white, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                            Text("min", color = white.copy(alpha = 0.75f), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                HeroInfoRow(status.block.label, nextText(status.next))
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HeroButton("Retraso", Icons.Filled.Timer, Modifier.weight(1f)) { onTardy(code) }
                    HeroButton("Falta", Icons.Filled.Add, Modifier.weight(1f)) { onAbsence(code) }
                    HeroButton("Ficha", Icons.Filled.ChevronRight, Modifier.weight(1f)) { onOpenSubject(code) }
                }
            }

            status is NowStatus.OnBreak -> {
                HeroLabel("Recreo", Icons.Filled.Coffee)
                HeroTitle("${status.minutesLeft} min")
                HeroSubtitle("de descanso")
                Spacer(Modifier.height(14.dp))
                HeroInfoRow(status.block.label, nextText(status.next))
            }

            status is NowStatus.BeforeClasses -> {
                val code = status.first.subject.orEmpty()
                HeroLabel("Hoy empiezas con", Icons.Filled.EventAvailable)
                HeroTitle(code)
                HeroSubtitle(Subjects.nameOf(code))
                Spacer(Modifier.height(14.dp))
                HeroInfoRow("A las ${formatMinutes(status.first.start)}", "Faltan ${durationText(status.minutesUntil)}")
            }

            status is NowStatus.NoClasses -> {
                val weekend = status.reason == NowStatus.Reason.WEEKEND
                HeroLabel(if (weekend) "Fin de semana" else "Clases terminadas", Icons.Filled.CheckCircle)
                HeroTitle(if (weekend) "A descansar" else "¡Buen trabajo!")
                val first = status.firstBlock
                if (first != null && first.subject != null) {
                    val whenText = if (weekend || status.nextDay == 0) "El lunes" else "Mañana"
                    HeroSubtitle("$whenText empiezas con ${first.subject} a las ${formatMinutes(first.start)}")
                }
            }
        }
    }
}

@Composable
private fun HeroLabel(text: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(text.uppercase(), color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun HeroTitle(text: String) {
    Text(
        text,
        color = Color.White,
        style = MaterialTheme.typography.displaySmall,
        modifier = Modifier.padding(top = 6.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun HeroSubtitle(text: String) {
    Text(
        text,
        color = Color.White.copy(alpha = 0.85f),
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun HeroInfoRow(left: String, right: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(left, color = Color.White, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.width(10.dp))
        Text(
            right,
            color = Color.White.copy(alpha = 0.8f),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HeroButton(text: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(15.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.18f), contentColor = Color.White),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

private fun nextText(next: Block?): String = when {
    next == null -> "Última clase del día"
    next.isBreak -> "Después: recreo"
    else -> "Después: ${next.subject} a las ${formatMinutes(next.start)}"
}

// ---------------------------------------------------------------------------------------------
// Tarjetas de resumen
// ---------------------------------------------------------------------------------------------

@Composable
private fun NextExamCard(exam: Task, todayUtc: Long, onClick: () -> Unit) {
    val due = exam.dueUtc ?: return
    val days = daysUntil(due, todayUtc)
    val accent = exam.kind.color()
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(accent.copy(alpha = 0.14f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (days == 0) "HOY" else days.toString(),
                    color = accent,
                    style = MaterialTheme.typography.headlineMedium,
                )
                if (days > 0) {
                    Text(if (days == 1) "día" else "días", color = accent, style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "PRÓXIMO ${exam.kind.label.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                )
                Text(
                    exam.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Dot(subjectColor(exam.subject), size = 7.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${exam.subject} · ${formatDateUtc(due, "EEEE d MMM").capitalized()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsRow(state: UiState, navigator: Navigator) {
    val attendance = state.overallAttendance
    val average = state.overallAverage
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatTile(
            label = "Asistencia",
            value = attendance?.let { formatPercent(it) } ?: "—",
            icon = Icons.Filled.TaskAlt,
            accent = AppColors.Safe,
            caption = plural(state.totalAbsences, "falta"),
            modifier = Modifier.weight(1f),
            onClick = { navigator.openTab(Destination.ATTENDANCE) },
        )
        StatTile(
            label = "Media",
            value = average?.let { formatScore((it * 10).toInt() / 10f) } ?: "—",
            icon = Icons.Filled.Star,
            accent = gradeColor(average),
            caption = if (average == null) "Sin notas" else if (average >= 5f) "Aprobando" else "Por debajo de 5",
            modifier = Modifier.weight(1f),
            onClick = { navigator.openTab(Destination.GRADES) },
        )
        StatTile(
            label = "Pendientes",
            value = state.pendingTasks.size.toString(),
            icon = Icons.Filled.DateRange,
            accent = MaterialTheme.colorScheme.primary,
            caption = state.pendingTasks.count { it.kind == TaskKind.EXAM }.let { if (it > 0) plural(it, "examen", "exámenes") else "Sin exámenes" },
            modifier = Modifier.weight(1f),
            onClick = { navigator.openTab(Destination.AGENDA) },
        )
    }
}

@Composable
private fun ConfigurePeriodCard(onConfigure: () -> Unit) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBubble(Icons.Filled.DateRange, MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text("Configura tu evaluación", style = MaterialTheme.typography.titleMedium)
        }
        Text(
            "Elige las fechas de inicio y fin para calcular el porcentaje de faltas y cuántas te quedan en cada asignatura.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp, bottom = 14.dp),
        )
        Button(onClick = onConfigure, shape = RoundedCornerShape(14.dp)) { Text("Elegir fechas") }
    }
}

@Composable
private fun PeriodCard(state: UiState, onClick: () -> Unit) {
    val period = state.period ?: return
    val progress = state.periodProgress ?: return
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("EVALUACIÓN", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(progress.remainingDays.toString(), style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (progress.remainingDays == 1) "día restante" else "días restantes",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                Text(
                    "${formatDateUtc(period.startUtc, "d MMM")} – ${formatDateUtc(period.endUtc, "d MMM yyyy")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RingProgress(progress = progress.fraction, color = MaterialTheme.colorScheme.primary, size = 76.dp) {
                Text(
                    "${(progress.fraction * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun RiskCard(state: UiState, onOpenSubject: (String) -> Unit) {
    AppCard(color = AppColors.Danger.copy(alpha = 0.06f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBubble(Icons.Filled.WarningAmber, AppColors.Danger, size = 36.dp)
            Spacer(Modifier.width(12.dp))
            Text("Asignaturas en riesgo", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(10.dp))
        state.subjectsAtRisk.sortedByDescending { it.percentage }.forEach { subject ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onOpenSubject(subject.code) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SubjectBadge(subject.code, size = 36.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    ProgressTrack(
                        fraction = if (subject.allowedAbsences > 0) subject.absences.toFloat() / subject.allowedAbsences else 1f,
                        color = subject.risk.color(),
                        height = 6.dp,
                    )
                    Text(
                        if (subject.remainingAbsences >= 0) {
                            "${formatPercent(subject.percentage)} · te quedan ${subject.remainingAbsences}"
                        } else {
                            "${formatPercent(subject.percentage)} · límite superado"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = subject.risk.color(),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Horario del día
// ---------------------------------------------------------------------------------------------

/** Fila de la línea de tiempo del día. */
@Composable
internal fun TimelineRow(block: Block, minuteOfDay: Int?, onOpenSubject: ((String) -> Unit)? = null) {
    val isNow = minuteOfDay != null && minuteOfDay >= block.start && minuteOfDay < block.end
    val isPast = minuteOfDay != null && minuteOfDay >= block.end
    val color = subjectColor(block.subject)
    val code = block.subject

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isPast) 0.45f else 1f)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isNow) color.copy(alpha = 0.12f) else Color.Transparent)
            .then(
                if (code != null && onOpenSubject != null) Modifier.clickable { onOpenSubject(code) } else Modifier
            )
            .padding(horizontal = 10.dp, vertical = if (block.isBreak) 6.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.width(50.dp)) {
            Text(formatMinutes(block.start), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            if (!block.isBreak) {
                Text(formatMinutes(block.end), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (block.isBreak) {
            Icon(Icons.Filled.Coffee, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
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
                    .height(38.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(code.orEmpty(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "${Subjects.nameOf(code.orEmpty())} · ${durationText(block.durationMinutes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (isNow) {
            Spacer(Modifier.width(8.dp))
            Pill("Ahora", color)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Próximas tareas
// ---------------------------------------------------------------------------------------------

@Composable
private fun UpcomingTasks(state: UiState, onToggle: (Task) -> Unit) {
    val upcoming = state.pendingTasks.sortedWith(taskOrder).take(4)
    AppCard(contentPadding = 10.dp) {
        if (upcoming.isEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                IconBubble(Icons.Filled.TaskAlt, AppColors.Safe, size = 36.dp)
                Spacer(Modifier.width(12.dp))
                Text(
                    "No tienes nada pendiente. ¡Bien hecho!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            upcoming.forEach { task ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = task.done, onCheckedChange = { onToggle(task) })
                    Column(modifier = Modifier.weight(1f)) {
                        Text(task.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Dot(subjectColor(task.subject), size = 6.dp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                buildString {
                                    append(task.subject)
                                    if (task.kind != TaskKind.TASK) append(" · ").append(task.kind.label)
                                },
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
        }
    }
}
