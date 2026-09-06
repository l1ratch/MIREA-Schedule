package com.jetbrains.kmpapp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class FreeRoomsData(
    val updatedAt: String = "",
    val dateRange: FreeRoomsDateRange? = null,
    val campuses: List<String> = emptyList(),
    val bellSlots: List<FreeRoomBellSlot> = emptyList(),
    val rooms: List<FreeRoomItem> = emptyList()
)

@Serializable
data class FreeRoomsDateRange(
    val start: String,
    val end: String
)

@Serializable
data class FreeRoomBellSlot(
    val bell: Int,
    val start: String,
    val end: String
)

@Serializable
data class FreeRoomItem(
    val id: Int,
    val name: String,
    val fullTitle: String,
    val campus: String,
    val floor: Int? = null,
    val busy: Map<String, List<Int>> = emptyMap()
) {
    /**
     * Checks if room is free at given date (YYYY-MM-DD) and bell number (1..7).
     */
    fun isFreeAt(dateIso: String, bellNumber: Int): Boolean {
        val busyBells = busy[dateIso] ?: return true
        return bellNumber !in busyBells
    }

    /**
     * Returns the list of free bell numbers (1..maxBells) on the specified day.
     */
    fun getFreeBells(dateIso: String, maxBells: Int = 7): List<Int> {
        val busyBells = busy[dateIso]?.toSet() ?: emptySet()
        return (1..maxBells).filter { it !in busyBells }
    }

    /**
     * Calculates until when this room remains free starting from the specified bell.
     */
    fun getFreeUntilDescription(dateIso: String, currentBell: Int, bellSlots: List<FreeRoomBellSlot>): String {
        val busyBells = busy[dateIso]?.toSet() ?: emptySet()
        if (busyBells.isEmpty()) {
            return "Весь день"
        }
        val nextBusyBell = (currentBell..7).firstOrNull { it in busyBells }
        return if (nextBusyBell != null) {
            val slot = bellSlots.firstOrNull { it.bell == nextBusyBell }
            if (slot != null) "До ${slot.start}" else "До пары $nextBusyBell"
        } else {
            "До конца дня"
        }
    }

    /**
     * Returns a summary description of room availability across all bells on given date.
     */
    fun getFreeSummaryDescription(dateIso: String, maxBells: Int = 7): String {
        val busyBells = busy[dateIso]?.toSet() ?: emptySet()
        if (busyBells.isEmpty()) return "Весь день"
        val freeCount = (1..maxBells).count { it !in busyBells }
        if (freeCount == 0) return "Занята весь день"
        if (freeCount == maxBells) return "Весь день"
        return "Свободно: $freeCount из $maxBells пар"
    }
}
