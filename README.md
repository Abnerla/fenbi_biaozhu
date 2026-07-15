# 粉笔标注

粉笔标注是一款 Android 全局屏幕批注工具，可在其他应用上方显示画笔、荧光笔、橡皮擦和截图工具。项目使用 Kotlin、Jetpack Compose、前台服务和 `TYPE_APPLICATION_OVERLAY` 实现。

当前版本：`1.3.3`

最低系统版本：Android 9（API 28）

## 主要功能

- 全局悬浮按钮与批注工具栏
- 画笔、荧光笔、橡皮擦、撤销和清空
- 工具顺序、颜色、粗细、透明度和布局设置
- 多品牌手写笔档案，以及主键、副键的单击、双击和长按功能映射
- 双指单击撤销、三指单击重做和双指移动底层页面
- 深色、浅色和跟随系统外观
- 按需申请屏幕捕获权限，截图完成后立即释放 MediaProjection
- 通过导出 Activity 接收其他应用的一键联动控制

## 构建

```bash
./gradlew assembleDebug
```

Windows：

```powershell
.\gradlew.bat :app:assembleDebug
```

调试 APK 输出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 使用准备

首次使用需要在应用首页授予基础权限：

- 悬浮窗权限：在其他应用上方显示批注界面
- 通知权限：维持前台服务
- 存储相关权限：保存截图

屏幕捕获不是启动批注服务的必需权限。只有点击应用内截图按钮时，系统才会按需显示屏幕捕获授权；截图结束后应用会立即停止本次 MediaProjection 会话。

## 手写笔设置

设置入口位于：

```text
粉笔标注 > 设置 > 手写笔
```

默认使用“自动识别”档案，根据设备制造商选择安卓原生、华为、荣耀、小米、三星、OPPO 或 vivo 的常见按键位。也可以手动选择品牌档案，或使用“用户自定义”直接填写 Android `MotionEvent.buttonState` 的主键、副键掩码，以兼容按键定义不同的设备。

主键和副键均可分别配置单击、双击和长按，支持以下动作：

- 无操作
- 切换画笔、荧光笔或橡皮擦
- 撤销、重做或清空批注
- 截图
- 退出批注模式

基础书写、压感和 Android 标准手写笔按键通过 `MotionEvent` 统一处理。品牌档案覆盖厂商在标准事件中常用的按键位；少数仅通过厂商封闭 SDK 提供的蓝牙或隔空手势，不属于通用按键事件，需使用厂商 SDK 后才能扩展。

## 手势设置

设置入口位于：

```text
粉笔标注 > 设置 > 手势设置
```

可独立开关以下手势：

- 双指单击画布：撤销一步
- 三指单击画布：重做一步
- 双指滑动画布：将滑动转发给批注层下方的应用，用于翻页或移动页面

前两项只操作当前批注，不需要额外权限。双指移动底层页面需要用户在系统无障碍设置中启用“粉笔标注手势服务”；该服务只在收到已开启的双指滑动时注入对应滑动，不读取窗口内容。关闭系统服务后，应用会自动关闭该手势开关。

## 跨应用联动控制

其他 Android 应用可以启动粉笔标注提供的 `ExternalControlActivity`，通过 Intent 参数控制悬浮窗服务和批注模式。

### 用户侧开关

外部控制默认关闭。用户需要先打开：

```text
粉笔标注 > 设置 > 其他 > 跨应用联动
```

该页面包含两个独立开关：

- `允许外部控制批注模式`
- `允许外部控制悬浮窗`

调用方只能使用用户主动开启的能力。启用悬浮窗或批注模式前，粉笔标注仍需已经获得系统悬浮窗权限。

### Activity 契约

| 项目 | 值 |
| --- | --- |
| 包名 | `com.example.annotation` |
| Activity | `com.example.annotation.ExternalControlActivity` |
| Action | `com.example.annotation.action.CONTROL` |
| 批注模式参数 | `annotation_mode` |
| 悬浮窗参数 | `floating_window` |
| 支持的参数值 | `on`、`off`、`toggle` |

两个参数都不是必填，但每次调用至少需要提供一个。建议使用显式组件调用，避免被其他应用的 Intent Filter 截获。

### 参数行为

`annotation_mode`：

| 值 | 行为 |
| --- | --- |
| `on` | 开启批注界面；服务未运行时会同时启动悬浮窗服务 |
| `off` | 关闭批注界面并返回悬浮按钮，不停止服务 |
| `toggle` | 在批注界面和悬浮按钮之间切换 |

`floating_window`：

| 值 | 行为 |
| --- | --- |
| `on` | 启动悬浮窗前台服务 |
| `off` | 停止服务并移除批注界面和悬浮按钮 |
| `toggle` | 根据当前服务状态启动或停止 |

如果一次调用同时传入两个参数，并且 `floating_window` 的最终状态为 `off`，停止悬浮窗服务优先，`annotation_mode` 不再执行。

### ADB 示例

启动悬浮窗：

```bash
adb shell am start -n com.example.annotation/.ExternalControlActivity \
  -a com.example.annotation.action.CONTROL \
  --es floating_window on
```

开启批注模式：

```bash
adb shell am start -n com.example.annotation/.ExternalControlActivity \
  -a com.example.annotation.action.CONTROL \
  --es annotation_mode on
```

切换批注模式：

```bash
adb shell am start -n com.example.annotation/.ExternalControlActivity \
  -a com.example.annotation.action.CONTROL \
  --es annotation_mode toggle
```

关闭批注并关闭悬浮窗：

```bash
adb shell am start -n com.example.annotation/.ExternalControlActivity \
  -a com.example.annotation.action.CONTROL \
  --es annotation_mode off \
  --es floating_window off
```

### Kotlin 调用示例

```kotlin
import android.content.ComponentName
import android.content.Context
import android.content.Intent

fun toggleAnnotationMode(context: Context) {
    val intent = Intent("com.example.annotation.action.CONTROL").apply {
        component = ComponentName(
            "com.example.annotation",
            "com.example.annotation.ExternalControlActivity"
        )
        putExtra("annotation_mode", "toggle")
    }
    context.startActivity(intent)
}

fun startFloatingWindow(context: Context) {
    val intent = Intent("com.example.annotation.action.CONTROL").apply {
        component = ComponentName(
            "com.example.annotation",
            "com.example.annotation.ExternalControlActivity"
        )
        putExtra("floating_window", "on")
    }
    context.startActivity(intent)
}
```

从非 Activity 的 `Context` 调用时，需要增加：

```kotlin
intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
```

### Java 调用示例

```java
Intent intent = new Intent("com.example.annotation.action.CONTROL");
intent.setComponent(new ComponentName(
        "com.example.annotation",
        "com.example.annotation.ExternalControlActivity"
));
intent.putExtra("floating_window", "on");
intent.putExtra("annotation_mode", "on");
context.startActivity(intent);
```

### 接收执行结果

调用方使用 Activity Result API 启动时，可以从返回 Intent 的 `result_code` 字段读取结果：

| `result_code` | 含义 |
| --- | --- |
| `ok` | 命令已接受 |
| `no_command` | 未提供控制参数 |
| `invalid_value` | 参数不是 `on/off/toggle` |
| `annotation_control_disabled` | 用户未允许外部控制批注模式 |
| `floating_control_disabled` | 用户未允许外部控制悬浮窗 |
| `overlay_permission_required` | 粉笔标注尚未获得系统悬浮窗权限 |

示例：

```kotlin
private val controlLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { result ->
    val code = result.data?.getStringExtra("result_code")
    // Activity.RESULT_OK 表示命令已接受；具体结果见 code。
}
```

注意：`ok` 表示控制命令已被粉笔标注接受。Android 前台服务启动和悬浮窗口创建是异步过程，调用方不应把 Activity 返回时刻当作界面已经完成绘制的时刻。

## 权限说明

| 权限 | 用途 | 是否影响基础批注 |
| --- | --- | --- |
| `SYSTEM_ALERT_WINDOW` | 显示悬浮按钮和批注界面 | 是 |
| `FOREGROUND_SERVICE` | 维持悬浮窗服务 | 是 |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Android 14+ 悬浮标注服务类型 | 是 |
| `POST_NOTIFICATIONS` | 显示前台服务通知 | 是 |
| `FOREGROUND_SERVICE_MEDIA_PROJECTION` | Android 14+ 按需截图 | 否 |
| `READ_MEDIA_IMAGES` / 旧版存储权限 | 保存和读取图片 | 视系统版本而定 |
| 无障碍手势服务（可选） | 将双指滑动转发给底层页面 | 否 |

所有绘图和截图处理均在本地完成，应用不会上传屏幕内容或批注数据。

## 项目结构

```text
app/src/main/java/com/example/annotation/
├── MainActivity.kt
├── ExternalControlActivity.kt
├── ScreenCapturePermissionActivity.kt
├── drawing/
├── model/
├── service/
│   └── OverlayService.kt
├── ui/
└── utils/
```

## 技术栈

- Kotlin
- Jetpack Compose / Material 3
- Android Foreground Service
- WindowManager / `TYPE_APPLICATION_OVERLAY`
- MediaProjection / VirtualDisplay / ImageReader
- SharedPreferences

## 许可证

本项目采用 MIT License。
