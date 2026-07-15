package com.example.annotation.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.compose.ui.graphics.Color
import com.example.annotation.model.StylusButtonAction
import com.example.annotation.model.StylusButtonMasks
import com.example.annotation.model.StylusButtonMappings
import com.example.annotation.model.StylusButton
import com.example.annotation.model.StylusDeviceIdentity
import com.example.annotation.model.StylusLearnedBindings
import com.example.annotation.model.StylusMode
import com.example.annotation.model.StylusProfile
import com.example.annotation.model.StylusVendorPreset
import com.example.annotation.model.StylusVendorPresetCatalog
import com.example.annotation.model.migrateLegacyStylusProfile
import com.example.annotation.model.resolveStylusButtonMasks

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

/**
 * 偏好设置管理器
 */
class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "annotation_preferences",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_HIGHLIGHTER_ALPHA = "highlighter_alpha"
        private const val DEFAULT_HIGHLIGHTER_ALPHA = 0.4f

        private const val KEY_AUTO_COLLAPSE_TOOLBAR = "auto_collapse_toolbar"
        private const val DEFAULT_AUTO_COLLAPSE_TOOLBAR = false

        private const val KEY_AUTO_CHECK_UPDATE = "auto_check_update"
        private const val DEFAULT_AUTO_CHECK_UPDATE = true

        private const val KEY_SHOW_USER_ENTRY = "show_user_entry"
        private const val DEFAULT_SHOW_USER_ENTRY = true

        private const val KEY_THEME_MODE = "theme_mode"

        private const val KEY_ALLOW_EXTERNAL_ANNOTATION_CONTROL = "allow_external_annotation_control"
        private const val KEY_ALLOW_EXTERNAL_FLOATING_WINDOW_CONTROL = "allow_external_floating_window_control"

        private const val KEY_STYLUS_ENABLED = "stylus_enabled"
        private const val KEY_STYLUS_PROFILE = "stylus_profile"
        private const val KEY_STYLUS_MODE = "stylus_mode"
        private const val KEY_STYLUS_MANUAL_PROFILE = "stylus_manual_profile"
        private const val KEY_STYLUS_MANUAL_PRESET = "stylus_manual_preset"
        private const val KEY_STYLUS_CUSTOM_PRIMARY_MASK = "stylus_custom_primary_mask"
        private const val KEY_STYLUS_CUSTOM_SECONDARY_MASK = "stylus_custom_secondary_mask"
        private const val KEY_STYLUS_LEARNED_PREFIX = "stylus_learned_"
        private const val KEY_STYLUS_LEARNED_GLOBAL_PRIMARY_MASK = "stylus_learned_global_primary_mask"
        private const val KEY_STYLUS_LEARNED_GLOBAL_SECONDARY_MASK = "stylus_learned_global_secondary_mask"
        private const val KEY_STYLUS_LEARNED_GLOBAL_PRIMARY_KEY = "stylus_learned_global_primary_key"
        private const val KEY_STYLUS_LEARNED_GLOBAL_SECONDARY_KEY = "stylus_learned_global_secondary_key"
        const val STYLUS_PRIMARY_SINGLE = "stylus_primary_single"
        const val STYLUS_PRIMARY_DOUBLE = "stylus_primary_double"
        const val STYLUS_PRIMARY_LONG = "stylus_primary_long"
        const val STYLUS_SECONDARY_SINGLE = "stylus_secondary_single"
        const val STYLUS_SECONDARY_DOUBLE = "stylus_secondary_double"
        const val STYLUS_SECONDARY_LONG = "stylus_secondary_long"

        private val stylusActionKeys = setOf(
            STYLUS_PRIMARY_SINGLE,
            STYLUS_PRIMARY_DOUBLE,
            STYLUS_PRIMARY_LONG,
            STYLUS_SECONDARY_SINGLE,
            STYLUS_SECONDARY_DOUBLE,
            STYLUS_SECONDARY_LONG
        )

        private const val KEY_GESTURE_TWO_FINGER_UNDO = "gesture_two_finger_undo"
        private const val KEY_GESTURE_THREE_FINGER_REDO = "gesture_three_finger_redo"
        private const val KEY_GESTURE_TWO_FINGER_PAGE_MOVE = "gesture_two_finger_page_move"

        // 自定义画笔颜色键
        private const val KEY_CUSTOM_PEN_COLOR_1 = "custom_pen_color_1"
        private const val KEY_CUSTOM_PEN_COLOR_2 = "custom_pen_color_2"
        private const val KEY_CUSTOM_PEN_COLOR_3 = "custom_pen_color_3"

        // 自定义荧光笔颜色键
        private const val KEY_CUSTOM_HIGHLIGHTER_COLOR_1 = "custom_highlighter_color_1"
        private const val KEY_CUSTOM_HIGHLIGHTER_COLOR_2 = "custom_highlighter_color_2"
        private const val KEY_CUSTOM_HIGHLIGHTER_COLOR_3 = "custom_highlighter_color_3"

        // 画笔颜色顺序键
        private const val KEY_PEN_COLOR_ORDER = "pen_color_order"

        // 荧光笔颜色顺序键
        private const val KEY_HIGHLIGHTER_COLOR_ORDER = "highlighter_color_order"

        // 工具栏工具顺序键
        private const val KEY_TOOLBAR_ORDER = "toolbar_order"

        // 工具栏工具可见性键前缀
        private const val KEY_TOOLBAR_VISIBLE_PREFIX = "toolbar_visible_"
    }

    /**
     * 获取荧光笔透明度
     */
    fun getHighlighterAlpha(): Float {
        return prefs.getFloat(KEY_HIGHLIGHTER_ALPHA, DEFAULT_HIGHLIGHTER_ALPHA)
    }

    /**
     * 设置荧光笔透明度
     */
    fun setHighlighterAlpha(alpha: Float) {
        prefs.edit().putFloat(KEY_HIGHLIGHTER_ALPHA, alpha.coerceIn(0f, 1f)).apply()
    }

    /**
     * 获取是否自动折叠二级功能栏
     */
    fun getAutoCollapseToolbar(): Boolean {
        return prefs.getBoolean(KEY_AUTO_COLLAPSE_TOOLBAR, DEFAULT_AUTO_COLLAPSE_TOOLBAR)
    }

    /**
     * 设置是否自动折叠二级功能栏
     */
    fun setAutoCollapseToolbar(autoCollapse: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_COLLAPSE_TOOLBAR, autoCollapse).apply()
    }

    /**
     * 获取是否自动检查更新
     */
    fun getAutoCheckUpdate(): Boolean {
        return prefs.getBoolean(KEY_AUTO_CHECK_UPDATE, DEFAULT_AUTO_CHECK_UPDATE)
    }

    /**
     * 设置是否自动检查更新
     */
    fun setAutoCheckUpdate(autoCheck: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_CHECK_UPDATE, autoCheck).apply()
    }

    /**
     * 获取是否显示用户入口
     */
    fun getShowUserEntry(): Boolean {
        return prefs.getBoolean(KEY_SHOW_USER_ENTRY, DEFAULT_SHOW_USER_ENTRY)
    }

    /**
     * 设置是否显示用户入口
     */
    fun setShowUserEntry(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_USER_ENTRY, show).apply()
    }

    fun getThemeMode(): AppThemeMode {
        val savedMode = prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name)
        return runCatching { AppThemeMode.valueOf(savedMode ?: AppThemeMode.SYSTEM.name) }
            .getOrDefault(AppThemeMode.SYSTEM)
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun getAllowExternalAnnotationControl(): Boolean {
        return prefs.getBoolean(KEY_ALLOW_EXTERNAL_ANNOTATION_CONTROL, false)
    }

    fun setAllowExternalAnnotationControl(allow: Boolean) {
        prefs.edit().putBoolean(KEY_ALLOW_EXTERNAL_ANNOTATION_CONTROL, allow).apply()
    }

    fun getAllowExternalFloatingWindowControl(): Boolean {
        return prefs.getBoolean(KEY_ALLOW_EXTERNAL_FLOATING_WINDOW_CONTROL, false)
    }

    fun setAllowExternalFloatingWindowControl(allow: Boolean) {
        prefs.edit().putBoolean(KEY_ALLOW_EXTERNAL_FLOATING_WINDOW_CONTROL, allow).apply()
    }

    fun getStylusEnabled(): Boolean = prefs.getBoolean(KEY_STYLUS_ENABLED, true)

    fun setStylusEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_STYLUS_ENABLED, enabled).apply()
    }

    fun getStylusMode(): StylusMode {
        migrateLegacyStylusSettingsIfNeeded()
        return enumPreference(KEY_STYLUS_MODE, StylusMode.AUTO)
    }

    fun setStylusMode(mode: StylusMode) {
        prefs.edit().putString(KEY_STYLUS_MODE, mode.name).apply()
    }

    fun getManualStylusProfile(): StylusProfile {
        migrateLegacyStylusSettingsIfNeeded()
        return enumPreference(KEY_STYLUS_MANUAL_PROFILE, StylusProfile.ANDROID)
    }

    fun setManualStylusProfile(profile: StylusProfile) {
        prefs.edit()
            .putString(KEY_STYLUS_MANUAL_PROFILE, profile.name)
            .remove(KEY_STYLUS_MANUAL_PRESET)
            .apply()
    }

    fun getManualStylusPresetId(): String? = prefs.getString(KEY_STYLUS_MANUAL_PRESET, null)

    fun setManualStylusPresetId(presetId: String?) {
        prefs.edit().apply {
            if (presetId == null) remove(KEY_STYLUS_MANUAL_PRESET)
            else putString(KEY_STYLUS_MANUAL_PRESET, presetId)
        }.apply()
    }

    fun resolveStylusPreset(identity: StylusDeviceIdentity?): StylusVendorPreset? = when (getStylusMode()) {
        StylusMode.AUTO -> identity?.let(StylusVendorPresetCatalog::detect)
        StylusMode.MANUAL -> StylusVendorPresetCatalog.byId(getManualStylusPresetId())
        StylusMode.CUSTOM -> null
    }

    fun getStylusButtonMasks(): StylusButtonMasks {
        return resolveStylusButtonMasks(
            mode = getStylusMode(),
            detectedProfile = StylusProfile.detectForDevice().profile,
            manualProfile = getManualStylusProfile(),
            customMasks = StylusButtonMasks(
                getStylusCustomPrimaryMask(),
                getStylusCustomSecondaryMask()
            )
        )
    }

    fun getStylusCustomPrimaryMask(): Int = prefs.getInt(
        KEY_STYLUS_CUSTOM_PRIMARY_MASK,
        android.view.MotionEvent.BUTTON_STYLUS_PRIMARY
    )

    fun setStylusCustomPrimaryMask(mask: Int) {
        prefs.edit().putInt(KEY_STYLUS_CUSTOM_PRIMARY_MASK, mask.coerceAtLeast(1)).apply()
    }

    fun getStylusCustomSecondaryMask(): Int = prefs.getInt(
        KEY_STYLUS_CUSTOM_SECONDARY_MASK,
        android.view.MotionEvent.BUTTON_STYLUS_SECONDARY
    )

    fun setStylusCustomSecondaryMask(mask: Int) {
        prefs.edit().putInt(KEY_STYLUS_CUSTOM_SECONDARY_MASK, mask.coerceAtLeast(1)).apply()
    }

    fun getStylusButtonMappings(): StylusButtonMappings = StylusButtonMappings(
        primarySingle = stylusAction(STYLUS_PRIMARY_SINGLE, StylusButtonAction.VENDOR_DEFAULT),
        primaryDouble = stylusAction(STYLUS_PRIMARY_DOUBLE, StylusButtonAction.VENDOR_DEFAULT),
        primaryLong = stylusAction(STYLUS_PRIMARY_LONG, StylusButtonAction.VENDOR_DEFAULT),
        secondarySingle = stylusAction(STYLUS_SECONDARY_SINGLE, StylusButtonAction.VENDOR_DEFAULT),
        secondaryDouble = stylusAction(STYLUS_SECONDARY_DOUBLE, StylusButtonAction.VENDOR_DEFAULT),
        secondaryLong = stylusAction(STYLUS_SECONDARY_LONG, StylusButtonAction.VENDOR_DEFAULT)
    )

    fun setStylusButtonAction(key: String, action: StylusButtonAction) {
        require(key in stylusActionKeys) { "Unknown stylus action key: $key" }
        prefs.edit().putString(key, action.name).apply()
    }

    fun resetStylusButtonMappings() {
        prefs.edit().apply {
            stylusActionKeys.forEach(::remove)
        }.apply()
    }

    fun getStylusLearnedBindings(deviceKey: String): StylusLearnedBindings {
        val prefix = learnedDevicePrefix(deviceKey)
        return StylusLearnedBindings(
            primaryMask = prefs.getInt(
                "${prefix}primary_mask",
                learnedBindingFallback(KEY_STYLUS_LEARNED_GLOBAL_PRIMARY_MASK, "primary_mask")
            ),
            secondaryMask = prefs.getInt(
                "${prefix}secondary_mask",
                learnedBindingFallback(KEY_STYLUS_LEARNED_GLOBAL_SECONDARY_MASK, "secondary_mask")
            ),
            primaryKeyCode = prefs.getInt(
                "${prefix}primary_key",
                learnedBindingFallback(KEY_STYLUS_LEARNED_GLOBAL_PRIMARY_KEY, "primary_key")
            ),
            secondaryKeyCode = prefs.getInt(
                "${prefix}secondary_key",
                learnedBindingFallback(KEY_STYLUS_LEARNED_GLOBAL_SECONDARY_KEY, "secondary_key")
            )
        )
    }

    fun setStylusLearnedMotionMask(deviceKey: String, button: StylusButton, mask: Int) {
        if (mask == 0) return
        val suffix = if (button == StylusButton.PRIMARY) "primary_mask" else "secondary_mask"
        val globalKey = if (button == StylusButton.PRIMARY) {
            KEY_STYLUS_LEARNED_GLOBAL_PRIMARY_MASK
        } else {
            KEY_STYLUS_LEARNED_GLOBAL_SECONDARY_MASK
        }
        prefs.edit()
            .putInt(learnedDevicePrefix(deviceKey) + suffix, mask)
            .putInt(globalKey, mask)
            .apply()
    }

    fun setStylusLearnedKeyCode(deviceKey: String, button: StylusButton, keyCode: Int) {
        if (keyCode == 0) return
        val suffix = if (button == StylusButton.PRIMARY) "primary_key" else "secondary_key"
        val globalKey = if (button == StylusButton.PRIMARY) {
            KEY_STYLUS_LEARNED_GLOBAL_PRIMARY_KEY
        } else {
            KEY_STYLUS_LEARNED_GLOBAL_SECONDARY_KEY
        }
        prefs.edit()
            .putInt(learnedDevicePrefix(deviceKey) + suffix, keyCode)
            .putInt(globalKey, keyCode)
            .apply()
    }

    fun clearStylusLearnedBindings(deviceKey: String) {
        val prefix = learnedDevicePrefix(deviceKey)
        val deviceBindings = StylusLearnedBindings(
            primaryMask = prefs.getInt("${prefix}primary_mask", 0),
            secondaryMask = prefs.getInt("${prefix}secondary_mask", 0),
            primaryKeyCode = prefs.getInt("${prefix}primary_key", 0),
            secondaryKeyCode = prefs.getInt("${prefix}secondary_key", 0)
        )
        prefs.edit().apply {
            remove("${prefix}primary_mask")
            remove("${prefix}secondary_mask")
            remove("${prefix}primary_key")
            remove("${prefix}secondary_key")
            if (deviceBindings.primaryMask != 0 &&
                prefs.getInt(KEY_STYLUS_LEARNED_GLOBAL_PRIMARY_MASK, 0) == deviceBindings.primaryMask
            ) remove(KEY_STYLUS_LEARNED_GLOBAL_PRIMARY_MASK)
            if (deviceBindings.secondaryMask != 0 &&
                prefs.getInt(KEY_STYLUS_LEARNED_GLOBAL_SECONDARY_MASK, 0) == deviceBindings.secondaryMask
            ) remove(KEY_STYLUS_LEARNED_GLOBAL_SECONDARY_MASK)
            if (deviceBindings.primaryKeyCode != 0 &&
                prefs.getInt(KEY_STYLUS_LEARNED_GLOBAL_PRIMARY_KEY, 0) == deviceBindings.primaryKeyCode
            ) remove(KEY_STYLUS_LEARNED_GLOBAL_PRIMARY_KEY)
            if (deviceBindings.secondaryKeyCode != 0 &&
                prefs.getInt(KEY_STYLUS_LEARNED_GLOBAL_SECONDARY_KEY, 0) == deviceBindings.secondaryKeyCode
            ) remove(KEY_STYLUS_LEARNED_GLOBAL_SECONDARY_KEY)
        }.apply()
    }

    fun getTwoFingerTapUndoEnabled(): Boolean = prefs.getBoolean(KEY_GESTURE_TWO_FINGER_UNDO, true)

    fun setTwoFingerTapUndoEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GESTURE_TWO_FINGER_UNDO, enabled).apply()
    }

    fun getThreeFingerTapRedoEnabled(): Boolean = prefs.getBoolean(KEY_GESTURE_THREE_FINGER_REDO, true)

    fun setThreeFingerTapRedoEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GESTURE_THREE_FINGER_REDO, enabled).apply()
    }

    fun getTwoFingerPageMoveEnabled(): Boolean = prefs.getBoolean(KEY_GESTURE_TWO_FINGER_PAGE_MOVE, false)

    fun setTwoFingerPageMoveEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GESTURE_TWO_FINGER_PAGE_MOVE, enabled).apply()
    }

    private fun stylusAction(key: String, default: StylusButtonAction): StylusButtonAction {
        return enumPreference(key, default)
    }

    private fun learnedDevicePrefix(deviceKey: String): String {
        val encoded = Base64.encodeToString(
            deviceKey.toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP or Base64.URL_SAFE
        )
        return KEY_STYLUS_LEARNED_PREFIX + encoded + "_"
    }

    private fun learnedBindingFallback(globalKey: String, suffix: String): Int {
        val global = prefs.getInt(globalKey, 0)
        if (global != 0) return global
        return prefs.all.asSequence()
            .filter { (key, value) ->
                key.startsWith(KEY_STYLUS_LEARNED_PREFIX) &&
                    key.endsWith("_$suffix") && value is Int && value != 0
            }
            .map { it.value as Int }
            .distinct()
            .singleOrNull() ?: 0
    }

    private fun migrateLegacyStylusSettingsIfNeeded() {
        if (prefs.contains(KEY_STYLUS_MODE)) return
        val legacy = migrateLegacyStylusProfile(prefs.getString(KEY_STYLUS_PROFILE, null))
        prefs.edit()
            .putString(KEY_STYLUS_MODE, legacy.mode.name)
            .putString(KEY_STYLUS_MANUAL_PROFILE, legacy.manualProfile.name)
            .apply()
    }

    private inline fun <reified T : Enum<T>> enumPreference(key: String, default: T): T {
        val saved = prefs.getString(key, default.name)
        return enumValues<T>().firstOrNull { it.name == saved } ?: default
    }

    /**
     * 获取自定义画笔颜色列表（最多3个）
     */
    fun getCustomPenColors(): List<Color> {
        val colors = mutableListOf<Color>()
        val color1 = prefs.getLong(KEY_CUSTOM_PEN_COLOR_1, -1L)
        val color2 = prefs.getLong(KEY_CUSTOM_PEN_COLOR_2, -1L)
        val color3 = prefs.getLong(KEY_CUSTOM_PEN_COLOR_3, -1L)

        if (color1 != -1L) colors.add(Color(color1.toULong()))
        if (color2 != -1L) colors.add(Color(color2.toULong()))
        if (color3 != -1L) colors.add(Color(color3.toULong()))

        return colors
    }

    /**
     * 设置自定义画笔颜色列表
     */
    fun setCustomPenColors(colors: List<Color>) {
        prefs.edit().apply {
            // 清除所有自定义颜色
            remove(KEY_CUSTOM_PEN_COLOR_1)
            remove(KEY_CUSTOM_PEN_COLOR_2)
            remove(KEY_CUSTOM_PEN_COLOR_3)

            // 保存新的颜色（最多3个）
            colors.take(3).forEachIndexed { index, color ->
                val key = when (index) {
                    0 -> KEY_CUSTOM_PEN_COLOR_1
                    1 -> KEY_CUSTOM_PEN_COLOR_2
                    2 -> KEY_CUSTOM_PEN_COLOR_3
                    else -> return@forEachIndexed
                }
                putLong(key, color.value.toLong())
            }
            apply()
        }
    }

    /**
     * 获取自定义荧光笔颜色列表（最多3个）
     */
    fun getCustomHighlighterColors(): List<Color> {
        val colors = mutableListOf<Color>()
        val color1 = prefs.getLong(KEY_CUSTOM_HIGHLIGHTER_COLOR_1, -1L)
        val color2 = prefs.getLong(KEY_CUSTOM_HIGHLIGHTER_COLOR_2, -1L)
        val color3 = prefs.getLong(KEY_CUSTOM_HIGHLIGHTER_COLOR_3, -1L)

        if (color1 != -1L) colors.add(Color(color1.toULong()))
        if (color2 != -1L) colors.add(Color(color2.toULong()))
        if (color3 != -1L) colors.add(Color(color3.toULong()))

        return colors
    }

    /**
     * 设置自定义荧光笔颜色列表
     */
    fun setCustomHighlighterColors(colors: List<Color>) {
        prefs.edit().apply {
            // 清除所有自定义颜色
            remove(KEY_CUSTOM_HIGHLIGHTER_COLOR_1)
            remove(KEY_CUSTOM_HIGHLIGHTER_COLOR_2)
            remove(KEY_CUSTOM_HIGHLIGHTER_COLOR_3)

            // 保存新的颜色（最多3个）
            colors.take(3).forEachIndexed { index, color ->
                val key = when (index) {
                    0 -> KEY_CUSTOM_HIGHLIGHTER_COLOR_1
                    1 -> KEY_CUSTOM_HIGHLIGHTER_COLOR_2
                    2 -> KEY_CUSTOM_HIGHLIGHTER_COLOR_3
                    else -> return@forEachIndexed
                }
                putLong(key, color.value.toLong())
            }
            apply()
        }
    }

    /**
     * 获取画笔颜色顺序（包含预设颜色和自定义颜色）
     * 返回颜色值的列表，以Long形式存储
     */
    fun getPenColorOrder(): List<Long> {
        val orderString = prefs.getString(KEY_PEN_COLOR_ORDER, null)
        return if (orderString != null && orderString.isNotEmpty()) {
            orderString.split(",").mapNotNull { it.toLongOrNull() }
        } else {
            emptyList()
        }
    }

    /**
     * 设置画笔颜色顺序
     */
    fun setPenColorOrder(colorOrder: List<Long>) {
        prefs.edit().putString(KEY_PEN_COLOR_ORDER, colorOrder.joinToString(",")).apply()
    }

    /**
     * 获取荧光笔颜色顺序
     */
    fun getHighlighterColorOrder(): List<Long> {
        val orderString = prefs.getString(KEY_HIGHLIGHTER_COLOR_ORDER, null)
        return if (orderString != null && orderString.isNotEmpty()) {
            orderString.split(",").mapNotNull { it.toLongOrNull() }
        } else {
            emptyList()
        }
    }

    /**
     * 设置荧光笔颜色顺序
     */
    fun setHighlighterColorOrder(colorOrder: List<Long>) {
        prefs.edit().putString(KEY_HIGHLIGHTER_COLOR_ORDER, colorOrder.joinToString(",")).apply()
    }

    /**
     * 获取工具栏工具顺序
     * 返回工具ID列表，默认顺序为：pen, highlighter, eraser, divider1, undo, clear, screenshot, divider2, layout, exit
     */
    fun getToolbarOrder(): List<String> {
        val orderString = prefs.getString(KEY_TOOLBAR_ORDER, null)
        return if (orderString != null && orderString.isNotEmpty()) {
            orderString.split(",")
        } else {
            // 默认顺序
            listOf("pen", "highlighter", "eraser", "divider1", "undo", "clear", "screenshot", "divider2", "layout", "exit")
        }
    }

    /**
     * 设置工具栏工具顺序
     */
    fun setToolbarOrder(toolOrder: List<String>) {
        prefs.edit().putString(KEY_TOOLBAR_ORDER, toolOrder.joinToString(",")).apply()
    }

    /**
     * 获取工具的可见性
     * @param toolId 工具ID
     * @return 是否可见，默认全部可见
     */
    fun getToolVisibility(toolId: String): Boolean {
        return prefs.getBoolean("$KEY_TOOLBAR_VISIBLE_PREFIX$toolId", true)
    }

    /**
     * 设置工具的可见性
     * @param toolId 工具ID
     * @param visible 是否可见
     */
    fun setToolVisibility(toolId: String, visible: Boolean) {
        prefs.edit().putBoolean("$KEY_TOOLBAR_VISIBLE_PREFIX$toolId", visible).apply()
    }

    /**
     * 批量设置工具可见性
     */
    fun setToolVisibilities(visibilities: Map<String, Boolean>) {
        prefs.edit().apply {
            visibilities.forEach { (toolId, visible) ->
                putBoolean("$KEY_TOOLBAR_VISIBLE_PREFIX$toolId", visible)
            }
            apply()
        }
    }
}
