package com.julen.clases

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.julen.clases.data.AttendanceMath
import com.julen.clases.data.AttendanceRepository
import com.julen.clases.data.EvaluationPeriod
import com.julen.clases.data.HistoryEntry
import com.julen.clases.data.NowStatus
import com.julen.clases.data.PeriodProgress
import com.julen.clases.data.RiskLevel
import com.julen.clases.data.SubjectAttendance
import com.julen.clases.data.Subjects
import com.julen.clases.data.TardyResult
import com.julen.clases.data.Task
import com.julen.clases.data.ThemeMode
import com.julen.clases.data.Timetable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar

data class UiState(
    val nowMillis: Long = System.currentTimeMillis(),
    /** Día lectivo actual (0 = lunes … 4 = viernes) o null en fin de semana. */
    val today: Int? = null,
    val nowStatus: NowStatus = NowStatus.NoClasses(NowStatus.Reason.WEEKEND, 0, null),
    val period: EvaluationPeriod? = null,
    val periodProgress: PeriodProgress? = null,
    val absenceLimit: Float = AttendanceRepository.DEFAULT_LIMIT,
    val subjects: List<SubjectAttendance> = emptyList(),
    val history: List<HistoryEntry> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
) {
    val totalAbsences: Int get() = subjects.sumOf { it.absences }
    val totalTardies: Int get() = subjects.sumOf { it.tardies }
    val subjectsAtRisk: List<SubjectAttendance> get() = subjects.filter { it.risk != RiskLevel.SAFE }
    val pendingTasks: List<Task> get() = tasks.filter { !it.done }
    val todayUtc: Long get() = AttendanceMath.todayUtc(nowMillis)

    fun attendanceOf(code: String): SubjectAttendance? = subjects.firstOrNull { it.code == code }
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = AttendanceRepository(application)

    private val _uiState = MutableStateFlow(buildState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        // Reloj: actualiza la clase actual al comienzo de cada minuto.
        viewModelScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                delay(60_000 - now % 60_000 + 50)
                refresh()
            }
        }
    }

    fun refresh() {
        _uiState.value = buildState()
    }

    private fun buildState(): UiState {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val weekday = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val today = weekday.takeIf { it < 5 }
        val minute = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val period = repo.getPeriod()?.takeIf { it.isValid }
        val limit = repo.getAbsenceLimit()

        val subjects = Subjects.codes.map { code ->
            AttendanceMath.subjectAttendance(
                code = code,
                absences = repo.getAbsences(code),
                tardies = repo.getTardies(code),
                period = period,
                limitPercent = limit,
            )
        }

        return UiState(
            nowMillis = now,
            today = today,
            nowStatus = Timetable.statusAt(today, minute),
            period = period,
            periodProgress = period?.let { AttendanceMath.progress(it, AttendanceMath.todayUtc(now)) },
            absenceLimit = limit,
            subjects = subjects,
            history = repo.getHistory().sortedByDescending { it.timestamp },
            tasks = repo.getTasks(),
            themeMode = repo.getThemeMode(),
        )
    }

    // ---------- Asistencia ----------

    fun addAbsence(subject: String) {
        repo.addAbsence(subject)
        refresh()
    }

    fun removeAbsence(subject: String) {
        repo.removeAbsence(subject)
        refresh()
    }

    fun addTardy(subject: String): TardyResult {
        val result = repo.addTardy(subject)
        refresh()
        return result
    }

    fun removeTardy(subject: String) {
        repo.removeTardy(subject)
        refresh()
    }

    fun undoTardy(subject: String, result: TardyResult) {
        repo.undoTardy(subject, result)
        refresh()
    }

    fun savePeriod(startUtc: Long, endUtc: Long) {
        repo.savePeriod(startUtc, endUtc)
        refresh()
    }

    // ---------- Apuntes ----------

    fun getNote(subject: String): String = repo.getNote(subject)

    fun saveNote(subject: String, note: String) = repo.saveNote(subject, note)

    fun subjectsWithNotes(): Set<String> =
        Subjects.codes.filter { repo.getNote(it).isNotBlank() }.toSet()

    // ---------- Tareas ----------

    fun addTask(subject: String, title: String, dueUtc: Long?) {
        val task = Task(System.currentTimeMillis(), subject, title.trim(), dueUtc)
        repo.saveTasks(repo.getTasks() + task)
        refresh()
    }

    fun toggleTask(id: Long) {
        repo.saveTasks(repo.getTasks().map { if (it.id == id) it.copy(done = !it.done) else it })
        refresh()
    }

    fun deleteTask(id: Long) {
        repo.saveTasks(repo.getTasks().filterNot { it.id == id })
        refresh()
    }

    fun restoreTask(task: Task) {
        repo.saveTasks(repo.getTasks().filterNot { it.id == task.id } + task)
        refresh()
    }

    fun clearCompletedTasks() {
        repo.saveTasks(repo.getTasks().filterNot { it.done })
        refresh()
    }

    // ---------- Ajustes ----------

    fun setAbsenceLimit(value: Float) {
        repo.setAbsenceLimit(value)
        refresh()
    }

    fun setThemeMode(mode: ThemeMode) {
        repo.setThemeMode(mode)
        refresh()
    }

    fun resetAttendance() {
        repo.resetAttendance()
        refresh()
    }

    fun resetAll() {
        repo.resetAll()
        refresh()
    }
}
