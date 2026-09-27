package com.animejapaneselab.nativeapp.platform

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.animejapaneselab.nativeapp.data.LocalLabStore

/**
 * Whether 放課後チャイム can actually reach the user. HyperOS freezes sideloaded apps unless
 * 自启动 is on and 省电策略 is 无限制, so on Xiaomi those count as much as the permission.
 */
data class ReminderHealth(
    val notifications: Boolean,
    val battery: Boolean,
    val autostart: Boolean,
    val isXiaomi: Boolean,
) {
    val missing: Int get() = listOf(notifications, battery, autostart).count { !it }
    val ok: Boolean get() = missing == 0
}

object ReminderHealthReader {
    private const val AutostartKey = "autostart"
    private const val BatteryKey = "battery"
    private const val MiuiAutostartOp = 10008

    val isXiaomi: Boolean
        get() = listOf(Build.MANUFACTURER, Build.BRAND).any { it.lowercase() in setOf("xiaomi", "redmi", "poco") }

    fun read(context: Context): ReminderHealth {
        val store = LocalLabStore(context)
        val power = context.getSystemService(PowerManager::class.java)
        val unrestricted = power?.isIgnoringBatteryOptimizations(context.packageName) == true
        return ReminderHealth(
            notifications = NotificationManagerCompat.from(context).areNotificationsEnabled(),
            battery = unrestricted || (isXiaomi && store.readReminderConfirmed(BatteryKey)),
            autostart = !isXiaomi || (miuiAutostart(context) ?: store.readReminderConfirmed(AutostartKey)),
            isXiaomi = isXiaomi,
        )
    }

    /** MIUI keeps 自启动 in a private app-op; null when the ROM refuses to say. */
    @SuppressLint("DiscouragedPrivateApi")
    private fun miuiAutostart(context: Context): Boolean? = runCatching {
        val ops = context.getSystemService(AppOpsManager::class.java)
        val method = AppOpsManager::class.java.getMethod(
            "checkOpNoThrow",
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            String::class.java,
        )
        method.invoke(ops, MiuiAutostartOp, Process.myUid(), context.packageName) as Int == AppOpsManager.MODE_ALLOWED
    }.getOrNull()

    /** Opens HyperOS 自启动 management (app info as fallback). */
    fun openAutostart(context: Context) {
        LocalLabStore(context).writeReminderConfirmed(AutostartKey)
        val miui = Intent().setComponent(
            ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
        )
        launchFirst(context, miui, appDetails(context))
    }

    /** Opens the per-app battery saver (无限制) on HyperOS, the system whitelist dialog elsewhere. */
    @SuppressLint("BatteryLife")
    fun openBattery(context: Context) {
        val request = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, "package:${context.packageName}".toUri())
        if (isXiaomi) {
            LocalLabStore(context).writeReminderConfirmed(BatteryKey)
            val miui = Intent("miui.intent.action.HIDDEN_APPS_CONFIG_ACTIVITY")
                .putExtra("package_name", context.packageName)
                .putExtra("package_label", context.applicationInfo.loadLabel(context.packageManager).toString())
            launchFirst(context, miui, request, appDetails(context))
        } else {
            launchFirst(context, request, appDetails(context))
        }
    }

    /** System notification page for the app (channels, and the permission once denied). */
    fun openNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        launchFirst(context, intent, appDetails(context))
    }

    private fun appDetails(context: Context) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())

    private fun launchFirst(context: Context, vararg intents: Intent) {
        for (intent in intents) {
            val started = runCatching {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.isSuccess
            if (started) return
        }
    }
}
