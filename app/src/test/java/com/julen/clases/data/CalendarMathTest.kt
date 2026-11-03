package com.julen.clases.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarMathTest {

    @Test
    fun monthLayout() {
        assertEquals(30, CalendarMath.daysInMonth(2026, 11))
        assertEquals(28, CalendarMath.daysInMonth(2027, 2))
        assertEquals(29, CalendarMath.daysInMonth(2028, 2))
        // El 1 de noviembre de 2026 es domingo (columna 6).
        assertEquals(6, CalendarMath.firstWeekdayOfMonth(2026, 11))
    }

    @Test
    fun addMonthsCrossesYears() {
        assertEquals(2027 to 2, CalendarMath.addMonths(2026, 11, 3))
        assertEquals(2025 to 12, CalendarMath.addMonths(2026, 1, -1))
        assertEquals(2026 to 11, CalendarMath.addMonths(2026, 11, 0))
    }

    @Test
    fun schoolDaysSkipWeekendsAndHolidays() {
        val start = CalendarMath.date(2026, 11, 2) // lunes
        val end = CalendarMath.date(2026, 11, 15) // domingo
        assertEquals(14, CalendarMath.daysBetweenInclusive(start, end))
        assertEquals(10, CalendarMath.schoolDays(start, end, emptyList()))
        val holiday = Holiday(1, "Puente", CalendarMath.date(2026, 11, 9), CalendarMath.date(2026, 11, 10))
        assertEquals(8, CalendarMath.schoolDays(start, end, listOf(holiday)))
        // 2 semanas × 30 h del horario por defecto.
        assertEquals(60, CalendarMath.classHours(start, end, emptyList()))
    }

    @Test
    fun presetsFollowTheSchoolYear() {
        val november = CalendarMath.academicPresets(CalendarMath.date(2026, 11, 3))
        assertEquals(CalendarMath.date(2026, 9, 8), november[0].startUtc)
        assertEquals(CalendarMath.date(2027, 6, 19), november[3].endUtc)
        // En marzo el curso empezó el septiembre anterior.
        val march = CalendarMath.academicPresets(CalendarMath.date(2027, 3, 1))
        assertEquals(CalendarMath.date(2026, 9, 8), march[0].startUtc)
    }
}
