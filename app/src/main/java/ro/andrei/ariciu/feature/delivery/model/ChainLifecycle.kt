package ro.andrei.ariciu.feature.delivery.model

import ro.andrei.ariciu.feature.reminders.model.*

data class ChainArea(val id: String, val latitude: Double, val longitude: Double,
    val position: AreaPosition = AreaPosition.UNKNOWN, val elapsedMs: Long = -1, val boot: Int = -1)

/** A chain is a union of selected store areas. Overlapping stores count as one visit. */
fun Reminder.crossChain(areaId: String, event: ReminderTransition, time: Long, boot: Int): Reminder {
    if (!delivery.active) return this
    val old = chainAreas.firstOrNull { it.id == areaId } ?: return this
    val position = if (event == ReminderTransition.ENTER) AreaPosition.INSIDE else AreaPosition.OUTSIDE
    if (old.position == position || (old.boot == boot && time <= old.elapsedMs)) return this
    val areas = chainAreas.map { if (it.id == areaId) it.copy(position = position, elapsedMs = time, boot = boot) else it }
    val aggregate = if (areas.any { it.position == AreaPosition.INSIDE }) ReminderTransition.ENTER else ReminderTransition.EXIT
    // Each area has its own ordering guard. Parent timestamps must not discard a valid different-area event.
    val state = delivery.onTransition(aggregate, transition, repeat,
        maxOf(time, delivery.lastEventElapsedMs + 1), boot)
    return copy(chainAreas = areas, delivery = state)
}

fun encodeChainAreas(areas: List<ChainArea>) = areas.joinToString(";") {
    "${it.id},${it.latitude},${it.longitude},${it.position.name},${it.elapsedMs},${it.boot}"
}
fun decodeChainAreas(value: String): List<ChainArea> = if (value.isBlank()) emptyList() else value.split(';').map {
    val p = it.split(','); ChainArea(p[0], p[1].toDouble(), p[2].toDouble(), AreaPosition.valueOf(p[3]), p[4].toLong(), p[5].toInt())
}

fun allocateAreas(records: List<DeliveryRecord>, capacity: Int = 100): List<DeliveryRecord> {
    val lists = records.sortedBy { it.reminder.id }.map { it.monitoringAreas }
    return (0 until (lists.maxOfOrNull { it.size } ?: 0)).flatMap { index -> lists.mapNotNull { it.getOrNull(index) } }.take(capacity)
}
