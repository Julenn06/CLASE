package com.julen.clases.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GradeMathTest {

    private fun grade(subject: String, score: Float, weight: Float, id: Long = 0) =
        Grade(id, subject, "Prueba", score, weight, null)

    @Test
    fun weightedAverageUsesWeights() {
        val grades = listOf(grade("AD", 4f, 40f), grade("AD", 8f, 60f))
        assertEquals(6.4f, GradeMath.weightedAverage(grades)!!, 0.001f)
    }

    @Test
    fun zeroWeightAndEmptyListHaveNoAverage() {
        assertNull(GradeMath.weightedAverage(emptyList()))
        assertNull(GradeMath.weightedAverage(listOf(grade("AD", 7f, 0f))))
    }

    @Test
    fun summariesAndOverallAverage() {
        val grades = listOf(grade("AD", 6f, 1f, 1), grade("DI", 9f, 1f, 2), grade("DI", 7f, 1f, 3))
        val summaries = GradeMath.summaries(grades)
        assertEquals(Subjects.codes.size, summaries.size)
        assertEquals(8f, summaries.first { it.subject == "DI" }.average!!, 0.001f)
        assertNull(summaries.first { it.subject == "SI" }.average)
        assertEquals(7f, GradeMath.overallAverage(summaries)!!, 0.001f)
    }
}
