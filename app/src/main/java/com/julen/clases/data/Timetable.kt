package com.julen.clases.data

/**
 * Asignaturas, franjas horarias y horario semanal.
 *
 * El horario es editable desde la app: la configuración activa vive en [Timetable.config]
 * (la carga y guarda el repositorio). Las horas semanales de cada asignatura, usadas en el
 * cálculo de faltas, se derivan siempre del horario.
 */

/** [colorIndex] apunta a la paleta de colores de asignatura de la interfaz. */
data class Subject(val code: String, val name: String, val colorIndex: Int = 0)

/** Configuración del curso: asignaturas y una asignatura (o null) por franja lectiva y día. */
data class ScheduleConfig(
    val subjects: List<Subject>,
    /** 5 días (lunes a viernes) × [Timetable.CLASS_SLOTS] franjas lectivas. */
    val classes: List<List<String?>>,
) {
    fun withCell(day: Int, classIndex: Int, subject: String?): ScheduleConfig = copy(
        classes = classes.mapIndexed { d, slots ->
            if (d != day) slots else slots.mapIndexed { i, s -> if (i == classIndex) subject else s }
        },
    )

    fun upsertSubject(subject: Subject): ScheduleConfig = copy(
        subjects = if (subjects.any { it.code == subject.code }) {
            subjects.map { if (it.code == subject.code) subject else it }
        } else {
            subjects + subject
        },
    )

    /** Elimina la asignatura y deja libres sus horas. */
    fun removeSubject(code: String): ScheduleConfig = copy(
        subjects = subjects.filterNot { it.code == code },
        classes = classes.map { slots -> slots.map { if (it == code) null else it } },
    )

    companion object {
        val DEFAULT = ScheduleConfig(
            subjects = listOf(
                Subject("SI", "Sistemas Informáticos", 0),
                Subject("SGE", "Sistemas de Gestión Empresarial", 1),
                Subject("DI", "Desarrollo de Interfaces", 2),
                Subject("AD", "Acceso a Datos", 3),
                Subject("PSP", "Programación de Servicios y Procesos", 4),
                Subject("PMDM", "Programación Multimedia y Disp. Móviles", 5),
                Subject("IPEII", "Itinerario para la Empleabilidad II", 6),
                Subject("OP1", "Optativa 1", 7),
                Subject("OP2", "Optativa 2", 8),
            ),
            classes = listOf(
                listOf("OP1", "OP1", "IPEII", "IPEII", "SI", "SI"),
                listOf("PMDM", "PMDM", "PMDM", "DI", "DI", "DI"),
                listOf("AD", "AD", "OP2", "OP2", "DI", "DI"),
                listOf("AD", "AD", "IPEII", "SI", "SI", "SI"),
                listOf("PSP", "PSP", "PSP", "SGE", "SGE", "SGE"),
            ),
        )
    }
}

object Subjects {
    val all: List<Subject> get() = Timetable.config.subjects

    val codes: List<String> get() = all.map { it.code }

    fun byCode(code: String?): Subject? = all.firstOrNull { it.code == code }

    fun nameOf(code: String): String = byCode(code)?.name ?: code
}

/** Franja horaria en minutos desde las 00:00. */
data class TimeSlot(val start: Int, val end: Int, val isBreak: Boolean = false)

/** Bloque continuo del día (clases consecutivas de la misma asignatura se fusionan). */
data class Block(
    val subject: String?,
    val start: Int,
    val end: Int,
    val isBreak: Boolean,
) {
    val durationMinutes: Int get() = end - start
    val label: String get() = "${formatMinutes(start)} – ${formatMinutes(end)}"

    /** Hora libre (sin asignatura) que no es el recreo. */
    val isFree: Boolean get() = !isBreak && subject == null
}

fun formatMinutes(minutes: Int): String = "%d:%02d".format(minutes / 60, minutes % 60)

/** Estado del día en un momento concreto. */
sealed interface NowStatus {
    data class InClass(val block: Block, val minutesLeft: Int, val progress: Float, val next: Block?) : NowStatus
    data class OnBreak(val block: Block, val minutesLeft: Int, val next: Block?) : NowStatus
    data class BeforeClasses(val first: Block, val minutesUntil: Int) : NowStatus
    data class NoClasses(val reason: Reason, val nextDay: Int, val firstBlock: Block?) : NowStatus

    enum class Reason { FINISHED, WEEKEND }
}

object Timetable {
    const val DAYS = 5
    const val CLASS_SLOTS = 6

    /** Configuración activa (asignaturas y horario). */
    @Volatile
    var config: ScheduleConfig = ScheduleConfig.DEFAULT

    val dayNames = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes")
    val dayShortNames = listOf("L", "M", "X", "J", "V")

    val slots = listOf(
        TimeSlot(8 * 60, 9 * 60),
        TimeSlot(9 * 60, 10 * 60),
        TimeSlot(10 * 60, 11 * 60),
        TimeSlot(11 * 60, 11 * 60 + 30, isBreak = true),
        TimeSlot(11 * 60 + 30, 12 * 60 + 30),
        TimeSlot(12 * 60 + 30, 13 * 60 + 30),
        TimeSlot(13 * 60 + 30, 14 * 60 + 30),
    )

    /** Índice de franja lectiva (sin contar el recreo) de la franja [slotIndex], o null si es el recreo. */
    fun classIndexOf(slotIndex: Int): Int? {
        val slot = slots.getOrNull(slotIndex) ?: return null
        if (slot.isBreak) return null
        return slots.take(slotIndex).count { !it.isBreak }
    }

    /** Asignatura en la franja [slotIndex] del día [day] (0 = lunes), o null si es recreo u hora libre. */
    fun subjectAt(day: Int, slotIndex: Int): String? {
        val classIndex = classIndexOf(slotIndex) ?: return null
        return config.classes.getOrNull(day)?.getOrNull(classIndex)
    }

    /**
     * Bloques del día con las clases consecutivas de la misma asignatura unidas.
     * Las horas libres al principio o al final del día se omiten.
     */
    fun blocksFor(day: Int): List<Block> {
        if (day !in 0 until DAYS) return emptyList()
        val result = mutableListOf<Block>()
        slots.forEachIndexed { index, slot ->
            val subject = subjectAt(day, index)
            val last = result.lastOrNull()
            if (last != null && !slot.isBreak && !last.isBreak && last.subject == subject && last.end == slot.start) {
                result[result.lastIndex] = last.copy(end = slot.end)
            } else {
                result += Block(subject, slot.start, slot.end, slot.isBreak)
            }
        }
        val firstClass = result.indexOfFirst { it.subject != null }
        if (firstClass < 0) return emptyList()
        val lastClass = result.indexOfLast { it.subject != null }
        return result.subList(firstClass, lastClass + 1).toList()
    }

    /** Horas lectivas de [subject] en el día de la semana [day]. */
    fun hoursOn(day: Int, subject: String): Int = config.classes.getOrNull(day)?.count { it == subject } ?: 0

    fun weeklyHours(subject: String): Int = (0 until DAYS).sumOf { hoursOn(it, subject) }

    /** Primer día lectivo con clases a partir de [fromDay] (dando la vuelta a la semana). */
    private fun nextDayWithClasses(fromDay: Int): Int? =
        (0 until DAYS).map { (fromDay + it) % DAYS }.firstOrNull { blocksFor(it).isNotEmpty() }

    /** Qué está pasando en el día [day] (0 = lunes, null = fin de semana o festivo) al minuto [minuteOfDay]. */
    fun statusAt(day: Int?, minuteOfDay: Int): NowStatus {
        if (day == null || day !in 0 until DAYS) {
            val next = nextDayWithClasses(0) ?: 0
            return NowStatus.NoClasses(NowStatus.Reason.WEEKEND, next, blocksFor(next).firstOrNull())
        }
        val blocks = blocksFor(day)
        val first = blocks.firstOrNull()
        if (first != null && minuteOfDay < first.start) {
            return NowStatus.BeforeClasses(first, first.start - minuteOfDay)
        }
        blocks.forEachIndexed { index, block ->
            if (minuteOfDay >= block.start && minuteOfDay < block.end) {
                val next = blocks.getOrNull(index + 1)
                val left = block.end - minuteOfDay
                return if (block.isBreak || block.isFree) {
                    NowStatus.OnBreak(block, left, next)
                } else {
                    val progress = (minuteOfDay - block.start).toFloat() / block.durationMinutes
                    NowStatus.InClass(block, left, progress, next)
                }
            }
        }
        val nextDay = if (day >= DAYS - 1) nextDayWithClasses(0) ?: 0 else nextDayWithClasses(day + 1) ?: 0
        return NowStatus.NoClasses(NowStatus.Reason.FINISHED, nextDay, blocksFor(nextDay).firstOrNull())
    }
}
