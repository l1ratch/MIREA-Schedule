package com.jetbrains.kmpapp.data.update

import com.jetbrains.kmpapp.data.model.AppVersion
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubAsset(
    val name: String,
    @SerialName("browser_download_url")
    val browserDownloadUrl: String
)

@Serializable
data class GitHubRelease(
    @SerialName("tag_name")
    val tagName: String,
    val name: String? = null,
    val body: String? = null,
    @SerialName("html_url")
    val htmlUrl: String,
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
    private val client: HttpClient
) {
    companion object {
        private const val GITHUB_REPO = AppVersion.GITHUB_REPO
    }

    suspend fun checkForUpdates(): UpdateCheckResult? {
        return try {
            val release = client.get("https://api.github.com/repos/$GITHUB_REPO/releases/latest") {
                header("User-Agent", "MIREA-Schedule-App")
            }.body<GitHubRelease>()

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
        } catch (e: Exception) {
            println("Update check error: ${e.message}")
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
}
