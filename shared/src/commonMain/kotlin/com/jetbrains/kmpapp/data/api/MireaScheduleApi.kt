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

@Serializable
private data class SearchResponse(
    val data: List<ScheduleTarget> = emptyList()
)

class MireaScheduleApi(private val client: HttpClient) {

    private val baseUrl = "https://schedule-of.mirea.ru"

    suspend fun search(query: String, limit: Int = 20): List<ScheduleTarget> {
        val trimmed = query.trim()
        val response: SearchResponse = client.get("/schedule/api/search") {
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
        return client.get("/schedule/api/ical//") {
            header(HttpHeaders.UserAgent, "university-app-schedule-fetcher/0.1")
            parameter("includeMeta", "true")
        }.bodyAsText()
    }
}
