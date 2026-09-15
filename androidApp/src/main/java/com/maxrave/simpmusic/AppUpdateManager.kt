package com.maxrave.simpmusic

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class UpdateAvailable(val versionName: String, val downloadUrl: String, val changelog: String) : UpdateState
    data class AlreadyLatest(val currentVersion: String) : UpdateState
    data class Downloading(val progress: Float) : UpdateState
    data class ReadyToInstall(val apkFile: File) : UpdateState
    data class Error(val message: String) : UpdateState
}

class AppUpdateManager(private val context: Context) {

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val repoOwner = "silenteye1"
    private val repoName = "DHUN-Music"

    private fun getUpdateDir(): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        val updateFolder = File(dir, "dhun_updates")
        if (!updateFolder.exists()) updateFolder.mkdirs()
        return updateFolder
    }

    fun getDownloadedApk(versionName: String): File? {
        val apkFile = File(getUpdateDir(), "dhun_${versionName}.apk")
        return if (apkFile.exists() && apkFile.length() > 1024 * 1024) apkFile else null
    }

    suspend fun checkForUpdates(currentVersion: String, isManual: Boolean = false) {
        if (isManual) {
            _updateState.value = UpdateState.Checking
        }
        withContext(Dispatchers.IO) {
            try {
                val apiUrl = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"
                val connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                    connectTimeout = 8000
                    readTimeout = 8000
                }

                if (connection.responseCode == 200) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(responseText)
                    val tagName = json.optString("tag_name", "").removePrefix("v").trim()
                    val changelog = json.optString("body", "Bug fixes and improvements")

                    var downloadUrl = ""
                    val assets = json.optJSONArray("assets")
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.optString("name", "")
                            if (name.endsWith(".apk", ignoreCase = true)) {
                                downloadUrl = asset.optString("browser_download_url", "")
                                break
                            }
                        }
                    }

                    if (isNewerVersion(currentVersion, tagName) && downloadUrl.isNotEmpty()) {
                        val existingApk = getDownloadedApk(tagName)
                        if (existingApk != null) {
                            // Agar naya version pehle se pura downloaded pada hai to direct install dikhao
                            _updateState.value = UpdateState.ReadyToInstall(existingApk)
                        } else {
                            _updateState.value = UpdateState.UpdateAvailable(
                                versionName = tagName,
                                downloadUrl = downloadUrl,
                                changelog = changelog
                            )
                        }
                    } else {
                        cleanOldApks()
                        if (isManual) {
                            _updateState.value = UpdateState.AlreadyLatest(currentVersion)
                        } else {
                            _updateState.value = UpdateState.Idle
                        }
                    }
                } else {
                    if (isManual) {
                        _updateState.value = UpdateState.AlreadyLatest(currentVersion)
                    } else {
                        _updateState.value = UpdateState.Idle
                    }
                }
            } catch (e: Exception) {
                if (isManual) {
                    _updateState.value = UpdateState.Error("Could not check for update: ${e.localizedMessage}")
                } else {
                    _updateState.value = UpdateState.Idle
                }
            }
        }
    }

    suspend fun downloadAndInstallApk(downloadUrl: String, versionName: String = "") {
        withContext(Dispatchers.IO) {
            try {
                val updateDir = getUpdateDir()
                val fileName = if (versionName.isNotEmpty()) "dhun_${versionName}.apk" else "dhun_update.apk"
                val apkFile = File(updateDir, fileName)
                val tempFile = File(updateDir, "${fileName}.temp")

                val connection = (URL(downloadUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15000
                    readTimeout = 15000
                    instanceFollowRedirects = true
                }

                val totalBytes = connection.contentLengthLong

                // Agar file already download ho chuki hai aur complete size ki hai, direct install karein
                if (apkFile.exists() && (totalBytes <= 0 || apkFile.length() == totalBytes)) {
                    _updateState.value = UpdateState.ReadyToInstall(apkFile)
                    installApk(apkFile)
                    return@withContext
                }

                if (tempFile.exists()) tempFile.delete()

                connection.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        var downloadedBytes = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            if (totalBytes > 0) {
                                val progress = downloadedBytes.toFloat() / totalBytes.toFloat()
                                _updateState.value = UpdateState.Downloading(progress.coerceIn(0f, 1f))
                            }
                        }
                        output.flush()
                    }
                }

                // Temporary file ko complete APK me tabhi badlein jab poora download ho chuka ho
                if (apkFile.exists()) apkFile.delete()
                if (tempFile.renameTo(apkFile)) {
                    _updateState.value = UpdateState.ReadyToInstall(apkFile)
                    installApk(apkFile)
                } else {
                    _updateState.value = UpdateState.Error("Failed to save APK file")
                }
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error("Download failed: ${e.localizedMessage}")
            }
        }
    }

    fun installApk(apkFile: File) {
        try {
            val authority = "${context.packageName}.FileProvider"
            val apkUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            _updateState.value = UpdateState.Error("Installation failed: ${e.localizedMessage}")
        }
    }

    fun cleanOldApks() {
        try {
            val dir = getUpdateDir()
            if (dir.exists()) {
                dir.deleteRecursively()
            }
        } catch (_: Exception) {}
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        val currParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(currParts.size, latestParts.size)

        for (i in 0 until maxLen) {
            val c = currParts.getOrElse(i) { 0 }
            val l = latestParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    fun resetState() {
        _updateState.value = UpdateState.Idle
    }
}