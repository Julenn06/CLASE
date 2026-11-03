package com.julen.clases.data

import java.util.Calendar
import java.util.TimeZone

/** Próximo momento en el que la app tiene que hacer algo en segundo plano. */
data class PlannedEvent(
    val type: Type,
    val atMillis: Long,
    /** Asignatura y franja de la clase (solo para [Type.CLASS]). */
    val subject: String? = null,
    val blockStart: Int = 0,
    val blockEnd: Int = 0,
) {
    enum class Type {
        /** Aviso de que empieza una clase. */
        CLASS,

        /** Resumen de lo que hay al día siguiente. */
        SUMMARY,

        /** Solo refrescar el widget (cambio de clase). */
        REFRESH,
    }
}

object ReminderPlanner {

    data class Settings(
        val classReminder: Boolean,
        val leadMinutes: Int,
        val dailySummary: Boolean,
        val summaryHour: Int,
        /** Programar también los cambios de clase para mantener el widget al día. */
        val refreshWidget: Boolean,
    ) {
        val anything: Boolean get() = classReminder || dailySummary || refreshWidget
    }

    /** Busca el siguiente evento a partir de [nowMillis] en los próximos 8 días. */
    fun nextEvent(
        nowMillis: Long,
        settings: Settings,
        holidays: List<Holiday>,
        zone: TimeZone = TimeZone.getDefault(),
    ): PlannedEvent? {
        if (!settings.anything) return null
        val threshold = nowMillis + 15_000
        val candidates = mutableListOf<PlannedEvent>()

        val midnight = Calendar.getInstance(zone).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        for (offset in 0..7) {
            val day = (midnight.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, offset) }
            val dayStart = day.timeInMillis
            fun at(minuteOfDay: Int): Long = (day.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
                set(Calendar.MINUTE, minuteOfDay % 60)
            }.timeInMillis

            if (settings.dailySummary) {
                candidates += PlannedEvent(PlannedEvent.Type.SUMMARY, at(settings.summaryHour * 60))
            }

            val weekday = (day.get(Calendar.DAY_OF_WEEK) + 5) % 7
            val dateUtc = AttendanceMath.todayUtc(dayStart, zone)
            if (weekday < Timetable.DAYS && holidays.none { dateUtc in it }) {
                Timetable.blocksFor(weekday).forEach { block ->
                    if (settings.classReminder && block.subject != null && !block.isBreak) {
                        candidates += PlannedEvent(
                            PlannedEvent.Type.CLASS,
                            at(block.start) - settings.leadMinutes * 60_000L,
                            block.subject,
                            block.start,
                            block.end,
                        )
                    }
                    if (settings.refreshWidget) {
                        candidates += PlannedEvent(PlannedEvent.Type.REFRESH, at(block.start))
                        candidates += PlannedEvent(PlannedEvent.Type.REFRESH, at(block.end))
                    }
                }
            }

            // Lo más cercano de este día ya basta: los días siguientes solo pueden ser posteriores.
            val best = candidates.filter { it.atMillis > threshold }.minByOrNull { it.atMillis }
            if (best != null && best.atMillis < at(0) + AttendanceMath.DAY_MS) {
                // Si coinciden, el aviso de clase o el resumen tienen prioridad sobre el refresco.
                return candidates
                    .filter { it.atMillis == best.atMillis }
                    .minByOrNull { it.type.ordinal }
            }
        }
        return candidates.filter { it.atMillis > threshold }.minByOrNull { it.atMillis }
    }
}
