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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.julen.clases.AppViewModel
import com.julen.clases.UiState
import com.julen.clases.data.AttendanceMath
import com.julen.clases.data.EntryType
import com.julen.clases.data.Grade
import com.julen.clases.data.GradeMath
import com.julen.clases.data.Subjects
import com.julen.clases.data.Task
import com.julen.clases.data.Timetable
import com.julen.clases.data.formatMinutes
import com.julen.clases.ui.ShowMessage
import com.julen.clases.ui.capitalized
import com.julen.clases.ui.components.AppCard
import com.julen.clases.ui.components.AppScreen
import com.julen.clases.ui.components.GroupDivider
import com.julen.clases.ui.components.ListGroup
import com.julen.clases.ui.components.ProgressTrack
import com.julen.clases.ui.components.SectionTitle
import com.julen.clases.ui.components.SingleDatePickerDialog
import com.julen.clases.ui.components.Sparkline
import com.julen.clases.ui.components.screenPadding
import com.julen.clases.ui.formatDateUtc
import com.julen.clases.ui.formatLocal
import com.julen.clases.ui.formatPercent
import com.julen.clases.ui.formatScore
import com.julen.clases.ui.plural
import com.julen.clases.ui.theme.AppColors
import com.julen.clases.ui.theme.color
import com.julen.clases.ui.theme.gradeColor
import com.julen.clases.ui.theme.label
import com.julen.clases.ui.theme.subjectBaseColor
import com.julen.clases.ui.theme.subjectColor
import kotlinx.coroutines.delay

@Composable
fun SubjectDetailScreen(
    code: String,
    state: UiState,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    showMessage: ShowMessage,
) {
    val attendance = state.attendanceOf(code)
    val grades = state.gradesOf(code)
    val tasks = state.tasks.filter { it.subject == code }.sortedWith(taskOrder)
    val history = state.history.filter { it.subject == code }
    var showTaskEditor by remember { mutableStateOf(false) }
    var showGradeEditor by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var pastEntry by remember { mutableStateOf<String?>(null) }
    var editingGrade by remember { mutableStateOf<Grade?>(null) }

    AppScreen(title = code, onBack = onBack, edgeToEdgeBottom = true) { inner ->
    LazyColumn(
        modifier = Modifier.imePadding(),
        contentPadding = screenPadding(inner, extraBottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { DetailHeader(code, state) }

        // Asistencia
        if (attendance != null) {
            item {
                SectionTitle("Asistencia")
                AppCard {
                    val hasPeriod = state.period != null
                    val usage = if (attendance.allowedAbsences > 0) attendance.absences.toFloat() / attendance.allowedAbsences else if (attendance.absences > 0 && hasPeriod) 1f else 0f
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            if (hasPeriod) formatPercent(attendance.percentage) else "—",
                            style = MaterialTheme.typography.displaySmall,
                            color = if (hasPeriod) attendance.risk.color() else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "de faltas",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                    if (hasPeriod) {
                        ProgressTrack(fraction = usage, color = attendance.risk.color(), height = 4.dp, modifier = Modifier.padding(vertical = 8.dp))
                    }
                    Row {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            if (hasPeriod) {
                                Text(
                                    when {
                                        attendance.remainingAbsences < 0 -> "Límite superado en ${plural(-attendance.remainingAbsences, "falta")}"
                                        else -> "Te quedan ${plural(attendance.remainingAbsences, "falta")}"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    "Máximo ${attendance.allowedAbsences} de ${attendance.totalHours} h lectivas",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                Text(
                                    "Configura el periodo de evaluación para ver el porcentaje.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Counter(
                            label = "Retrasos",
                            value = "${attendance.tardies}/${AttendanceMath.TARDIES_PER_ABSENCE}",
                            onDecrement = { viewModel.removeTardy(code) },
                            onIncrement = { registerTardy(viewModel, code, showMessage) },
                            decrementEnabled = attendance.tardies > 0,
                            subjectCode = code,
                            modifier = Modifier.weight(1f),
                        )
                        Counter(
                            label = "Faltas",
                            value = attendance.absences.toString(),
                            onDecrement = { viewModel.removeAbsence(code) },
                            onIncrement = { registerAbsence(viewModel, code, showMessage) },
                            decrementEnabled = attendance.absences > 0,
                            subjectCode = code,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(modifier = Modifier.padding(top = 6.dp)) {
                        TextButton(onClick = { pastEntry = "tardy" }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Retraso otro día")
                        }
                        TextButton(onClick = { pastEntry = "absence" }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Falta otro día")
                        }
                    }
                }
            }
        }

        // Calificaciones
        item {
            SectionTitle("Calificaciones", actionLabel = "Añadir", onAction = { showGradeEditor = true })
            AppCard {
                val average = grades?.average
                if (average != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            formatScore((average * 100).toInt() / 100f),
                            style = MaterialTheme.typography.displaySmall,
                            color = gradeColor(average),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (average >= GradeMath.PASS_MARK) "Media ponderada · aprobando" else "Media ponderada · por debajo de 5",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    ProgressTrack(fraction = average / GradeMath.MAX_SCORE, color = gradeColor(average), height = 6.dp)
                    Spacer(Modifier.height(6.dp))
                    val scores = grades.grades.map { it.score }
                    if (scores.size >= 2) {
                        Sparkline(
                            values = scores,
                            color = subjectColor(code),
                            guide = GradeMath.PASS_MARK,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .padding(vertical = 8.dp),
                        )
                    }
                }
                if (grades == null || grades.grades.isEmpty()) {
                    Text("Sin notas todavía.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    TargetCalculator(grades.grades)
                    Spacer(Modifier.height(4.dp))
                    grades.grades.forEach { grade ->
                        GradeRow(grade, onClick = { editingGrade = grade }, onDelete = {
                            viewModel.deleteGrade(grade.id)
                            showMessage("Nota eliminada", "Deshacer") { viewModel.restoreGrade(grade) }
                        })
                    }
                }
            }
        }

        // Tareas
        item {
            Column {
                SectionTitle("Agenda", actionLabel = "Añadir", onAction = { showTaskEditor = true })
                if (tasks.isEmpty()) {
                    Text(
                        "Sin tareas ni exámenes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                } else ListGroup { tasks.forEachIndexed { index, task ->
                    if (index > 0) GroupDivider(inset = 56.dp)
                    TaskRow(
                        task = task,
                        todayUtc = state.todayUtc,
                        showSubject = false,
                        onClick = { editingTask = task },
                        onToggle = { viewModel.toggleTask(task.id) },
                        onDelete = {
                            viewModel.deleteTask(task.id)
                            showMessage("Eliminado: ${task.title}", "Deshacer") { viewModel.restoreTasks(listOf(task)) }
                        },
                    )
                } }
            }
        }

        // Apuntes
        item { NotesCard(code, viewModel) }

        // Historial
        item {
            SectionTitle("Historial de asistencia")
            AppCard {
                if (history.isEmpty()) {
                    Text(
                        "Aquí aparecerán la fecha y hora de cada falta o retraso que anotes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    history.take(30).forEachIndexed { index, entry ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        val isAbsence = entry.type == EntryType.ABSENCE
                        Row(modifier = Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isAbsence) Icons.Filled.EventBusy else Icons.Filled.Timer,
                                contentDescription = null,
                                tint = if (isAbsence) AppColors.Danger else AppColors.Warning,
                            )
                            Spacer(Modifier.width(14.dp))
                            Column {
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
                    }
                }
            }
        }

    }
    }

    pastEntry?.let { kind ->
        SingleDatePickerDialog(
            initialUtc = state.todayUtc - AttendanceMath.DAY_MS,
            onDismiss = { pastEntry = null },
            onConfirm = { date ->
                pastEntry = null
                if (date != null) {
                    val label = formatDateUtc(date, "EEEE d MMM")
                    if (kind == "absence") {
                        viewModel.addAbsenceOn(code, date)
                        showMessage("Falta anotada el $label", "Deshacer") { viewModel.removeAbsence(code) }
                    } else {
                        val result = viewModel.addTardyOn(code, date)
                        showMessage(
                            if (result.convertedToAbsence) "Retraso del $label: suma 1 falta" else "Retraso anotado el $label",
                            "Deshacer",
                        ) { viewModel.undoTardy(code, result) }
                    }
                }
            },
        )
    }

    if (showTaskEditor || editingTask != null) {
        val editing = editingTask
        TaskEditorSheet(
            initial = editing,
            initialSubject = code,
            lockSubject = true,
            onDismiss = {
                showTaskEditor = false
                editingTask = null
            },
            onConfirm = { subject, title, due, kind ->
                if (editing != null) {
                    viewModel.updateTask(editing.copy(title = title.trim(), dueUtc = due, kind = kind))
                } else {
                    viewModel.addTask(subject, title, due, kind)
                }
                showTaskEditor = false
                editingTask = null
            },
        )
    }
    if (showGradeEditor || editingGrade != null) {
        val editing = editingGrade
        GradeEditorDialog(
            initial = editing,
            initialSubject = code,
            lockSubject = true,
            onDismiss = {
                showGradeEditor = false
                editingGrade = null
            },
            onConfirm = { subject, title, score, weight, date ->
                if (editing != null) {
                    viewModel.updateGrade(editing.copy(title = title.trim(), score = score, weight = weight, dateUtc = date))
                } else {
                    viewModel.addGrade(subject, title, score, weight, date)
                }
                showGradeEditor = false
                editingGrade = null
            },
        )
    }
}

@Composable
private fun DetailHeader(code: String, state: UiState) {
    val color = subjectColor(code)
    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(Subjects.nameOf(code), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        Text(
            buildString {
                append(Timetable.weeklyHours(code)).append(" h a la semana")
                state.today?.let { day ->
                    Timetable.blocksFor(day).firstOrNull { it.subject == code }?.let { append(" · hoy a las ").append(formatMinutes(it.start)) }
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Timetable.dayShortNames.forEachIndexed { day, short ->
                val hours = Timetable.hoursOn(day, code)
                val start = Timetable.blocksFor(day).firstOrNull { it.subject == code }?.start
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (hours > 0) color.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(short, style = MaterialTheme.typography.labelLarge, color = if (hours > 0) color else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (hours > 0 && start != null) formatMinutes(start) else "—",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Apuntes de la asignatura con autoguardado. */
@Composable
private fun NotesCard(code: String, viewModel: AppViewModel) {
    var text by rememberSaveable(code) { mutableStateOf(viewModel.getNote(code)) }
    var saved by remember(code) { mutableStateOf(true) }

    LaunchedEffect(code, text) {
        if (saved) return@LaunchedEffect
        delay(500)
        viewModel.saveNote(code, text)
        saved = true
    }
    DisposableEffect(code) {
        onDispose { if (!saved) viewModel.saveNote(code, text) }
    }

    SectionTitle("Apuntes")
    AppCard {
        Text(
            if (saved) "Se guarda solo" else "Guardando…",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                saved = false
            },
            placeholder = { Text("Dudas para clase, criterios de evaluación, enlaces…") },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 140.dp),
        )
    }
}

/** "¿Qué nota necesito?": calcula la media necesaria en lo que queda para llegar al objetivo. */
@Composable
private fun TargetCalculator(grades: List<com.julen.clases.data.Grade>) {
    var targetText by rememberSaveable { mutableStateOf("5") }
    var remainingText by rememberSaveable(grades.size) {
        mutableStateOf(GradeMath.remainingPercentWeight(grades)?.let { formatScore(it) } ?: "")
    }
    val target = targetText.replace(',', '.').toFloatOrNull()
    val remaining = remainingText.replace(',', '.').toFloatOrNull()
    val needed = if (target != null && remaining != null) GradeMath.requiredScore(grades, target, remaining) else null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("¿Qué nota necesito?", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = targetText,
                onValueChange = { targetText = it.take(4) },
                label = { Text("Objetivo") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = remainingText,
                onValueChange = { remainingText = it.take(5) },
                label = { Text("Peso que queda") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f),
            )
        }
        val (message, color) = when {
            needed == null -> "Indica el peso de las pruebas que faltan (mismas unidades que tus pesos)." to MaterialTheme.colorScheme.onSurfaceVariant
            needed <= 0f -> "¡Ya lo tienes! Aunque saques un 0 llegarías a ${targetText}." to AppColors.Safe
            needed > GradeMath.MAX_SCORE -> "Necesitarías un ${formatScore((needed * 10).toInt() / 10f)}: no es alcanzable con ese peso." to AppColors.Danger
            else -> "Necesitas una media de ${formatScore(kotlin.math.ceil(needed * 10f) / 10f)} en lo que queda." to gradeColor(needed)
        }
        Text(message, style = MaterialTheme.typography.bodyMedium, color = color, fontWeight = FontWeight.SemiBold)
    }
}
