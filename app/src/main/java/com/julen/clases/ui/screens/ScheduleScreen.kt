package com.julen.clases.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
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
import com.julen.clases.UiState
import com.julen.clases.data.Subjects
import com.julen.clases.data.Timetable
import com.julen.clases.data.formatMinutes
import com.julen.clases.ui.components.AppCard
import com.julen.clases.ui.components.ScreenHeader
import com.julen.clases.ui.components.SectionLabel
import com.julen.clases.ui.components.SubjectBadge
import com.julen.clases.ui.durationText
import com.julen.clases.ui.theme.subjectColor
import java.util.Calendar

@Composable
fun ScheduleScreen(state: UiState) {
    var weekView by rememberSaveable { mutableStateOf(false) }
    var selectedDay by rememberSaveable { mutableIntStateOf(state.today ?: 0) }
    val minute = remember(state.nowMillis) {
        Calendar.getInstance().apply { timeInMillis = state.nowMillis }.let {
            it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ScreenHeader(title = "Horario", subtitle = "Semana lectiva")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !weekView, onClick = { weekView = false }, label = { Text("Por día") })
                FilterChip(selected = weekView, onClick = { weekView = true }, label = { Text("Semana") })
            }
        }

        if (!weekView) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Timetable.dayShortNames.forEachIndexed { index, short ->
                        DayChip(
                            text = short,
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
                val hours = blocks.filter { !it.isBreak }.sumOf { it.durationMinutes } / 60
                AppCard {
                    SectionLabel(Timetable.dayNames[selectedDay]) {
                        Text(
                            "${formatMinutes(blocks.first().start)} – ${formatMinutes(blocks.last().end)} · $hours h",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    blocks.forEach { block ->
                        TimelineRow(block = block, minuteOfDay = if (selectedDay == state.today) minute else null)
                    }
                }
            }
        } else {
            item { WeekGrid(today = state.today, minuteOfDay = minute) }
        }

        item {
            AppCard {
                SectionLabel("Asignaturas")
                Subjects.all.forEach { subject ->
                    Row(
                        modifier = Modifier.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SubjectBadge(subject.code, size = 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            subject.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${Timetable.weeklyHours(subject.code)} h/sem",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayChip(
    text: String,
    selected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) scheme.primary else scheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = if (selected) scheme.onPrimary else scheme.onSurface,
        )
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .width(16.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
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
private fun WeekGrid(today: Int?, minuteOfDay: Int) {
    val scheme = MaterialTheme.colorScheme
    AppCard(contentPadding = 12.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Timetable.slots.forEachIndexed { slotIndex, slot ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                                .clip(RoundedCornerShape(8.dp))
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
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(color.copy(alpha = 0.16f))
                                    .then(
                                        if (isNow) Modifier.border(2.dp, color, RoundedCornerShape(10.dp)) else Modifier
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    subject.orEmpty(),
                                    color = color,
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
