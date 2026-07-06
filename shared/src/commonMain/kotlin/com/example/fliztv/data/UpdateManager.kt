package com.example.fliztv.data

import com.example.fliztv.network.httpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class GitHubRelease(
    val tag_name: String = "",
    val name: String = "",
    val body: String = "",
    val html_url: String = "",
    val assets: List<GitHubAsset> = emptyList()
)

@Serializable
data class GitHubAsset(
    val name: String = "",
    val browser_download_url: String = "",
    val size: Long = 0
)

data class UpdateInfo(
    val hasUpdate: Boolean = false,
    val latestVersion: String = "",
    val downloadUrl: String = "",
    val releaseNotes: String = "",
    val currentVersion: String = "2.0.1"
)

object UpdateManager {
    private const val GITHUB_API = "https://api.github.com/repos/yourusername/fliztv/releases/latest"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun checkForUpdate(): UpdateInfo {
        return try {
            val response = httpClient.get(GITHUB_API).bodyAsText()
            val release = json.decodeFromString<GitHubRelease>(response)
            val latestTag = release.tag_name.removePrefix("v")
            val current = "2.0.1"

            val hasUpdate = compareVersions(latestTag, current) > 0
            val apkAsset = release.assets.find { it.name.endsWith(".apk") }

            UpdateInfo(
                hasUpdate = hasUpdate,
                latestVersion = latestTag,
                downloadUrl = apkAsset?.browser_download_url ?: release.html_url,
                releaseNotes = release.body,
                currentVersion = current
            )
        } catch (_: Exception) {
            UpdateInfo()
        }
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) return p1 - p2
        }
        return 0
    }
}
