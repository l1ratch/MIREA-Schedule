package com.jetbrains.kmpapp.data.sync

import com.jetbrains.kmpapp.data.DebugConfig
import com.jetbrains.kmpapp.data.model.AppErrorCode
import com.jetbrains.kmpapp.data.storage.PlatformStorage
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlin.time.Clock
import kotlin.time.Duration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

enum class CacheStrategy {
    CACHE_FIRST,
    NETWORK_FIRST,
    STALE_WHILE_REVALIDATE
}

private fun currentEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()

sealed class SyncResult<out T> {
    data class Success<T>(
        val data: T,
        val isFromCache: Boolean,
        val timestampMillis: Long = currentEpochMillis()
    ) : SyncResult<T>()

    data class Error<T>(
        val code: AppErrorCode,
        val message: String,
        val cachedData: T? = null
    ) : SyncResult<T>()
}

class UnifiedSyncManager(
    private val storage: PlatformStorage
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private fun getTimestampKey(key: String) = "${key}_sync_timestamp"

    fun getLastSyncTime(key: String): Long {
        return storage.getString(getTimestampKey(key))?.toLongOrNull() ?: 0L
    }

    private fun saveSyncTime(key: String, timestamp: Long) {
        storage.saveString(getTimestampKey(key), timestamp.toString())
    }

    fun isCacheValid(key: String, ttl: Duration): Boolean {
        if (ttl == Duration.INFINITE) return true
        val lastTime = getLastSyncTime(key)
        if (lastTime <= 0L) return false
        val now = currentEpochMillis()
        val elapsed = now - lastTime
        val limit = ttl.inWholeMilliseconds
        return elapsed < limit
    }

    suspend fun <T> execute(
        cacheKey: String,
        serializer: KSerializer<T>,
        strategy: CacheStrategy = CacheStrategy.CACHE_FIRST,
        ttl: Duration = Duration.INFINITE,
        forceRefresh: Boolean = false,
        fetcher: suspend () -> String
    ): SyncResult<T> {
        // Check offline simulation
        if (DebugConfig.isOfflineSimulated.value) {
            val cached = getCachedData(cacheKey, serializer)
            return if (cached != null) {
                SyncResult.Error(
                    code = AppErrorCode.ERR_NO_NETWORK,
                    message = "Симуляция отсутствия сети (данные из кеша)",
                    cachedData = cached
                )
            } else {
                SyncResult.Error(
                    code = AppErrorCode.ERR_NO_NETWORK,
                    message = "Симуляция отсутствия сети (кеш пуст)"
                )
            }
        }

        val simulatedDelay = DebugConfig.networkDelayMs.value
        if (simulatedDelay > 0) {
            delay(simulatedDelay)
        }

        val hasValidCache = !forceRefresh && isCacheValid(cacheKey, ttl)

        when (strategy) {
            CacheStrategy.CACHE_FIRST -> {
                if (hasValidCache) {
                    val cached = getCachedData(cacheKey, serializer)
                    if (cached != null) {
                        return SyncResult.Success(cached, isFromCache = true, timestampMillis = getLastSyncTime(cacheKey))
                    }
                }
                return fetchAndCache(cacheKey, serializer, fetcher)
            }

            CacheStrategy.NETWORK_FIRST -> {
                try {
                    val rawJson = fetcher()
                    val data = json.decodeFromString(serializer, rawJson)
                    storage.saveString(cacheKey, rawJson)
                    val now = currentEpochMillis()
                    saveSyncTime(cacheKey, now)
                    return SyncResult.Success(data, isFromCache = false, timestampMillis = now)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    val cached = getCachedData(cacheKey, serializer)
                    val (code, msg) = mapExceptionToError(e)
                    return SyncResult.Error(code, msg, cached)
                }
            }

            CacheStrategy.STALE_WHILE_REVALIDATE -> {
                val cached = getCachedData(cacheKey, serializer)
                if (cached != null && hasValidCache) {
                    return SyncResult.Success(cached, isFromCache = true, timestampMillis = getLastSyncTime(cacheKey))
                }
                return fetchAndCache(cacheKey, serializer, fetcher)
            }
        }
    }

    private suspend fun <T> fetchAndCache(
        cacheKey: String,
        serializer: KSerializer<T>,
        fetcher: suspend () -> String
    ): SyncResult<T> {
        return try {
            val rawJson = fetcher()
            val data = json.decodeFromString(serializer, rawJson)
            storage.saveString(cacheKey, rawJson)
            val now = currentEpochMillis()
            saveSyncTime(cacheKey, now)
            SyncResult.Success(data, isFromCache = false, timestampMillis = now)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            val cached = getCachedData(cacheKey, serializer)
            val (code, msg) = mapExceptionToError(e)
            SyncResult.Error(code, msg, cached)
        }
    }

    fun <T> getCachedData(cacheKey: String, serializer: KSerializer<T>): T? {
        val raw = storage.getString(cacheKey) ?: return null
        return try {
            json.decodeFromString(serializer, raw)
        } catch (e: Exception) {
            println("UnifiedSyncManager: error decoding cache for $cacheKey: ${e.message}")
            null
        }
    }

    fun clearCache(cacheKey: String) {
        storage.saveString(cacheKey, "")
        saveSyncTime(cacheKey, 0L)
    }

    private fun mapExceptionToError(e: Exception): Pair<AppErrorCode, String> {
        return when (e) {
            is SocketTimeoutException,
            is ConnectTimeoutException,
            is HttpRequestTimeoutException -> {
                Pair(AppErrorCode.ERR_TIMEOUT, "Превышено время ожидания сервера")
            }
            is SerializationException -> {
                Pair(AppErrorCode.ERR_PARSE_ERROR, "Ошибка обработки ответа сервера")
            }
            else -> {
                val msg = e.message ?: ""
                if (msg.contains("Failed to connect", ignoreCase = true) ||
                    msg.contains("No route to host", ignoreCase = true) ||
                    msg.contains("UnknownHost", ignoreCase = true) ||
                    msg.contains("Network is unreachable", ignoreCase = true)
                ) {
                    Pair(AppErrorCode.ERR_NO_NETWORK, "Нет подключения к интернету")
                } else {
                    Pair(AppErrorCode.ERR_SERVER_ERROR, e.message ?: "Сетевая ошибка")
                }
            }
        }
    }
}
