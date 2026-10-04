package com.techpulse.app

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.techpulse.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/** Проверяет GitHub Releases, скачивает release APK и передаёт его установщику Android. */
class UpdateManager(private val context: Context) {
    data class Release(val versionCode: Int, val versionName: String, val apkUrl: String)

    private val downloads = context.getSystemService(DownloadManager::class.java)
    private var downloadId = -1L

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(receiverContext: Context, intent: Intent) {
            if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) == downloadId) {
                installDownloadedApk()
            }
        }
    }

    fun register() {
        ContextCompat.registerReceiver(
            context,
            downloadReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    fun unregister() {
        runCatching { context.unregisterReceiver(downloadReceiver) }
    }

    suspend fun check(): Release? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.github.com/repos/zigorminsk-debug/NEWS/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "TechPulse/${BuildConfig.VERSION_NAME}")
            .build()

        OkHttpClient().newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            val release = JSONObject(response.body?.string().orEmpty())
            val tag = release.optString("tag_name")
            val versionCode = tag.substringAfterLast('.').toIntOrNull() ?: return@withContext null
            if (versionCode <= BuildConfig.VERSION_CODE) return@withContext null

            val assets = release.optJSONArray("assets") ?: return@withContext null
            for (index in 0 until assets.length()) {
                val asset = assets.getJSONObject(index)
                if (asset.optString("name").endsWith("-release.apk")) {
                    return@withContext Release(
                        versionCode = versionCode,
                        versionName = tag.removePrefix("v"),
                        apkUrl = asset.getString("browser_download_url")
                    )
                }
            }
            null
        }
    }

    fun canInstallPackages(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings() {
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            )
        )
    }

    fun download(release: Release) {
        val request = DownloadManager.Request(Uri.parse(release.apkUrl))
            .setTitle("TechPulse ${release.versionName}")
            .setDescription("Загрузка обновления")
            .setMimeType(APK_MIME_TYPE)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                "TechPulse-v${release.versionName}-${System.currentTimeMillis()}-release.apk"
            )
        downloadId = downloads.enqueue(request)
    }

    private fun installDownloadedApk() {
        val query = DownloadManager.Query().setFilterById(downloadId)
        downloads.query(query).use { cursor ->
            if (!cursor.moveToFirst()) return
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            if (status != DownloadManager.STATUS_SUCCESSFUL) return
        }
        val uri = downloads.getUriForDownloadedFile(downloadId) ?: return
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, APK_MIME_TYPE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        )
    }

    private companion object {
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    }
}
