package com.julen.clases.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.julen.clases.MainActivity
import com.julen.clases.R
import com.julen.clases.data.AttendanceMath
import com.julen.clases.data.AttendanceRepository
import com.julen.clases.data.PlannedEvent
import com.julen.clases.data.ReminderPlanner
import com.julen.clases.data.ScheduleConfig
import com.julen.clases.data.Subjects
import com.julen.clases.data.TaskKind
import com.julen.clases.data.Timetable
import com.julen.clases.data.formatMinutes
import com.julen.clases.widget.ClassWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

/** Programa en AlarmManager el siguiente aviso o refresco del widget. */
object ReminderScheduler {
    private const val REQUEST_CODE = 4021
    private const val EXTRA_TYPE = "type"
    private const val EXTRA_SUBJECT = "subject"
    private const val EXTRA_START = "start"
    private const val EXTRA_END = "end"
    const val ACTION_FIRE = "com.julen.clases.action.REMINDER"
    private const val WINDOW_MS = 60_000L

    fun reschedule(context: Context) {
        val repo = AttendanceRepository(context)
        Timetable.config = repo.getSchedule() ?: ScheduleConfig.DEFAULT
        val settings = ReminderPlanner.Settings(
            classReminder = repo.isClassReminderEnabled(),
            leadMinutes = repo.getReminderLeadMinutes(),
            dailySummary = repo.isDailySummaryEnabled(),
            summaryHour = repo.getSummaryHour(),
            refreshWidget = ClassWidget.hasInstances(context),
        )
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val event = ReminderPlanner.nextEvent(System.currentTimeMillis(), settings, repo.getHolidays())
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ACTION_FIRE)
        if (event == null) {
            val existing = PendingIntent.getBroadcast(
                context, REQUEST_CODE, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
            existing?.let { alarmManager.cancel(it) }
            return
        }
        intent.putExtra(EXTRA_TYPE, event.type.name)
            .putExtra(EXTRA_SUBJECT, event.subject)
            .putExtra(EXTRA_START, event.blockStart)
            .putExtra(EXTRA_END, event.blockEnd)
        val pending = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        // Ventana de 1 minuto: no necesita el permiso de alarmas exactas. En reposo profundo
        // (Doze) Android puede retrasarla hasta su siguiente ventana de mantenimiento.
        alarmManager.setWindow(AlarmManager.RTC_WAKEUP, event.atMillis, WINDOW_MS, pending)
    }

    internal fun eventFrom(intent: Intent): PlannedEvent? {
        val type = runCatching { PlannedEvent.Type.valueOf(intent.getStringExtra(EXTRA_TYPE) ?: "") }.getOrNull() ?: return null
        return PlannedEvent(
            type = type,
            atMillis = System.currentTimeMillis(),
            subject = intent.getStringExtra(EXTRA_SUBJECT),
            blockStart = intent.getIntExtra(EXTRA_START, 0),
            blockEnd = intent.getIntExtra(EXTRA_END, 0),
        )
    }
}

/** Recibe las alarmas: muestra el aviso que toque, refresca el widget y programa la siguiente. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = ReminderScheduler.eventFrom(intent)
        when (event?.type) {
            PlannedEvent.Type.CLASS -> Notifications.showClassReminder(context, event)
            PlannedEvent.Type.SUMMARY -> Notifications.showDailySummary(context)
            else -> Unit
        }
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                ClassWidget.refresh(context)
                ReminderScheduler.reschedule(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/** Vuelve a programar los avisos al reiniciar el móvil o actualizar la app. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> ReminderScheduler.reschedule(context)
        }
    }
}

object Notifications {
    private const val CHANNEL_CLASSES = "classes"
    private const val CHANNEL_SUMMARY = "summary"
    private const val ID_CLASS = 1
    private const val ID_SUMMARY = 2

    fun canPost(context: Context): Boolean =
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_CLASSES, "Inicio de clase", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Aviso unos minutos antes de cada clase"
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_SUMMARY, "Resumen diario", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Exámenes, entregas y tareas del día siguiente"
            }
        )
    }

    private fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    @SuppressLint("MissingPermission") // comprobado en canPost()
    fun showClassReminder(context: Context, event: PlannedEvent) {
        if (!canPost(context)) return
        createChannels(context)
        val code = event.subject ?: return
        val notification = NotificationCompat.Builder(context, CHANNEL_CLASSES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$code a las ${formatMinutes(event.blockStart)}")
            .setContentText("${Subjects.nameOf(code)} · ${formatMinutes(event.blockStart)} – ${formatMinutes(event.blockEnd)}")
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .setTimeoutAfter(20 * 60_000L)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(ID_CLASS, notification) }
    }

    @SuppressLint("MissingPermission") // comprobado en canPost()
    fun showDailySummary(context: Context) {
        if (!canPost(context)) return
        createChannels(context)
        val repo = AttendanceRepository(context)
        Timetable.config = repo.getSchedule() ?: ScheduleConfig.DEFAULT
        val now = System.currentTimeMillis()
        val tomorrowUtc = AttendanceMath.todayUtc(now) + AttendanceMath.DAY_MS
        val due = repo.getTasks().filter { !it.done && it.dueUtc == tomorrowUtc }
        val tomorrowWeekday = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_MONTH, 1)
        }.let { (it.get(Calendar.DAY_OF_WEEK) + 5) % 7 }
        val isHoliday = repo.getHolidays().any { tomorrowUtc in it }
        val firstClass = if (tomorrowWeekday < Timetable.DAYS && !isHoliday) {
            Timetable.blocksFor(tomorrowWeekday).firstOrNull { it.subject != null }
        } else {
            null
        }
        if (due.isEmpty() && firstClass == null) return

        val exams = due.count { it.kind == TaskKind.EXAM }
        val title = when {
            exams > 0 -> "Mañana tienes ${if (exams == 1) "un examen" else "$exams exámenes"}"
            due.isNotEmpty() -> "Mañana: ${due.size} ${if (due.size == 1) "entrega o tarea" else "entregas o tareas"}"
            else -> "Mañana empiezas con ${firstClass?.subject}"
        }
        val lines = buildList {
            firstClass?.let { add("Primera clase: ${it.subject} a las ${formatMinutes(it.start)}") }
            due.forEach { add("${it.kind.label} · ${it.subject}: ${it.title}") }
        }
        val style = NotificationCompat.InboxStyle().also { inbox -> lines.forEach { inbox.addLine(it) } }
        val notification = NotificationCompat.Builder(context, CHANNEL_SUMMARY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(lines.firstOrNull() ?: "")
            .setStyle(style)
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(ID_SUMMARY, notification) }
    }
}
