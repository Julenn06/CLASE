package com.julen.clases.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
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
import com.julen.clases.data.RiskLevel
import com.julen.clases.data.EntryType
import com.julen.clases.data.Timetable
import com.julen.clases.data.SubjectAttendance
import com.julen.clases.data.Subjects
import com.julen.clases.ui.Navigator
import com.julen.clases.ui.ShowMessage
import com.julen.clases.ui.components.AppCard
import com.julen.clases.ui.components.AppScreen
import com.julen.clases.ui.components.GroupDivider
import com.julen.clases.ui.components.GroupItem
import com.julen.clases.ui.components.ListGroup
import com.julen.clases.ui.components.PeriodPickerDialog
import com.julen.clases.ui.components.ProgressTrack
import com.julen.clases.ui.components.RowIcon
import com.julen.clases.ui.components.SectionTitle
import com.julen.clases.ui.components.SubjectBadge
import com.julen.clases.ui.components.screenPadding
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

    AppScreen(title = "Asistencia") { inner ->
        LazyColumn(contentPadding = screenPadding(inner)) {
            item { Overview(state) }

            item { SectionTitle("Periodo de evaluación") }
            item {
                ListGroup {
                    val period = state.period
                    val progress = state.periodProgress
                    GroupItem(
                        headline = if (period != null) {
                            "${formatDateUtc(period.startUtc, "d MMM")} – ${formatDateUtc(period.endUtc, "d MMM yyyy")}"
                        } else {
                            "Sin configurar"
                        },
                        supporting = if (progress != null) {
                            "Quedan ${plural(progress.remainingDays, "día", "días")} · límite del ${state.absenceLimit.toInt()} % por asignatura"
                        } else {
                            "Elige las fechas para calcular el porcentaje con las horas reales de tu horario"
                        },
                        leading = { RowIcon(Icons.Outlined.DateRange, MaterialTheme.colorScheme.primary) },
                        trailing = { TextButton(onClick = { showPeriodPicker = true }) { Text(if (period == null) "Elegir" else "Cambiar") } },
                        onClick = { showPeriodPicker = true },
                    )
                }
            }

            if (state.history.any { it.type == EntryType.ABSENCE }) {
                item { SectionTitle("Cuándo faltas") }
                item { StatsCard(state) }
            }

            item { SectionTitle("Asignaturas") }
            item {
                ListGroup {
                    state.subjects.sortedByDescending { it.percentage }.forEachIndexed { index, subject ->
                        if (index > 0) GroupDivider(inset = 72.dp)
                        SubjectAttendanceRow(
                            subject = subject,
                            hasPeriod = state.period != null,
                            onOpen = { navigator.openSubject(subject.code) },
                            onAddAbsence = { registerAbsence(viewModel, subject.code, showMessage) },
                        )
                    }
                }
            }
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

/** Resumen como texto: el número que importa en grande y su contexto debajo. */
@Composable
private fun Overview(state: UiState) {
    val attendance = state.overallAttendance
    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                attendance?.let { formatPercent(it) } ?: "—",
                style = MaterialTheme.typography.displayMedium,
                color = if (state.subjectsAtRisk.isEmpty()) MaterialTheme.colorScheme.onBackground else AppColors.Danger,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "de asistencia",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }
        Text(
            buildString {
                append(plural(state.totalAbsences, "falta"))
                append(" · ").append(plural(state.totalTardies, "retraso"))
                if (state.subjectsAtRisk.isNotEmpty()) append(" · ").append(state.subjectsAtRisk.size).append(" en riesgo")
                state.streak?.let { append(" · ").append(plural(it, "día", "días")).append(" seguidos sin faltar") }
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SubjectAttendanceRow(
    subject: SubjectAttendance,
    hasPeriod: Boolean,
    onOpen: () -> Unit,
    onAddAbsence: () -> Unit,
) {
    val riskColor by animateColorAsState(subject.risk.color(), label = "risk")
    val usage = when {
        subject.allowedAbsences > 0 -> subject.absences.toFloat() / subject.allowedAbsences
        subject.absences > 0 && hasPeriod -> 1f
        else -> 0f
    }
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SubjectBadge(subject.code)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    Subjects.nameOf(subject.code),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (hasPeriod) {
                    Text(formatPercent(subject.percentage), style = MaterialTheme.typography.labelLarge, color = riskColor)
                }
            }
            if (hasPeriod) {
                ProgressTrack(
                    fraction = usage,
                    color = riskColor,
                    height = 4.dp,
                    modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
                )
                Text(
                    when {
                        subject.remainingAbsences < 0 -> "${subject.absences} de ${subject.allowedAbsences} faltas · límite superado"
                        subject.remainingAbsences == 0 -> "${subject.absences} de ${subject.allowedAbsences} faltas · estás en el límite"
                        else -> "${subject.absences} de ${subject.allowedAbsences} faltas · te quedan ${subject.remainingAbsences}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (subject.risk == RiskLevel.SAFE) MaterialTheme.colorScheme.onSurfaceVariant else riskColor,
                )
            } else {
                Text(
                    "${plural(subject.absences, "falta")} · ${subject.tardies}/${AttendanceMath.TARDIES_PER_ABSENCE} retrasos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onAddAbsence()
        }) {
            Icon(Icons.Filled.Add, contentDescription = "Añadir falta en ${subject.code}", tint = MaterialTheme.colorScheme.primary)
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
        Text("Faltas por día de la semana", style = MaterialTheme.typography.titleSmall)
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
