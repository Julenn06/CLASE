package com.julen.clases.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import com.julen.clases.AppViewModel
import com.julen.clases.UiState
import com.julen.clases.data.NowStatus
import com.julen.clases.data.Subjects
import com.julen.clases.data.Task
import com.julen.clases.data.TaskKind
import com.julen.clases.ui.ShowMessage
import com.julen.clases.ui.components.EmptyState
import com.julen.clases.ui.components.ScreenHeader
import com.julen.clases.ui.components.SegmentedControl
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

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                val pending = state.pendingTasks.size
                ScreenHeader(
                    title = "Agenda",
                    subtitle = if (pending == 0) "Todo al día" else "$pending pendiente${if (pending == 1) "" else "s"}",
                )
                SegmentedControl(
                    options = listOf("Pendientes", "Hechas"),
                    selectedIndex = tab,
                    onSelect = { tab = it },
                )
            }
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
                        icon = if (showDone) Icons.Filled.TaskAlt else Icons.Filled.EventNote,
                    )
                }
            }

            if (showDone) {
                if (visible.isNotEmpty()) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${visible.size} completadas",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = {
                                val removed = state.tasks.filter { it.done }
                                viewModel.clearCompletedTasks()
                                showMessage("Tareas hechas eliminadas", "Deshacer") { viewModel.restoreTasks(removed) }
                            }) { Text("Limpiar todo") }
                        }
                    }
                }
                items(visible, key = { it.id }) { task ->
                    TaskRow(task, state.todayUtc, onToggle = { viewModel.toggleTask(task.id) }, onDelete = { onDelete(task) }, onClick = { editingTask = task })
                }
            } else {
                visible.groupBy { groupOf(it, state.todayUtc) }.toSortedMap().forEach { (group, tasks) ->
                    item(key = "header-${group.name}") {
                        Text(
                            "${group.title} · ${tasks.size}",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (group == DueGroup.OVERDUE) AppColors.Danger else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                        )
                    }
                    items(tasks, key = { it.id }) { task ->
                        TaskRow(task, state.todayUtc, onToggle = { viewModel.toggleTask(task.id) }, onDelete = { onDelete(task) }, onClick = { editingTask = task })
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showEditor = true },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Nueva") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        )
    }

    if (showEditor || editingTask != null) {
        val current = (state.nowStatus as? NowStatus.InClass)?.block?.subject
        val editing = editingTask
        TaskEditorSheet(
            initial = editing,
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
