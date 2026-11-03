package com.julen.clases.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.julen.clases.data.AttendanceMath
import com.julen.clases.data.CalendarMath
import com.julen.clases.data.EvaluationPeriod
import com.julen.clases.data.Holiday
import com.julen.clases.ui.capitalized
import com.julen.clases.ui.formatDateUtc
import com.julen.clases.ui.plural
import com.julen.clases.ui.theme.AppColors
import com.julen.clases.ui.theme.heroGradient
import kotlinx.coroutines.launch

private val WeekdayInitials = listOf("L", "M", "X", "J", "V", "S", "D")
private const val MONTHS_BEFORE = 12
private const val MONTHS_AFTER = 24

/** Cabecera con las iniciales de los días (lunes primero). */
@Composable
fun WeekdayHeader(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth()) {
        WeekdayInitials.forEachIndexed { index, initial ->
            Text(
                initial,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = if (index >= 5) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

/**
 * Cuadrícula de un mes (semanas de lunes a domingo). [dayCell] dibuja cada día; los huecos
 * antes del día 1 y después del último quedan vacíos.
 */
@Composable
fun MonthGrid(
    year: Int,
    month: Int,
    modifier: Modifier = Modifier,
    cellHeight: androidx.compose.ui.unit.Dp = 46.dp,
    dayCell: @Composable (dayUtc: Long, dayOfMonth: Int, weekday: Int) -> Unit,
) {
    val offset = CalendarMath.firstWeekdayOfMonth(year, month)
    val days = CalendarMath.daysInMonth(year, month)
    val weeks = (offset + days + 6) / 7
    val first = CalendarMath.date(year, month, 1)
    Column(modifier = modifier.fillMaxWidth()) {
        for (week in 0 until weeks) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (column in 0 until 7) {
                    val dayOfMonth = week * 7 + column - offset + 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(cellHeight),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (dayOfMonth in 1..days) {
                            dayCell(first + (dayOfMonth - 1) * AttendanceMath.DAY_MS, dayOfMonth, column)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Selector de rango de fechas a pantalla completa: atajos de evaluación, resumen en directo
 * (semanas, días lectivos y horas de clase) y meses con el rango dibujado como una banda.
 */
@Composable
fun PeriodPickerDialog(
    initial: EvaluationPeriod?,
    onDismiss: () -> Unit,
    title: String = "Periodo de evaluación",
    holidays: List<Holiday> = emptyList(),
    showPresets: Boolean = true,
    onConfirm: (startUtc: Long, endUtc: Long) -> Unit,
) {
    val todayUtc = remember { AttendanceMath.todayUtc(System.currentTimeMillis()) }
    var start by rememberSaveable { mutableStateOf(initial?.startUtc) }
    var end by rememberSaveable { mutableStateOf(initial?.endUtc) }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Meses visibles, centrados en el inicio elegido o en el mes actual.
    val anchor = initial?.startUtc ?: todayUtc
    val anchorYear = CalendarMath.yearOf(anchor)
    val anchorMonth = CalendarMath.monthOf(anchor)
    val months = remember(anchorYear, anchorMonth) {
        (-MONTHS_BEFORE..MONTHS_AFTER).map { CalendarMath.addMonths(anchorYear, anchorMonth, it) }
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = MONTHS_BEFORE)

    fun scrollTo(dateUtc: Long) {
        val target = CalendarMath.yearOf(dateUtc) to CalendarMath.monthOf(dateUtc)
        val index = months.indexOf(target)
        if (index >= 0) scope.launch { listState.animateScrollToItem(index) }
    }

    fun onDayTap(day: Long) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val currentStart = start
        when {
            currentStart == null || end != null -> {
                start = day
                end = null
            }
            day < currentStart -> start = day
            else -> end = day
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                RangeHeader(
                    title = title,
                    start = start,
                    end = end,
                    holidays = holidays,
                    onClose = onDismiss,
                )

                if (showPresets) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 14.dp),
                    ) {
                        items(CalendarMath.academicPresets(todayUtc)) { preset ->
                            val selected = start == preset.startUtc && end == preset.endUtc
                            PresetChip(
                                label = preset.label,
                                caption = "${formatDateUtc(preset.startUtc, "d MMM")} – ${formatDateUtc(preset.endUtc, "d MMM")}",
                                selected = selected,
                                onClick = {
                                    start = preset.startUtc
                                    end = preset.endUtc
                                    scrollTo(preset.startUtc)
                                },
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 12.dp, top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        when {
                            start == null -> "Toca el primer día"
                            end == null -> "Ahora toca el último día"
                            else -> "Toca otra fecha para empezar de nuevo"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { scrollTo(todayUtc) }) {
                        Icon(Icons.Filled.Today, contentDescription = "Ir a hoy", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                WeekdayHeader(Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                ) {
                    items(months, key = { "${it.first}-${it.second}" }) { (year, month) ->
                        Column(modifier = Modifier.padding(top = 14.dp)) {
                            Text(
                                formatDateUtc(CalendarMath.date(year, month, 1), "MMMM yyyy").capitalized(),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
                            )
                            MonthGrid(year, month) { day, dayOfMonth, weekday ->
                                RangeDayCell(
                                    dayOfMonth = dayOfMonth,
                                    weekday = weekday,
                                    isToday = day == todayUtc,
                                    isHoliday = holidays.any { day in it },
                                    start = start,
                                    end = end,
                                    day = day,
                                    onClick = { onDayTap(day) },
                                )
                            }
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = 8.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),
                        ) { Text("Cancelar") }
                        val s = start
                        val e = end
                        Button(
                            onClick = { if (s != null && e != null) onConfirm(s, e) },
                            enabled = s != null && e != null,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1.4f)
                                .height(54.dp),
                        ) {
                            Text("Guardar", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RangeHeader(
    title: String,
    start: Long?,
    end: Long?,
    holidays: List<Holiday>,
    onClose: () -> Unit,
) {
    val white = Color.White
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(heroGradient().first()),
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 8.dp, end = 20.dp, top = 4.dp, bottom = 22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose, colors = IconButtonDefaults.iconButtonColors(contentColor = white)) {
                    Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                }
                Text(title, color = white, style = MaterialTheme.typography.titleLarge)
            }
            Row(
                modifier = Modifier.padding(start = 12.dp, top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DateBox("Inicio", start, Modifier.weight(1f))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = white.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 10.dp),
                )
                DateBox("Fin", end, Modifier.weight(1f))
            }
            AnimatedContent(
                targetState = if (start != null && end != null) start to end else null,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "summary",
                modifier = Modifier.padding(start = 12.dp, top = 14.dp),
            ) { range ->
                if (range == null) {
                    Text(
                        "Elige el primer y el último día de clase de la evaluación.",
                        color = white.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    val (s, e) = range
                    val days = CalendarMath.daysBetweenInclusive(s, e)
                    val school = CalendarMath.schoolDays(s, e, holidays)
                    val hours = CalendarMath.classHours(s, e, holidays)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SummaryChip(plural((days + 6) / 7, "semana"))
                        SummaryChip(plural(school, "día lectivo", "días lectivos"))
                        SummaryChip("$hours h de clase")
                    }
                }
            }
            if (start != null && end != null) {
                val lost = CalendarMath.schoolDays(start, end, emptyList()) - CalendarMath.schoolDays(start, end, holidays)
                if (lost > 0) {
                    Text(
                        plural(lost, "día festivo descontado", "días festivos descontados"),
                        color = white.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 12.dp, top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DateBox(label: String, dateUtc: Long?, modifier: Modifier = Modifier) {
    val white = Color.White
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(white.copy(alpha = if (dateUtc != null) 0.18f else 0.08f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(label.uppercase(), color = white.copy(alpha = 0.75f), style = MaterialTheme.typography.labelSmall)
        AnimatedContent(targetState = dateUtc, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "date") { date ->
            if (date == null) {
                Text("—", color = white, style = MaterialTheme.typography.headlineSmall)
            } else {
                Column {
                    Text(formatDateUtc(date, "d MMM"), color = white, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Text(formatDateUtc(date, "EEEE yyyy").capitalized(), color = white.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun SummaryChip(text: String) {
    Text(
        text,
        color = Color.White,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun PresetChip(label: String, caption: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val background by animateColorAsState(if (selected) scheme.primary else scheme.surfaceContainerLow, label = "preset")
    val content by animateColorAsState(if (selected) scheme.onPrimary else scheme.onSurface, label = "presetText")
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .border(1.dp, if (selected) Color.Transparent else scheme.outlineVariant, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = if (selected) content else scheme.primary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, color = content, style = MaterialTheme.typography.labelLarge)
            Text(caption, color = content.copy(alpha = 0.75f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun RangeDayCell(
    dayOfMonth: Int,
    weekday: Int,
    isToday: Boolean,
    isHoliday: Boolean,
    start: Long?,
    end: Long?,
    day: Long,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val isStart = start == day
    val inRange = start != null && end != null && day in start..end
    val isEndpoint = isStart || (end == day)
    val bandColor = scheme.primary.copy(alpha = 0.16f)
    val weekend = weekday >= 5

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                if (inRange && start != end) {
                    val left = if (isStart) size.width / 2 else 0f
                    val right = if (end == day) size.width / 2 else size.width
                    val top = size.height * 0.1f
                    val height = size.height * 0.8f
                    // Extremos redondeados al principio y final de cada semana.
                    val roundLeft = weekday == 0 && !isStart
                    val roundRight = weekday == 6 && end != day
                    if (roundLeft || roundRight) {
                        drawRoundRect(
                            bandColor,
                            topLeft = Offset(left, top),
                            size = Size(right - left, height),
                            cornerRadius = CornerRadius(height / 2, height / 2),
                        )
                    } else {
                        drawRect(bandColor, topLeft = Offset(left, top), size = Size(right - left, height))
                    }
                }
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isEndpoint) scheme.primary else Color.Transparent)
                .then(if (isToday && !isEndpoint) Modifier.border(1.5.dp, scheme.primary, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isEndpoint || isToday) FontWeight.ExtraBold else FontWeight.Medium,
                color = when {
                    isEndpoint -> scheme.onPrimary
                    weekend -> scheme.onSurfaceVariant.copy(alpha = 0.5f)
                    else -> scheme.onSurface
                },
            )
            if (isHoliday) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp)
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(if (isEndpoint) scheme.onPrimary else AppColors.Safe),
                )
            }
        }
    }
}
