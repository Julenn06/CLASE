package com.julen.clases.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.julen.clases.data.AttendanceMath
import com.julen.clases.data.CalendarMath
import com.julen.clases.ui.capitalized
import com.julen.clases.ui.formatDateUtc
import com.julen.clases.ui.theme.heroGradient

/**
 * Selector de una única fecha con el mismo estilo que el calendario del periodo.
 * [onConfirm] recibe la fecha a medianoche UTC, o null si se pulsa «Sin fecha» ([allowClear]).
 */
@Composable
fun SingleDatePickerDialog(
    initialUtc: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long?) -> Unit,
    allowClear: Boolean = false,
) {
    val todayUtc = remember { AttendanceMath.todayUtc(System.currentTimeMillis()) }
    var selected by rememberSaveable { mutableLongStateOf(initialUtc ?: todayUtc) }
    var shown by rememberSaveable {
        mutableIntStateOf(CalendarMath.yearOf(selected) * 12 + CalendarMath.monthOf(selected) - 1)
    }
    val year = Math.floorDiv(shown, 12)
    val month = Math.floorMod(shown, 12) + 1
    val white = Color.White

    fun select(day: Long) {
        selected = day
        shown = CalendarMath.yearOf(day) * 12 + CalendarMath.monthOf(day) - 1
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(heroGradient()))
                        .padding(horizontal = 22.dp, vertical = 18.dp),
                ) {
                    Text(formatDateUtc(selected, "yyyy"), color = white.copy(alpha = 0.75f), style = MaterialTheme.typography.labelLarge)
                    Text(
                        formatDateUtc(selected, "EEE, d MMM").capitalized(),
                        color = white,
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }

                Row(
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("Hoy" to 0, "Mañana" to 1, "En 1 semana" to 7).forEach { (label, offset) ->
                        AssistChip(
                            onClick = { select(todayUtc + offset * AttendanceMath.DAY_MS) },
                            label = { Text(label) },
                        )
                    }
                }

                Row(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { shown-- }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mes anterior")
                    }
                    Text(
                        formatDateUtc(CalendarMath.date(year, month, 1), "MMMM yyyy").capitalized(),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { shown++ }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Mes siguiente")
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                    WeekdayHeader(Modifier.padding(bottom = 4.dp))
                    MonthGrid(year, month, cellHeight = 42.dp) { day, dayOfMonth, weekday ->
                        val isSelected = day == selected
                        val isToday = day == todayUtc
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .then(
                                    if (isToday && !isSelected) {
                                        Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { selected = day },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                dayOfMonth.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.Medium,
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    weekday >= 5 -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (allowClear) {
                        TextButton(onClick = { onConfirm(null) }) { Text("Sin fecha") }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    TextButton(onClick = { onConfirm(selected) }) { Text("Aceptar", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
