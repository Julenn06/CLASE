package com.julen.clases

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.concurrent.TimeUnit

class AttendanceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("attendance_prefs", Context.MODE_PRIVATE)

    private val subjectHours = mapOf(
        "SI" to 5,
        "SGE" to 3,
        "DI" to 5,
        "AD" to 4,
        "PSP" to 3,
        "PMDM" to 3,
        "IPEII" to 3,
        "OP1" to 2,
        "OP2" to 2
    )

    fun addTardy(subject: String): AttendanceUpdate {
        val currentTardies = getTardies(subject) + 1
        var currentAbsences = getAbsences(subject)
        var absenceAdded = false

        val finalTardies = if (currentTardies >= 3) {
            currentAbsences++
            absenceAdded = true
            0
        } else {
            currentTardies
        }

        saveTardies(subject, finalTardies)
        saveAbsences(subject, currentAbsences)
        
        return AttendanceUpdate(finalTardies, currentAbsences, absenceAdded)
    }

    fun addAbsence(subject: String): AttendanceUpdate {
        val currentAbsences = getAbsences(subject) + 1
        saveAbsences(subject, currentAbsences)
        return AttendanceUpdate(getTardies(subject), currentAbsences, true)
    }

    fun removeTardy(subject: String): AttendanceUpdate {
        val currentTardies = getTardies(subject)
        if (currentTardies > 0) {
            saveTardies(subject, currentTardies - 1)
        }
        return AttendanceUpdate(getTardies(subject), getAbsences(subject), false)
    }

    fun removeAbsence(subject: String): AttendanceUpdate {
        val currentAbsences = getAbsences(subject)
        if (currentAbsences > 0) {
            saveAbsences(subject, currentAbsences - 1)
        }
        return AttendanceUpdate(getTardies(subject), getAbsences(subject), false)
    }

    fun getTardies(subject: String): Int = prefs.getInt("${subject}_tardies", 0)
    fun getAbsences(subject: String): Int = prefs.getInt("${subject}_absences", 0)

    private fun saveTardies(subject: String, count: Int) = prefs.edit { putInt("${subject}_tardies", count) }
    private fun saveAbsences(subject: String, count: Int) = prefs.edit { putInt("${subject}_absences", count) }

    fun saveEvaluationDates(startDate: Long, endDate: Long) {
        prefs.edit {
            putLong("eval_start", startDate)
            putLong("eval_end", endDate)
        }
    }

    fun getEvaluationDates(): Pair<Long, Long>? {
        val start = prefs.getLong("eval_start", -1L)
        val end = prefs.getLong("eval_end", -1L)
        return if (start != -1L && end != -1L) start to end else null
    }

    fun calculateAbsencePercentage(subject: String): Float {
        val dates = getEvaluationDates() ?: return 0f
        val diffInMillies = dates.second - dates.first
        if (diffInMillies <= 0) return 0f
        
        val days = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS)
        val weeks = (days / 7f).coerceAtLeast(1f)
        
        val weeklyHours = subjectHours[subject] ?: return 0f
        val totalHours = weeklyHours * weeks
        
        val absences = getAbsences(subject).toFloat()
        return (absences / totalHours) * 100f
    }

    fun getTotalAbsences(): Int {
        return subjectHours.keys.sumOf { getAbsences(it) }
    }

    fun getEvaluationProgress(): Int {
        val dates = getEvaluationDates() ?: return 0
        val total = dates.second - dates.first
        val current = System.currentTimeMillis() - dates.first
        if (total <= 0) return 0
        return ((current.toFloat() / total.toFloat()) * 100).toInt().coerceIn(0, 100)
    }

    fun getRemainingDays(): Long {
        val dates = getEvaluationDates() ?: return 0
        val diff = dates.second - System.currentTimeMillis()
        return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS).coerceAtLeast(0)
    }

    fun saveNote(subject: String, note: String) {
        prefs.edit { putString("${subject}_note", note) }
    }

    fun getNote(subject: String): String {
        return prefs.getString("${subject}_note", "") ?: ""
    }

    data class AttendanceUpdate(val tardies: Int, val absences: Int, val absenceAdded: Boolean)
}