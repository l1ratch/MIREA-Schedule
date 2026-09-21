package com.jetbrains.kmpapp.data.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.Serializable

@Serializable
private data class VkGroupDto(
    val id: Long,
    val photo_200: String? = null,
    val photo_100: String? = null
)

@Serializable
private data class VkGroupsResponse(
    val response: List<VkGroupDto>? = null,
    val error: VkErrorDto? = null
)

@Serializable
private data class VkErrorDto(
    val error_msg: String? = null
)

/**
 * Best-effort получение аватарки сообщества ВК по screen_name (метод groups.getById
 * не требует токена). Если сети нет / метод недоступен / сообщества нет — вернёт null,
 * и UI покажет символ-эмблему вместо картинки. Кэш: каждый screen_name запрашивается
 * не более одного раза за сессию.
 */
class VkAvatarResolver(private val client: HttpClient) {
    private val cache = mutableMapOf<String, String>()

    suspend fun resolveAvatarUrl(screenName: String): String? {
        if (screenName.isBlank()) return null
        cache[screenName]?.let { return it.ifEmpty { null } }

        val result = try {
            val url = "https://api.vk.com/method/groups.getById" +
                "?group_ids=$screenName&fields=photo_200&v=5.131"
            val response: VkGroupsResponse = client.get(url).body()
            response.response
                ?.firstOrNull()
                ?.photo_200
                ?: response.error?.let { null }
        } catch (_: Exception) {
            null
        }

        cache[screenName] = result ?: ""
        return result
    }
}