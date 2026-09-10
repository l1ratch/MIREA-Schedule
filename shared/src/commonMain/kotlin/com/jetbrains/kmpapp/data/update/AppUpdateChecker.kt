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
    MINOR_BUILD,
    NEW_VERSION,
    CRITICAL
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
    val ipaUrl: String? = null,
    val channel: String = "stable",
    val prerelease: Boolean = false
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
    val releaseUrl: String,
    val apkUrl: String? = null,
    val channel: String = "stable",
    val isPrerelease: Boolean = false
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

    suspend fun checkForUpdates(includeBeta: Boolean = false): UpdateCheckResult? = withContext(Dispatchers.IO) {
        if (AppVersion.isTestBuild && !includeBeta) {
            return@withContext upToDateResult()
        }

        val stableResult = fetchFeedResult(AppVersion.UPDATE_FEED_URL, channel = "stable", isPrerelease = false)
        val betaResult = if (includeBeta) {
            fetchFeedResult(AppVersion.BETA_FEED_URL, channel = "beta", isPrerelease = true)
        } else {
            null
        }

        pickBestResult(betaResult, stableResult) ?: fetchLatestReleaseResult()
    }

    private suspend fun fetchFeedResult(url: String, channel: String, isPrerelease: Boolean): UpdateCheckResult? {
        return try {
            val response = client.get(url) {
                header("User-Agent", "MIREA-Schedule-App")
            }
            if (response.status.value !in 200..299) return null
            val feed = json.decodeFromString<VersionFeed>(response.body<String>())
            if (feed.version.isBlank()) return null

            val hasNewerVersion = VersionComparator.compare(feed.version, AppVersion.VERSION_NAME) > 0
            val hasNewerBuild = feed.build > AppVersion.BUILD_NUMBER
            val isUnderMinSupported = AppVersion.BUILD_NUMBER < feed.minSupportedBuild
            val isCritical = !isPrerelease && (isUnderMinSupported || (feed.critical && (hasNewerVersion || hasNewerBuild)))

            // NEW_VERSION требует и новую версию, и новую сборку: легаси-релизы (26.9.x)
            // численно больше всей линии 26.0.0, но их build (79) меньше любого epoch —
            // без этой проверки dev/beta-сборкам предлагался бы даунгрейд до 26.9.1
            val urgency = when {
                isCritical -> UpdateUrgency.CRITICAL
                hasNewerVersion && hasNewerBuild -> UpdateUrgency.NEW_VERSION
                hasNewerBuild -> UpdateUrgency.MINOR_BUILD
                else -> UpdateUrgency.UP_TO_DATE
            }

            val releaseUrl = if (isPrerelease) {
                "https://github.com/$GITHUB_REPO/releases/tag/preview"
            } else {
                "https://github.com/$GITHUB_REPO/releases/latest"
            }
            val downloadUrl = feed.downloadUrl ?: feed.apkUrl ?: releaseUrl

            UpdateCheckResult(
                urgency = urgency,
                latestVersion = feed.version,
                latestBuild = feed.build,
                currentVersion = AppVersion.VERSION_NAME,
                currentBuild = AppVersion.BUILD_NUMBER,
                isCritical = isCritical,
                changelog = feed.changelog,
                downloadUrl = downloadUrl,
                releaseUrl = releaseUrl,
                apkUrl = feed.apkUrl ?: feed.downloadUrl,
                channel = feed.channel.ifBlank { channel },
                isPrerelease = isPrerelease
            )
        } catch (e: Throwable) {
            println("Feed check error ($url): ${e.message}")
            null
        }
    }

    /** Из двух кандидатов выбирает более свежую версию; при равенстве выигрывает стабильная. */
    private fun pickBestResult(preview: UpdateCheckResult?, stable: UpdateCheckResult?): UpdateCheckResult? {
        if (preview == null) return stable
        if (stable == null) return preview
        // Кандидат с доступным обновлением приоритетнее «актуального»:
        // иначе легаси-стабильный (26.9.x, «актуально» после guard'а сборок)
        // строково обыгрывал бы новую бету (26.0.0-beta.1)
        if (preview.hasUpdate != stable.hasUpdate) {
            return if (preview.hasUpdate) preview else stable
        }
        val c = VersionComparator.compare(preview.latestVersion, stable.latestVersion)
        return if (c > 0) preview else stable
    }

    private suspend fun fetchLatestReleaseResult(): UpdateCheckResult? {
        return try {
            val response = client.get("https://api.github.com/repos/$GITHUB_REPO/releases/latest") {
                header("User-Agent", "MIREA-Schedule-App")
            }
            if (response.status.value !in 200..299) return null
            val release = response.body<GitHubRelease>()
            if (release.tagName.isBlank()) return null

            val latestTag = release.tagName.trimStart('v', 'V')
            val isNewerVersion = VersionComparator.compare(latestTag, AppVersion.VERSION_NAME) > 0

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
                releaseUrl = release.htmlUrl,
                apkUrl = apkAsset?.browserDownloadUrl,
                channel = "stable",
                isPrerelease = false
            )
        } catch (t: Throwable) {
            println("GitHub API update check error: ${t.message}")
            null
        }
    }

    private fun upToDateResult() = UpdateCheckResult(
        urgency = UpdateUrgency.UP_TO_DATE,
        latestVersion = AppVersion.VERSION_NAME,
        latestBuild = AppVersion.BUILD_NUMBER,
        currentVersion = AppVersion.VERSION_NAME,
        currentBuild = AppVersion.BUILD_NUMBER,
        downloadUrl = "https://github.com/$GITHUB_REPO/releases/latest",
        releaseUrl = "https://github.com/$GITHUB_REPO/releases/latest",
        channel = AppVersion.BUILD_CHANNEL,
        isPrerelease = AppVersion.isTestBuild
    )

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
