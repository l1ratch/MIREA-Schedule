package com.jetbrains.kmpapp.data

import com.jetbrains.kmpapp.data.model.FreeRoomItem
import com.jetbrains.kmpapp.data.model.FreeRoomsData
import com.jetbrains.kmpapp.data.storage.PlatformStorage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.json.Json

class FreeRoomsRepository(
    private val client: HttpClient,
    private val storage: PlatformStorage
) {
    private val cdnUrl = "https://raw.githubusercontent.com/l1ratch/MIREA-Schedule/gh-pages/free_rooms.json"
    private val cacheKey = "cached_free_rooms_json"
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Loads free rooms data: from disk cache first, or fetches fresh copy from GitHub Pages CDN.
     */
    suspend fun getFreeRooms(forceRefresh: Boolean = false): FreeRoomsData {
        if (DebugConfig.isOfflineSimulated.value) {
            val cached = storage.getString(cacheKey)
            return if (!cached.isNullOrEmpty()) {
                try {
                    json.decodeFromString<FreeRoomsData>(cached)
                } catch (_: Exception) {
                    FreeRoomsData()
                }
            } else {
                FreeRoomsData()
            }
        }

        if (!forceRefresh) {
            val cached = storage.getString(cacheKey)
            if (!cached.isNullOrEmpty()) {
                try {
                    return json.decodeFromString<FreeRoomsData>(cached)
                } catch (e: Exception) {
                    println("FreeRoomsRepository: cache decode failed: ${e.message}")
                }
            }
        }

        val delayMs = DebugConfig.networkDelayMs.value
        if (delayMs > 0) {
            kotlinx.coroutines.delay(delayMs)
        }

        return try {
            val responseText: String = client.get(cdnUrl).body()
            storage.saveString(cacheKey, responseText)
            json.decodeFromString<FreeRoomsData>(responseText)
        } catch (e: Exception) {
            println("FreeRoomsRepository: remote fetch failed: ${e.message}")
            val fallback = storage.getString(cacheKey)
            if (!fallback.isNullOrEmpty()) {
                try {
                    json.decodeFromString<FreeRoomsData>(fallback)
                } catch (_: Exception) {
                    FreeRoomsData()
                }
            } else {
                FreeRoomsData()
            }
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
