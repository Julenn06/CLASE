package com.julen.clases.data

/**
 * Única fuente de verdad del curso: asignaturas, franjas horarias y horario semanal.
 * Si cambia el horario, solo hay que tocar este archivo; las horas semanales de cada
 * asignatura (para el cálculo de faltas) se derivan automáticamente.
 */

data class Subject(val code: String, val name: String)

object Subjects {
    val all = listOf(
        Subject("SI", "Sistemas Informáticos"),
        Subject("SGE", "Sistemas de Gestión Empresarial"),
        Subject("DI", "Desarrollo de Interfaces"),
        Subject("AD", "Acceso a Datos"),
        Subject("PSP", "Programación de Servicios y Procesos"),
        Subject("PMDM", "Programación Multimedia y Disp. Móviles"),
        Subject("IPEII", "Itinerario para la Empleabilidad II"),
        Subject("OP1", "Optativa 1"),
        Subject("OP2", "Optativa 2"),
    )

    val codes: List<String> = all.map { it.code }

    fun byCode(code: String): Subject? = all.firstOrNull { it.code == code }

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

    /** Una entrada por franja lectiva (sin el recreo), de lunes (0) a viernes (4). */
    private val classes: List<List<String>> = listOf(
        listOf("OP1", "OP1", "IPEII", "IPEII", "SI", "SI"),
        listOf("PMDM", "PMDM", "PMDM", "DI", "DI", "DI"),
        listOf("AD", "AD", "OP2", "OP2", "DI", "DI"),
        listOf("AD", "AD", "IPEII", "SI", "SI", "SI"),
        listOf("PSP", "PSP", "PSP", "SGE", "SGE", "SGE"),
    )

    /** Asignatura en la franja [slotIndex] del día [day] (0 = lunes), o null si es recreo. */
    fun subjectAt(day: Int, slotIndex: Int): String? {
        val slot = slots.getOrNull(slotIndex) ?: return null
        if (slot.isBreak) return null
        val classIndex = slots.take(slotIndex).count { !it.isBreak }
        return classes.getOrNull(day)?.getOrNull(classIndex)
    }

    /** Bloques del día con las clases consecutivas de la misma asignatura unidas. */
    fun blocksFor(day: Int): List<Block> {
        if (day !in classes.indices) return emptyList()
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
        return result
    }

    /** Horas lectivas de [subject] en el día de la semana [day]. */
    fun hoursOn(day: Int, subject: String): Int = classes.getOrNull(day)?.count { it == subject } ?: 0

    fun weeklyHours(subject: String): Int = classes.indices.sumOf { hoursOn(it, subject) }

    /** Qué está pasando en el día [day] (0 = lunes, null = fin de semana) al minuto [minuteOfDay]. */
    fun statusAt(day: Int?, minuteOfDay: Int): NowStatus {
        if (day == null || day !in classes.indices) {
            return NowStatus.NoClasses(NowStatus.Reason.WEEKEND, 0, blocksFor(0).firstOrNull())
        }
        val blocks = blocksFor(day)
        val first = blocks.first()
        if (minuteOfDay < first.start) {
            return NowStatus.BeforeClasses(first, first.start - minuteOfDay)
        }
        blocks.forEachIndexed { index, block ->
            if (minuteOfDay >= block.start && minuteOfDay < block.end) {
                val next = blocks.getOrNull(index + 1)
                val left = block.end - minuteOfDay
                return if (block.isBreak) {
                    NowStatus.OnBreak(block, left, next)
                } else {
                    val progress = (minuteOfDay - block.start).toFloat() / block.durationMinutes
                    NowStatus.InClass(block, left, progress, next)
                }
            }
        }
        val nextDay = if (day >= classes.lastIndex) 0 else day + 1
        return NowStatus.NoClasses(NowStatus.Reason.FINISHED, nextDay, blocksFor(nextDay).firstOrNull())
    }
}
