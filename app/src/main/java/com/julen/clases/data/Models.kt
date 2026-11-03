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

enum class TaskKind(val label: String) {
    TASK("Tarea"),
    EXAM("Examen"),
    DELIVERY("Entrega"),
}

data class Task(
    val id: Long,
    val subject: String,
    val title: String,
    /** Fecha límite a medianoche UTC, o null si no tiene. */
    val dueUtc: Long?,
    val done: Boolean = false,
    val kind: TaskKind = TaskKind.TASK,
)

/** Calificación de una prueba. [score] sobre 10 y [weight] como peso relativo (p. ej. % de la nota). */
data class Grade(
    val id: Long,
    val subject: String,
    val title: String,
    val score: Float,
    val weight: Float,
    val dateUtc: Long?,
)

/** Periodo no lectivo (festivo, puente, vacaciones…), fechas a medianoche UTC e inclusivas. */
data class Holiday(
    val id: Long,
    val name: String,
    val startUtc: Long,
    val endUtc: Long,
) {
    operator fun contains(dayUtc: Long): Boolean = dayUtc in startUtc..endUtc
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Color de acento de la interfaz. */
enum class AccentColor(val label: String) {
    INDIGO("Índigo"),
    VIOLET("Violeta"),
    OCEAN("Océano"),
    EMERALD("Esmeralda"),
    SUNSET("Atardecer"),
    ROSE("Rosa"),
}

/** Resultado de añadir un retraso: indica si se ha convertido en falta. */
data class TardyResult(val convertedToAbsence: Boolean)
