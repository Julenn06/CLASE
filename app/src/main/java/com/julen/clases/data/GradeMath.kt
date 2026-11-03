package com.julen.clases.data

/** Resumen de calificaciones de una asignatura. */
data class GradeSummary(
    val subject: String,
    val grades: List<Grade>,
    /** Media ponderada sobre 10, o null si no hay notas con peso. */
    val average: Float?,
) {
    val passed: Boolean? get() = average?.let { it >= GradeMath.PASS_MARK }
    val best: Float? get() = grades.maxOfOrNull { it.score }
}

object GradeMath {
    const val PASS_MARK = 5f
    const val MAX_SCORE = 10f

    /** Media ponderada. Las notas con peso 0 no cuentan. */
    fun weightedAverage(grades: List<Grade>): Float? {
        val counted = grades.filter { it.weight > 0f }
        val totalWeight = counted.sumOf { it.weight.toDouble() }
        if (totalWeight <= 0.0) return null
        return (counted.sumOf { it.score.toDouble() * it.weight } / totalWeight).toFloat()
    }

    fun summaries(grades: List<Grade>): List<GradeSummary> = Subjects.codes.map { code ->
        val own = grades.filter { it.subject == code }.sortedWith(compareBy<Grade> { it.dateUtc ?: Long.MAX_VALUE }.thenBy { it.id })
        GradeSummary(code, own, weightedAverage(own))
    }

    /** Media de las medias de cada asignatura que tiene notas. */
    fun overallAverage(summaries: List<GradeSummary>): Float? {
        val averages = summaries.mapNotNull { it.average }
        return if (averages.isEmpty()) null else averages.average().toFloat()
    }

    /**
     * Nota media que hace falta en las pruebas que quedan (con peso total [remainingWeight])
     * para terminar con una media de [target]. Puede ser ≤ 0 (ya asegurado) o > 10 (imposible).
     */
    fun requiredScore(grades: List<Grade>, target: Float, remainingWeight: Float): Float? {
        if (remainingWeight <= 0f) return null
        val counted = grades.filter { it.weight > 0f }
        val weight = counted.sumOf { it.weight.toDouble() }
        val points = counted.sumOf { it.score.toDouble() * it.weight }
        return ((target * (weight + remainingWeight) - points) / remainingWeight).toFloat()
    }

    /** Peso que falta si los pesos están en porcentaje (suman menos de 100), o null si no se sabe. */
    fun remainingPercentWeight(grades: List<Grade>): Float? {
        val total = grades.sumOf { it.weight.toDouble() }.toFloat()
        return if (total > 1f && total < 100f) 100f - total else null
    }
}
