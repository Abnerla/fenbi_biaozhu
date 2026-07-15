# 粉笔标注

粉笔标注是一款 Android 全局屏幕批注工具，可在其他应用上方显示画笔、荧光笔、橡皮擦和截图工具。项目使用 Kotlin、Jetpack Compose、前台服务和 `TYPE_APPLICATION_OVERLAY` 实现。

当前版本：`1.3.3`

最低系统版本：Android 9（API 28）

## 主要功能

- 全局悬浮按钮与批注工具栏
- 画笔、荧光笔、橡皮擦、撤销和清空
- 工具顺序、颜色、粗细、透明度和布局设置
- 型号级手写笔档案、按键测试与学习，以及主键、副键的单击、双击和长按功能映射
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

设备档案默认使用“自动识别”，也可以切换为“手动选择”或“自定义”：

- 自动识别：先识别设备制造商，再使用 Android `InputDevice` 的名称、descriptor、vendor ID 和 product ID 尝试精确匹配笔型号。只识别出品牌时显示“笔型号未知”，不会套用其他型号的动作。
- 手动选择：依次选择品牌和笔型号，并记住上次选择。没有对应型号时可保留“型号未知”，继续使用系统透传或按键学习。
- 自定义：直接填写 Android `MotionEvent.buttonState` 的主键、副键掩码，以兼容按键定义不同的设备。

主键和副键均可分别配置单击、双击和长按。默认选项为“跟随厂商默认”，此时应用不消费整组按键事件。支持以下动作：

- 跟随厂商默认
- 无操作
- 切换画笔、荧光笔或橡皮擦
- 撤销、重做或清空批注
- 截图
- 退出批注模式

只要同一物理按键的任一手势被改为自定义动作，应用就会接管该按键的完整单击、双击和长按事件流。未被官方资料确认且无法可靠重放的厂商默认动作，在该按键被接管后不能继续保留；设置页会在对应动作行显示提示。点击“恢复当前设备默认”可将所有动作重新设为系统透传。

### 按键测试与学习

“按键测试与学习”会显示最近一次 MotionEvent 或 KeyEvent 的设备名、action、`buttonState`、`actionButton` 或 keyCode。点击“学习主键”或“学习副键”后，在 10 秒内按下对应按键，应用会按输入设备 descriptor 单独保存掩码或键码；不会把单台设备的实测结果扩散到整个品牌。

悬浮服务拥有唯一的手写笔输入路由器。画布 MotionEvent、悬浮窗口通用动作和可选无障碍 KeyEvent 都进入同一路由，并对重复按下/释放上报去重。无障碍服务只在批注模式或按键学习期间观察手写笔键；没有被用户接管的事件始终返回系统处理。

### 厂商规则与来源

| 型号 | 官方资料结论 | 应用策略 |
| --- | --- | --- |
| [Xiaomi Focus Pen](https://www.mi.com/global/product/xiaomi-focus-pen/) | 按住截图键并触碰屏幕后执行全局或局部截图 | 作为组合手势记录，仅系统透传，不简化为“副键长按截图” |
| [REDMI 灵感触控笔](https://www.mi.com/shop/buy?product_id=1230801851) | 官方商品页没有可用于确认统一按键动作的明确说明 | 系统透传或按键学习；用户可自行设置“副键长按 → 截图” |
| [HUAWEI M-Pencil 2/3/Pro](https://consumer.huawei.com/en/support/content/en-us16085408/) | 双击笔身感应区由系统或兼容应用切换工具 | 不伪装成 Android 主键/副键 |
| [HONOR Magic-Pencil 4s](https://www.honor.com/global/accessories/honor-magic-pencil-4s/) | 单击、双击和长按动作随桌面、笔记和媒体场景变化 | 仅建立 4s 型号记录，默认透传 |
| Samsung S Pen | 不同设备、蓝牙 Air Actions 和应用场景的功能不同 | 不设置品牌级动作，使用系统透传或学习 |
| [OPPO Pencil](https://support.oppo.com/en/answer/?aid=2196322) | 官方页面只确认基础兼容状态，没有统一按键映射 | 系统透传或学习 |
| [vivo Pencil2s](https://shop.vivo.com.cn/product/10010004) | 官方确认双按键设计，没有公开统一动作映射 | 仅建立型号记录，使用系统透传或学习 |

基础书写、压感和公开的 Android 手写笔按键仍通过标准事件处理。厂商固件在 Android 或无障碍层之前截获的按键、蓝牙隔空手势和封闭 SDK 功能无法由通用适配覆盖。

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
| 无障碍手势与按键桥接服务（可选） | 转发双指滑动、接收用户学习或接管的手写笔 KeyEvent、调用系统截图 | 否 |

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
