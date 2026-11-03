package com.julen.clases.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.julen.clases.AppViewModel
import com.julen.clases.UiState
import com.julen.clases.data.Subject
import com.julen.clases.data.Subjects
import com.julen.clases.data.Timetable
import com.julen.clases.data.formatMinutes
import com.julen.clases.ui.Navigator
import com.julen.clases.ui.ShowMessage
import com.julen.clases.ui.components.AppCard
import com.julen.clases.ui.components.ScreenHeader
import com.julen.clases.ui.components.SectionHeader
import com.julen.clases.ui.components.SegmentedControl
import com.julen.clases.ui.components.SubjectBadge
import com.julen.clases.ui.durationText
import com.julen.clases.ui.theme.subjectColor

@Composable
fun ScheduleScreen(state: UiState, viewModel: AppViewModel, navigator: Navigator, showMessage: ShowMessage) {
    var mode by rememberSaveable { mutableIntStateOf(0) }
    var selectedDay by rememberSaveable { mutableIntStateOf(state.today ?: 0) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var pickerCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var editingSubject by remember { mutableStateOf<Subject?>(null) }
    var creatingSubject by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ScreenHeader(title = "Horario", subtitle = if (editing) "Modo edición" else "Semana lectiva") {
                FilledTonalIconButton(onClick = { editing = !editing }) {
                    Icon(
                        if (editing) Icons.Filled.Check else Icons.Filled.Edit,
                        contentDescription = if (editing) "Terminar de editar" else "Editar horario",
                    )
                }
            }
            if (!editing) {
                SegmentedControl(
                    options = listOf("Por día", "Semana"),
                    selectedIndex = mode,
                    onSelect = { mode = it },
                )
            } else {
                AppCard(color = MaterialTheme.colorScheme.primaryContainer, contentPadding = 14.dp) {
                    Text(
                        "Toca cualquier hora para cambiar la asignatura. Abajo puedes crear, renombrar o cambiar el color de las asignaturas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        if (editing) {
            item {
                WeekGrid(
                    today = null,
                    minuteOfDay = state.minuteOfDay,
                    editing = true,
                    onCellClick = { day, slotIndex, _ -> pickerCell = day to slotIndex },
                )
            }
        } else if (mode == 0) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Timetable.dayShortNames.forEachIndexed { index, short ->
                        DayChip(
                            text = short,
                            hours = Timetable.blocksFor(index).filter { !it.isBreak }.sumOf { it.durationMinutes } / 60,
                            selected = index == selectedDay,
                            isToday = index == state.today,
                            onClick = { selectedDay = index },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            item {
                val blocks = Timetable.blocksFor(selectedDay)
                if (blocks.isEmpty()) {
                    AppCard {
                        Text("${Timetable.dayNames[selectedDay]} sin clases", style = MaterialTheme.typography.titleMedium)
                    }
                } else AppCard(contentPadding = 14.dp) {
                    Row(
                        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(Timetable.dayNames[selectedDay], style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        Text(
                            "${formatMinutes(blocks.first().start)} – ${formatMinutes(blocks.last().end)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    blocks.forEach { block ->
                        TimelineRow(
                            block = block,
                            minuteOfDay = if (selectedDay == state.today) state.minuteOfDay else null,
                            onOpenSubject = navigator.openSubject,
                        )
                    }
                }
            }
        } else {
            item {
                WeekGrid(
                    today = state.today,
                    minuteOfDay = state.minuteOfDay,
                    editing = false,
                    onCellClick = { _, _, subject -> subject?.let(navigator.openSubject) },
                )
            }
        }

        item {
            SectionHeader(
                "Asignaturas · ${Subjects.all.size}",
                actionLabel = if (editing) "Añadir" else null,
                onAction = { creatingSubject = true },
            )
        }
        items(Subjects.all, key = { it.code }) { subject ->
            AppCard(
                onClick = { if (editing) editingSubject = subject else navigator.openSubject(subject.code) },
                contentPadding = 14.dp,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SubjectBadge(subject.code, size = 42.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            subject.name,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            Timetable.dayShortNames.indices
                                .filter { Timetable.hoursOn(it, subject.code) > 0 }
                                .joinToString(" · ") { Timetable.dayNames[it].take(3) },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        "${Timetable.weeklyHours(subject.code)} h",
                        style = MaterialTheme.typography.titleSmall,
                        color = subjectColor(subject.code),
                    )
                    Icon(
                        if (editing) Icons.Filled.Edit else Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp).size(20.dp),
                    )
                }
            }
        }
        if (editing) {
            item {
                TextButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Restaurar el horario original")
                }
            }
        }
    }

    pickerCell?.let { (day, slotIndex) ->
        SubjectPickerDialog(
            day = day,
            slotIndex = slotIndex,
            current = Timetable.subjectAt(day, slotIndex),
            onDismiss = { pickerCell = null },
            onPick = { code ->
                Timetable.classIndexOf(slotIndex)?.let { viewModel.setScheduleCell(day, it, code) }
                pickerCell = null
            },
        )
    }

    if (creatingSubject || editingSubject != null) {
        val initial = editingSubject
        SubjectEditorDialog(
            initial = initial,
            onDismiss = {
                creatingSubject = false
                editingSubject = null
            },
            onSave = { subject ->
                viewModel.saveSubject(subject)
                showMessage(if (initial == null) "Asignatura ${subject.code} creada" else "Asignatura ${subject.code} guardada", null, null)
                creatingSubject = false
                editingSubject = null
            },
            onDelete = initial?.let { subject ->
                {
                    val previous = Timetable.config
                    viewModel.deleteSubject(subject.code)
                    editingSubject = null
                    showMessage("Asignatura ${subject.code} eliminada", "Deshacer") { viewModel.restoreSchedule(previous) }
                }
            },
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("¿Restaurar el horario original?") },
            text = { Text("Se recuperan las asignaturas y horas por defecto. Tus faltas, notas y tareas no se tocan.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetSchedule()
                    confirmReset = false
                    showMessage("Horario restaurado", null, null)
                }) { Text("Restaurar") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun DayChip(
    text: String,
    hours: Int,
    selected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val background by animateColorAsState(if (selected) scheme.primary else scheme.surfaceContainerLow, label = "day")
    val content by animateColorAsState(if (selected) scheme.onPrimary else scheme.onSurface, label = "dayText")
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .border(1.dp, if (selected) Color.Transparent else scheme.outlineVariant, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = content)
        Text("$hours h", style = MaterialTheme.typography.labelSmall, color = content.copy(alpha = 0.7f))
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(5.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    when {
                        !isToday -> Color.Transparent
                        selected -> scheme.onPrimary
                        else -> scheme.primary
                    }
                ),
        )
    }
}

@Composable
private fun WeekGrid(
    today: Int?,
    minuteOfDay: Int,
    editing: Boolean,
    onCellClick: (day: Int, slotIndex: Int, subject: String?) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    AppCard(contentPadding = 12.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Spacer(Modifier.width(40.dp))
            Timetable.dayShortNames.forEachIndexed { index, day ->
                Text(
                    day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (index == today) scheme.primary else scheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Timetable.slots.forEachIndexed { slotIndex, slot ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        formatMinutes(slot.start),
                        modifier = Modifier.width(40.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                    )
                    if (slot.isBreak) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(26.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(scheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "RECREO · ${durationText(slot.end - slot.start)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        for (day in 0 until 5) {
                            val subject = Timetable.subjectAt(day, slotIndex)
                            val color = subjectColor(subject)
                            val isNow = day == today && minuteOfDay >= slot.start && minuteOfDay < slot.end
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when {
                                            subject == null -> scheme.surfaceVariant.copy(alpha = 0.5f)
                                            isNow -> color
                                            else -> color.copy(alpha = 0.15f)
                                        }
                                    )
                                    .then(
                                        if (editing) Modifier.border(1.dp, scheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp)) else Modifier
                                    )
                                    .clickable(enabled = editing || subject != null) { onCellClick(day, slotIndex, subject) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    subject ?: if (editing) "+" else "",
                                    color = when {
                                        subject == null -> scheme.onSurfaceVariant
                                        isNow -> Color.White
                                        else -> color
                                    },
                                    fontWeight = FontWeight.Black,
                                    fontSize = if ((subject?.length ?: 0) > 4) 10.sp else 12.sp,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
