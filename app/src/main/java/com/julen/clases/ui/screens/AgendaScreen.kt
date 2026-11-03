package com.julen.clases.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import com.julen.clases.AppViewModel
import com.julen.clases.UiState
import com.julen.clases.data.CalendarMath
import com.julen.clases.data.NowStatus
import com.julen.clases.data.Subjects
import com.julen.clases.data.Task
import com.julen.clases.data.TaskKind
import com.julen.clases.ui.ShowMessage
import com.julen.clases.ui.capitalized
import com.julen.clases.ui.components.AppCard
import com.julen.clases.ui.components.Dot
import com.julen.clases.ui.components.EmptyState
import com.julen.clases.ui.components.MonthGrid
import com.julen.clases.ui.components.WeekdayHeader
import com.julen.clases.ui.formatDateUtc
import com.julen.clases.ui.components.AppScreen
import com.julen.clases.ui.components.GroupDivider
import com.julen.clases.ui.components.ListGroup
import com.julen.clases.ui.components.SectionTitle
import com.julen.clases.ui.components.screenPadding
import com.julen.clases.ui.components.Segments
import com.julen.clases.ui.plural
import com.julen.clases.ui.daysUntil
import com.julen.clases.ui.theme.AppColors

private enum class DueGroup(val title: String) {
    OVERDUE("Vencidas"),
    TODAY("Hoy"),
    TOMORROW("Mañana"),
    WEEK("Próximos 7 días"),
    LATER("Más adelante"),
    NO_DATE("Sin fecha"),
}

private fun groupOf(task: Task, todayUtc: Long): DueGroup {
    val due = task.dueUtc ?: return DueGroup.NO_DATE
    val days = daysUntil(due, todayUtc)
    return when {
        days < 0 -> DueGroup.OVERDUE
        days == 0 -> DueGroup.TODAY
        days == 1 -> DueGroup.TOMORROW
        days <= 7 -> DueGroup.WEEK
        else -> DueGroup.LATER
    }
}

@Composable
fun AgendaScreen(state: UiState, viewModel: AppViewModel, showMessage: ShowMessage) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var kindFilter by rememberSaveable { mutableStateOf<TaskKind?>(null) }
    var subjectFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var selectedDay by rememberSaveable { mutableStateOf(state.todayUtc) }

    val showDone = tab == 1
    val visible = state.tasks
        .filter { it.done == showDone }
        .filter { kindFilter == null || it.kind == kindFilter }
        .filter { subjectFilter == null || it.subject == subjectFilter }
        .sortedWith(taskOrder)

    val onDelete: (Task) -> Unit = { task ->
        viewModel.deleteTask(task.id)
        showMessage("Eliminado: ${task.title}", "Deshacer") { viewModel.restoreTasks(listOf(task)) }
    }

    val taskList: @Composable (List<Task>) -> Unit = { tasks ->
        ListGroup {
            tasks.forEachIndexed { index, task ->
                if (index > 0) GroupDivider(inset = 56.dp)
                TaskRow(task, state.todayUtc, onToggle = { viewModel.toggleTask(task.id) }, onDelete = { onDelete(task) }, onClick = { editingTask = task })
            }
        }
    }

    AppScreen(
        title = "Agenda",
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showEditor = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Nueva") },
            )
        },
    ) { inner ->
        LazyColumn(
            contentPadding = screenPadding(inner, extraBottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Segments(
                    options = listOf("Pendientes", "Hechas", "Calendario"),
                    selectedIndex = tab,
                    onSelect = { tab = it },
                )
            }
            if (tab == 2) {
                item {
                    AgendaCalendar(
                        state = state,
                        selectedDay = selectedDay,
                        onSelectDay = { selectedDay = it },
                    )
                }
                val dayTasks = state.tasks.filter { it.dueUtc == selectedDay }.sortedWith(taskOrder)
                item {
                    SectionTitle(formatDateUtc(selectedDay, "EEEE d 'de' MMMM").capitalized())
                }
                item {
                    if (dayTasks.isEmpty()) {
                        Text(
                            "Nada apuntado para este día.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    } else {
                        taskList(dayTasks)
                    }
                }
            } else {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(selected = kindFilter == null, onClick = { kindFilter = null }, label = { Text("Todo") })
                        }
                        items(TaskKind.entries.toList()) { kind ->
                            FilterChip(
                                selected = kindFilter == kind,
                                onClick = { kindFilter = if (kindFilter == kind) null else kind },
                                label = { Text(kind.label) },
                                leadingIcon = { Icon(kind.icon(), null, tint = kind.color(), modifier = Modifier.size(18.dp)) },
                            )
                        }
                    }
                }
                item { SubjectChips(selected = subjectFilter, onSelect = { subjectFilter = it }, includeAll = true) }

                if (visible.isEmpty()) {
                    item {
                        EmptyState(
                            title = if (showDone) "Nada completado aún" else "Nada pendiente",
                            message = if (showDone) {
                                "Las tareas que marques como hechas aparecerán aquí."
                            } else {
                                "Pulsa «Nueva» para apuntar deberes, entregas o fechas de examen."
                            },
                            icon = if (showDone) Icons.Outlined.TaskAlt else Icons.Outlined.EventNote,
                        )
                    }
                }

                if (showDone) {
                    if (visible.isNotEmpty()) {
                        item {
                            SectionTitle(
                                plural(visible.size, "completada", "completadas"),
                                actionLabel = "Limpiar todo",
                                onAction = {
                                    val removed = state.tasks.filter { it.done }
                                    viewModel.clearCompletedTasks()
                                    showMessage("Tareas hechas eliminadas", "Deshacer") { viewModel.restoreTasks(removed) }
                                },
                            )
                        }
                        item { taskList(visible) }
                    }
                } else {
                    visible.groupBy { groupOf(it, state.todayUtc) }.toSortedMap().forEach { (group, tasks) ->
                        item(key = "group-${group.name}") {
                            Column {
                                Text(
                                    "${group.title} · ${tasks.size}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (group == DueGroup.OVERDUE) AppColors.Danger else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 12.dp, start = 4.dp, bottom = 8.dp),
                                )
                                taskList(tasks)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditor || editingTask != null) {
        val current = (state.nowStatus as? NowStatus.InClass)?.block?.subject
        val editing = editingTask
        TaskEditorSheet(
            initial = editing,
            initialDueUtc = if (tab == 2) selectedDay else null,
            initialSubject = subjectFilter ?: current ?: Subjects.codes.firstOrNull() ?: "",
            onDismiss = {
                showEditor = false
                editingTask = null
            },
            onConfirm = { subject, title, due, kind ->
                if (editing != null) {
                    viewModel.updateTask(editing.copy(subject = subject, title = title.trim(), dueUtc = due, kind = kind))
                } else {
                    viewModel.addTask(subject, title, due, kind)
                }
                showEditor = false
                editingTask = null
                showMessage("Guardado en $subject: ${title.trim()}", null, null)
            },
        )
    }
}

/** Calendario mensual de la agenda con puntos por cada tarea, examen o entrega. */
@Composable
private fun AgendaCalendar(state: UiState, selectedDay: Long, onSelectDay: (Long) -> Unit) {
    var shown by rememberSaveable { mutableStateOf(CalendarMath.yearOf(selectedDay) * 12 + CalendarMath.monthOf(selectedDay) - 1) }
    val year = Math.floorDiv(shown, 12)
    val month = Math.floorMod(shown, 12) + 1
    val byDay = state.tasks.filter { it.dueUtc != null }.groupBy { it.dueUtc }

    AppCard(contentPadding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { shown-- }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mes anterior") }
            Text(
                formatDateUtc(CalendarMath.date(year, month, 1), "MMMM yyyy").capitalized(),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { shown++ }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Mes siguiente") }
        }
        WeekdayHeader(Modifier.padding(vertical = 6.dp))
        MonthGrid(year, month, cellHeight = 52.dp) { day, dayOfMonth, weekday ->
            val tasks = byDay[day].orEmpty()
            val selected = day == selectedDay
            val isToday = day == state.todayUtc
            val holiday = state.holidays.any { day in it }
            Column(
                modifier = Modifier
                    .padding(2.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        when {
                            selected -> MaterialTheme.colorScheme.primary
                            isToday -> MaterialTheme.colorScheme.primaryContainer
                            else -> Color.Transparent
                        }
                    )
                    .clickable { onSelectDay(day) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected || isToday) FontWeight.ExtraBold else FontWeight.Medium,
                    color = when {
                        selected -> MaterialTheme.colorScheme.onPrimary
                        holiday -> AppColors.Safe
                        weekday >= 5 -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 3.dp).height(5.dp)) {
                    tasks.take(3).forEach { task ->
                        Dot(
                            if (selected) MaterialTheme.colorScheme.onPrimary else if (task.done) MaterialTheme.colorScheme.outline else task.kind.color(),
                            size = 5.dp,
                        )
                    }
                }
            }
        }
    }
}
