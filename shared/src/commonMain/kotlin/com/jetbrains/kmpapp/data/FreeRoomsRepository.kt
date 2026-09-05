package com.jetbrains.kmpapp.data

import com.jetbrains.kmpapp.data.model.FreeRoomItem
import com.jetbrains.kmpapp.data.model.FreeRoomsData
import com.jetbrains.kmpapp.data.sync.CacheStrategy
import com.jetbrains.kmpapp.data.sync.SyncResult
import com.jetbrains.kmpapp.data.sync.UnifiedSyncManager
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlin.time.Duration.Companion.hours

class FreeRoomsRepository(
    private val client: HttpClient,
    private val syncManager: UnifiedSyncManager
) {
    private val cdnUrl = "https://raw.githubusercontent.com/l1ratch/MIREA-Schedule/gh-pages/free_rooms.json"
    private val cacheKey = "cached_free_rooms_json"

    /**
     * Loads free rooms data: from cache or fetches fresh copy via UnifiedSyncManager.
     */
    suspend fun getFreeRooms(forceRefresh: Boolean = false): SyncResult<FreeRoomsData> {
        val strategy = if (forceRefresh) CacheStrategy.NETWORK_FIRST else CacheStrategy.CACHE_FIRST
        return syncManager.execute(
            cacheKey = cacheKey,
            serializer = FreeRoomsData.serializer(),
            strategy = strategy,
            ttl = 4.hours,
            forceRefresh = forceRefresh
        ) {
            client.get(cdnUrl).body()
        }
    }

    /**
     * Filters rooms by campus, floor, day, bell number, and optional search query.
     */
    fun filterFreeRooms(
        allRooms: List<FreeRoomItem>,
        campus: String,
        floor: Int?,
        dateIso: String,
        bellNumber: Int,
        searchQuery: String = ""
    ): List<FreeRoomItem> {
        val query = searchQuery.trim().lowercase()
        return allRooms.filter { room ->
            val matchCampus = room.campus.equals(campus, ignoreCase = true)
            val matchFloor = floor == null || room.floor == floor
            val matchFree = room.isFreeAt(dateIso, bellNumber)
            val matchQuery = query.isEmpty() || room.name.lowercase().contains(query)
            matchCampus && matchFloor && matchFree && matchQuery
        }
    }
}
