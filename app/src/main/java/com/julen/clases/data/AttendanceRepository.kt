package com.julen.clases.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistencia local en SharedPreferences.
 * Mantiene las mismas claves que la versión anterior para no perder datos al actualizar.
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

    private fun saveHistory(entries: List<HistoryEntry>) {
        val array = JSONArray()
        entries.forEach { e ->
            array.put(
                JSONObject()
                    .put("s", e.subject)
                    .put("t", if (e.type == EntryType.TARDY) "T" else "A")
                    .put("ts", e.timestamp)
                    .put("auto", e.fromTardies)
            )
        }
        prefs.edit { putString(KEY_HISTORY, array.toString()) }
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
        )
    }

    fun saveTasks(tasks: List<Task>) {
        val array = JSONArray()
        tasks.forEach { t ->
            val o = JSONObject()
                .put("id", t.id)
                .put("s", t.subject)
                .put("title", t.title)
                .put("done", t.done)
            t.dueUtc?.let { o.put("due", it) }
            array.put(o)
        }
        prefs.edit { putString(KEY_TASKS, array.toString()) }
    }

    // ---------- Ajustes ----------

    fun getAbsenceLimit(): Float = prefs.getFloat(KEY_LIMIT, DEFAULT_LIMIT)

    fun setAbsenceLimit(value: Float) = prefs.edit { putFloat(KEY_LIMIT, value) }

    fun getThemeMode(): ThemeMode =
        runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, null) ?: "") }.getOrDefault(ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) = prefs.edit { putString(KEY_THEME, mode.name) }

    // ---------- Borrado ----------

    fun resetAttendance() = prefs.edit {
        Subjects.codes.forEach {
            remove("${it}_absences")
            remove("${it}_tardies")
        }
        remove(KEY_HISTORY)
    }

    fun resetAll() = prefs.edit { clear() }

    // ---------- Utilidades JSON ----------

    private fun readArray(key: String): JSONArray =
        runCatching { JSONArray(prefs.getString(key, "[]") ?: "[]") }.getOrElse { JSONArray() }

    private inline fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
        (0 until length()).mapNotNull { i -> runCatching { transform(getJSONObject(i)) }.getOrNull() }

    companion object {
        private const val PREFS_NAME = "attendance_prefs"
        private const val KEY_HISTORY = "history"
        private const val KEY_TASKS = "tasks"
        private const val KEY_LIMIT = "absence_limit"
        private const val KEY_THEME = "theme_mode"
        const val DEFAULT_LIMIT = 15f
    }
}
