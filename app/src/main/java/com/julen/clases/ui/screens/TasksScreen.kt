package com.julen.clases.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.julen.clases.AppViewModel
import com.julen.clases.UiState
import com.julen.clases.data.AttendanceMath
import com.julen.clases.data.NowStatus
import com.julen.clases.data.Subjects
import com.julen.clases.data.Task
import com.julen.clases.ui.ShowMessage
import com.julen.clases.ui.components.AppCard
import com.julen.clases.ui.components.Dot
import com.julen.clases.ui.components.EmptyState
import com.julen.clases.ui.components.ScreenHeader
import com.julen.clases.ui.components.SectionLabel
import com.julen.clases.ui.components.SingleDatePickerDialog
import com.julen.clases.ui.formatDateUtc
import com.julen.clases.ui.relativeDay
import com.julen.clases.ui.theme.AppColors
import com.julen.clases.ui.theme.subjectColor
import kotlinx.coroutines.delay

@Composable
fun TasksScreen(state: UiState, viewModel: AppViewModel, showMessage: ShowMessage) {
    var notesTab by rememberSaveable { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            ScreenHeader(title = if (notesTab) "Apuntes" else "Tareas", subtitle = "Organiza tu curso")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !notesTab, onClick = { notesTab = false }, label = { Text("Tareas y exámenes") })
                FilterChip(selected = notesTab, onClick = { notesTab = true }, label = { Text("Apuntes") })
            }
            Spacer(Modifier.size(8.dp))
        }
        if (notesTab) {
            NotesTab(viewModel = viewModel, defaultSubject = currentOrFirstSubject(state))
        } else {
            TasksTab(state = state, viewModel = viewModel, showMessage = showMessage)
        }
    }
}

private fun currentOrFirstSubject(state: UiState): String =
    (state.nowStatus as? NowStatus.InClass)?.block?.subject ?: Subjects.codes.first()

// ---------------------------------------------------------------------------------------------
// Tareas
// ---------------------------------------------------------------------------------------------

@Composable
private fun TasksTab(state: UiState, viewModel: AppViewModel, showMessage: ShowMessage) {
    var showAddDialog by remember { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf<String?>(null) }

    val visible = state.tasks
        .filter { filter == null || it.subject == filter }
        .sortedWith(taskOrder)
    val pending = visible.filter { !it.done }
    val done = visible.filter { it.done }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SubjectFilterRow(
                    selected = filter,
                    onSelect = { filter = it },
                    includeAll = true,
                )
            }

            if (visible.isEmpty()) {
                item {
                    EmptyState(
                        title = "Nada por aquí",
                        message = "Pulsa «Nueva tarea» para apuntar deberes, entregas o fechas de examen.",
                    )
                }
            }

            if (pending.isNotEmpty()) {
                item { SectionLabel("Pendientes · ${pending.size}") }
                items(pending, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        todayUtc = state.todayUtc,
                        onToggle = { viewModel.toggleTask(task.id) },
                        onDelete = {
                            viewModel.deleteTask(task.id)
                            showMessage("Tarea eliminada", "Deshacer") { viewModel.restoreTask(task) }
                        },
                    )
                }
            }

            if (done.isNotEmpty()) {
                item {
                    SectionLabel("Hechas · ${done.size}") {
                        TextButton(onClick = {
                            val removed = done
                            viewModel.clearCompletedTasks()
                            showMessage("Tareas hechas eliminadas", "Deshacer") {
                                removed.forEach { viewModel.restoreTask(it) }
                            }
                        }) { Text("Limpiar") }
                    }
                }
                items(done, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        todayUtc = state.todayUtc,
                        onToggle = { viewModel.toggleTask(task.id) },
                        onDelete = {
                            viewModel.deleteTask(task.id)
                            showMessage("Tarea eliminada", "Deshacer") { viewModel.restoreTask(task) }
                        },
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showAddDialog = true },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Nueva tarea") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        )
    }

    if (showAddDialog) {
        AddTaskDialog(
            initialSubject = filter ?: currentOrFirstSubject(state),
            onDismiss = { showAddDialog = false },
            onConfirm = { subject, title, due ->
                viewModel.addTask(subject, title, due)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun TaskRow(task: Task, todayUtc: Long, onToggle: () -> Unit, onDelete: () -> Unit) {
    val color = subjectColor(task.subject)
    AppCard(contentPadding = 6.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = task.done, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.done) TextDecoration.LineThrough else null,
                    color = if (task.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Dot(color, size = 7.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        task.subject,
                        style = MaterialTheme.typography.labelMedium,
                        color = color,
                        fontWeight = FontWeight.Bold,
                    )
                    task.dueUtc?.let { due ->
                        val overdue = !task.done && due < todayUtc
                        val soon = !task.done && due - todayUtc <= 2 * AttendanceMath.DAY_MS
                        Text(
                            "  ·  ${relativeDay(due, todayUtc)} (${formatDateUtc(due, "d MMM")})",
                            style = MaterialTheme.typography.labelMedium,
                            color = when {
                                overdue -> AppColors.Danger
                                soon -> AppColors.Warning
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Eliminar tarea",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SubjectFilterRow(selected: String?, onSelect: (String?) -> Unit, includeAll: Boolean) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (includeAll) {
            item {
                FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("Todas") })
            }
        }
        items(Subjects.codes) { code ->
            FilterChip(
                selected = selected == code,
                onClick = { onSelect(code) },
                label = { Text(code) },
                leadingIcon = { Dot(subjectColor(code)) },
            )
        }
    }
}

@Composable
private fun AddTaskDialog(
    initialSubject: String,
    onDismiss: () -> Unit,
    onConfirm: (subject: String, title: String, dueUtc: Long?) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var subject by rememberSaveable { mutableStateOf(initialSubject) }
    var due by rememberSaveable { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva tarea") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("¿Qué hay que hacer?") },
                    placeholder = { Text("Ej.: Examen tema 3, entrega práctica…") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Asignatura", style = MaterialTheme.typography.labelLarge)
                SubjectFilterRow(selected = subject, onSelect = { if (it != null) subject = it }, includeAll = false)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Filled.Event, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(due?.let { "Fecha: ${formatDateUtc(it, "EEE d MMM")}" } ?: "Añadir fecha límite")
                    }
                    if (due != null) {
                        IconButton(onClick = { due = null }) {
                            Icon(Icons.Filled.Close, contentDescription = "Quitar fecha")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = { onConfirm(subject, title, due) },
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )

    if (showDatePicker) {
        SingleDatePickerDialog(
            initialUtc = due,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                due = it
                showDatePicker = false
            },
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Apuntes (se guardan solos)
// ---------------------------------------------------------------------------------------------

@Composable
private fun NotesTab(viewModel: AppViewModel, defaultSubject: String) {
    var subject by rememberSaveable { mutableStateOf(defaultSubject) }
    var withNotes by remember { mutableStateOf(viewModel.subjectsWithNotes()) }
    var text by remember(subject) { mutableStateOf(viewModel.getNote(subject)) }
    var saved by remember(subject) { mutableStateOf(true) }

    // Autoguardado medio segundo después de dejar de escribir.
    LaunchedEffect(subject, text) {
        if (saved) return@LaunchedEffect
        delay(500)
        viewModel.saveNote(subject, text)
        withNotes = viewModel.subjectsWithNotes()
        saved = true
    }

    // Si se sale de la pantalla antes del autoguardado, se guarda igualmente.
    DisposableEffect(subject) {
        onDispose { if (!saved) viewModel.saveNote(subject, text) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 20.dp),
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Subjects.codes) { code ->
                FilterChip(
                    selected = subject == code,
                    onClick = {
                        if (!saved) viewModel.saveNote(subject, text)
                        subject = code
                    },
                    label = { Text(code) },
                    leadingIcon = if (code in withNotes) {
                        { Dot(subjectColor(code)) }
                    } else {
                        null
                    },
                )
            }
        }
        Spacer(Modifier.size(10.dp))
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                saved = false
            },
            label = { Text(Subjects.nameOf(subject)) },
            placeholder = { Text("Apuntes, dudas para clase, enlaces, criterios de evaluación…") },
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .heightIn(min = 160.dp),
        )
        Text(
            text = if (saved) "Guardado · ${text.length} caracteres" else "Guardando…",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 10.dp),
        )
    }
}
