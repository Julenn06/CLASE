package com.julen.clases.data

import java.util.Calendar
import java.util.TimeZone

/** Utilidades de calendario sobre fechas a medianoche UTC (el formato de todas las fechas guardadas). */
object CalendarMath {
    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    private fun calendar(): Calendar = Calendar.getInstance(utc).apply { clear() }

    /** Medianoche UTC del día [day] del mes [month] (1-12) de [year]. */
    fun date(year: Int, month: Int, day: Int): Long =
        calendar().apply { set(year, month - 1, day) }.timeInMillis

    fun yearOf(dateUtc: Long): Int = calendar().apply { timeInMillis = dateUtc }.get(Calendar.YEAR)

    /** Mes 1-12. */
    fun monthOf(dateUtc: Long): Int = calendar().apply { timeInMillis = dateUtc }.get(Calendar.MONTH) + 1

    fun dayOf(dateUtc: Long): Int = calendar().apply { timeInMillis = dateUtc }.get(Calendar.DAY_OF_MONTH)

    fun daysInMonth(year: Int, month: Int): Int =
        calendar().apply { set(year, month - 1, 1) }.getActualMaximum(Calendar.DAY_OF_MONTH)

    /** Columna (0 = lunes) en la que cae el día 1 del mes. */
    fun firstWeekdayOfMonth(year: Int, month: Int): Int = AttendanceMath.weekdayOfUtc(date(year, month, 1))

    /** Desplaza un (año, mes) [delta] meses. */
    fun addMonths(year: Int, month: Int, delta: Int): Pair<Int, Int> {
        val index = year * 12 + (month - 1) + delta
        return Math.floorDiv(index, 12) to Math.floorMod(index, 12) + 1
    }

    fun daysBetweenInclusive(startUtc: Long, endUtc: Long): Int =
        ((endUtc - startUtc) / AttendanceMath.DAY_MS).toInt() + 1

    /** Días de lunes a viernes del rango que no son festivos. */
    fun schoolDays(startUtc: Long, endUtc: Long, holidays: List<Holiday>): Int {
        var count = 0
        var day = startUtc
        while (day <= endUtc) {
            if (AttendanceMath.weekdayOfUtc(day) < 5 && holidays.none { day in it }) count++
            day += AttendanceMath.DAY_MS
        }
        return count
    }

    /** Horas de clase de todas las asignaturas en el rango, según el horario y sin festivos. */
    fun classHours(startUtc: Long, endUtc: Long, holidays: List<Holiday>): Int {
        val period = EvaluationPeriod(startUtc, endUtc)
        return Subjects.codes.sumOf { AttendanceMath.classHoursInPeriod(period, it, holidays) }
    }

    data class Preset(val label: String, val startUtc: Long, val endUtc: Long)

    /**
     * Evaluaciones típicas del curso escolar español que contiene [todayUtc]
     * (el curso empieza en septiembre). Son aproximadas: se pueden ajustar tocando el calendario.
     */
    fun academicPresets(todayUtc: Long): List<Preset> {
        val year = yearOf(todayUtc)
        val startYear = if (monthOf(todayUtc) >= 8) year else year - 1
        val next = startYear + 1
        return listOf(
            Preset("1ª evaluación", date(startYear, 9, 8), date(startYear, 12, 19)),
            Preset("2ª evaluación", date(next, 1, 7), date(next, 3, 27)),
            Preset("3ª evaluación", date(next, 4, 7), date(next, 6, 19)),
            Preset("Curso completo", date(startYear, 9, 8), date(next, 6, 19)),
        )
    }
}
