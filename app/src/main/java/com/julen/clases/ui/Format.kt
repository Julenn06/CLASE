package com.julen.clases.ui

import com.julen.clases.data.AttendanceMath
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val Spanish: Locale = Locale.forLanguageTag("es-ES")
private val Utc: TimeZone = TimeZone.getTimeZone("UTC")

/** Formatea una fecha guardada como medianoche UTC (fechas del calendario). */
fun formatDateUtc(utcMillis: Long, pattern: String = "d MMM yyyy"): String =
    SimpleDateFormat(pattern, Spanish).apply { timeZone = Utc }.format(Date(utcMillis))

/** Formatea un instante en la zona horaria del dispositivo. */
fun formatLocal(millis: Long, pattern: String): String =
    SimpleDateFormat(pattern, Spanish).format(Date(millis))

fun String.capitalized(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Spanish) else it.toString() }

fun durationText(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${minutes % 60} min"
}

fun greeting(nowMillis: Long): String {
    val hour = Calendar.getInstance().apply { timeInMillis = nowMillis }.get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 6..13 -> "Buenos días"
        in 14..20 -> "Buenas tardes"
        else -> "Buenas noches"
    }
}

/** "Hoy", "Mañana", "En 3 días", "Hace 2 días"… para fechas límite. */
fun relativeDay(dueUtc: Long, todayUtc: Long): String {
    val days = ((dueUtc - todayUtc) / AttendanceMath.DAY_MS).toInt()
    return when {
        days == 0 -> "Hoy"
        days == 1 -> "Mañana"
        days == -1 -> "Ayer"
        days in 2..6 -> "En $days días"
        days < -1 -> "Hace ${-days} días"
        else -> formatDateUtc(dueUtc, "d MMM")
    }
}

fun plural(count: Int, singular: String, pluralForm: String = singular + "s"): String =
    "$count ${if (count == 1) singular else pluralForm}"
