package com.techpulse.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import com.techpulse.app.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Состояния процесса обновления приложения. */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState

    data class Available(
        val versionName: String,
        val versionCode: Int,
        val notes: String,
        val apkUrl: String,
        val sizeBytes: Long
    ) : UpdateState

    data class Downloading(
        val info: Available,
        /** 0..1; отрицательное значение — размер неизвестен. */
        val progress: Float,
        val downloadedBytes: Long
    ) : UpdateState

    data class Ready(val info: Available, val file: File) : UpdateState

    data class Failed(val message: String) : UpdateState
}

/**
 * Автообновление приложения через GitHub Releases.
 *
 *  - Раз в [CHECK_INTERVAL_MS] (или принудительно) запрашивает последний релиз
 *    `BuildConfig.GITHUB_REPO` и сравнивает номер сборки из тега (`v1.0.<N>`)
 *    c `BuildConfig.BUILD_NUMBER`.
 *  - Если релиз новее — берёт из него asset `*-release.apk`, скачивает его
 *    в приватную папку приложения и запускает системный установщик через
 *    FileProvider.
 */
class AppUpdateManager(private val context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("techpulse_updates", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    // ------------------------------------------------------------------
    // Проверка
    // ------------------------------------------------------------------

    /** Проверить наличие новой версии. [force] — ручная проверка из UI. */
    fun checkIfNeeded(force: Boolean) {
        if (_state.value is UpdateState.Checking || _state.value is UpdateState.Downloading) return
        val now = System.currentTimeMillis()
        if (!force && now - prefs.getLong(KEY_LAST_CHECK, 0L) < CHECK_INTERVAL_MS) return
        prefs.edit().putLong(KEY_LAST_CHECK, now).apply()
        scope.launch { performCheck(reportUpToDate = force) }
    }

    private suspend fun performCheck(reportUpToDate: Boolean) {
        _state.value = UpdateState.Checking
        try {
            val api = "https://api.github.com/repos/" + BuildConfig.GITHUB_REPO + "/releases/latest"
            val request = Request.Builder()
                .url(api)
                .header("User-Agent", "TechPulse-Android/" + BuildConfig.BUILD_NUMBER)
                .header("Accept", "application/vnd.github+json")
                .build()

            val body = withContext(Dispatchers.IO) {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) response.body?.string() else null
                }
            }
            if (body == null) {
                _state.value = if (reportUpToDate) {
                    UpdateState.Failed("Не удалось связаться с сервером обновлений.")
                } else {
                    UpdateState.Idle
                }
                return
            }

            val json = JSONObject(body)
            val tag = json.optString("tag_name")
            val remoteCode = tag.takeLastWhile { it.isDigit() }.toIntOrNull()
            if (remoteCode == null) {
                _state.value = UpdateState.Idle
                return
            }

            if (remoteCode <= BuildConfig.BUILD_NUMBER) {
                _state.value = if (reportUpToDate) UpdateState.UpToDate else UpdateState.Idle
                return
            }

            var apkUrl: String? = null
            var sizeBytes = 0L
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name")
                    if (name.endsWith("-release.apk")) {
                        apkUrl = asset.optString("browser_download_url")
                        sizeBytes = asset.optLong("size")
                        break
                    }
                }
            }
            if (apkUrl.isNullOrBlank()) {
                _state.value = UpdateState.Idle
                return
            }

            _state.value = UpdateState.Available(
                versionName = if (tag.startsWith("v")) tag.drop(1) else tag,
                versionCode = remoteCode,
                notes = json.optString("body").lineSequence().take(8).joinToString("\n").take(500),
                apkUrl = apkUrl,
                sizeBytes = sizeBytes
            )
        } catch (_: Exception) {
            _state.value = UpdateState.Idle
        }
    }

    // ------------------------------------------------------------------
    // Скачивание и установка
    // ------------------------------------------------------------------

    /** Скачать APK из состояния [UpdateState.Available]. */
    fun download() {
        val info = _state.value as? UpdateState.Available ?: return
        _state.value = UpdateState.Downloading(info, 0f, 0L)
        scope.launch {
            try {
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: throw IOException("Нет доступа к хранилищу")
                dir.mkdirs()
                val file = File(dir, "TechPulse-${info.versionName}.apk")

                val request = Request.Builder()
                    .url(info.apkUrl)
                    .header("User-Agent", "TechPulse-Android/" + BuildConfig.BUILD_NUMBER)
                    .build()

                withContext(Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                        val body = response.body ?: throw IOException("Пустой ответ")
                        val total = body.contentLength().takeIf { it > 0 } ?: info.sizeBytes
                        body.byteStream().use { input ->
                            file.outputStream().use { output ->
                                val buffer = ByteArray(64 * 1024)
                                var downloaded = 0L
                                while (true) {
                                    val read = input.read(buffer)
                                    if (read == -1) break
                                    output.write(buffer, 0, read)
                                    downloaded += read
                                    _state.value = UpdateState.Downloading(
                                        info = info,
                                        progress = if (total > 0) {
                                            downloaded.toFloat() / total.toFloat()
                                        } else {
                                            -1f
                                        },
                                        downloadedBytes = downloaded
                                    )
                                }
                                output.flush()
                            }
                        }
                    }
                }

                if (file.length() < 200_000) {
                    file.delete()
                    throw IOException("Скачан повреждённый файл")
                }
                _state.value = UpdateState.Ready(info, file)
                // Сразу предлагаем установку
                requestInstall(file)
            } catch (e: Exception) {
                _state.value = UpdateState.Failed(
                    "Не удалось скачать обновление: " + (e.message ?: "ошибка сети")
                )
            }
        }
    }

    /** Повторный запуск установки (например, после выдачи разрешения). */
    fun installNow() {
        val ready = _state.value as? UpdateState.Ready ?: return
        requestInstall(ready.file)
    }

    private fun requestInstall(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                context.packageName + ".fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            _state.value = UpdateState.Failed(
                "Не удалось запустить установку: " + (e.message ?: "неизвестная ошибка")
            )
        }
    }

    // ------------------------------------------------------------------
    // Разрешение на установку (Android 8+)
    // ------------------------------------------------------------------

    fun needsInstallPermission(): Boolean =
        !context.packageManager.canRequestPackageInstalls()

    fun installPermissionIntent(): Intent =
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:" + context.packageName)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun dismiss() {
        _state.value = UpdateState.Idle
    }

    companion object {
        private const val KEY_LAST_CHECK = "last_check_at"
        private const val CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L // раз в 6 часов
    }
}
