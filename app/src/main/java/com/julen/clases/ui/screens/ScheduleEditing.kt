package com.julen.clases.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.julen.clases.data.Subject
import com.julen.clases.data.Subjects
import com.julen.clases.data.Timetable
import com.julen.clases.data.formatMinutes
import com.julen.clases.ui.components.IconBubble
import com.julen.clases.ui.components.SubjectBadge
import com.julen.clases.ui.theme.AppColors
import com.julen.clases.ui.theme.SubjectPalette
import com.julen.clases.ui.theme.isDark

/** Elige qué asignatura hay en una hora concreta del horario. */
@Composable
internal fun SubjectPickerDialog(
    day: Int,
    slotIndex: Int,
    current: String?,
    onDismiss: () -> Unit,
    onPick: (String?) -> Unit,
) {
    val slot = Timetable.slots[slotIndex]
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { IconBubble(Icons.Filled.EditCalendar, MaterialTheme.colorScheme.primary) },
        title = { Text("${Timetable.dayNames[day]} · ${formatMinutes(slot.start)}") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                items(Subjects.all, key = { it.code }) { subject ->
                    PickerRow(selected = subject.code == current, onClick = { onPick(subject.code) }) {
                        SubjectBadge(subject.code, size = 36.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            subject.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                item {
                    PickerRow(selected = current == null, onClick = { onPick(null) }) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.DoNotDisturbOn, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text("Hora libre", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
    )
}

@Composable
private fun PickerRow(selected: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
        if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

/** Crea o edita una asignatura (el código no se puede cambiar porque identifica sus datos). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SubjectEditorDialog(
    initial: Subject?,
    onDismiss: () -> Unit,
    onSave: (Subject) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val isNew = initial == null
    var code by rememberSaveable { mutableStateOf(initial?.code ?: "") }
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var colorIndex by rememberSaveable {
        mutableIntStateOf(initial?.colorIndex ?: (Subjects.all.size % SubjectPalette.size))
    }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    val cleanCode = code.trim().uppercase()
    val codeTaken = isNew && Subjects.codes.any { it == cleanCode }
    val codeValid = cleanCode.matches(Regex("[A-Z0-9ÁÉÍÓÚÑ]{1,6}")) && !codeTaken
    val dark = isDark()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Nueva asignatura" else "Editar ${initial?.code}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.take(6) },
                    enabled = isNew,
                    label = { Text("Código (p. ej. PMDM)") },
                    isError = code.isNotEmpty() && !codeValid,
                    supportingText = {
                        Text(
                            when {
                                codeTaken -> "Ya existe una asignatura con ese código"
                                isNew -> "Hasta 6 letras o números"
                                else -> "El código no se puede cambiar"
                            }
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(50) },
                    label = { Text("Nombre completo") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Color", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SubjectPalette.forEachIndexed { index, pair ->
                        val color = if (dark) pair.second else pair.first
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (index == colorIndex) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape,
                                )
                                .clickable { colorIndex = index },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (index == colorIndex) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                if (onDelete != null) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text("Eliminar asignatura", color = AppColors.Danger)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = (if (isNew) codeValid else true) && name.isNotBlank(),
                onClick = { onSave(Subject(if (isNew) cleanCode else initial!!.code, name.trim(), colorIndex)) },
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("¿Eliminar ${initial?.code}?") },
            text = {
                Text("Sus horas quedarán libres en el horario. Las faltas, notas y tareas guardadas no se borran, pero dejarán de mostrarse.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Eliminar", color = AppColors.Danger) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } },
        )
    }
}
