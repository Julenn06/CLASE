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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.julen.clases.UiState
import com.julen.clases.data.Subjects
import com.julen.clases.data.Timetable
import com.julen.clases.data.formatMinutes
import com.julen.clases.ui.Navigator
import com.julen.clases.ui.components.AppCard
import com.julen.clases.ui.components.ScreenHeader
import com.julen.clases.ui.components.SectionHeader
import com.julen.clases.ui.components.SegmentedControl
import com.julen.clases.ui.components.SubjectBadge
import com.julen.clases.ui.durationText
import com.julen.clases.ui.theme.subjectColor

@Composable
fun ScheduleScreen(state: UiState, navigator: Navigator) {
    var mode by rememberSaveable { mutableIntStateOf(0) }
    var selectedDay by rememberSaveable { mutableIntStateOf(state.today ?: 0) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ScreenHeader(title = "Horario", subtitle = "Semana lectiva")
            SegmentedControl(
                options = listOf("Por día", "Semana"),
                selectedIndex = mode,
                onSelect = { mode = it },
            )
        }

        if (mode == 0) {
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
                AppCard(contentPadding = 14.dp) {
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
            item { WeekGrid(today = state.today, minuteOfDay = state.minuteOfDay, onOpenSubject = navigator.openSubject) }
        }

        item { SectionHeader("Asignaturas") }
        items(Subjects.all, key = { it.code }) { subject ->
            AppCard(onClick = { navigator.openSubject(subject.code) }, contentPadding = 14.dp) {
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
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
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
private fun WeekGrid(today: Int?, minuteOfDay: Int, onOpenSubject: (String) -> Unit) {
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
                                    .background(if (isNow) color else color.copy(alpha = 0.15f))
                                    .clickable(enabled = subject != null) { subject?.let(onOpenSubject) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    subject.orEmpty(),
                                    color = if (isNow) Color.White else color,
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
