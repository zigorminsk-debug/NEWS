package com.techpulse.app.ui.update

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.techpulse.app.BuildConfig
import com.techpulse.app.ui.theme.AccentCyan
import com.techpulse.app.ui.theme.AccentGreen
import com.techpulse.app.ui.theme.SurfaceDark
import com.techpulse.app.ui.theme.TextPrimary
import com.techpulse.app.ui.theme.TextSecondary
import com.techpulse.app.update.AppUpdateManager
import com.techpulse.app.update.UpdateState

/**
 * Диалоги автообновления: «доступна новая версия», прогресс скачивания,
 * «готово к установке», ошибки. Показываются поверх любого экрана.
 */
@Composable
fun UpdateHost(manager: AppUpdateManager, state: UpdateState) {
    val context = LocalContext.current

    when (state) {
        is UpdateState.Available -> AlertDialog(
            onDismissRequest = { manager.dismiss() },
            containerColor = SurfaceDark,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                Text(
                    "Доступно обновление",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "TechPulse v${state.versionName} · сборка №${state.versionCode}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = AccentGreen
                    )
                    Text(
                        "Установлена: v${BuildConfig.VERSION_NAME} (№${BuildConfig.BUILD_NUMBER})",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                    if (state.sizeBytes > 0) {
                        Text(
                            "Размер: ${"%.1f".format(state.sizeBytes / 1048576f)} МБ",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                    if (state.notes.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(state.notes, fontSize = 13.sp, lineHeight = 18.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { manager.download() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentCyan,
                        contentColor = TextPrimary
                    )
                ) {
                    Text("Обновить", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { manager.dismiss() }) {
                    Text("Позже", color = TextSecondary)
                }
            }
        )

        is UpdateState.Downloading -> AlertDialog(
            onDismissRequest = { /* не прерываем скачивание */ },
            containerColor = SurfaceDark,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                Text(
                    "Скачивание обновления…",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    if (state.progress >= 0f) {
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.fillMaxWidth(),
                            color = AccentCyan
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${(state.progress * 100).toInt()}% · " +
                                "${state.downloadedBytes / 1048576} МБ",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = AccentCyan
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${state.downloadedBytes / 1048576} МБ",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            },
            confirmButton = {}
        )

        is UpdateState.Ready -> AlertDialog(
            onDismissRequest = { manager.dismiss() },
            containerColor = SurfaceDark,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                Text(
                    "Готово к установке",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Обновление v${state.info.versionName} скачано. " +
                        "Если система не запустила установку автоматически — нажмите «Установить»."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (manager.needsInstallPermission()) {
                            Toast.makeText(
                                context,
                                "Разрешите установку из этого источника и вернитесь",
                                Toast.LENGTH_LONG
                            ).show()
                            context.startActivity(manager.installPermissionIntent())
                        } else {
                            manager.installNow()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentCyan,
                        contentColor = TextPrimary
                    )
                ) {
                    Text("Установить", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { manager.dismiss() }) {
                    Text("Позже", color = TextSecondary)
                }
            }
        )

        is UpdateState.Failed -> AlertDialog(
            onDismissRequest = { manager.dismiss() },
            containerColor = SurfaceDark,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                Text(
                    "Обновление не удалось",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            text = { Text(state.message) },
            confirmButton = {
                TextButton(onClick = { manager.dismiss() }) {
                    Text("Закрыть", color = AccentCyan)
                }
            }
        )

        UpdateState.UpToDate -> LaunchedEffect(state) {
            Toast.makeText(
                context,
                "У вас последняя версия — v${BuildConfig.VERSION_NAME}",
                Toast.LENGTH_SHORT
            ).show()
            manager.dismiss()
        }

        UpdateState.Idle, UpdateState.Checking -> Unit
    }
}
