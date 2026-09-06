package com.jetbrains.kmpapp.data.update

import com.jetbrains.kmpapp.data.model.AppVersion
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

enum class UpdateUrgency {
    UP_TO_DATE,
    MINOR_BUILD, // Жёлтый: новая сборка той же версии (микро-правки, необязательно)
    NEW_VERSION, // Красный: вышла новая версия (важное обновление, уведомить)
    CRITICAL     // Пурпурный/бордовый: критические уязвимости/ошибки (обязательно)
}

@Serializable
data class VersionFeed(
    val version: String = "",
    val build: Int = 0,
    val critical: Boolean = false,
    @SerialName("min_supported_build")
    val minSupportedBuild: Int = 0,
    val changelog: String? = null,
    @SerialName("download_url")
    val downloadUrl: String? = null,
    @SerialName("apk_url")
    val apkUrl: String? = null,
    @SerialName("ipa_url")
    val ipaUrl: String? = null
)

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
    val urgency: UpdateUrgency,
    val latestVersion: String,
    val latestBuild: Int,
    val currentVersion: String = AppVersion.VERSION_NAME,
    val currentBuild: Int = AppVersion.BUILD_NUMBER,
    val isCritical: Boolean = false,
    val changelog: String? = null,
    val downloadUrl: String,
    val releaseUrl: String
) {
    val hasUpdate: Boolean get() = urgency != UpdateUrgency.UP_TO_DATE
}

class AppUpdateChecker(
    private val client: HttpClient,
    private val syncManager: com.jetbrains.kmpapp.data.sync.UnifiedSyncManager
) {
    companion object {
        private const val GITHUB_REPO = AppVersion.GITHUB_REPO
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun checkForUpdates(): UpdateCheckResult? = withContext(Dispatchers.IO) {
        // 1. Try fetching our dedicated version.json from the gh-pages branch
        try {
            val response = client.get(AppVersion.VERSION_FEED_URL) {
                header("User-Agent", "MIREA-Schedule-App")
            }
            if (response.status.value in 200..299) {
                val rawJson = response.body<String>()
                val feed = json.decodeFromString<VersionFeed>(rawJson)

                val hasNewerVersion = compareVersions(feed.version, AppVersion.VERSION_NAME) > 0
                val hasNewerBuild = feed.build > AppVersion.BUILD_NUMBER
                val isUnderMinSupported = AppVersion.BUILD_NUMBER < feed.minSupportedBuild
                val isCritical = isUnderMinSupported || (feed.critical && (hasNewerVersion || hasNewerBuild))

                val urgency = when {
                    isCritical -> UpdateUrgency.CRITICAL
                    hasNewerVersion -> UpdateUrgency.NEW_VERSION
                    hasNewerBuild -> UpdateUrgency.MINOR_BUILD
                    else -> UpdateUrgency.UP_TO_DATE
                }

                val downloadUrl = feed.downloadUrl 
                    ?: feed.apkUrl 
                    ?: "https://github.com/$GITHUB_REPO/releases/latest"

                return@withContext UpdateCheckResult(
                    urgency = urgency,
                    latestVersion = feed.version.ifBlank { AppVersion.VERSION_NAME },
                    latestBuild = feed.build,
                    currentVersion = AppVersion.VERSION_NAME,
                    currentBuild = AppVersion.BUILD_NUMBER,
                    isCritical = isCritical,
                    changelog = feed.changelog,
                    downloadUrl = downloadUrl,
                    releaseUrl = "https://github.com/$GITHUB_REPO/releases/latest"
                )
            }
        } catch (e: Throwable) {
            println("Version feed check error: ${e.message}, falling back to GitHub API")
        }

        // 2. Fallback to GitHub Releases API
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
            val isNewerVersion = compareVersions(latestTag, current) > 0

            val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
            val downloadUrl = apkAsset?.browserDownloadUrl ?: release.htmlUrl

            UpdateCheckResult(
                urgency = if (isNewerVersion) UpdateUrgency.NEW_VERSION else UpdateUrgency.UP_TO_DATE,
                latestVersion = release.tagName,
                latestBuild = AppVersion.BUILD_NUMBER,
                currentVersion = AppVersion.VERSION_NAME,
                currentBuild = AppVersion.BUILD_NUMBER,
                isCritical = false,
                changelog = release.body,
                downloadUrl = downloadUrl,
                releaseUrl = release.htmlUrl
            )
        } catch (t: Throwable) {
            println("GitHub API update check error: ${t.message}")
            null
        }
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.trimStart('v', 'V').split('.').mapNotNull { it.toIntOrNull() }
        val parts2 = v2.trimStart('v', 'V').split('.').mapNotNull { it.toIntOrNull() }
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
