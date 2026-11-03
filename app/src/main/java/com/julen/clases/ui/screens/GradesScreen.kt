package com.julen.clases.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.julen.clases.AppViewModel
import com.julen.clases.UiState
import com.julen.clases.data.Grade
import com.julen.clases.data.GradeMath
import com.julen.clases.data.GradeSummary
import com.julen.clases.data.Subjects
import com.julen.clases.ui.Navigator
import com.julen.clases.ui.ShowMessage
import com.julen.clases.ui.components.AppScreen
import com.julen.clases.ui.components.EmptyState
import com.julen.clases.ui.components.GroupDivider
import com.julen.clases.ui.components.ListGroup
import com.julen.clases.ui.components.SectionTitle
import com.julen.clases.ui.components.Sparkline
import com.julen.clases.ui.components.SubjectBadge
import com.julen.clases.ui.components.screenPadding
import com.julen.clases.ui.formatScore
import com.julen.clases.ui.plural
import com.julen.clases.ui.theme.AppColors
import com.julen.clases.ui.theme.gradeColor
import com.julen.clases.ui.theme.subjectColor

@Composable
fun GradesScreen(state: UiState, viewModel: AppViewModel, navigator: Navigator, showMessage: ShowMessage) {
    var showEditor by remember { mutableStateOf(false) }
    var editingGrade by remember { mutableStateOf<Grade?>(null) }
    val withGrades = state.gradeSummaries.filter { it.average != null }
    val withoutGrades = state.gradeSummaries.filter { it.average == null }

    AppScreen(
        title = "Notas",
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showEditor = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Nota") },
            )
        },
    ) { inner ->
        LazyColumn(contentPadding = screenPadding(inner, extraBottom = 96.dp)) {
            item { AverageOverview(state) }

            if (state.grades.isEmpty()) {
                item {
                    EmptyState(
                        title = "Aún no hay notas",
                        message = "Añade tus calificaciones con su peso y verás la media ponderada de cada asignatura.",
                        icon = Icons.Outlined.School,
                    )
                }
            }

            if (withGrades.isNotEmpty()) {
                item { SectionTitle("Por asignatura") }
                item {
                    ListGroup {
                        withGrades.sortedByDescending { it.average }.forEachIndexed { index, summary ->
                            if (index > 0) GroupDivider(inset = 72.dp)
                            SubjectGradeRow(summary, onClick = { navigator.openSubject(summary.subject) })
                        }
                    }
                }
                if (withoutGrades.isNotEmpty()) {
                    item {
                        Text(
                            "Sin notas: " + withoutGrades.joinToString(" · ") { it.subject },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                        )
                    }
                }
            }

            if (state.grades.isNotEmpty()) {
                item { SectionTitle("Últimas notas") }
                item {
                    ListGroup {
                        state.grades.sortedByDescending { it.dateUtc ?: it.id }.take(8).forEachIndexed { index, grade ->
                            if (index > 0) GroupDivider(inset = 76.dp)
                            GradeRow(grade, showSubject = true, onClick = { editingGrade = grade }, onDelete = {
                                viewModel.deleteGrade(grade.id)
                                showMessage("Nota eliminada", "Deshacer") { viewModel.restoreGrade(grade) }
                            })
                        }
                    }
                }
            }
        }
    }

    if (showEditor || editingGrade != null) {
        val editing = editingGrade
        GradeEditorDialog(
            initial = editing,
            initialSubject = Subjects.codes.firstOrNull() ?: "",
            onDismiss = {
                showEditor = false
                editingGrade = null
            },
            onConfirm = { subject, title, score, weight, date ->
                if (editing != null) {
                    viewModel.updateGrade(editing.copy(subject = subject, title = title.trim(), score = score, weight = weight, dateUtc = date))
                } else {
                    viewModel.addGrade(subject, title, score, weight, date)
                }
                showEditor = false
                editingGrade = null
                showMessage("Nota guardada en $subject", null, null)
            },
        )
    }
}

@Composable
private fun AverageOverview(state: UiState) {
    val average = state.overallAverage
    val passed = state.gradeSummaries.count { it.passed == true }
    val failed = state.gradeSummaries.count { it.passed == false }
    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                average?.let { formatScore((it * 100).toInt() / 100f) } ?: "—",
                style = MaterialTheme.typography.displayMedium,
                color = if (average == null) MaterialTheme.colorScheme.onSurfaceVariant else gradeColor(average),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "de media",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }
        Text(
            "${plural(state.grades.size, "nota")} · $passed aprobadas · $failed por debajo de 5",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SubjectGradeRow(summary: GradeSummary, onClick: () -> Unit) {
    val average = summary.average ?: return
    val color = gradeColor(average)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SubjectBadge(summary.subject)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(Subjects.nameOf(summary.subject), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    plural(summary.grades.size, "nota") + (summary.best?.let { " · mejor ${formatScore(it)}" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(formatScore((average * 100).toInt() / 100f), style = MaterialTheme.typography.titleLarge, color = color)
        }
        if (summary.grades.size >= 2) {
            Sparkline(
                values = summary.grades.map { it.score },
                color = subjectColor(summary.subject),
                guide = GradeMath.PASS_MARK,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(start = 56.dp, top = 10.dp),
            )
        }
    }
}
