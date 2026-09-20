package com.maxrave.simpmusic.expect.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import java.io.File

// Modifier.blur is backed by RenderEffect, added in API 31 (Android 12, VERSION_CODES.S). Below
// that the modifier is a documented no-op, so the style has to be gated rather than degraded.
actual fun isLyricsBlurSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

actual fun openAppLinkingSettings(context: Any?) {
    try {
        val androidContext = context as? Context ?: return
        val packageName = androidContext.packageName
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(
                Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                Uri.parse("package:$packageName"),
            )
        } else {
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName"),
            )
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        androidContext.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

actual fun getDownloadedUpdateApkSize(context: Any?): Long {
    return try {
        val androidContext = context as? Context ?: return 0L
        val dir = androidContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: androidContext.filesDir
        val updateFolder = File(dir, "dhun_updates")
        if (updateFolder.exists()) {
            updateFolder.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } else {
            0L
        }
    } catch (_: Exception) {
        0L
    }
}

actual fun clearDownloadedUpdateApks(context: Any?): Boolean {
    return try {
        val androidContext = context as? Context ?: return false
        val dir = androidContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: androidContext.filesDir
        val updateFolder = File(dir, "dhun_updates")
        if (updateFolder.exists()) {
            updateFolder.deleteRecursively()
        } else {
            true
        }
    } catch (_: Exception) {
        false
    }
}