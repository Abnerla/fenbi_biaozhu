# 粉笔标注 (Chalk Annotation)

<div align="center">

![Version](https://img.shields.io/badge/version-1.2.2-blue.svg)
![Android](https://img.shields.io/badge/platform-Android%2028%2B-green.svg)
![Kotlin](https://img.shields.io/badge/language-Kotlin-purple.svg)
![License](https://img.shields.io/badge/license-MIT-orange.svg)

一款功能强大的Android屏幕标注工具，支持实时绘图、智能截图等功能

[特性](#-特性) • [安装](#-安装) • [使用](#-使用) • [技术栈](#️-技术栈) • [贡献](#-贡献)

</div>

---

## 📖 简介

**粉笔标注**是一款专为Android设计的全局屏幕标注应用，允许用户在任何应用界面上进行实时绘图和标注。无论是在线教学、远程协作还是问题反馈，都能轻松捕捉并标注屏幕内容。

### 💡 应用场景

- 📚 **在线教学**：教师可以在课件或应用上实时标注讲解
- 💼 **远程协作**：团队成员标注屏幕内容进行讨论
- 🐛 **问题反馈**：快速标注问题区域并截图保存
- 🎮 **游戏直播**：游戏主播标注游戏画面进行讲解
- 📱 **应用演示**：产品经理展示应用功能时进行标注

---

## ✨ 特性

### 🎨 多样化绘图工具
- **画笔工具** - 支持8种预设颜色和自定义笔触粗细（1-20px），适合精确标注
- **荧光笔工具** - 半透明效果，可调节透明度（10%-80%），适合重点标记
- **智能橡皮擦** - 按路径擦除，触碰任意部分即可删除整条路径，操作更直观

### 🖼️ 智能截图系统
- **高质量捕获** - 基于MediaProjection API的高质量屏幕截图
- **标注合成** - 自动将标注内容与屏幕内容合成为一张图片
- **智能隐藏UI** - 截图时自动隐藏工具栏和弹窗，确保截图纯净无遮挡
- **一键保存** - 截图自动保存到系统相册，方便分享和查看

### 🎯 悬浮窗体验
- **全局悬浮按钮** - 在任何应用中快速开启标注功能
- **自动吸附边缘** - 悬浮按钮和工具栏智能吸附到屏幕边缘
- **可拖拽工具栏** - 工具栏位置可自由调整并记忆
- **双布局切换** - 支持竖排和横排两种工具栏布局，适应不同使用场景

### ⚙️ 丰富的自定义选项
- **颜色自定义** - 画笔和荧光笔各有6-8种预设颜色可选
- **笔触调节** - 画笔粗细（1-20px）、荧光笔粗细（10-50px）、橡皮擦大小（20-100px）
- **透明度控制** - 荧光笔透明度可在设置中精确调整
- **自动折叠** - 可选择绘图时是否自动折叠二级菜单

### 🎮 便捷操作
- **压感支持** - 支持触控笔压感，笔触粗细随压力变化（30%-100%）
- **双指双击撤销** - 快速撤销上一步操作
- **贝塞尔平滑** - 使用二次贝塞尔曲线平滑路径，书写更流畅
- **状态持久化** - 工具配置、工具栏位置自动保存
- **前台服务** - 保证标注功能稳定运行

---

## 🚀 安装

### 系统要求
- Android 9.0 (API 28) 或更高版本
- 建议设备内存 2GB 以上
- 建议屏幕分辨率 1080p 以上

### 安装步骤

#### 方式一：下载APK安装
1. 从 [Releases](../../releases) 页面下载最新版本的APK文件
2. 在手机上打开APK文件进行安装
3. 授予必要的权限（悬浮窗权限、屏幕捕获权限、存储权限）

#### 方式二：从源码构建
```bash
# 克隆仓库
git clone https://github.com/yourusername/annotation.git
cd annotation

# 使用Gradle构建
./gradlew assembleDebug

# APK输出路径
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 📱 使用

### 快速开始

1. **授予权限**
   - 打开应用，在主界面依次授予以下权限：
     - ✅ 悬浮窗权限：用于显示绘图工具
     - ✅ 屏幕捕获权限：用于截图功能
     - ✅ 存储权限：用于保存截图到相册

2. **启动服务**
   - 授权完成后，点击"启动服务"按钮
   - 屏幕上会出现一个蓝色悬浮按钮

3. **开始标注**
   - 点击悬浮按钮进入标注模式
   - 选择绘图工具开始在屏幕上绘制

4. **截图保存**
   - 点击工具栏中的截图按钮
   - 截图将自动保存到系统相册

### 详细功能说明

#### 🖌️ 绘图工具使用

**画笔 ✏️**
- 点击画笔图标选择画笔工具
- 点击色板图标选择颜色（8种预设颜色）
- 点击笔触图标调节粗细（1-20像素）
- 完全不透明，适合绘制精确的线条和图形

**荧光笔 🖍️**
- 点击荧光笔图标选择荧光笔工具
- 点击色板图标选择颜色（6种预设颜色）
- 点击笔触图标调节粗细（10-50像素）
- 在设置中可调节透明度（10%-80%）
- 半透明效果，适合高亮标记重点区域

**橡皮擦 🧹**
- 点击橡皮擦图标选择橡皮擦工具
- 点击大小图标调节橡皮擦大小（20-100像素）
- 触碰任何路径即可删除整条路径（按路径擦除）
- 橡皮擦会显示半透明圆圈和十字准星

#### 🔧 工具栏操作

**移动工具栏**
- 长按工具栏拖动到任意位置
- 松开后自动吸附到最近的屏幕边缘

**切换布局**
- 点击工具栏底部的切换图标
- 可在竖排和横排布局间切换
- 布局偏好自动保存

**折叠菜单**
- 二级菜单（颜色、粗细选择）可自动折叠
- 在设置中可开启"绘图时自动折叠"功能

#### ⚡ 快捷操作

- **撤销**：双指同时快速点击屏幕两次（或点击撤销按钮）
- **清空**：点击工具栏中的清空图标删除所有标注
- **截图**：点击截图图标保存当前屏幕+标注
- **退出**：点击退出图标返回悬浮按钮模式

---

## 🛠️ 技术栈

### 开发框架
- **Kotlin** - 主要开发语言
- **Jetpack Compose** - 现代化声明式UI框架
- **Material Design 3** - Google最新UI设计规范

### 核心技术

#### 前台服务 & 悬浮窗
- `WindowManager` - 管理悬浮窗视图
- `TYPE_APPLICATION_OVERLAY` - 系统级悬浮窗类型
- `FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION` - 屏幕捕获前台服务类型
- `FOREGROUND_SERVICE_TYPE_SPECIAL_USE` - 特殊用途前台服务类型

#### 屏幕捕获
- `MediaProjection API` - Android官方屏幕捕获API
- `VirtualDisplay` - 虚拟显示器用于捕获屏幕
- `ImageReader` - 读取屏幕图像数据
- 自动隐藏UI机制确保截图纯净

#### 绘图引擎
- Compose `Canvas` - 高性能绘图画布
- **贝塞尔曲线平滑算法** - 使路径更加流畅自然
- **压感支持** - 根据触控压力调整笔触粗细和透明度
- **路径碰撞检测** - 实现智能橡皮擦按路径擦除

#### 数据持久化
- `SharedPreferences` - 保存用户配置和权限状态
- `StateFlow` & `MutableState` - 响应式状态管理
- 工具配置、工具栏位置自动保存

### 项目架构
```
app/
├── drawing/                  # 绘图引擎核心
│   └── DrawingEngine.kt      # 绘图逻辑管理、状态管理、历史记录
├── model/                    # 数据模型
│   ├── DrawingModels.kt      # 绘图相关数据类（路径、配置等）
│   └── VersionInfo.kt        # 版本信息
├── service/                  # 服务层
│   └── OverlayService.kt     # 悬浮窗前台服务、生命周期管理
├── ui/                       # UI组件
│   ├── DrawingCanvas.kt      # 绘图画布组件、手势检测
│   ├── OverlayContent.kt     # 悬浮层内容、工具栏定位
│   └── ToolbarView.kt        # 工具栏组件、工具选择
└── utils/                    # 工具类
    ├── ScreenCaptureManager.kt    # 屏幕捕获管理、MediaProjection
    ├── ScreenshotHelper.kt        # 截图保存、Bitmap处理
    ├── PermissionHelper.kt        # 权限检查和请求
    └── PreferencesManager.kt      # 配置管理
```

---

## 🎯 核心实现亮点

### 1. 智能屏幕捕获

```kotlin
// 在前台服务中初始化MediaProjection（Android 14+要求）
fun initializeMediaProjection(resultCode: Int, data: Intent): Boolean {
    val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
        as MediaProjectionManager
    mediaProjection = projectionManager.getMediaProjection(resultCode, data)
    return mediaProjection != null
}

// 捕获屏幕并合成标注
fun captureScreenWithAnnotation(
    drawingEngine: DrawingEngine,
    onSuccess: (Uri) -> Unit,
    onError: (Exception) -> Unit
) {
    // 1. 隐藏工具栏UI
    toolbarVisibleState.value = false

    // 2. 延迟等待UI更新
    handler.postDelayed({
        // 3. 创建VirtualDisplay捕获屏幕
        // 4. 转换为Bitmap
        // 5. 在Bitmap上绘制标注路径
        // 6. 保存到系统相册
        // 7. 恢复工具栏UI显示
        toolbarVisibleState.value = true
    }, 100)
}
```

### 2. 按路径智能橡皮擦

```kotlin
// 路径与橡皮擦碰撞检测
private fun isPathIntersectingEraser(
    path: DrawingPath,
    eraserPosition: Offset,
    eraserRadius: Float
): Boolean {
    // 检查路径中的任何点是否在橡皮擦范围内
    return path.points.any { point ->
        val dx = point.offset.x - eraserPosition.x
        val dy = point.offset.y - eraserPosition.y
        val distance = sqrt(dx * dx + dy * dy)
        distance <= eraserRadius
    }
}

// 按路径擦除
private fun erasePathsAt(eraserPosition: Offset) {
    val eraserRadius = _eraserConfig.value.size / 2
    val pathsToRemove = paths.filter { path ->
        isPathIntersectingEraser(path, eraserPosition, eraserRadius)
    }

    // 如果有路径被擦除，保存到历史记录以支持撤销
    if (pathsToRemove.isNotEmpty()) {
        saveToHistory()
        paths.removeAll(pathsToRemove)
    }
}
```

### 3. 贝塞尔曲线平滑

```kotlin
// 使用二次贝塞尔曲线平滑路径，使绘制更流畅
val drawPath = Path()
drawPath.moveTo(firstPoint.offset.x, firstPoint.offset.y)

for (i in 1 until path.points.size) {
    val prevPoint = path.points[i - 1]
    val currentPoint = path.points[i]

    if (i < path.points.size - 1) {
        val controlPoint = Offset(
            (prevPoint.offset.x + currentPoint.offset.x) / 2,
            (prevPoint.offset.y + currentPoint.offset.y) / 2
        )
        drawPath.quadraticTo(
            prevPoint.offset.x,
            prevPoint.offset.y,
            controlPoint.x,
            controlPoint.y
        )
    } else {
        drawPath.lineTo(currentPoint.offset.x, currentPoint.offset.y)
    }
}
```

### 4. 压感支持

```kotlin
// 计算压感调整后的笔触宽度
fun calculatePressureAdjustedWidth(baseWidth: Float, pressure: Float): Float {
    // 最小宽度为基础宽度的30%，最大为100%
    val minScale = 0.3f
    val maxScale = 1.0f
    val scale = minScale + (maxScale - minScale) * pressure
    return baseWidth * scale
}

// 计算压感调整后的透明度（仅荧光笔）
fun calculatePressureAdjustedAlpha(baseAlpha: Float, pressure: Float): Float {
    // 轻触时透明度降低，重压时接近完全不透明
    val minScale = 0.5f
    val maxScale = 1.0f
    val scale = minScale + (maxScale - minScale) * pressure
    return baseAlpha * scale
}
```

---

## 📋 权限说明

### 必需权限

```xml
<!-- 悬浮窗权限 -->
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />

<!-- 前台服务权限 -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />

<!-- 通知权限（Android 13+） -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- 存储权限（Android 13+） -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />

<!-- 存储权限（Android 10-12） -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
```

### 为什么需要这些权限？

| 权限 | 用途 | 是否必需  |
|------|------|-------|
| SYSTEM_ALERT_WINDOW | 在其他应用上层显示悬浮窗和绘图工具 | ✅ 必需  |
| FOREGROUND_SERVICE | 确保标注功能在后台稳定运行 | ✅ 必需  |
| FOREGROUND_SERVICE_MEDIA_PROJECTION | 在前台服务中进行屏幕捕获（Android 14+要求） | ✅ 必需  |
| POST_NOTIFICATIONS | 显示前台服务通知 | ✅ 必需  |
| READ_MEDIA_IMAGES / WRITE_EXTERNAL_STORAGE | 将截图保存到系统相册 | ✅ 非必需 |

**隐私承诺**：所有权限仅用于本地功能，不上传任何数据到云端或第三方服务器。

---

## 🔧 配置与设置

### 应用内设置

进入应用主界面，点击右上角设置图标可配置：

- **荧光笔透明度**：调节荧光笔的透明度（10%-80%）
- **自动折叠工具栏**：开启后绘图时自动折叠二级菜单
- **版本信息**：查看当前应用版本

### 高级配置（开发者）

开发者可以修改以下常量自定义行为：

```kotlin
// DrawingEngine.kt
private val maxHistorySize = 50  // 撤销历史记录数量

// ScreenCaptureManager.kt
handler.postDelayed({ ... }, 300)  // VirtualDisplay渲染等待时间（ms）

// OverlayService.kt
handler.postDelayed({ ... }, 100)  // UI隐藏等待时间（ms）
```


---

## 🤝 贡献

欢迎贡献代码、报告问题或提出新功能建议！

### 贡献方式

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

### 报告问题

在 [Issues](../../issues) 页面提交问题时，请包含：
- 设备型号和Android版本
- 详细的问题描述
- 复现步骤
- 相关截图或日志（如有）

### 代码规范

- 使用Kotlin编写代码
- 遵循Android官方编码规范
- 为新功能编写单元测试
- 提交前运行`./gradlew test`确保测试通过

---

## 📄 开源协议

本项目采用 MIT 协议开源 - 查看 [LICENSE](LICENSE) 文件了解详情

```
MIT License

Copyright (c) 2025

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 🙏 致谢

感谢以下开源项目和资源：

- [Jetpack Compose](https://developer.android.com/jetpack/compose) - 现代化Android UI框架
- [Material Design 3](https://m3.material.io/) - Google设计规范
- [Kotlin](https://kotlinlang.org/) - 优雅的现代编程语言
- [Android Developer Documentation](https://developer.android.com/) - 完善的官方文档

---

## 📞 联系方式

- 项目主页：[GitHub Repository](https://github.com/Abnerla/fenbi_biaozhu)
- 问题反馈：[GitHub Issues](https://github.com/Abnerla/fenbi_biaozhu/issues)

---

## 📈 性能指标

- **内存占用**：~30-50MB
- **历史记录**：最多保存50步操作
- **最低系统要求**：Android 9.0 (API 28)
- **推荐系统**：Android 10.0 (API 29) 或更高

---

<div align="center">

**如果这个项目对你有帮助，请给个 ⭐️ Star 支持一下！**

Made with ❤️ by Developer

</div>
