package com.julen.clases.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ReminderPlannerTest {

    private val zone = TimeZone.getTimeZone("Europe/Madrid")

    private fun local(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Long =
        Calendar.getInstance(zone).apply {
            clear()
            set(year, month - 1, day, hour, minute)
        }.timeInMillis

    private val classOnly = ReminderPlanner.Settings(
        classReminder = true, leadMinutes = 5, dailySummary = false, summaryHour = 20, refreshWidget = false,
    )

    @Test
    fun nothingEnabledMeansNoEvent() {
        val none = classOnly.copy(classReminder = false)
        assertNull(ReminderPlanner.nextEvent(local(2026, 11, 3, 9), none, emptyList(), zone))
    }

    @Test
    fun nextClassReminderIsLeadMinutesBefore() {
        // Martes 3 nov 2026 a las 9:00: PMDM ya ha empezado; la siguiente es DI a las 11:30.
        val event = ReminderPlanner.nextEvent(local(2026, 11, 3, 9), classOnly, emptyList(), zone)!!
        assertEquals(PlannedEvent.Type.CLASS, event.type)
        assertEquals("DI", event.subject)
        assertEquals(local(2026, 11, 3, 11, 25), event.atMillis)
    }

    @Test
    fun afterFridayClassesJumpsToMonday() {
        val event = ReminderPlanner.nextEvent(local(2026, 11, 6, 15), classOnly, emptyList(), zone)!!
        assertEquals("OP1", event.subject)
        assertEquals(local(2026, 11, 9, 7, 55), event.atMillis)
    }

    @Test
    fun holidaysAreSkipped() {
        val utc = TimeZone.getTimeZone("UTC")
        val wednesdayUtc = Calendar.getInstance(utc).apply { clear(); set(2026, Calendar.NOVEMBER, 4) }.timeInMillis
        val holidays = listOf(Holiday(1, "Fiesta", wednesdayUtc, wednesdayUtc))
        val event = ReminderPlanner.nextEvent(local(2026, 11, 3, 15), classOnly, holidays, zone)!!
        // El miércoles es festivo: el siguiente aviso es el del jueves (AD a las 8:00).
        assertEquals("AD", event.subject)
        assertEquals(local(2026, 11, 5, 7, 55), event.atMillis)
    }

    @Test
    fun dailySummaryAtConfiguredHour() {
        val settings = classOnly.copy(classReminder = false, dailySummary = true, summaryHour = 20)
        val event = ReminderPlanner.nextEvent(local(2026, 11, 3, 21), settings, emptyList(), zone)!!
        assertEquals(PlannedEvent.Type.SUMMARY, event.type)
        assertEquals(local(2026, 11, 4, 20), event.atMillis)
    }
}
