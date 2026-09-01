package com.laxotters.clipy.feature.session.webview

import android.app.Instrumentation
import java.io.FileInputStream

/** emulator의 최초 표시 설정을 보관하고 테스트 변경과 복원을 한 경로로 관리합니다. */
internal class EmulatorDisplaySettings(
    private val instrumentation: Instrumentation,
) {
    private val originalSizeOverride = readOverride("wm size", "Override size")
    private val physicalSize = checkNotNull(readOverride("wm size", "Physical size"))
    private val originalDensityOverride = readDensityOverride("Override density")
    private val physicalDensity = checkNotNull(readDensityOverride("Physical density"))
    private val originalFontScale = readSetting(SYSTEM_SETTINGS, FONT_SCALE)
    private val originalFontWeightAdjustment = readSetting(SECURE_SETTINGS, FONT_WEIGHT_ADJUSTMENT)

    val changedWindowSize: DisplaySize = DisplaySize(
        width = (currentWindowSize().width * WINDOW_SIZE_SCALE).toInt(),
        height = (currentWindowSize().height * WINDOW_SIZE_SCALE).toInt(),
    )

    val changedDensity: Int = currentDensity() + DENSITY_CHANGE_DPI

    fun setWindowSize(size: DisplaySize) {
        executeShellCommand("wm size ${size.width}x${size.height}")
    }

    fun restoreWindowSize() {
        val override = originalSizeOverride
        if (override == null) {
            executeShellCommand("wm size reset")
        } else {
            setWindowSize(override)
        }
    }

    fun setDensity(densityDpi: Int) {
        executeShellCommand("wm density $densityDpi")
    }

    fun restoreDensity() {
        val override = originalDensityOverride
        if (override == null) {
            executeShellCommand("wm density reset")
        } else {
            setDensity(override)
        }
    }

    fun setFontScale(fontScale: Float) {
        writeSetting(SYSTEM_SETTINGS, FONT_SCALE, fontScale.toString())
    }

    fun restoreFontScale() {
        restoreSetting(SYSTEM_SETTINGS, FONT_SCALE, originalFontScale)
    }

    fun setFontWeightAdjustment(adjustment: Int) {
        writeSetting(SECURE_SETTINGS, FONT_WEIGHT_ADJUSTMENT, adjustment.toString())
    }

    fun restoreFontWeightAdjustment() {
        restoreSetting(
            namespace = SECURE_SETTINGS,
            key = FONT_WEIGHT_ADJUSTMENT,
            value = originalFontWeightAdjustment,
        )
    }

    fun restoreAll() {
        restoreWindowSize()
        restoreDensity()
        restoreFontScale()
        restoreFontWeightAdjustment()
    }

    private fun currentWindowSize(): DisplaySize = originalSizeOverride ?: physicalSize

    private fun currentDensity(): Int = originalDensityOverride ?: physicalDensity

    private fun readOverride(
        command: String,
        label: String,
    ): DisplaySize? {
        val match = Regex("$label: (\\d+)x(\\d+)").find(executeShellCommand(command))
            ?: return null
        return DisplaySize(
            width = checkNotNull(match.groupValues[1].toIntOrNull()),
            height = checkNotNull(match.groupValues[2].toIntOrNull()),
        )
    }

    private fun readDensityOverride(label: String): Int? {
        val match = Regex("$label: (\\d+)").find(executeShellCommand("wm density"))
            ?: return null
        return match.groupValues[1].toIntOrNull()
    }

    private fun readSetting(
        namespace: String,
        key: String,
    ): String? = executeShellCommand("settings get $namespace $key")
        .trim()
        .takeUnless { it.isEmpty() || it == "null" }

    private fun writeSetting(
        namespace: String,
        key: String,
        value: String,
    ) {
        executeShellCommand("settings put $namespace $key $value")
    }

    private fun restoreSetting(
        namespace: String,
        key: String,
        value: String?,
    ) {
        if (value == null) {
            executeShellCommand("settings delete $namespace $key")
        } else {
            writeSetting(namespace, key, value)
        }
    }

    private fun executeShellCommand(command: String): String {
        val descriptor = instrumentation.uiAutomation.executeShellCommand(command)
        return try {
            FileInputStream(descriptor.fileDescriptor).bufferedReader().use { it.readText() }
        } finally {
            descriptor.close()
        }
    }

    internal data class DisplaySize(
        val width: Int,
        val height: Int,
    )

    private companion object {
        const val SYSTEM_SETTINGS = "system"
        const val SECURE_SETTINGS = "secure"
        const val FONT_SCALE = "font_scale"
        const val FONT_WEIGHT_ADJUSTMENT = "font_weight_adjustment"
        const val WINDOW_SIZE_SCALE = 0.9f
        const val DENSITY_CHANGE_DPI = 40
    }
}
