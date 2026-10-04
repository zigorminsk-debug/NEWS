package com.techpulse.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.techpulse.app.BuildConfig
import com.techpulse.app.ui.openUrl
import com.techpulse.app.ui.theme.AccentCyan
import com.techpulse.app.ui.theme.SurfaceDark
import com.techpulse.app.ui.theme.TextPrimary
import com.techpulse.app.ui.theme.TextSecondary

/** Имя разработчика приложения. */
const val DEVELOPER_NAME = "Zakharevich Igor"

/** Телефон разработчика (для tel:-ссылки — только цифры и «+»). */
const val DEVELOPER_PHONE_RAW = "+375293371412"

/** Телефон разработчика для отображения. */
const val DEVELOPER_PHONE_DISPLAY = "+375 29 337-14-12"

private const val REPO_URL = "https://github.com/zigorminsk-debug/NEWS"

/**
 * Диалог «О приложении»: версия и сборка, разработчик с телефоном
 * (тап — звонок), ссылка на репозиторий.
 */
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.RssFeed,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = "TECHPULSE",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "v${BuildConfig.VERSION_NAME} · сборка №${BuildConfig.BUILD_NUMBER}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }
        },
        text = {
            Column {
                AboutRow(
                    icon = Icons.Filled.Person,
                    label = "Разработчик",
                    value = DEVELOPER_NAME
                )
                Spacer(Modifier.height(14.dp))
                AboutRow(
                    icon = Icons.Filled.Call,
                    label = "Телефон",
                    value = DEVELOPER_PHONE_DISPLAY,
                    valueColor = AccentCyan,
                    onClick = {
                        try {
                            context.startActivity(
                                Intent(Intent.ACTION_DIAL, Uri.parse("tel:$DEVELOPER_PHONE_RAW"))
                            )
                        } catch (_: Exception) {
                        }
                    }
                )
                Spacer(Modifier.height(14.dp))
                AboutRow(
                    icon = Icons.Filled.Code,
                    label = "Репозиторий",
                    value = "github.com/zigorminsk-debug/NEWS",
                    valueColor = AccentCyan,
                    onClick = { openUrl(context, REPO_URL) }
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "IT-новости с авто-переводом на русский, " +
                        "режимом обучения чтению английского и автообновлением.",
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть", color = AccentCyan)
            }
        }
    )
}

@Composable
private fun AboutRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = TextPrimary,
    onClick: (() -> Unit)? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            tint = AccentCyan,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = label.uppercase(),
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = TextSecondary
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = valueColor,
                modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
        }
    }
}
