package com.julen.clases.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.julen.clases.AppViewModel
import com.julen.clases.UiState
import com.julen.clases.data.AttendanceMath
import com.julen.clases.data.EntryType
import com.julen.clases.data.Timetable
import com.julen.clases.data.SubjectAttendance
import com.julen.clases.data.Subjects
import com.julen.clases.ui.Navigator
import com.julen.clases.ui.ShowMessage
import com.julen.clases.ui.components.AppCard
import com.julen.clases.ui.components.GradientCard
import com.julen.clases.ui.components.PeriodPickerDialog
import com.julen.clases.ui.components.Pill
import com.julen.clases.ui.components.ProgressTrack
import com.julen.clases.ui.components.RingProgress
import com.julen.clases.ui.components.ScreenHeader
import com.julen.clases.ui.components.SectionHeader
import com.julen.clases.ui.components.SectionLabel
import com.julen.clases.ui.components.SubjectBadge
import com.julen.clases.ui.formatDateUtc
import com.julen.clases.ui.formatPercent
import com.julen.clases.ui.plural
import com.julen.clases.ui.theme.AppColors
import com.julen.clases.ui.theme.color
import com.julen.clases.ui.theme.label
import com.julen.clases.ui.theme.subjectColor
import java.util.Calendar

@Composable
fun AttendanceScreen(state: UiState, viewModel: AppViewModel, navigator: Navigator, showMessage: ShowMessage) {
    var showPeriodPicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { ScreenHeader(title = "Asistencia", subtitle = "Límite ${state.absenceLimit.toInt()}% por asignatura") }
        item { OverviewCard(state, onConfigure = { showPeriodPicker = true }) }
        if (state.history.any { it.type == EntryType.ABSENCE }) {
            item { StatsCard(state) }
        }
        item {
            SectionHeader("Asignaturas · de más a menos faltas")
        }
        items(state.subjects.sortedByDescending { it.percentage }, key = { it.code }) { subject ->
            SubjectAttendanceCard(
                subject = subject,
                hasPeriod = state.period != null,
                onOpen = { navigator.openSubject(subject.code) },
                onAddAbsence = { registerAbsence(viewModel, subject.code, showMessage) },
                onRemoveAbsence = { viewModel.removeAbsence(subject.code) },
                onAddTardy = { registerTardy(viewModel, subject.code, showMessage) },
                onRemoveTardy = { viewModel.removeTardy(subject.code) },
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
}

@Composable
private fun OverviewCard(state: UiState, onConfigure: () -> Unit) {
    val period = state.period
    val attendance = state.overallAttendance
    val white = Color.White
    GradientCard(colors = if (state.subjectsAtRisk.isEmpty()) AppColors.SuccessGradient else AppColors.HeroGradient) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RingProgress(
                progress = (attendance ?: 0f) / 100f,
                color = white,
                trackColor = white.copy(alpha = 0.2f),
                size = 104.dp,
                strokeWidth = 11.dp,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        attendance?.let { formatPercent(it) } ?: "—",
                        color = white,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                    )
                    Text("asistencia", color = white.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OverviewLine("Faltas", state.totalAbsences.toString())
                OverviewLine("Retrasos", state.totalTardies.toString())
                OverviewLine("En riesgo", state.subjectsAtRisk.size.toString())
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(white.copy(alpha = 0.12f))
                .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("PERIODO", color = white.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                Text(
                    if (period != null) "${formatDateUtc(period.startUtc, "d MMM")} – ${formatDateUtc(period.endUtc, "d MMM yyyy")}" else "Sin configurar",
                    color = white,
                    style = MaterialTheme.typography.labelLarge,
                )
                state.periodProgress?.let {
                    Text("Quedan ${plural(it.remainingDays, "día")}", color = white.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall)
                }
            }
            FilledTonalButton(onClick = onConfigure, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (period == null) "Elegir" else "Cambiar")
            }
        }
        if (period == null) {
            Text(
                "Sin fechas no se puede calcular el porcentaje. Las horas se cuentan según tu horario real, descontando festivos.",
                color = white.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun OverviewLine(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun SubjectAttendanceCard(
    subject: SubjectAttendance,
    hasPeriod: Boolean,
    onOpen: () -> Unit,
    onAddAbsence: () -> Unit,
    onRemoveAbsence: () -> Unit,
    onAddTardy: () -> Unit,
    onRemoveTardy: () -> Unit,
) {
    val riskColor by animateColorAsState(subject.risk.color(), label = "risk")
    val usage = when {
        subject.allowedAbsences > 0 -> subject.absences.toFloat() / subject.allowedAbsences
        subject.absences > 0 && hasPeriod -> 1f
        else -> 0f
    }

    AppCard(onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SubjectBadge(subject.code)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    Subjects.nameOf(subject.code),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                if (hasPeriod) Pill(subject.risk.label(), riskColor) else Pill("Sin periodo", MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (hasPeriod) {
                RingProgress(progress = usage, color = riskColor, size = 52.dp, strokeWidth = 6.dp) {
                    Text(
                        formatPercent(subject.percentage),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        if (hasPeriod) {
            Spacer(Modifier.height(12.dp))
            ProgressTrack(fraction = usage, color = riskColor, height = 6.dp)
            Spacer(Modifier.height(6.dp))
            Text(
                when {
                    subject.remainingAbsences < 0 ->
                        "Límite superado en ${plural(-subject.remainingAbsences, "falta")} · máx. ${subject.allowedAbsences} de ${subject.totalHours} h"
                    subject.remainingAbsences == 0 ->
                        "Estás en el límite · máx. ${subject.allowedAbsences} de ${subject.totalHours} h"
                    else ->
                        "Te quedan ${plural(subject.remainingAbsences, "falta")} · máx. ${subject.allowedAbsences} de ${subject.totalHours} h"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (subject.remainingAbsences <= 0) riskColor else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Counter(
                label = "Retrasos",
                value = "${subject.tardies}/${AttendanceMath.TARDIES_PER_ABSENCE}",
                onDecrement = onRemoveTardy,
                onIncrement = onAddTardy,
                decrementEnabled = subject.tardies > 0,
                subjectCode = subject.code,
                modifier = Modifier.weight(1f),
            )
            Counter(
                label = "Faltas",
                value = subject.absences.toString(),
                onDecrement = onRemoveAbsence,
                onIncrement = onAddAbsence,
                decrementEnabled = subject.absences > 0,
                subjectCode = subject.code,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
internal fun Counter(
    label: String,
    value: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    decrementEnabled: Boolean,
    subjectCode: String,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    Column(modifier = modifier) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CounterButton(
                icon = Icons.Filled.Remove,
                contentDescription = "Quitar ${label.lowercase()} en $subjectCode",
                enabled = decrementEnabled,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDecrement()
                },
                modifier = Modifier.weight(1f),
            )
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            CounterButton(
                icon = Icons.Filled.Add,
                contentDescription = "Añadir ${label.lowercase()} en $subjectCode",
                enabled = true,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onIncrement()
                },
                modifier = Modifier.weight(1f),
                highlighted = true,
            )
        }
    }
}

@Composable
private fun CounterButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        IconButton(onClick = onClick, enabled = enabled) {
            Icon(
                icon,
                contentDescription = contentDescription,
                tint = when {
                    !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    highlighted -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

/** Estadísticas: faltas por día de la semana y por asignatura. */
@Composable
private fun StatsCard(state: UiState) {
    val absences = state.history.filter { it.type == EntryType.ABSENCE }
    val byWeekday = IntArray(5)
    absences.forEach { entry ->
        val cal = Calendar.getInstance().apply { timeInMillis = entry.timestamp }
        val weekday = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        if (weekday < 5) byWeekday[weekday]++
    }
    val maxDay = (byWeekday.maxOrNull() ?: 0).coerceAtLeast(1)
    val worstDay = byWeekday.indices.maxByOrNull { byWeekday[it] }
    val bySubject = state.subjects.filter { it.absences > 0 }.sortedByDescending { it.absences }
    val maxSubject = (bySubject.maxOfOrNull { it.absences } ?: 0).coerceAtLeast(1)

    AppCard {
        SectionLabel("Estadísticas")
        Text("Faltas por día", style = MaterialTheme.typography.titleSmall)
        if (worstDay != null && byWeekday[worstDay] > 0) {
            Text(
                "Los ${Timetable.dayNames[worstDay].lowercase()} es cuando más faltas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            byWeekday.forEachIndexed { day, count ->
                val fraction by animateFloatAsState(count.toFloat() / maxDay, label = "bar")
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(count.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((80 * fraction).dp.coerceAtLeast(4.dp))
                            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                            .background(
                                if (day == worstDay && count > 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            ),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(Timetable.dayShortNames[day], style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        if (bySubject.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            Text("Faltas por asignatura", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            bySubject.forEach { subject ->
                Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(subject.code, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(56.dp))
                    ProgressTrack(
                        fraction = subject.absences.toFloat() / maxSubject,
                        color = subjectColor(subject.code),
                        height = 10.dp,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        subject.absences.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = 10.dp).width(24.dp),
                    )
                }
            }
        }
    }
}
