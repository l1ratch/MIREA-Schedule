package com.jetbrains.kmpapp.data.update

import com.jetbrains.kmpapp.data.model.AppVersion
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

@Serializable
data class GitHubAsset(
    val name: String = "",
    @SerialName("browser_download_url")
    val browserDownloadUrl: String = ""
)

@Serializable
data class GitHubRelease(
    @SerialName("tag_name")
    val tagName: String = "",
    val name: String? = null,
    val body: String? = null,
    @SerialName("html_url")
    val htmlUrl: String = "",
    val assets: List<GitHubAsset> = emptyList()
)

data class UpdateCheckResult(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val currentVersion: String = AppVersion.VERSION_NAME,
    val changelog: String? = null,
    val downloadUrl: String,
    val releaseUrl: String
)

class AppUpdateChecker(
    private val client: HttpClient,
    private val syncManager: com.jetbrains.kmpapp.data.sync.UnifiedSyncManager
) {
    companion object {
        private const val GITHUB_REPO = AppVersion.GITHUB_REPO
    }

    suspend fun checkForUpdates(): UpdateCheckResult? = withContext(Dispatchers.IO) {
        try {
            val response = client.get("https://api.github.com/repos/$GITHUB_REPO/releases/latest") {
                header("User-Agent", "MIREA-Schedule-App")
            }
            if (response.status.value !in 200..299) {
                return@withContext null
            }
            val release = response.body<GitHubRelease>()
            if (release.tagName.isBlank()) {
                return@withContext null
            }

            val latestTag = release.tagName.trimStart('v', 'V')
            val current = AppVersion.VERSION_NAME.trimStart('v', 'V')
            val isNewer = compareVersions(latestTag, current) > 0

            val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
            val downloadUrl = apkAsset?.browserDownloadUrl ?: release.htmlUrl

            UpdateCheckResult(
                hasUpdate = isNewer,
                latestVersion = release.tagName,
                currentVersion = AppVersion.VERSION_NAME,
                changelog = release.body,
                downloadUrl = downloadUrl,
                releaseUrl = release.htmlUrl
            )
        } catch (t: Throwable) {
            println("Update check error: ${t.message}")
            null
        }
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split('.').mapNotNull { it.toIntOrNull() }
        val parts2 = v2.split('.').mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) return p1.compareTo(p2)
        }
        return 0
    }

    suspend fun fetchContributors(forceRefresh: Boolean = false): List<com.jetbrains.kmpapp.data.model.GitHubContributor> = withContext(Dispatchers.IO) {
        try {
            val strategy = if (forceRefresh) com.jetbrains.kmpapp.data.sync.CacheStrategy.NETWORK_FIRST else com.jetbrains.kmpapp.data.sync.CacheStrategy.CACHE_FIRST
            val result = syncManager.execute(
                cacheKey = "cached_github_contributors_json",
                serializer = kotlinx.serialization.builtins.ListSerializer(com.jetbrains.kmpapp.data.model.GitHubContributor.serializer()),
                strategy = strategy,
                ttl = kotlin.time.Duration.parse("7d"),
                forceRefresh = forceRefresh
            ) {
                client.get("https://api.github.com/repos/$GITHUB_REPO/contributors") {
                    header("User-Agent", "MIREA-Schedule-App")
                }.body<String>()
            }
            when (result) {
                is com.jetbrains.kmpapp.data.sync.SyncResult.Success -> result.data
                is com.jetbrains.kmpapp.data.sync.SyncResult.Error -> result.cachedData ?: emptyList()
            }
        } catch (t: Throwable) {
            println("Fetch contributors error: ${t.message}")
            emptyList()
        }
    }
}
