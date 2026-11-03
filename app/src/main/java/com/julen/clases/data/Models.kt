package com.julen.clases.data

enum class EntryType { ABSENCE, TARDY }

/** Registro de una falta o retraso, para poder consultar cuándo ocurrió. */
data class HistoryEntry(
    val subject: String,
    val type: EntryType,
    val timestamp: Long,
    /** true si la falta se generó automáticamente al acumular retrasos. */
    val fromTardies: Boolean = false,
)

data class Task(
    val id: Long,
    val subject: String,
    val title: String,
    /** Fecha límite a medianoche UTC, o null si no tiene. */
    val dueUtc: Long?,
    val done: Boolean = false,
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Resultado de añadir un retraso: indica si se ha convertido en falta. */
data class TardyResult(val convertedToAbsence: Boolean)
