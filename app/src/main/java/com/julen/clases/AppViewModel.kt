package com.julen.clases

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.core.content.edit

class AppViewModel(application: Application) : AndroidViewModel(application) {
    
    private val manager = AttendanceManager(application)
    val subjects = listOf("SI", "SGE", "DI", "AD", "PSP", "PMDM", "IPEII", "OP1", "OP2")

    // Estado reactivo global de la app
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        refreshDashboard()
    }

    fun refreshDashboard() {
        val dates = manager.getEvaluationDates()
        val formattedRange = if (dates != null) {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            "${sdf.format(Date(dates.first))} - ${sdf.format(Date(dates.second))}"
        } else {
            "Fechas no seleccionadas"
        }

        val subjectItems = subjects.map { subject ->
            SubjectState(
                name = subject,
                absences = manager.getAbsences(subject),
                tardies = manager.getTardies(subject),
                percentage = manager.calculateAbsencePercentage(subject)
            )
        }

        _uiState.value = UiState(
            totalAbsences = manager.getTotalAbsences(),
            progress = manager.getEvaluationProgress(),
            remainingDays = manager.getRemainingDays(),
            currentSubject = getCurrentSubjectName(),
            dateRangeText = formattedRange,
            subjectList = subjectItems
        )
    }

    fun addAbsence(subject: String) {
        manager.addAbsence(subject)
        refreshDashboard()
    }

    fun removeAbsence(subject: String) {
        manager.removeAbsence(subject)
        refreshDashboard()
    }

    fun addTardy(subject: String): Boolean {
        val result = manager.addTardy(subject)
        refreshDashboard()
        return result.absenceAdded
    }

    fun removeTardy(subject: String) {
        manager.removeTardy(subject)
        refreshDashboard()
    }

    fun saveDates(start: Long, end: Long) {
        manager.saveEvaluationDates(start, end)
        refreshDashboard()
    }

    fun getNote(subject: String): String = manager.getNote(subject)

    fun saveNote(subject: String, note: String) {
        manager.saveNote(subject, note)
    }

    fun resetAllData() {
        val prefs = getApplication<Application>().getSharedPreferences("attendance_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit { clear() }
        refreshDashboard()
    }

    private fun getCurrentSubjectName(): String {
        val now = Calendar.getInstance()
        val day = now.get(Calendar.DAY_OF_WEEK)
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val minute = now.get(Calendar.MINUTE)
        val time = hour * 100 + minute

        if (day == Calendar.SATURDAY || day == Calendar.SUNDAY) return "Libre (Finde)"
        
        return when (time) {
            in 800..859 -> getSubjectForTime(day, 0)
            in 900..959 -> getSubjectForTime(day, 1)
            in 1000..1059 -> getSubjectForTime(day, 2)
            in 1100..1129 -> "Recreo"
            in 1130..1229 -> getSubjectForTime(day, 3)
            in 1230..1329 -> getSubjectForTime(day, 4)
            in 1330..1430 -> getSubjectForTime(day, 5)
            else -> "Fuera de clase"
        }
    }

    private fun getSubjectForTime(day: Int, slot: Int): String {
        val schedule = mapOf(
            Calendar.MONDAY to listOf("OP1", "OP1", "IPEII", "IPEII", "SI", "SI"),
            Calendar.TUESDAY to listOf("PMDM", "PMDM", "PMDM", "DI", "DI", "DI"),
            Calendar.WEDNESDAY to listOf("AD", "AD", "OP2", "OP2", "DI", "DI"),
            Calendar.THURSDAY to listOf("AD", "AD", "IPEII", "SI", "SI", "SI"),
            Calendar.FRIDAY to listOf("PSP", "PSP", "PSP", "SGE", "SGE", "SGE")
        )
        return schedule[day]?.getOrNull(slot) ?: "Libre"
    }

    data class UiState(
        val totalAbsences: Int = 0,
        val progress: Int = 0,
        val remainingDays: Long = 0,
        val currentSubject: String = "Libre",
        val dateRangeText: String = "Fechas no seleccionadas",
        val subjectList: List<SubjectState> = emptyList()
    )

    data class SubjectState(
        val name: String,
        val absences: Int,
        val tardies: Int,
        val percentage: Float
    )
}
