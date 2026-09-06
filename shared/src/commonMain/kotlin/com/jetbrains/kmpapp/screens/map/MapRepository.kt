package com.jetbrains.kmpapp.screens.map

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kmp_app_template.shared.generated.resources.Res

object MapRepository {
    private val svgCache = mutableMapOf<String, String>()
    private var allObjects: List<MapObject>? = null

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun loadFloorSvg(campusId: String, floor: Int): String? = withContext(Dispatchers.IO) {
        val cacheKey = "$campusId-$floor"
        svgCache[cacheKey]?.let { return@withContext it }

        val path = when (campusId) {
            "s-20" -> "files/maps/s-20/floor_$floor.svg"
            "v-78" -> "files/maps/v-78/floor_$floor.svg"
            "v-86" -> "files/maps/v-86/floor_$floor.svg"
            "mp-1" -> if (floor == -1) "files/maps/mp-1/floor_m1.svg" else "files/maps/mp-1/floor_$floor.svg"
            else -> return@withContext null
        }

        try {
            val bytes = Res.readBytes(path)
            val content = bytes.decodeToString()
            svgCache[cacheKey] = content
            content
        } catch (e: Exception) {
            println("Failed to load SVG for $path: ${e.message}")
            null
        }
    }

    suspend fun loadObjects(): List<MapObject> = withContext(Dispatchers.IO) {
        allObjects?.let { return@withContext it }

        try {
            val bytes = Res.readBytes("files/maps/objects.json")
            val container = json.decodeFromString<MapObjectsContainer>(bytes.decodeToString())
            allObjects = container.objects
            container.objects
        } catch (e: Exception) {
            println("Failed to load objects.json: ${e.message}")
            emptyList()
        }
    }

    suspend fun searchRooms(query: String): List<RoomSearchResult> {
        val trimmed = query.trim()
        if (trimmed.length < 2) return emptyList()

        val objects = loadObjects()
        val queryLower = trimmed.lowercase()

        return objects
            .filter { it.type == "room" && it.name.lowercase().contains(queryLower) }
            .take(15)
            .map { obj ->
                val (campusId, campusName, floor) = guessCampusAndFloor(obj.name)
                RoomSearchResult(
                    id = obj.id,
                    name = obj.name,
                    type = obj.type,
                    campusId = campusId,
                    campusName = campusName,
                    floor = floor
                )
            }
    }

    private fun guessCampusAndFloor(name: String): Triple<String, String, Int> {
        val trimmed = name.trim()
        // Check V-78 style: letter + hyphen + digits, e.g. "Г-302", "А-438", "В-201"
        val v78Match = Regex("^[А-ЯA-Z]-([0-9])").find(trimmed)
        if (v78Match != null) {
            val floor = v78Match.groupValues[1].toIntOrNull() ?: 1
            return Triple("v-78", "Вернадского 78", floor)
        }

        // Check 3-digit room, e.g. "127", "234", "315" -> likely S-20 or MP-1
        val numMatch = Regex("^([0-9])").find(trimmed)
        if (numMatch != null) {
            val floor = numMatch.groupValues[1].toIntOrNull() ?: 1
            return Triple("s-20", "Стромынка 20", floor)
        }

        return Triple("s-20", "Стромынка 20", 1)
    }
}
