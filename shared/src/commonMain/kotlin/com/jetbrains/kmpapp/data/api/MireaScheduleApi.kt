package com.jetbrains.kmpapp.data.api

import com.jetbrains.kmpapp.data.model.ScheduleTarget
import com.jetbrains.kmpapp.data.model.ScheduleTargetType
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.serialization.Serializable

import com.jetbrains.kmpapp.data.DebugConfig
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.delay

@Serializable
private data class SearchResponse(
    val data: List<ScheduleTarget> = emptyList()
)

class MireaScheduleApi(private val client: HttpClient) {

    private val baseUrl = "https://schedule-of.mirea.ru"

    suspend fun search(query: String, limit: Int = 20): List<ScheduleTarget> {
        if (DebugConfig.isOfflineSimulated.value) {
            throw IOException("Simulated network offline")
        }
        val delayMs = DebugConfig.networkDelayMs.value
        if (delayMs > 0) {
            delay(delayMs)
        }
        val trimmed = query.trim()
        val response: SearchResponse = client.get("$baseUrl/schedule/api/search") {
            header(HttpHeaders.UserAgent, "university-app-schedule-fetcher/0.1")
            header(HttpHeaders.Accept, "application/json")
            if (trimmed.isNotEmpty()) {
                parameter("match", trimmed)
            }
            parameter("limit", limit)
        }.body()
        return response.data
    }

    suspend fun getIcal(targetType: ScheduleTargetType, id: Int): String {
        if (DebugConfig.isOfflineSimulated.value) {
            throw IOException("Simulated network offline")
        }
        val delayMs = DebugConfig.networkDelayMs.value
        if (delayMs > 0) {
            delay(delayMs)
        }
        return client.get("$baseUrl/schedule/api/ical/${targetType.pathName}/$id") {
            header(HttpHeaders.UserAgent, "university-app-schedule-fetcher/0.1")
            parameter("includeMeta", "true")
        }.bodyAsText()
    }
}

