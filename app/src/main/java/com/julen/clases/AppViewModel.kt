package com.julen.clases

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.julen.clases.data.AccentColor
import com.julen.clases.data.AttendanceMath
import com.julen.clases.data.AttendanceRepository
import com.julen.clases.data.EvaluationPeriod
import com.julen.clases.data.Grade
import com.julen.clases.data.GradeMath
import com.julen.clases.data.GradeSummary
import com.julen.clases.data.HistoryEntry
import com.julen.clases.data.Holiday
import com.julen.clases.data.NowStatus
import com.julen.clases.data.PeriodProgress
import com.julen.clases.data.RiskLevel
import com.julen.clases.data.ScheduleConfig
import com.julen.clases.data.Subject
import com.julen.clases.data.SubjectAttendance
import com.julen.clases.data.Subjects
import com.julen.clases.data.TardyResult
import com.julen.clases.data.Task
import com.julen.clases.data.TaskKind
import com.julen.clases.data.ThemeMode
import com.julen.clases.data.Timetable
import com.julen.clases.notify.ReminderScheduler
import com.julen.clases.widget.ClassWidget
import kotlinx.coroutines.Dispatchers
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
    val minuteOfDay: Int = 0,
    val nowStatus: NowStatus = NowStatus.NoClasses(NowStatus.Reason.WEEKEND, 0, null),
    val period: EvaluationPeriod? = null,
    val periodProgress: PeriodProgress? = null,
    val absenceLimit: Float = AttendanceRepository.DEFAULT_LIMIT,
    val subjects: List<SubjectAttendance> = emptyList(),
    val history: List<HistoryEntry> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val grades: List<Grade> = emptyList(),
    val gradeSummaries: List<GradeSummary> = emptyList(),
    val holidays: List<Holiday> = emptyList(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentColor = AccentColor.INDIGO,
    val userName: String = "",
    val schedule: ScheduleConfig = ScheduleConfig.DEFAULT,
    val onboarded: Boolean = true,
    val reminders: ReminderSettings = ReminderSettings(),
    /** Días lectivos seguidos sin faltar (null si nunca ha faltado). */
    val streak: Int? = null,
) {
    val todayUtc: Long get() = AttendanceMath.todayUtc(nowMillis)
    val totalAbsences: Int get() = subjects.sumOf { it.absences }
    val totalTardies: Int get() = subjects.sumOf { it.tardies }
    val subjectsAtRisk: List<SubjectAttendance> get() = subjects.filter { it.risk != RiskLevel.SAFE }
    val pendingTasks: List<Task> get() = tasks.filter { !it.done }
    val overallAverage: Float? get() = GradeMath.overallAverage(gradeSummaries)

    /** Asistencia global ponderada por horas, o null si no hay periodo. */
    val overallAttendance: Float?
        get() {
            val hours = subjects.sumOf { it.totalHours }
            if (hours <= 0) return null
            return (100f - totalAbsences * 100f / hours).coerceIn(0f, 100f)
        }

    /** Próximo examen o entrega pendiente (hoy o en el futuro). */
    val nextExam: Task?
        get() = tasks
            .filter { !it.done && it.kind != TaskKind.TASK && it.dueUtc != null && it.dueUtc >= todayUtc }
            .minByOrNull { it.dueUtc ?: Long.MAX_VALUE }

    val isHolidayToday: Holiday? get() = holidays.firstOrNull { todayUtc in it }

    fun attendanceOf(code: String): SubjectAttendance? = subjects.firstOrNull { it.code == code }
    fun gradesOf(code: String): GradeSummary? = gradeSummaries.firstOrNull { it.subject == code }
}

data class ReminderSettings(
    val classReminder: Boolean = false,
    val leadMinutes: Int = 5,
    val dailySummary: Boolean = false,
    val summaryHour: Int = 20,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = AttendanceRepository(application).also { it.migrateOnboarding() }

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
        // El horario activo se recarga siempre (puede cambiar al editarlo, importar o borrar datos).
        val schedule = repo.getSchedule() ?: ScheduleConfig.DEFAULT
        Timetable.config = schedule
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val weekday = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val holidays = repo.getHolidays().sortedBy { it.startUtc }
        val todayUtc = AttendanceMath.todayUtc(now)
        val isHoliday = holidays.any { todayUtc in it }
        val today = weekday.takeIf { it < 5 && !isHoliday }
        val minute = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val period = repo.getPeriod()?.takeIf { it.isValid }
        val limit = repo.getAbsenceLimit()
        val grades = repo.getGrades()
        val history = repo.getHistory().sortedByDescending { it.timestamp }

        val subjects = Subjects.codes.map { code ->
            AttendanceMath.subjectAttendance(
                code = code,
                absences = repo.getAbsences(code),
                tardies = repo.getTardies(code),
                period = period,
                limitPercent = limit,
                holidays = holidays,
            )
        }

        return UiState(
            nowMillis = now,
            today = today,
            minuteOfDay = minute,
            nowStatus = Timetable.statusAt(today, minute),
            period = period,
            periodProgress = period?.let { AttendanceMath.progress(it, todayUtc) },
            absenceLimit = limit,
            subjects = subjects,
            history = history,
            streak = AttendanceMath.schoolDaysWithoutAbsence(
                history.filter { it.type == com.julen.clases.data.EntryType.ABSENCE }.map { it.timestamp },
                todayUtc,
                holidays,
            ),
            tasks = repo.getTasks(),
            grades = grades,
            gradeSummaries = GradeMath.summaries(grades),
            holidays = holidays,
            themeMode = repo.getThemeMode(),
            accent = repo.getAccent(),
            userName = repo.getUserName(),
            schedule = schedule,
            onboarded = repo.isOnboarded(),
            reminders = ReminderSettings(
                classReminder = repo.isClassReminderEnabled(),
                leadMinutes = repo.getReminderLeadMinutes(),
                dailySummary = repo.isDailySummaryEnabled(),
                summaryHour = repo.getSummaryHour(),
            ),
        )
    }

    private inline fun update(block: AttendanceRepository.() -> Unit) {
        repo.block()
        refresh()
        syncBackgroundWork()
    }

    /** Reprograma los avisos y refresca el widget con los datos actuales. */
    fun syncBackgroundWork() {
        val app = getApplication<Application>()
        viewModelScope.launch(Dispatchers.Default) {
            runCatching { ReminderScheduler.reschedule(app) }
            runCatching { ClassWidget.refresh(app) }
        }
    }

    // ---------- Avisos ----------

    fun setClassReminder(enabled: Boolean) = update { setClassReminderEnabled(enabled) }
    fun setReminderLead(minutes: Int) = update { setReminderLeadMinutes(minutes) }
    fun setDailySummary(enabled: Boolean) = update { setDailySummaryEnabled(enabled) }
    fun setSummaryHour(hour: Int) = update { setSummaryHour(hour) }

    // ---------- Asistencia ----------

    fun addAbsence(subject: String) = update { addAbsence(subject) }
    fun removeAbsence(subject: String) = update { removeAbsence(subject) }
    fun removeTardy(subject: String) = update { removeTardy(subject) }
    fun undoTardy(subject: String, result: TardyResult) = update { undoTardy(subject, result) }

    /** Registra una falta o retraso de un día pasado ([dateUtc] a medianoche UTC). */
    fun addAbsenceOn(subject: String, dateUtc: Long) = update { addAbsence(subject, AttendanceMath.localNoonOf(dateUtc)) }

    fun addTardyOn(subject: String, dateUtc: Long): TardyResult {
        val result = repo.addTardy(subject, AttendanceMath.localNoonOf(dateUtc))
        refresh()
        syncBackgroundWork()
        return result
    }

    fun addTardy(subject: String): TardyResult {
        val result = repo.addTardy(subject)
        refresh()
        return result
    }

    fun savePeriod(startUtc: Long, endUtc: Long) = update { savePeriod(startUtc, endUtc) }

    // ---------- Horario y asignaturas ----------

    fun setScheduleCell(day: Int, classIndex: Int, subject: String?) = update {
        saveSchedule(Timetable.config.withCell(day, classIndex, subject))
    }

    fun saveSubject(subject: Subject) = update { saveSchedule(Timetable.config.upsertSubject(subject)) }

    fun deleteSubject(code: String) = update { saveSchedule(Timetable.config.removeSubject(code)) }

    fun resetSchedule() = update { resetSchedule() }

    fun restoreSchedule(config: ScheduleConfig) = update { saveSchedule(config) }

    // ---------- Festivos ----------

    fun addHoliday(name: String, startUtc: Long, endUtc: Long) = update {
        saveHolidays(getHolidays() + Holiday(System.currentTimeMillis(), name.trim(), startUtc, endUtc))
    }

    fun deleteHoliday(id: Long) = update { saveHolidays(getHolidays().filterNot { it.id == id }) }

    fun restoreHoliday(holiday: Holiday) = update {
        saveHolidays(getHolidays().filterNot { it.id == holiday.id } + holiday)
    }

    // ---------- Apuntes ----------

    fun getNote(subject: String): String = repo.getNote(subject)

    fun saveNote(subject: String, note: String) = repo.saveNote(subject, note)

    // ---------- Tareas ----------

    fun addTask(subject: String, title: String, dueUtc: Long?, kind: TaskKind) = update {
        saveTasks(getTasks() + Task(System.currentTimeMillis(), subject, title.trim(), dueUtc, kind = kind))
    }

    fun updateTask(task: Task) = update { saveTasks(getTasks().map { if (it.id == task.id) task else it }) }

    fun toggleTask(id: Long) = update {
        saveTasks(getTasks().map { if (it.id == id) it.copy(done = !it.done) else it })
    }

    fun deleteTask(id: Long) = update { saveTasks(getTasks().filterNot { it.id == id }) }

    fun restoreTasks(tasks: List<Task>) = update {
        val ids = tasks.map { it.id }.toSet()
        saveTasks(getTasks().filterNot { it.id in ids } + tasks)
    }

    fun clearCompletedTasks() = update { saveTasks(getTasks().filterNot { it.done }) }

    // ---------- Calificaciones ----------

    fun addGrade(subject: String, title: String, score: Float, weight: Float, dateUtc: Long?) = update {
        saveGrades(getGrades() + Grade(System.currentTimeMillis(), subject, title.trim(), score, weight, dateUtc))
    }

    fun updateGrade(grade: Grade) = update { saveGrades(getGrades().map { if (it.id == grade.id) grade else it }) }

    fun deleteGrade(id: Long) = update { saveGrades(getGrades().filterNot { it.id == id }) }

    fun restoreGrade(grade: Grade) = update {
        saveGrades(getGrades().filterNot { it.id == grade.id } + grade)
    }

    // ---------- Ajustes ----------

    fun setAbsenceLimit(value: Float) = update { setAbsenceLimit(value) }
    fun setThemeMode(mode: ThemeMode) = update { setThemeMode(mode) }
    fun setAccent(accent: AccentColor) = update { setAccent(accent) }
    fun setUserName(name: String) = update { setUserName(name) }
    fun completeOnboarding() = update { setOnboarded() }
    fun resetAttendance() = update { resetAttendance() }
    fun resetAll() = update { resetAll() }

    // ---------- Copia de seguridad ----------

    fun exportBackup(uri: Uri): Boolean = runCatching {
        val json = repo.exportBackup()
        getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use { out ->
            out.write(json.toByteArray(Charsets.UTF_8))
        } ?: error("No se pudo abrir el archivo")
    }.isSuccess

    fun importBackup(uri: Uri): Boolean {
        val text = runCatching {
            getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            }
        }.getOrNull() ?: return false
        val ok = repo.importBackup(text)
        refresh()
        return ok
    }
}
