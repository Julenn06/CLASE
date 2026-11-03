package com.julen.clases.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.julen.clases.AppViewModel
import com.julen.clases.data.AttendanceMath
import com.julen.clases.data.Grade
import com.julen.clases.data.GradeMath
import com.julen.clases.data.Subjects
import com.julen.clases.data.Task
import com.julen.clases.data.TaskKind
import com.julen.clases.ui.ShowMessage
import com.julen.clases.ui.components.AppCard
import com.julen.clases.ui.components.Dot
import com.julen.clases.ui.components.IconBubble
import com.julen.clases.ui.components.Pill
import com.julen.clases.ui.components.SingleDatePickerDialog
import com.julen.clases.ui.daysUntil
import com.julen.clases.ui.formatDateUtc
import com.julen.clases.ui.formatScore
import com.julen.clases.ui.relativeDay
import com.julen.clases.ui.theme.AppColors
import com.julen.clases.ui.theme.gradeColor
import com.julen.clases.ui.theme.subjectColor

// ---------------------------------------------------------------------------------------------
// Asistencia con "Deshacer"
// ---------------------------------------------------------------------------------------------

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

// ---------------------------------------------------------------------------------------------
// Tareas
// ---------------------------------------------------------------------------------------------

/** Pendientes primero, después por fecha límite (sin fecha al final) y por antigüedad. */
internal val taskOrder: Comparator<Task> =
    compareBy<Task> { it.done }
        .thenBy { it.dueUtc ?: Long.MAX_VALUE }
        .thenBy { it.id }

internal fun TaskKind.icon(): ImageVector = when (this) {
    TaskKind.TASK -> Icons.Filled.Assignment
    TaskKind.EXAM -> Icons.Filled.Quiz
    TaskKind.DELIVERY -> Icons.Filled.UploadFile
}

internal fun TaskKind.color(): Color = when (this) {
    TaskKind.TASK -> Color(0xFF3B82F6)
    TaskKind.EXAM -> Color(0xFFE11D48)
    TaskKind.DELIVERY -> Color(0xFFF59E0B)
}

@Composable
internal fun TaskRow(
    task: Task,
    todayUtc: Long,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    showSubject: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val color = subjectColor(task.subject)
    AppCard(contentPadding = 6.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = task.done, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (task.done) TextDecoration.LineThrough else null,
                    color = if (task.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                ) {
                    if (task.kind != TaskKind.TASK) {
                        Pill(task.kind.label, task.kind.color(), icon = task.kind.icon())
                    }
                    if (showSubject) {
                        Dot(color, size = 7.dp)
                        Text(
                            task.subject,
                            style = MaterialTheme.typography.labelMedium,
                            color = color,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    task.dueUtc?.let { due ->
                        val days = daysUntil(due, todayUtc)
                        Text(
                            relativeDay(due, todayUtc),
                            style = MaterialTheme.typography.labelMedium,
                            color = when {
                                task.done -> MaterialTheme.colorScheme.onSurfaceVariant
                                days < 0 -> AppColors.Danger
                                days <= 2 -> AppColors.Warning
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.DeleteOutline,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Hoja inferior para crear una tarea, examen o entrega. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TaskEditorSheet(
    initialSubject: String,
    onDismiss: () -> Unit,
    onConfirm: (subject: String, title: String, dueUtc: Long?, kind: TaskKind) -> Unit,
    lockSubject: Boolean = false,
    initial: Task? = null,
) {
    var title by rememberSaveable { mutableStateOf(initial?.title ?: "") }
    var subject by rememberSaveable { mutableStateOf(initial?.subject ?: initialSubject) }
    var kind by rememberSaveable { mutableStateOf(initial?.kind ?: TaskKind.TASK) }
    var due by rememberSaveable { mutableStateOf(initial?.dueUtc) }
    var showDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(if (initial == null) "Nueva entrada" else "Editar", style = MaterialTheme.typography.headlineSmall)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TaskKind.entries.forEach { option ->
                    FilterChip(
                        selected = kind == option,
                        onClick = { kind = option },
                        label = { Text(option.label) },
                        leadingIcon = { Icon(option.icon(), null, modifier = Modifier.size(18.dp)) },
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(if (kind == TaskKind.EXAM) "¿De qué es el examen?" else "¿Qué hay que hacer?") },
                placeholder = { Text("Ej.: Tema 3, práctica de Room…") },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (!lockSubject) {
                SubjectChips(selected = subject, onSelect = { if (it != null) subject = it })
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Filled.Event, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(due?.let { "Fecha: ${formatDateUtc(it, "EEEE d 'de' MMMM")}" } ?: "Añadir fecha")
                }
                if (due != null) {
                    IconButton(onClick = { due = null }) {
                        Icon(Icons.Filled.Close, contentDescription = "Quitar fecha")
                    }
                }
            }

            Button(
                onClick = { onConfirm(subject, title, due, kind) },
                enabled = title.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Guardar", modifier = Modifier.padding(vertical = 6.dp)) }
        }
    }

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

/** Fila horizontal de chips para elegir asignatura. */
@Composable
internal fun SubjectChips(selected: String?, onSelect: (String?) -> Unit, includeAll: Boolean = false) {
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

// ---------------------------------------------------------------------------------------------
// Calificaciones
// ---------------------------------------------------------------------------------------------

@Composable
internal fun GradeRow(grade: Grade, onDelete: () -> Unit, showSubject: Boolean = false, onClick: (() -> Unit)? = null) {
    val color = gradeColor(grade.score)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(formatScore(grade.score), color = color, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(grade.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                buildString {
                    if (showSubject) append(grade.subject).append(" · ")
                    append("Peso ").append(formatScore(grade.weight))
                    grade.dateUtc?.let { append(" · ").append(formatDateUtc(it, "d MMM")) }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.DeleteOutline, contentDescription = "Eliminar nota", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Diálogo para añadir una calificación. */
@Composable
internal fun GradeEditorDialog(
    initialSubject: String,
    onDismiss: () -> Unit,
    onConfirm: (subject: String, title: String, score: Float, weight: Float, dateUtc: Long?) -> Unit,
    lockSubject: Boolean = false,
    initial: Grade? = null,
) {
    var subject by rememberSaveable { mutableStateOf(initial?.subject ?: initialSubject) }
    var title by rememberSaveable { mutableStateOf(initial?.title ?: "") }
    var scoreText by rememberSaveable { mutableStateOf(initial?.let { formatScore(it.score) } ?: "") }
    var weightText by rememberSaveable { mutableStateOf(initial?.let { formatScore(it.weight) } ?: "1") }
    var date by rememberSaveable { mutableStateOf(initial?.dateUtc) }
    var showDatePicker by remember { mutableStateOf(false) }

    val score = scoreText.replace(',', '.').toFloatOrNull()
    val weight = weightText.replace(',', '.').toFloatOrNull()
    val scoreValid = score != null && score in 0f..GradeMath.MAX_SCORE
    val weightValid = weight != null && weight > 0f

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { IconBubble(Icons.Filled.Quiz, MaterialTheme.colorScheme.primary) },
        title = { Text(if (initial == null) "Nueva calificación" else "Editar calificación") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Prueba") },
                    placeholder = { Text("Examen T1, práctica 2…") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = scoreText,
                        onValueChange = { scoreText = it.take(5) },
                        label = { Text("Nota (0-10)") },
                        isError = scoreText.isNotEmpty() && !scoreValid,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it.take(5) },
                        label = { Text("Peso") },
                        isError = weightText.isNotEmpty() && !weightValid,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    "El peso es relativo: usa el % de la nota (40, 60…) o 1 si todas cuentan igual.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!lockSubject) {
                    SubjectChips(selected = subject, onSelect = { if (it != null) subject = it })
                }
                TextButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Filled.Event, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(date?.let { formatDateUtc(it, "d MMM yyyy") } ?: "Fecha (opcional)")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank() && scoreValid && weightValid,
                onClick = {
                    if (score != null && weight != null) onConfirm(subject, title, score, weight, date)
                },
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )

    if (showDatePicker) {
        SingleDatePickerDialog(
            initialUtc = date,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                date = it
                showDatePicker = false
            },
        )
    }
}
