package com.julen.clases.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.julen.clases.data.EvaluationPeriod

/** Selector del rango de fechas del periodo de evaluación. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodPickerDialog(
    initial: EvaluationPeriod?,
    onDismiss: () -> Unit,
    title: String = "Periodo de evaluación",
    onConfirm: (startUtc: Long, endUtc: Long) -> Unit,
) {
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initial?.startUtc,
        initialSelectedEndDateMillis = initial?.endUtc,
    )
    val start = state.selectedStartDateMillis
    val end = state.selectedEndDateMillis
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = start != null && end != null,
                onClick = {
                    if (start != null && end != null) onConfirm(start, end)
                },
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    ) {
        DateRangePicker(
            state = state,
            title = { Text(title, modifier = Modifier.padding(start = 24.dp, top = 16.dp)) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp),
        )
    }
}

/** Selector de una única fecha (fecha límite de una tarea). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleDatePickerDialog(
    initialUtc: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long?) -> Unit,
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initialUtc)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.selectedDateMillis) }) { Text("Aceptar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    ) {
        DatePicker(state = state)
    }
}
