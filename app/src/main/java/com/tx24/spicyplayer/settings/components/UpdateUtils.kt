package com.tx24.spicyplayer.settings.components

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL

data class GitHubRelease(
    val tagName: String,
    val htmlUrl: String,
    val body: String
)

data class AppVersion(
    val name: String,
    val code: Long
)

fun getInstalledAppVersion(context: Context): AppVersion {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    return AppVersion(
        name = packageInfo.versionName.orEmpty(),
        code = PackageInfoCompat.getLongVersionCode(packageInfo)
    )
}

sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Checking : UpdateStatus()
    data class NewVersion(val release: GitHubRelease) : UpdateStatus()
    data class UpToDate(val isManual: Boolean) : UpdateStatus()
    data class Error(val isManual: Boolean, val message: String) : UpdateStatus()
}

object GitHubUpdateChecker {
    private const val REPO_OWNER = "TheX24"
    private const val REPO_NAME = "Spicy-Player"
    private const val LATEST_RELEASE_URL = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"

    suspend fun getLatestRelease(): GitHubRelease? = withContext(Dispatchers.IO) {
        try {
            val url = URL(LATEST_RELEASE_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                GitHubRelease(
                    tagName = json.getString("tag_name"),
                    htmlUrl = json.getString("html_url"),
                    body = json.optString("body", "")
                )
            } else {
                Timber.e("Failed to fetch release: %d", connection.responseCode)
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching latest release")
            null
        }
    }
}
