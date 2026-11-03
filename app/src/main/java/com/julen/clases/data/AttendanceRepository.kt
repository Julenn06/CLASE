package com.julen.clases.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistencia local en SharedPreferences.
 * Mantiene las mismas claves que las versiones anteriores para no perder datos al actualizar.
 */
class AttendanceRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ---------- Faltas y retrasos ----------

    fun getAbsences(subject: String): Int = prefs.getInt("${subject}_absences", 0)
    fun getTardies(subject: String): Int = prefs.getInt("${subject}_tardies", 0)

    private fun setAbsences(subject: String, value: Int) =
        prefs.edit { putInt("${subject}_absences", value.coerceAtLeast(0)) }

    private fun setTardies(subject: String, value: Int) =
        prefs.edit { putInt("${subject}_tardies", value.coerceAtLeast(0)) }

    fun addAbsence(subject: String, now: Long = System.currentTimeMillis()) {
        setAbsences(subject, getAbsences(subject) + 1)
        appendHistory(HistoryEntry(subject, EntryType.ABSENCE, now))
    }

    fun removeAbsence(subject: String) {
        val current = getAbsences(subject)
        if (current <= 0) return
        setAbsences(subject, current - 1)
        removeLastHistory(subject, EntryType.ABSENCE)
    }

    /** Añade un retraso; cada [AttendanceMath.TARDIES_PER_ABSENCE] retrasos suman una falta. */
    fun addTardy(subject: String, now: Long = System.currentTimeMillis()): TardyResult {
        val tardies = getTardies(subject) + 1
        appendHistory(HistoryEntry(subject, EntryType.TARDY, now))
        return if (tardies >= AttendanceMath.TARDIES_PER_ABSENCE) {
            setTardies(subject, 0)
            setAbsences(subject, getAbsences(subject) + 1)
            appendHistory(HistoryEntry(subject, EntryType.ABSENCE, now, fromTardies = true))
            TardyResult(convertedToAbsence = true)
        } else {
            setTardies(subject, tardies)
            TardyResult(convertedToAbsence = false)
        }
    }

    fun removeTardy(subject: String) {
        val current = getTardies(subject)
        if (current <= 0) return
        setTardies(subject, current - 1)
        removeLastHistory(subject, EntryType.TARDY)
    }

    /** Deshace exactamente un [addTardy], incluida la falta que pudo generar. */
    fun undoTardy(subject: String, result: TardyResult) {
        if (result.convertedToAbsence) {
            setAbsences(subject, getAbsences(subject) - 1)
            setTardies(subject, AttendanceMath.TARDIES_PER_ABSENCE - 1)
            removeLastHistory(subject, EntryType.ABSENCE)
            removeLastHistory(subject, EntryType.TARDY)
        } else {
            removeTardy(subject)
        }
    }

    // ---------- Historial ----------

    fun getHistory(): List<HistoryEntry> = readArray(KEY_HISTORY).mapObjects { o ->
        HistoryEntry(
            subject = o.getString("s"),
            type = if (o.optString("t") == "T") EntryType.TARDY else EntryType.ABSENCE,
            timestamp = o.getLong("ts"),
            fromTardies = o.optBoolean("auto", false),
        )
    }

    private fun saveHistory(entries: List<HistoryEntry>) = writeArray(KEY_HISTORY, entries) { e ->
        JSONObject()
            .put("s", e.subject)
            .put("t", if (e.type == EntryType.TARDY) "T" else "A")
            .put("ts", e.timestamp)
            .put("auto", e.fromTardies)
    }

    private fun appendHistory(entry: HistoryEntry) = saveHistory(getHistory() + entry)

    private fun removeLastHistory(subject: String, type: EntryType) {
        val history = getHistory().toMutableList()
        val index = history.indexOfLast { it.subject == subject && it.type == type }
        if (index >= 0) {
            history.removeAt(index)
            saveHistory(history)
        }
    }

    // ---------- Periodo de evaluación ----------

    fun getPeriod(): EvaluationPeriod? {
        val start = prefs.getLong("eval_start", -1L)
        val end = prefs.getLong("eval_end", -1L)
        return if (start != -1L && end != -1L) EvaluationPeriod(start, end) else null
    }

    fun savePeriod(startUtc: Long, endUtc: Long) = prefs.edit {
        putLong("eval_start", startUtc)
        putLong("eval_end", endUtc)
    }

    // ---------- Horario y asignaturas ----------

    /** Horario guardado, o null si se usa el de por defecto. */
    fun getSchedule(): ScheduleConfig? {
        val raw = prefs.getString(KEY_SCHEDULE, null) ?: return null
        return runCatching {
            val root = JSONObject(raw)
            val subjects = root.getJSONArray("subjects").let { arr ->
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    Subject(o.getString("code"), o.getString("name"), o.optInt("color", 0))
                }
            }
            val classes = root.getJSONArray("classes").let { days ->
                (0 until Timetable.DAYS).map { d ->
                    val day = days.optJSONArray(d) ?: JSONArray()
                    (0 until Timetable.CLASS_SLOTS).map { i ->
                        if (day.isNull(i)) null else day.optString(i).takeIf { it.isNotBlank() }
                    }
                }
            }
            ScheduleConfig(subjects, classes)
        }.getOrNull()
    }

    fun saveSchedule(config: ScheduleConfig) {
        val subjects = JSONArray()
        config.subjects.forEach { s ->
            subjects.put(JSONObject().put("code", s.code).put("name", s.name).put("color", s.colorIndex))
        }
        val classes = JSONArray()
        config.classes.forEach { day ->
            val arr = JSONArray()
            day.forEach { arr.put(it ?: JSONObject.NULL) }
            classes.put(arr)
        }
        prefs.edit { putString(KEY_SCHEDULE, JSONObject().put("subjects", subjects).put("classes", classes).toString()) }
    }

    fun resetSchedule() = prefs.edit { remove(KEY_SCHEDULE) }

    // ---------- Días no lectivos ----------

    fun getHolidays(): List<Holiday> = readArray(KEY_HOLIDAYS).mapObjects { o ->
        Holiday(o.getLong("id"), o.getString("name"), o.getLong("start"), o.getLong("end"))
    }

    fun saveHolidays(holidays: List<Holiday>) = writeArray(KEY_HOLIDAYS, holidays) { h ->
        JSONObject().put("id", h.id).put("name", h.name).put("start", h.startUtc).put("end", h.endUtc)
    }

    // ---------- Apuntes ----------

    fun getNote(subject: String): String = prefs.getString("${subject}_note", "") ?: ""

    fun saveNote(subject: String, note: String) = prefs.edit { putString("${subject}_note", note) }

    // ---------- Tareas ----------

    fun getTasks(): List<Task> = readArray(KEY_TASKS).mapObjects { o ->
        Task(
            id = o.getLong("id"),
            subject = o.getString("s"),
            title = o.getString("title"),
            dueUtc = if (o.has("due") && !o.isNull("due")) o.getLong("due") else null,
            done = o.optBoolean("done", false),
            kind = runCatching { TaskKind.valueOf(o.optString("kind")) }.getOrDefault(TaskKind.TASK),
        )
    }

    fun saveTasks(tasks: List<Task>) = writeArray(KEY_TASKS, tasks) { t ->
        JSONObject()
            .put("id", t.id)
            .put("s", t.subject)
            .put("title", t.title)
            .put("done", t.done)
            .put("kind", t.kind.name)
            .also { o -> t.dueUtc?.let { o.put("due", it) } }
    }

    // ---------- Calificaciones ----------

    fun getGrades(): List<Grade> = readArray(KEY_GRADES).mapObjects { o ->
        Grade(
            id = o.getLong("id"),
            subject = o.getString("s"),
            title = o.getString("title"),
            score = o.getDouble("score").toFloat(),
            weight = o.optDouble("weight", 1.0).toFloat(),
            dateUtc = if (o.has("date") && !o.isNull("date")) o.getLong("date") else null,
        )
    }

    fun saveGrades(grades: List<Grade>) = writeArray(KEY_GRADES, grades) { g ->
        JSONObject()
            .put("id", g.id)
            .put("s", g.subject)
            .put("title", g.title)
            .put("score", g.score.toDouble())
            .put("weight", g.weight.toDouble())
            .also { o -> g.dateUtc?.let { o.put("date", it) } }
    }

    // ---------- Ajustes ----------

    fun getAbsenceLimit(): Float = prefs.getFloat(KEY_LIMIT, DEFAULT_LIMIT)

    fun setAbsenceLimit(value: Float) = prefs.edit { putFloat(KEY_LIMIT, value) }

    fun getThemeMode(): ThemeMode =
        runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, null) ?: "") }.getOrDefault(ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) = prefs.edit { putString(KEY_THEME, mode.name) }

    // Avisos
    fun isClassReminderEnabled(): Boolean = prefs.getBoolean(KEY_CLASS_REMINDER, false)
    fun setClassReminderEnabled(value: Boolean) = prefs.edit { putBoolean(KEY_CLASS_REMINDER, value) }
    fun getReminderLeadMinutes(): Int = prefs.getInt(KEY_REMINDER_LEAD, 5)
    fun setReminderLeadMinutes(value: Int) = prefs.edit { putInt(KEY_REMINDER_LEAD, value) }
    fun isDailySummaryEnabled(): Boolean = prefs.getBoolean(KEY_SUMMARY, false)
    fun setDailySummaryEnabled(value: Boolean) = prefs.edit { putBoolean(KEY_SUMMARY, value) }
    fun getSummaryHour(): Int = prefs.getInt(KEY_SUMMARY_HOUR, 20)
    fun setSummaryHour(value: Int) = prefs.edit { putInt(KEY_SUMMARY_HOUR, value) }

    fun getAccent(): AccentColor =
        runCatching { AccentColor.valueOf(prefs.getString(KEY_ACCENT, null) ?: "") }.getOrDefault(AccentColor.INDIGO)

    fun setAccent(accent: AccentColor) = prefs.edit { putString(KEY_ACCENT, accent.name) }

    fun getUserName(): String = prefs.getString(KEY_NAME, "") ?: ""

    fun setUserName(name: String) = prefs.edit { putString(KEY_NAME, name.trim()) }

    /** true si ya se ha completado (o saltado) la bienvenida. */
    fun isOnboarded(): Boolean = prefs.getBoolean(KEY_ONBOARDED, false)

    /** Quien ya usaba una versión anterior (hay datos guardados) no ve la bienvenida. */
    fun migrateOnboarding() {
        if (!prefs.contains(KEY_ONBOARDED) && prefs.all.isNotEmpty()) setOnboarded()
    }

    fun setOnboarded() = prefs.edit { putBoolean(KEY_ONBOARDED, true) }

    // ---------- Borrado ----------

    fun resetAttendance() = prefs.edit {
        Subjects.codes.forEach {
            remove("${it}_absences")
            remove("${it}_tardies")
        }
        remove(KEY_HISTORY)
    }

    /** Borra todo menos el aviso de bienvenida ya visto. */
    fun resetAll() = prefs.edit(commit = true) {
        clear()
        putBoolean(KEY_ONBOARDED, true)
    }

    // ---------- Copia de seguridad ----------

    /** Exporta todos los datos a un JSON con tipos, para poder restaurarlos tal cual. */
    fun exportBackup(now: Long = System.currentTimeMillis()): String {
        val data = JSONObject()
        prefs.all.toSortedMap().forEach { (key, value) ->
            val entry = when (value) {
                is Int -> JSONObject().put("t", "i").put("v", value)
                is Long -> JSONObject().put("t", "l").put("v", value)
                is Float -> JSONObject().put("t", "f").put("v", value.toDouble())
                is Boolean -> JSONObject().put("t", "b").put("v", value)
                is String -> JSONObject().put("t", "s").put("v", value)
                else -> null
            }
            if (entry != null) data.put(key, entry)
        }
        return JSONObject()
            .put("app", BACKUP_APP_ID)
            .put("version", BACKUP_VERSION)
            .put("exportedAt", now)
            .put("data", data)
            .toString(2)
    }

    /** Sustituye todos los datos por los de la copia. Devuelve false si el archivo no es válido. */
    fun importBackup(json: String): Boolean {
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return false
        if (root.optString("app") != BACKUP_APP_ID) return false
        val data = root.optJSONObject("data") ?: return false
        prefs.edit(commit = true) {
            clear()
            data.keys().forEach { key ->
                val entry = data.optJSONObject(key) ?: return@forEach
                when (entry.optString("t")) {
                    "i" -> putInt(key, entry.getInt("v"))
                    "l" -> putLong(key, entry.getLong("v"))
                    "f" -> putFloat(key, entry.getDouble("v").toFloat())
                    "b" -> putBoolean(key, entry.getBoolean("v"))
                    "s" -> putString(key, entry.getString("v"))
                }
            }
            putBoolean(KEY_ONBOARDED, true)
        }
        return true
    }

    // ---------- Utilidades JSON ----------

    private fun readArray(key: String): JSONArray =
        runCatching { JSONArray(prefs.getString(key, "[]") ?: "[]") }.getOrElse { JSONArray() }

    private inline fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
        (0 until length()).mapNotNull { i -> runCatching { transform(getJSONObject(i)) }.getOrNull() }

    private inline fun <T> writeArray(key: String, items: List<T>, toJson: (T) -> JSONObject) {
        val array = JSONArray()
        items.forEach { array.put(toJson(it)) }
        prefs.edit { putString(key, array.toString()) }
    }

    companion object {
        private const val PREFS_NAME = "attendance_prefs"
        private const val KEY_HISTORY = "history"
        private const val KEY_TASKS = "tasks"
        private const val KEY_GRADES = "grades"
        private const val KEY_HOLIDAYS = "holidays"
        private const val KEY_SCHEDULE = "schedule"
        private const val KEY_CLASS_REMINDER = "reminder_class"
        private const val KEY_REMINDER_LEAD = "reminder_lead"
        private const val KEY_SUMMARY = "reminder_summary"
        private const val KEY_SUMMARY_HOUR = "reminder_summary_hour"
        private const val KEY_LIMIT = "absence_limit"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_ACCENT = "accent"
        private const val KEY_NAME = "user_name"
        private const val KEY_ONBOARDED = "onboarded"
        private const val BACKUP_APP_ID = "com.julen.clases"
        private const val BACKUP_VERSION = 1
        const val DEFAULT_LIMIT = 15f
    }
}
