package com.julen.clases.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class AttendanceMathTest {

    private fun utc(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month - 1, day)
        }.timeInMillis

    @Test
    fun weekdayOfUtcStartsOnMonday() {
        assertEquals(0, AttendanceMath.weekdayOfUtc(utc(2026, 11, 2))) // lunes
        assertEquals(4, AttendanceMath.weekdayOfUtc(utc(2026, 11, 6))) // viernes
        assertEquals(6, AttendanceMath.weekdayOfUtc(utc(2026, 11, 8))) // domingo
    }

    @Test
    fun classHoursCountOnlySchoolDays() {
        // Dos semanas completas (lunes 2 nov – domingo 15 nov).
        val period = EvaluationPeriod(utc(2026, 11, 2), utc(2026, 11, 15))
        assertEquals(10, AttendanceMath.classHoursInPeriod(period, "SI"))
        assertEquals(6, AttendanceMath.classHoursInPeriod(period, "PMDM"))
        // Solo el martes 3: 3 h de PMDM, 0 de SI.
        val tuesday = EvaluationPeriod(utc(2026, 11, 3), utc(2026, 11, 3))
        assertEquals(3, AttendanceMath.classHoursInPeriod(tuesday, "PMDM"))
        assertEquals(0, AttendanceMath.classHoursInPeriod(tuesday, "SI"))
    }

    @Test
    fun allowedAbsencesAndRisk() {
        // 10 semanas → 50 h de SI; con 15 % se permiten 7 faltas.
        val period = EvaluationPeriod(utc(2026, 9, 7), utc(2026, 11, 15))
        val ok = AttendanceMath.subjectAttendance("SI", absences = 2, tardies = 1, period = period, limitPercent = 15f)
        assertEquals(50, ok.totalHours)
        assertEquals(7, ok.allowedAbsences)
        assertEquals(5, ok.remainingAbsences)
        assertEquals(4f, ok.percentage, 0.001f)
        assertEquals(RiskLevel.SAFE, ok.risk)

        val warning = AttendanceMath.subjectAttendance("SI", 5, 0, period, 15f)
        assertEquals(RiskLevel.WARNING, warning.risk)

        val danger = AttendanceMath.subjectAttendance("SI", 8, 0, period, 15f)
        assertEquals(RiskLevel.DANGER, danger.risk)
        assertEquals(-1, danger.remainingAbsences)
    }

    @Test
    fun withoutPeriodNothingIsCalculated() {
        val result = AttendanceMath.subjectAttendance("DI", 3, 0, null, 15f)
        assertEquals(0, result.totalHours)
        assertEquals(0f, result.percentage, 0f)
        assertEquals(RiskLevel.SAFE, result.risk)
    }

    @Test
    fun periodProgress() {
        val period = EvaluationPeriod(utc(2026, 11, 1), utc(2026, 11, 10))
        val progress = AttendanceMath.progress(period, utc(2026, 11, 5))
        assertEquals(10, progress.totalDays)
        assertEquals(5, progress.elapsedDays)
        assertEquals(5, progress.remainingDays)
        assertEquals(0.5f, progress.fraction, 0.001f)

        val before = AttendanceMath.progress(period, utc(2026, 10, 20))
        assertEquals(0, before.elapsedDays)
        val after = AttendanceMath.progress(period, utc(2026, 12, 20))
        assertEquals(0, after.remainingDays)
        assertEquals(1f, after.fraction, 0.001f)
    }

    @Test
    fun todayUtcUsesLocalDate() {
        val madrid = TimeZone.getTimeZone("Europe/Madrid")
        val lateNight = Calendar.getInstance(madrid).apply {
            clear()
            set(2026, Calendar.NOVEMBER, 3, 23, 30)
        }.timeInMillis
        assertEquals(utc(2026, 11, 3), AttendanceMath.todayUtc(lateNight, madrid))
    }
}
