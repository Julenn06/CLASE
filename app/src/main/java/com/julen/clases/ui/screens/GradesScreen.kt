package com.julen.clases.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.School
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
import com.julen.clases.data.GradeMath
import com.julen.clases.data.GradeSummary
import com.julen.clases.data.Subjects
import com.julen.clases.ui.Navigator
import com.julen.clases.ui.ShowMessage
import com.julen.clases.ui.components.AppCard
import com.julen.clases.ui.components.EmptyState
import com.julen.clases.ui.components.GradientCard
import com.julen.clases.ui.components.ProgressTrack
import com.julen.clases.ui.components.RingProgress
import com.julen.clases.ui.components.ScreenHeader
import com.julen.clases.ui.components.SectionHeader
import com.julen.clases.ui.components.SubjectBadge
import com.julen.clases.ui.formatScore
import com.julen.clases.ui.plural
import com.julen.clases.ui.theme.AppColors
import com.julen.clases.ui.theme.gradeColor

@Composable
fun GradesScreen(state: UiState, viewModel: AppViewModel, navigator: Navigator, showMessage: ShowMessage) {
    var showEditor by remember { mutableStateOf(false) }
    val withGrades = state.gradeSummaries.filter { it.average != null }
    val withoutGrades = state.gradeSummaries.filter { it.average == null }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { ScreenHeader(title = "Notas", subtitle = "Calificaciones del curso") }
            item { AverageHero(state) }

            if (state.grades.isEmpty()) {
                item {
                    EmptyState(
                        title = "Aún no hay notas",
                        message = "Añade tus calificaciones con su peso y verás la media ponderada de cada asignatura.",
                        icon = Icons.Filled.School,
                    )
                }
            }

            if (withGrades.isNotEmpty()) {
                item { SectionHeader("Por asignatura") }
                items(withGrades.sortedByDescending { it.average }, key = { it.subject }) { summary ->
                    SubjectGradeCard(summary, onClick = { navigator.openSubject(summary.subject) })
                }
            }

            if (withoutGrades.isNotEmpty() && state.grades.isNotEmpty()) {
                item {
                    Text(
                        "Sin notas: " + withoutGrades.joinToString(" · ") { it.subject },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }

            if (state.grades.isNotEmpty()) {
                item { SectionHeader("Últimas notas") }
                item {
                    AppCard(contentPadding = 10.dp) {
                        state.grades.sortedByDescending { it.dateUtc ?: it.id }.take(6).forEach { grade ->
                            GradeRow(grade, showSubject = true, onDelete = {
                                viewModel.deleteGrade(grade.id)
                                showMessage("Nota eliminada", "Deshacer") { viewModel.restoreGrade(grade) }
                            })
                        }
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showEditor = true },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Nota") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        )
    }

    if (showEditor) {
        GradeEditorDialog(
            initialSubject = Subjects.codes.first(),
            onDismiss = { showEditor = false },
            onConfirm = { subject, title, score, weight, date ->
                viewModel.addGrade(subject, title, score, weight, date)
                showEditor = false
                showMessage("Nota guardada en $subject", null, null)
            },
        )
    }
}

@Composable
private fun AverageHero(state: UiState) {
    val average = state.overallAverage
    val passed = state.gradeSummaries.count { it.passed == true }
    val failed = state.gradeSummaries.count { it.passed == false }
    val white = Color.White
    GradientCard(colors = AppColors.CalmGradient) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RingProgress(
                progress = (average ?: 0f) / GradeMath.MAX_SCORE,
                color = white,
                trackColor = white.copy(alpha = 0.2f),
                size = 104.dp,
                strokeWidth = 11.dp,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        average?.let { formatScore((it * 100).toInt() / 100f) } ?: "—",
                        color = white,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                    )
                    Text("media", color = white.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("RESUMEN", color = white.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                Text(plural(state.grades.size, "nota"), color = white, style = MaterialTheme.typography.titleLarge)
                Text("$passed aprobadas · $failed por recuperar", color = white.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SubjectGradeCard(summary: GradeSummary, onClick: () -> Unit) {
    val average = summary.average ?: return
    val color = gradeColor(average)
    AppCard(onClick = onClick, contentPadding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SubjectBadge(summary.subject, size = 42.dp)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(Subjects.nameOf(summary.subject), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    plural(summary.grades.size, "nota") + (summary.best?.let { " · mejor ${formatScore(it)}" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                formatScore((average * 100).toInt() / 100f),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = color,
            )
        }
        Spacer(Modifier.height(12.dp))
        ProgressTrack(fraction = average / GradeMath.MAX_SCORE, color = color, height = 6.dp)
    }
}
