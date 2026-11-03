package com.julen.clases.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.julen.clases.MainActivity
import com.julen.clases.data.AttendanceMath
import com.julen.clases.data.AttendanceRepository
import com.julen.clases.data.NowStatus
import com.julen.clases.data.ScheduleConfig
import com.julen.clases.data.Subjects
import com.julen.clases.data.TaskKind
import com.julen.clases.data.Timetable
import com.julen.clases.data.formatMinutes
import java.util.Calendar

/** Datos que muestra el widget, calculados en el momento de dibujarlo. */
private data class WidgetData(
    val label: String,
    val title: String,
    val subtitle: String,
    val footer: String,
)

private fun loadWidgetData(context: Context): WidgetData {
    val repo = AttendanceRepository(context)
    Timetable.config = repo.getSchedule() ?: ScheduleConfig.DEFAULT
    val now = System.currentTimeMillis()
    val cal = Calendar.getInstance().apply { timeInMillis = now }
    val todayUtc = AttendanceMath.todayUtc(now)
    val holiday = repo.getHolidays().firstOrNull { todayUtc in it }
    val weekday = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val today = weekday.takeIf { it < Timetable.DAYS && holiday == null }
    val minute = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

    val nextExam = repo.getTasks()
        .filter { !it.done && it.kind != TaskKind.TASK && it.dueUtc != null && it.dueUtc >= todayUtc }
        .minByOrNull { it.dueUtc ?: Long.MAX_VALUE }
    val footer = nextExam?.dueUtc?.let { due ->
        val days = ((due - todayUtc) / AttendanceMath.DAY_MS).toInt()
        val whenText = when (days) {
            0 -> "hoy"
            1 -> "mañana"
            else -> "en $days días"
        }
        "${nextExam.kind.label} de ${nextExam.subject} $whenText"
    } ?: "Sin exámenes a la vista"

    if (holiday != null) return WidgetData("DÍA NO LECTIVO", holiday.name, "Hoy no hay clase", footer)

    return when (val status = Timetable.statusAt(today, minute)) {
        is NowStatus.InClass -> {
            val code = status.block.subject.orEmpty()
            val next = status.next?.takeIf { !it.isBreak && it.subject != null }
            WidgetData(
                label = "AHORA · quedan ${status.minutesLeft} min",
                title = code,
                subtitle = Subjects.nameOf(code),
                footer = next?.let { "Después: ${it.subject} a las ${formatMinutes(it.start)}" } ?: footer,
            )
        }
        is NowStatus.OnBreak -> {
            val next = status.next
            WidgetData(
                label = if (status.block.isFree) "HORA LIBRE" else "RECREO",
                title = "${status.minutesLeft} min",
                subtitle = if (next?.subject != null) "Luego ${next.subject} a las ${formatMinutes(next.start)}" else "",
                footer = footer,
            )
        }
        is NowStatus.BeforeClasses -> WidgetData(
            label = "HOY EMPIEZAS CON",
            title = status.first.subject.orEmpty(),
            subtitle = "A las ${formatMinutes(status.first.start)}",
            footer = footer,
        )
        is NowStatus.NoClasses -> {
            val first = status.firstBlock
            WidgetData(
                label = if (status.reason == NowStatus.Reason.WEEKEND) "FIN DE SEMANA" else "CLASES TERMINADAS",
                title = first?.subject ?: "Libre",
                subtitle = first?.let { "${Timetable.dayNames[status.nextDay]} a las ${formatMinutes(it.start)}" } ?: "",
                footer = footer,
            )
        }
    }
}

class ClassWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetData(context)
        provideContent { WidgetContent(data) }
    }

    companion object {
        fun hasInstances(context: Context): Boolean = runCatching {
            AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, ClassWidgetReceiver::class.java))
                .isNotEmpty()
        }.getOrDefault(false)

        suspend fun refresh(context: Context) {
            if (hasInstances(context)) runCatching { ClassWidget().updateAll(context) }
        }
    }
}

class ClassWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ClassWidget()
}

private val White = ColorProvider(Color.White)
private val WhiteSoft = ColorProvider(Color.White.copy(alpha = 0.78f))

@Composable
private fun WidgetContent(data: WidgetData) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(Color(0xFF4338CA))
            .clickable(actionStartActivity<MainActivity>())
            .padding(16.dp),
        verticalAlignment = Alignment.Vertical.Top,
    ) {
        Text(
            data.label,
            style = TextStyle(color = WhiteSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            data.title,
            style = TextStyle(color = White, fontSize = 30.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
        if (data.subtitle.isNotEmpty()) {
            Text(data.subtitle, style = TextStyle(color = WhiteSoft, fontSize = 13.sp), maxLines = 1)
        }
        Spacer(GlanceModifier.defaultWeight())
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .cornerRadius(12.dp)
                .background(Color(0x33FFFFFF))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(data.footer, style = TextStyle(color = White, fontSize = 12.sp, fontWeight = FontWeight.Medium), maxLines = 1)
        }
    }
}
