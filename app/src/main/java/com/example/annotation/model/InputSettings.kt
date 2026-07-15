package com.example.annotation.model

import android.os.Build
import android.view.MotionEvent

enum class StylusProfile(
    val displayName: String,
    val description: String,
    val primaryButtonMask: Int,
    val secondaryButtonMask: Int
) {
    AUTO("自动识别", "根据设备品牌选择兼容档案", MotionEvent.BUTTON_STYLUS_PRIMARY, MotionEvent.BUTTON_STYLUS_SECONDARY),
    ANDROID("安卓原生", "使用 Android 标准 MotionEvent", MotionEvent.BUTTON_STYLUS_PRIMARY, MotionEvent.BUTTON_STYLUS_SECONDARY),
    HUAWEI("华为", "兼容 M-Pencil 标准按键事件", MotionEvent.BUTTON_STYLUS_PRIMARY, MotionEvent.BUTTON_STYLUS_SECONDARY),
    HONOR("荣耀", "兼容 Magic Pencil 标准按键事件", MotionEvent.BUTTON_STYLUS_PRIMARY, MotionEvent.BUTTON_STYLUS_SECONDARY),
    XIAOMI("小米", "兼容 Focus Pen 常见按键位", MotionEvent.BUTTON_STYLUS_PRIMARY, MotionEvent.BUTTON_SECONDARY),
    SAMSUNG("三星", "兼容 S Pen 侧键事件", MotionEvent.BUTTON_STYLUS_PRIMARY, MotionEvent.BUTTON_STYLUS_SECONDARY),
    OPPO("OPPO", "兼容 Pencil 标准按键事件", MotionEvent.BUTTON_STYLUS_PRIMARY, MotionEvent.BUTTON_STYLUS_SECONDARY),
    VIVO("vivo", "兼容 Pencil 标准按键事件", MotionEvent.BUTTON_STYLUS_PRIMARY, MotionEvent.BUTTON_STYLUS_SECONDARY),
    CUSTOM("用户自定义", "使用自定义主键和副键掩码", MotionEvent.BUTTON_STYLUS_PRIMARY, MotionEvent.BUTTON_STYLUS_SECONDARY);

    fun resolvedForDevice(): StylusProfile {
        if (this != AUTO) return this
        val manufacturer = Build.MANUFACTURER.lowercase()
        return when {
            "huawei" in manufacturer -> HUAWEI
            "honor" in manufacturer -> HONOR
            "xiaomi" in manufacturer || "redmi" in manufacturer -> XIAOMI
            "samsung" in manufacturer -> SAMSUNG
            "oppo" in manufacturer || "oneplus" in manufacturer -> OPPO
            "vivo" in manufacturer || "iqoo" in manufacturer -> VIVO
            else -> ANDROID
        }
    }
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
