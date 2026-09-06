package com.jetbrains.kmpapp.screens.map

import kotlinx.serialization.Serializable

data class Campus(
    val id: String,
    val name: String,
    val shortName: String,
    val address: String,
    val floors: List<Int>,
    val defaultFloor: Int
)

@Serializable
data class MapObject(
    val id: String,
    val type: String,
    val name: String,
    val description: String? = null
)

@Serializable
data class MapObjectsContainer(
    val objects: List<MapObject> = emptyList()
)

data class RoomSearchResult(
    val id: String,
    val name: String,
    val type: String,
    val campusId: String,
    val campusName: String,
    val floor: Int
)

val CAMPUSES = listOf(
    Campus(
        id = "s-20",
        name = "Стромынка 20",
        shortName = "С-20",
        address = "ул. Стромынка, 20",
        floors = listOf(4, 3, 2, 1),
        defaultFloor = 1
    ),
    Campus(
        id = "v-78",
        name = "Вернадского 78",
        shortName = "В-78",
        address = "проспект Вернадского, 78",
        floors = listOf(4, 3, 2, 1, 0),
        defaultFloor = 1
    ),
    Campus(
        id = "v-86",
        name = "Вернадского 86",
        shortName = "В-86",
        address = "проспект Вернадского, 86",
        floors = listOf(7, 6, 5, 4, 3, 2, 1, 0),
        defaultFloor = 1
    ),
    Campus(
        id = "mp-1",
        name = "Малая Пироговская 1",
        shortName = "МП-1",
        address = "Малая Пироговская ул., 1",
        floors = listOf(5, 4, 3, 2, 1, -1),
        defaultFloor = 1
    )
)
