package com.animejapaneselab.nativeapp.platform

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.Display
import androidx.core.net.toUri
import androidx.core.app.NotificationManagerCompat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

data class DeviceCapabilitySnapshot(
    val deviceName: String,
    val androidVersion: String,
    val sdkInt: Int,
    val sdkMinor: Int,
    val hyperOsFocusProtocol: Int,
    val hyperOsFocusPermission: Boolean?,
    val adaptiveRefreshRate: Boolean,
    val currentRefreshRateHz: Float,
    val supportedRefreshRatesHz: List<Float>,
    val hasVibrator: Boolean,
    val amplitudeControl: Boolean,
    val envelopeHaptics: Boolean,
    val notificationsEnabled: Boolean,
    val promotedNotificationsAllowed: Boolean,
    val primaryAbi: String,
    val resolution: String,
) {
    val isAndroid16OrNewer: Boolean get() = sdkInt >= Build.VERSION_CODES.BAKLAVA
    val supportsPromotedOngoingRuntime: Boolean get() = sdkInt >= 36 && sdkMinor >= 1
    val supportsHyperOsIsland: Boolean get() = hyperOsFocusProtocol >= 3
}

object DeviceCapabilityReader {
    fun read(context: Context): DeviceCapabilitySnapshot {
        val display = context.getSystemService(DisplayManager::class.java)
            ?.getDisplay(Display.DEFAULT_DISPLAY)
        val vibrator = context.defaultVibrator()
        val sdkMinor = currentMinorSdkVersion()
        val focusProtocol = runCatching {
            Settings.System.getInt(context.contentResolver, "notification_focus_protocol", 0)
        }.getOrDefault(0)
        val mode = display?.mode

        return DeviceCapabilitySnapshot(
            deviceName = listOf(Build.MANUFACTURER, Build.MODEL)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { "未知设备" },
            androidVersion = formatAndroidVersion(Build.VERSION.RELEASE, Build.VERSION.SDK_INT, sdkMinor),
            sdkInt = Build.VERSION.SDK_INT,
            sdkMinor = sdkMinor,
            hyperOsFocusProtocol = focusProtocol,
            hyperOsFocusPermission = if (focusProtocol > 0) queryHyperOsFocusPermission(context) else null,
            adaptiveRefreshRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                display?.hasArrSupport() == true
            } else {
                false
            },
            currentRefreshRateHz = display?.refreshRate ?: 0f,
            supportedRefreshRatesHz = display?.supportedModes
                .orEmpty()
                .map { it.refreshRate }
                .distinctBy { rate -> (rate * 10).toInt() }
                .sorted(),
            hasVibrator = vibrator?.hasVibrator() == true,
            amplitudeControl = vibrator?.hasAmplitudeControl() == true,
            envelopeHaptics = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                vibrator?.areEnvelopeEffectsSupported() == true
            } else {
                false
            },
            notificationsEnabled = context.getSystemService(NotificationManager::class.java)
                ?.areNotificationsEnabled() == true,
            promotedNotificationsAllowed = NotificationManagerCompat.from(context)
                .canPostPromotedNotifications(),
            primaryAbi = Build.SUPPORTED_ABIS.firstOrNull().orEmpty().ifBlank { "未知" },
            resolution = mode?.let { "${it.physicalWidth} × ${it.physicalHeight}" } ?: "未知",
        )
    }

    private fun queryHyperOsFocusPermission(context: Context): Boolean {
        return runCatching {
            val extras = Bundle().apply { putString("package", context.packageName) }
            context.contentResolver.call(
                "content://miui.statusbar.notification.public".toUri(),
                "canShowFocus",
                null,
                extras,
            )?.getBoolean("canShowFocus", false) == true
        }.getOrDefault(false)
    }

    @SuppressLint("WrongConstant")
    private fun currentMinorSdkVersion(): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) return 0
        // SDK_INT_FULL is a runtime full-version value. Lint's IntDef currently accepts only
        // VERSION_CODES_FULL constants here, although parsing the runtime value is intentional.
        return Build.getMinorSdkVersion(Build.VERSION.SDK_INT_FULL)
    }

    private fun Context.defaultVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }
    }
}

internal fun formatAndroidVersion(release: String, sdkInt: Int, sdkMinor: Int): String {
    val fullApi = if (sdkMinor > 0) "$sdkInt.$sdkMinor" else sdkInt.toString()
    return "Android ${release.ifBlank { sdkInt.toString() }} · API $fullApi"
}

internal fun formatRefreshRates(rates: List<Float>): String {
    if (rates.isEmpty()) return "系统未报告"
    return rates.joinToString(" / ") { rate ->
        val rounded = rate.roundToInt()
        String.format(Locale.ROOT, if (abs(rate - rounded) < 0.05f) "%.0f Hz" else "%.1f Hz", rate)
    }
}
