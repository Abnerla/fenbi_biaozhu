package com.example.annotation.model

import android.os.Build
import android.view.MotionEvent

enum class StylusMode(val displayName: String, val description: String) {
    AUTO("自动识别", "根据设备自动匹配兼容档案"),
    MANUAL("手动选择", "使用用户指定的品牌档案"),
    CUSTOM("自定义", "使用自定义主键和副键掩码")
}

enum class StylusProfile(
    val displayName: String,
    val description: String,
    val primaryButtonMask: Int,
    val secondaryButtonMask: Int
) {
    ANDROID("安卓原生", "使用 Android 标准 MotionEvent", MotionEvent.BUTTON_STYLUS_PRIMARY, MotionEvent.BUTTON_STYLUS_SECONDARY),
    HUAWEI("华为", "兼容 M-Pencil 标准按键事件", MotionEvent.BUTTON_STYLUS_PRIMARY, MotionEvent.BUTTON_STYLUS_SECONDARY),
    HONOR("荣耀", "兼容 Magic Pencil 标准按键事件", MotionEvent.BUTTON_STYLUS_PRIMARY, MotionEvent.BUTTON_STYLUS_SECONDARY),
    XIAOMI("小米", "兼容 Focus Pen 常见按键位", MotionEvent.BUTTON_STYLUS_PRIMARY, MotionEvent.BUTTON_SECONDARY),
    SAMSUNG("三星", "兼容 S Pen 侧键事件", MotionEvent.BUTTON_STYLUS_PRIMARY, MotionEvent.BUTTON_STYLUS_SECONDARY),
    OPPO("OPPO", "兼容 Pencil 标准按键事件", MotionEvent.BUTTON_STYLUS_PRIMARY, MotionEvent.BUTTON_STYLUS_SECONDARY),
    VIVO("vivo", "兼容 Pencil 标准按键事件", MotionEvent.BUTTON_STYLUS_PRIMARY, MotionEvent.BUTTON_STYLUS_SECONDARY);

    companion object {
        fun detectForDevice(): StylusDetection = detect(Build.MANUFACTURER)

        fun detect(manufacturer: String): StylusDetection {
            val normalized = manufacturer.trim().lowercase()
            val profile = when {
                "huawei" in normalized -> HUAWEI
                "honor" in normalized -> HONOR
                "xiaomi" in normalized || "redmi" in normalized -> XIAOMI
                "samsung" in normalized -> SAMSUNG
                "oppo" in normalized || "oneplus" in normalized -> OPPO
                "vivo" in normalized || "iqoo" in normalized -> VIVO
                else -> ANDROID
            }
            val knownBrand = profile != ANDROID
            val systemName = manufacturer.trim().ifBlank { "未知品牌" }
            return StylusDetection(profile, systemName, knownBrand)
        }
    }
}

data class StylusDetection(
    val profile: StylusProfile,
    val systemManufacturer: String,
    val knownBrand: Boolean
) {
    val displayName: String
        get() = if (knownBrand) profile.displayName else "$systemManufacturer（${profile.displayName}）"
}

data class StylusButtonMasks(val primary: Int, val secondary: Int)

fun resolveStylusButtonMasks(
    mode: StylusMode,
    detectedProfile: StylusProfile,
    manualProfile: StylusProfile,
    customMasks: StylusButtonMasks
): StylusButtonMasks {
    val profile = when (mode) {
        StylusMode.AUTO -> detectedProfile
        StylusMode.MANUAL -> manualProfile
        StylusMode.CUSTOM -> return customMasks
    }
    return StylusButtonMasks(profile.primaryButtonMask, profile.secondaryButtonMask)
}

data class LegacyStylusSettings(
    val mode: StylusMode,
    val manualProfile: StylusProfile
)

fun migrateLegacyStylusProfile(savedProfile: String?): LegacyStylusSettings = when (savedProfile) {
    "CUSTOM" -> LegacyStylusSettings(StylusMode.CUSTOM, StylusProfile.ANDROID)
    "ANDROID" -> LegacyStylusSettings(StylusMode.MANUAL, StylusProfile.ANDROID)
    "HUAWEI" -> LegacyStylusSettings(StylusMode.MANUAL, StylusProfile.HUAWEI)
    "HONOR" -> LegacyStylusSettings(StylusMode.MANUAL, StylusProfile.HONOR)
    "XIAOMI" -> LegacyStylusSettings(StylusMode.MANUAL, StylusProfile.XIAOMI)
    "SAMSUNG" -> LegacyStylusSettings(StylusMode.MANUAL, StylusProfile.SAMSUNG)
    "OPPO" -> LegacyStylusSettings(StylusMode.MANUAL, StylusProfile.OPPO)
    "VIVO" -> LegacyStylusSettings(StylusMode.MANUAL, StylusProfile.VIVO)
    else -> LegacyStylusSettings(StylusMode.AUTO, StylusProfile.ANDROID)
}

enum class StylusButtonAction(val displayName: String) {
    NONE("无操作"),
    PEN("切换画笔"),
    HIGHLIGHTER("切换荧光笔"),
    ERASER("切换橡皮擦"),
    UNDO("撤销"),
    REDO("重做"),
    CLEAR("清空批注"),
    SCREENSHOT("截图"),
    EXIT_ANNOTATION("退出批注模式")
}

enum class StylusButton { PRIMARY, SECONDARY }

enum class StylusPressType { SINGLE, DOUBLE, LONG }

data class StylusButtonMappings(
    val primarySingle: StylusButtonAction = StylusButtonAction.ERASER,
    val primaryDouble: StylusButtonAction = StylusButtonAction.UNDO,
    val primaryLong: StylusButtonAction = StylusButtonAction.REDO,
    val secondarySingle: StylusButtonAction = StylusButtonAction.PEN,
    val secondaryDouble: StylusButtonAction = StylusButtonAction.HIGHLIGHTER,
    val secondaryLong: StylusButtonAction = StylusButtonAction.SCREENSHOT
) {
    fun actionFor(button: StylusButton, pressType: StylusPressType): StylusButtonAction = when (button) {
        StylusButton.PRIMARY -> when (pressType) {
            StylusPressType.SINGLE -> primarySingle
            StylusPressType.DOUBLE -> primaryDouble
            StylusPressType.LONG -> primaryLong
        }
        StylusButton.SECONDARY -> when (pressType) {
            StylusPressType.SINGLE -> secondarySingle
            StylusPressType.DOUBLE -> secondaryDouble
            StylusPressType.LONG -> secondaryLong
        }
    }
}
