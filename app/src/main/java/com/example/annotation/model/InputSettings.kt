package com.example.annotation.model

import android.os.Build
import android.view.InputDevice
import android.view.KeyEvent
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
    HUAWEI("华为", "识别品牌，按键默认由系统处理", MotionEvent.BUTTON_STYLUS_PRIMARY, MotionEvent.BUTTON_STYLUS_SECONDARY),
    HONOR("荣耀", "识别品牌，按键默认由系统处理", MotionEvent.BUTTON_STYLUS_PRIMARY, MotionEvent.BUTTON_STYLUS_SECONDARY),
    XIAOMI("小米 / REDMI", "识别品牌，不套用跨型号默认动作", MotionEvent.BUTTON_STYLUS_PRIMARY, MotionEvent.BUTTON_STYLUS_SECONDARY),
    SAMSUNG("三星", "识别品牌，按键默认由系统处理", MotionEvent.BUTTON_STYLUS_PRIMARY, MotionEvent.BUTTON_STYLUS_SECONDARY),
    OPPO("OPPO", "识别品牌，按键默认由系统处理", MotionEvent.BUTTON_STYLUS_PRIMARY, MotionEvent.BUTTON_STYLUS_SECONDARY),
    VIVO("vivo / iQOO", "识别品牌，按键默认由系统处理", MotionEvent.BUTTON_STYLUS_PRIMARY, MotionEvent.BUTTON_STYLUS_SECONDARY);

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

data class StylusDeviceIdentity(
    val manufacturer: String,
    val name: String,
    val descriptor: String,
    val vendorId: Int,
    val productId: Int
) {
    val stableKey: String
        get() = descriptor.ifBlank { "$vendorId:$productId:$name" }

    val displayName: String
        get() = name.ifBlank { manufacturer.ifBlank { "未知手写笔" } }

    companion object {
        fun fromInputDevice(device: InputDevice?): StylusDeviceIdentity = StylusDeviceIdentity(
            manufacturer = Build.MANUFACTURER.orEmpty(),
            name = device?.name.orEmpty(),
            descriptor = device?.descriptor.orEmpty(),
            vendorId = device?.vendorId ?: 0,
            productId = device?.productId ?: 0
        )

        fun connectedStyluses(): List<StylusDeviceIdentity> = InputDevice.getDeviceIds()
            .map { deviceId -> InputDevice.getDevice(deviceId) }
            .filterNotNull()
            .filter { device ->
                device.sources and InputDevice.SOURCE_STYLUS == InputDevice.SOURCE_STYLUS ||
                    StylusVendorPresetCatalog.looksLikeStylus(device.name)
            }
            .map(::fromInputDevice)
    }
}

data class StylusGesture(val button: StylusButton, val pressType: StylusPressType)

data class StylusVendorPreset(
    val id: String,
    val profile: StylusProfile,
    val displayName: String,
    val aliases: List<String>,
    val verifiedBehavior: String,
    val sourceUrl: String?,
    val emulatedActions: Map<StylusGesture, StylusButtonAction> = emptyMap()
)

object StylusVendorPresetCatalog {
    val presets = listOf(
        StylusVendorPreset(
            id = "xiaomi_focus_pen",
            profile = StylusProfile.XIAOMI,
            displayName = "Xiaomi Focus Pen",
            aliases = listOf("xiaomi focus pen", "focus pen", "焦点触控笔"),
            verifiedBehavior = "按住截图键并触碰屏幕后截图；属于厂商组合手势，仅透传",
            sourceUrl = "https://www.mi.com/global/product/xiaomi-focus-pen/"
        ),
        StylusVendorPreset(
            id = "redmi_inspiration_stylus",
            profile = StylusProfile.XIAOMI,
            displayName = "REDMI 灵感触控笔",
            aliases = listOf("redmi inspiration stylus", "redmi smart pen", "灵感触控笔"),
            verifiedBehavior = "官方未确认可跨设备套用的按键动作，使用系统透传或按键学习",
            sourceUrl = "https://www.mi.com/shop/buy?product_id=1230801851"
        ),
        StylusVendorPreset(
            id = "huawei_m_pencil_2_3_pro",
            profile = StylusProfile.HUAWEI,
            displayName = "HUAWEI M-Pencil 2/3/Pro",
            aliases = listOf("huawei m-pencil", "huawei m pencil", "m-pencil"),
            verifiedBehavior = "双击笔身感应区由系统或兼容应用处理，不等同于 Android 侧键",
            sourceUrl = "https://consumer.huawei.com/en/support/content/en-us16085408/"
        ),
        StylusVendorPreset(
            id = "honor_magic_pencil_4s",
            profile = StylusProfile.HONOR,
            displayName = "HONOR Magic-Pencil 4s",
            aliases = listOf("honor magic-pencil 4s", "magic-pencil 4s", "magic pencil 4s"),
            verifiedBehavior = "单击、双击和长按动作随桌面、笔记或媒体场景变化，仅透传",
            sourceUrl = "https://www.honor.com/global/accessories/honor-magic-pencil-4s/"
        ),
        StylusVendorPreset(
            id = "vivo_pencil_2s",
            profile = StylusProfile.VIVO,
            displayName = "vivo Pencil2s",
            aliases = listOf("vivo pencil2s", "vivo pencil 2s"),
            verifiedBehavior = "官方确认双按键设计，但没有统一公开的精确动作映射",
            sourceUrl = "https://shop.vivo.com.cn/product/10010004"
        )
    )

    fun byId(id: String?): StylusVendorPreset? = presets.firstOrNull { it.id == id }

    fun forProfile(profile: StylusProfile): List<StylusVendorPreset> =
        presets.filter { it.profile == profile }

    fun detect(identity: StylusDeviceIdentity): StylusVendorPreset? {
        val normalized = identity.name.trim().lowercase()
        return presets.firstOrNull { preset ->
            preset.aliases.any { alias -> normalized.contains(alias.lowercase()) }
        }
    }

    fun looksLikeStylus(name: String): Boolean {
        val normalized = name.lowercase()
        return listOf("stylus", "pencil", "s pen", "spen", "触控笔", "手写笔")
            .any(normalized::contains)
    }
}

data class StylusLearnedBindings(
    val primaryMask: Int = 0,
    val secondaryMask: Int = 0,
    val primaryKeyCode: Int = 0,
    val secondaryKeyCode: Int = 0
) {
    fun maskFor(button: StylusButton): Int = when (button) {
        StylusButton.PRIMARY -> primaryMask
        StylusButton.SECONDARY -> secondaryMask
    }

    fun keyCodeFor(button: StylusButton): Int = when (button) {
        StylusButton.PRIMARY -> primaryKeyCode
        StylusButton.SECONDARY -> secondaryKeyCode
    }
}

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
    VENDOR_DEFAULT("跟随厂商默认"),
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
    val primarySingle: StylusButtonAction = StylusButtonAction.VENDOR_DEFAULT,
    val primaryDouble: StylusButtonAction = StylusButtonAction.VENDOR_DEFAULT,
    val primaryLong: StylusButtonAction = StylusButtonAction.VENDOR_DEFAULT,
    val secondarySingle: StylusButtonAction = StylusButtonAction.VENDOR_DEFAULT,
    val secondaryDouble: StylusButtonAction = StylusButtonAction.VENDOR_DEFAULT,
    val secondaryLong: StylusButtonAction = StylusButtonAction.VENDOR_DEFAULT
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

    fun customActionFor(button: StylusButton, pressType: StylusPressType): StylusButtonAction? =
        actionFor(button, pressType).takeUnless { it == StylusButtonAction.VENDOR_DEFAULT }

    fun actionsFor(button: StylusButton): List<StylusButtonAction> = when (button) {
        StylusButton.PRIMARY -> listOf(primarySingle, primaryDouble, primaryLong)
        StylusButton.SECONDARY -> listOf(secondarySingle, secondaryDouble, secondaryLong)
    }

    fun owns(button: StylusButton): Boolean =
        actionsFor(button).any { it != StylusButtonAction.VENDOR_DEFAULT }

    fun isMixed(button: StylusButton): Boolean = owns(button) &&
        actionsFor(button).any { it == StylusButtonAction.VENDOR_DEFAULT }
}

fun requiredStylusKeyBridgeButtons(
    mappings: StylusButtonMappings,
    bindings: StylusLearnedBindings
): Set<StylusButton> = StylusButton.entries
    .filterTo(linkedSetOf()) { button ->
        mappings.owns(button) && bindings.keyCodeFor(button) != 0
    }

fun standardStylusButtonForKeyCode(keyCode: Int): StylusButton? = when (keyCode) {
    KeyEvent.KEYCODE_STYLUS_BUTTON_PRIMARY -> StylusButton.PRIMARY
    KeyEvent.KEYCODE_STYLUS_BUTTON_SECONDARY,
    KeyEvent.KEYCODE_STYLUS_BUTTON_TERTIARY,
    KeyEvent.KEYCODE_STYLUS_BUTTON_TAIL -> StylusButton.SECONDARY
    else -> null
}
