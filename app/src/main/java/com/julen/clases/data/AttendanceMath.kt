package com.julen.clases.data

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.floor

/** Nivel de riesgo de una asignatura respecto al límite de faltas. */
enum class RiskLevel { SAFE, WARNING, DANGER }

/** Periodo de evaluación. Las fechas son la medianoche UTC de cada día (formato del DatePicker). */
data class EvaluationPeriod(val startUtc: Long, val endUtc: Long) {
    val isValid: Boolean get() = endUtc >= startUtc
}

data class PeriodProgress(val totalDays: Int, val elapsedDays: Int, val remainingDays: Int) {
    val fraction: Float get() = if (totalDays <= 0) 0f else (elapsedDays.toFloat() / totalDays).coerceIn(0f, 1f)
}

data class SubjectAttendance(
    val code: String,
    val absences: Int,
    val tardies: Int,
    /** Horas lectivas totales de la asignatura en el periodo (0 si no hay periodo). */
    val totalHours: Int,
    /** Faltas máximas permitidas con el límite configurado. */
    val allowedAbsences: Int,
    val percentage: Float,
    val risk: RiskLevel,
) {
    val remainingAbsences: Int get() = allowedAbsences - absences
}

object AttendanceMath {
    const val DAY_MS = 24L * 60 * 60 * 1000
    const val TARDIES_PER_ABSENCE = 3

    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    /** Medianoche UTC correspondiente a la fecha local de [nowMillis]. */
    fun todayUtc(nowMillis: Long, zone: TimeZone = TimeZone.getDefault()): Long {
        val local = Calendar.getInstance(zone).apply { timeInMillis = nowMillis }
        return Calendar.getInstance(utc).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }

    /** Día de la semana (0 = lunes … 6 = domingo) de una fecha a medianoche UTC. */
    fun weekdayOfUtc(dateUtc: Long): Int {
        val cal = Calendar.getInstance(utc).apply { timeInMillis = dateUtc }
        return (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
    }

    /** Horas lectivas reales de [subject] entre ambas fechas (inclusive) según el horario. */
    fun classHoursInPeriod(period: EvaluationPeriod, subject: String): Int {
        if (!period.isValid) return 0
        var hours = 0
        var day = period.startUtc
        while (day <= period.endUtc) {
            val weekday = weekdayOfUtc(day)
            if (weekday < 5) hours += Timetable.hoursOn(weekday, subject)
            day += DAY_MS
        }
        return hours
    }

    fun progress(period: EvaluationPeriod, todayUtc: Long): PeriodProgress {
        val total = ((period.endUtc - period.startUtc) / DAY_MS + 1).toInt().coerceAtLeast(1)
        val elapsed = ((todayUtc - period.startUtc) / DAY_MS + 1).toInt().coerceIn(0, total)
        val remaining = ((period.endUtc - todayUtc) / DAY_MS).toInt().coerceIn(0, total)
        return PeriodProgress(total, elapsed, remaining)
    }

    fun riskFor(percentage: Float, limitPercent: Float): RiskLevel = when {
        percentage >= limitPercent -> RiskLevel.DANGER
        percentage >= limitPercent * 2f / 3f -> RiskLevel.WARNING
        else -> RiskLevel.SAFE
    }

    fun subjectAttendance(
        code: String,
        absences: Int,
        tardies: Int,
        period: EvaluationPeriod?,
        limitPercent: Float,
    ): SubjectAttendance {
        val totalHours = period?.let { classHoursInPeriod(it, code) } ?: 0
        val percentage = if (totalHours > 0) absences * 100f / totalHours else 0f
        val allowed = if (totalHours > 0) floor(totalHours * limitPercent / 100f).toInt() else 0
        val risk = if (totalHours > 0) riskFor(percentage, limitPercent) else RiskLevel.SAFE
        return SubjectAttendance(code, absences, tardies, totalHours, allowed, percentage, risk)
    }
}
