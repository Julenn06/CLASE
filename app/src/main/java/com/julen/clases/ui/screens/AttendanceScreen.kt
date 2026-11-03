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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.julen.clases.AppViewModel
import com.julen.clases.UiState
import com.julen.clases.data.AttendanceMath
import com.julen.clases.data.EntryType
import com.julen.clases.data.HistoryEntry
import com.julen.clases.data.SubjectAttendance
import com.julen.clases.data.Subjects
import com.julen.clases.ui.ShowMessage
import com.julen.clases.ui.capitalized
import com.julen.clases.ui.components.AppCard
import com.julen.clases.ui.components.EmptyState
import com.julen.clases.ui.components.PeriodPickerDialog
import com.julen.clases.ui.components.Pill
import com.julen.clases.ui.components.ProgressTrack
import com.julen.clases.ui.components.ScreenHeader
import com.julen.clases.ui.components.SectionLabel
import com.julen.clases.ui.components.SubjectBadge
import com.julen.clases.ui.formatDateUtc
import com.julen.clases.ui.formatLocal
import com.julen.clases.ui.plural
import com.julen.clases.ui.theme.AppColors
import com.julen.clases.ui.theme.color
import com.julen.clases.ui.theme.label

internal fun registerAbsence(viewModel: AppViewModel, subject: String, showMessage: ShowMessage) {
    viewModel.addAbsence(subject)
    showMessage("Falta añadida en $subject", "Deshacer") { viewModel.removeAbsence(subject) }
}

internal fun registerTardy(viewModel: AppViewModel, subject: String, showMessage: ShowMessage) {
    val result = viewModel.addTardy(subject)
    val message = if (result.convertedToAbsence) {
        "${AttendanceMath.TARDIES_PER_ABSENCE} retrasos en $subject: se suma 1 falta"
    } else {
        "Retraso añadido en $subject"
    }
    showMessage(message, "Deshacer") { viewModel.undoTardy(subject, result) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(state: UiState, viewModel: AppViewModel, showMessage: ShowMessage) {
    var showPeriodPicker by remember { mutableStateOf(false) }
    var historySubject by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ScreenHeader(
                title = "Asistencia",
                subtitle = "Límite de faltas: ${state.absenceLimit.toInt()}%",
            )
            PeriodCard(state = state, onConfigure = { showPeriodPicker = true })
        }

        item {
            SectionLabel("Asignaturas") {
                Text(
                    "${plural(state.totalAbsences, "falta")} · ${plural(state.totalTardies, "retraso")}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(state.subjects, key = { it.code }) { subject ->
            SubjectAttendanceCard(
                subject = subject,
                hasPeriod = state.period != null,
                onAddAbsence = { registerAbsence(viewModel, subject.code, showMessage) },
                onRemoveAbsence = { viewModel.removeAbsence(subject.code) },
                onAddTardy = { registerTardy(viewModel, subject.code, showMessage) },
                onRemoveTardy = { viewModel.removeTardy(subject.code) },
                onShowHistory = { historySubject = subject.code },
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

    historySubject?.let { code ->
        ModalBottomSheet(onDismissRequest = { historySubject = null }) {
            HistorySheet(code = code, entries = state.history.filter { it.subject == code })
        }
    }
}

@Composable
private fun PeriodCard(state: UiState, onConfigure: () -> Unit) {
    val period = state.period
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "PERIODO DE EVALUACIÓN",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (period != null) {
                        "${formatDateUtc(period.startUtc)} – ${formatDateUtc(period.endUtc)}"
                    } else {
                        "Sin configurar"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (period != null) MaterialTheme.colorScheme.onSurface else AppColors.Warning,
                    modifier = Modifier.padding(top = 2.dp),
                )
                state.periodProgress?.let {
                    Text(
                        "Quedan ${plural(it.remainingDays, "día")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            FilledTonalButton(onClick = onConfigure, shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (period == null) "Elegir" else "Cambiar")
            }
        }
        if (period == null) {
            Text(
                "Sin fechas no se puede calcular el porcentaje de faltas. Las horas se cuentan según tu horario real.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun SubjectAttendanceCard(
    subject: SubjectAttendance,
    hasPeriod: Boolean,
    onAddAbsence: () -> Unit,
    onRemoveAbsence: () -> Unit,
    onAddTardy: () -> Unit,
    onRemoveTardy: () -> Unit,
    onShowHistory: () -> Unit,
) {
    val riskColor by animateColorAsState(subject.risk.color(), label = "risk")
    val usage = if (subject.allowedAbsences > 0) {
        subject.absences.toFloat() / subject.allowedAbsences
    } else if (subject.absences > 0 && hasPeriod) 1f else 0f
    val animatedUsage by animateFloatAsState(usage.coerceIn(0f, 1f), label = "usage")

    AppCard {
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
                if (hasPeriod) {
                    Pill(subject.risk.label(), riskColor)
                } else {
                    Pill("Sin periodo", MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (hasPeriod) {
                Text(
                    "${"%.1f".format(subject.percentage)}%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = riskColor,
                )
            }
            IconButton(onClick = onShowHistory) {
                Icon(Icons.Filled.History, contentDescription = "Historial de ${subject.code}")
            }
        }

        if (hasPeriod) {
            Spacer(Modifier.height(12.dp))
            ProgressTrack(fraction = animatedUsage, color = riskColor)
            Spacer(Modifier.height(6.dp))
            Text(
                when {
                    subject.remainingAbsences < 0 ->
                        "Has superado el límite en ${plural(-subject.remainingAbsences, "falta")} (máx. ${subject.allowedAbsences} de ${subject.totalHours} h)"
                    subject.remainingAbsences == 0 ->
                        "No te quedan faltas: estás en el límite (${subject.allowedAbsences} de ${subject.totalHours} h)"
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
private fun Counter(
    label: String,
    value: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    decrementEnabled: Boolean,
    subjectCode: String,
    modifier: Modifier = Modifier,
) {
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
                .height(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CounterButton(
                icon = Icons.Filled.Remove,
                contentDescription = "Quitar ${label.lowercase()} en $subjectCode",
                enabled = decrementEnabled,
                onClick = onDecrement,
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
                onClick = onIncrement,
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

@Composable
private fun HistorySheet(code: String, entries: List<HistoryEntry>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SubjectBadge(code, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Historial", style = MaterialTheme.typography.titleLarge)
                Text(
                    Subjects.nameOf(code),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        if (entries.isEmpty()) {
            EmptyState(
                title = "Sin registros",
                message = "Aquí aparecerán la fecha y hora de cada falta o retraso que anotes.",
            )
        } else {
            LazyColumn {
                items(entries) { entry ->
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val isAbsence = entry.type == EntryType.ABSENCE
                        Icon(
                            if (isAbsence) Icons.Filled.EventBusy else Icons.Filled.Timer,
                            contentDescription = null,
                            tint = if (isAbsence) AppColors.Danger else AppColors.Warning,
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                when {
                                    entry.fromTardies -> "Falta (por ${AttendanceMath.TARDIES_PER_ABSENCE} retrasos)"
                                    isAbsence -> "Falta"
                                    else -> "Retraso"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                formatLocal(entry.timestamp, "EEEE d MMM yyyy · HH:mm").capitalized(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}
