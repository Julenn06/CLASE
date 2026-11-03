package com.julen.clases.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimetableTest {

    private fun at(hour: Int, minute: Int = 0) = hour * 60 + minute

    @Test
    fun weeklyHoursMatchTimetable() {
        val expected = mapOf(
            "SI" to 5, "SGE" to 3, "DI" to 5, "AD" to 4, "PSP" to 3,
            "PMDM" to 3, "IPEII" to 3, "OP1" to 2, "OP2" to 2,
        )
        expected.forEach { (code, hours) -> assertEquals(code, hours, Timetable.weeklyHours(code)) }
        assertEquals(30, Subjects.codes.sumOf { Timetable.weeklyHours(it) })
    }

    @Test
    fun blocksMergeConsecutiveClasses() {
        val tuesday = Timetable.blocksFor(1)
        assertEquals(listOf("PMDM", null, "DI"), tuesday.map { it.subject })
        assertEquals(at(8) to at(11), tuesday[0].start to tuesday[0].end)
        assertTrue(tuesday[1].isBreak)
        assertEquals(at(11, 30) to at(14, 30), tuesday[2].start to tuesday[2].end)
    }

    @Test
    fun subjectAtSkipsBreak() {
        assertEquals("IPEII", Timetable.subjectAt(0, 2))
        assertNull(Timetable.subjectAt(0, 3))
        assertEquals("IPEII", Timetable.subjectAt(0, 4))
        assertEquals("SI", Timetable.subjectAt(0, 6))
    }

    @Test
    fun statusDuringClass() {
        val status = Timetable.statusAt(1, at(9, 30)) as NowStatus.InClass
        assertEquals("PMDM", status.block.subject)
        assertEquals(90, status.minutesLeft)
        assertEquals(0.5f, status.progress, 0.001f)
        assertTrue(status.next!!.isBreak)
    }

    @Test
    fun statusOnBreakBeforeAndAfter() {
        val onBreak = Timetable.statusAt(2, at(11, 10)) as NowStatus.OnBreak
        assertEquals(20, onBreak.minutesLeft)
        assertEquals("OP2", onBreak.next!!.subject)

        val before = Timetable.statusAt(4, at(7, 15)) as NowStatus.BeforeClasses
        assertEquals("PSP", before.first.subject)
        assertEquals(45, before.minutesUntil)

        val after = Timetable.statusAt(3, at(14, 30)) as NowStatus.NoClasses
        assertEquals(NowStatus.Reason.FINISHED, after.reason)
        assertEquals(4, after.nextDay)
        assertEquals("PSP", after.firstBlock!!.subject)

        val friday = Timetable.statusAt(4, at(18)) as NowStatus.NoClasses
        assertEquals(0, friday.nextDay)
        assertEquals("OP1", friday.firstBlock!!.subject)
    }

    @Test
    fun weekendStatus() {
        val status = Timetable.statusAt(null, at(10)) as NowStatus.NoClasses
        assertEquals(NowStatus.Reason.WEEKEND, status.reason)
        assertEquals("OP1", status.firstBlock!!.subject)
    }
}
