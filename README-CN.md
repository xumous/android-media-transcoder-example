# Android Media Transcoder - 视频/音频/图片转码器实例

[![Java](https://img.shields.io/badge/language-Java-blue.svg)](https://www.java.com/)
[![FFmpegKit](https://img.shields.io/badge/FFmpegKit-6.0--2-red.svg)](https://github.com/arthenica/ffmpeg-kit)
[![API](https://img.shields.io/badge/API-28%2B-brightgreen.svg)](https://developer.android.com/about/versions/pie)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

> **写着玩儿** – 因为手机上好像不能命令行操作 FFmpeg ，市面上的转码 App 动不动就要收费、充 VIP，干脆自己写一个。  
> 能跑就行，Bug 什么的随缘修，兼容性可能差点意思，但你愿意折腾的话，或许能用。

---

## ✨ 功能特性

* **支持三大类型转码**：视频、音频、图片，常见格式几乎全覆盖。
* **基于 FFmpeg**：底层使用 `ffmpeg-kit-full-gpl`，解码/编码能力强大。
* **多线程任务队列**：可配置同时运行的任务数（1~10），自动排队执行。
* **实时进度显示**：任务列表中显示转码进度、输出文件大小及体积变化百分比（↑ 增大 / ↓ 减小）。
* **详细参数调整**：
    * **视频**：格式（MP4/MKV/MOV…）、编码器（H.264/HEVC/VP9…）、分辨率、码率/CRF、帧率、宽高比、二次编码、旋转/翻转。
    * **音频**：格式（MP3/AAC/FLAC…）、编码器、采样率、比特率、声道数、音量调整。
    * **图片**：格式（JPEG/PNG/GIF…）、JPEG 质量调节。
* **智能输出路径**：可选择输出到源文件目录，或手动选择任意文件夹（SAF 支持）。
* **任务管理**：添加、移除选中、清空列表、停止所有、开始所有，简单粗暴。
* **全局设置**：调节并发线程数，避免把手机搞卡。
* **权限友好（？）**：虽然要求多，但都是为了能读写文件。

---

## 🧰 技术栈

- **语言**：Java
- **最小 SDK**：28 (Android 9)
- **FFmpeg 绑定**：[ffmpeg-kit-full-gpl](https://github.com/arthenica/ffmpeg-kit) v6.0-2
- **UI**：Material Design 3 (通过 Material Components)
- **架构组件**：ViewModel + LiveData + RecyclerView
- **文件访问**：Storage Access Framework (SAF) + 文件描述符直通

---

## 📁 项目结构

```
com.masterxu.video_transcoder
│
├── MainActivity.java                 // 主界面
├── model
│   ├── TaskItem.java                 // 任务实体
│   └── TaskStatus.java               // 状态枚举
├── viewmodel
│   └── TaskViewModel.java            // 任务管理 + 线程池
├── adapter
│   └── TaskListAdapter.java          // 列表适配器
├── dialog
│   ├── GlobalSettingsDialog.java     // 全局设置
│   ├── VideoOptionsDialog.java       // 视频选项
│   ├── AudioOptionsDialog.java       // 音频选项
│   └── ImageOptionsDialog.java       // 图片选项
├── utils
│   ├── FfmpegCommandBuilder.java     // FFmpeg 命令生成器
│   ├── FileUtils.java                // SAF 文件工具
│   └── PermissionHelper.java         // 权限请求辅助
└── worker
    └── FfmpegTaskRunner.java         // 实际执行转码的线程
```

---

## ⚙️ 全局设置

点击右上角菜单 **“全局设置”**：

- **线程数**：同时运行的任务数量（1~10），默认为 5。  
  注意：设得太高可能让手机发烫、卡顿，请根据设备性能调整。

---

## 🛠 构建与运行

### 环境要求

- Android Studio Flamingo 或更高版本
- Gradle 7.4+
- Android SDK 34

### 步骤

1. 克隆仓库
   ```bash
   git clone https://github.com/xumous/android-media-transcoder-example.git
   ```
2. 用 Android Studio 打开项目，等待 Gradle 同步。
3. 连接设备或启动模拟器，点击 Run。

### 依赖项

```gradle
dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.recyclerview:recyclerview:1.3.2'
    implementation 'androidx.lifecycle:lifecycle-viewmodel:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-livedata:2.7.0'
    implementation 'androidx.documentfile:documentfile:1.0.1'
    implementation 'com.arthenica:ffmpeg-kit-full-gpl:6.0-2'
}
```

---

## ⚠️ 注意事项

1. **权限**：如前所述，请务必在系统设置中授予所有存储权限，尤其是 Android 11+ 的“所有文件访问权限”。
2. **转码性能**：FFmpeg 是 CPU 密集型的，长时间高负载转码可能导致手机发热、耗电快。
3. **格式兼容性**：部分编码器可能需要设备硬件支持（如硬件加速编码），若失败会自动回退到软件编码。
4. **取消任务**：点击“停止所有”或移除运行中的任务会立即终止 FFmpeg 进程，但已写入的部分数据不会删除。
5. **已知 Bug**：进度条偶尔抽风，大小变化计算可能有偏差，反正“能跑就行” 😅。

---

## ❓ 常见问题

### Q: 为什么总是提示“无法在源目录创建文件”？

A: 因为 Android SAF 限制，某些目录（如 DCIM、Downloads）可能没有直接写入权限。请选择 **“选择目录”**
并手动指定一个有写入权限的文件夹（比如你创建的一个专用文件夹）。

### Q: 转码开始后一直卡在 0%？

A: 可能输入文件损坏，或 FFmpeg 无法解码。请查看 Logcat 日志（搜索 `FfmpegTaskRunner`）获取详细错误。也可能是并发数已满，等待其他任务完成即可。

### Q: 能不能后台运行？

A: 目前没有实现 Service，退出应用后任务会被强制停止。后续版本可能会考虑加入前台服务。

### Q: 支持 GPU 加速吗？

A: FFmpegKit full-gpl 包含 NVENC、VAAPI 等硬件加速编码器，但 Android
设备上硬件加速支持取决于设备的编解码器。如果设备支持，可以选择对应的编码器（如 `h264_mediacodec`
），不过本应用默认的编码器列表里没有包含它们，你可以手动修改代码添加。

### Q: 为什么生成的视频在某些播放器里打不开？

A: 可能是编码器与容器不兼容（例如将 HEVC 视频放入 AVI 容器）。请尽量选择主流组合（如 H.264 + MP4，HEVC + MKV）。

---

## 📄 许可证

本项目代码采用 **GNU General Public License v3.0**。  
因为使用了 [ffmpeg-kit-full-gpl](https://github.com/arthenica/ffmpeg-kit)（GPL 版本），所以整个项目必须遵循 GPL 协议。  
详见 [LICENSE](LICENSE) 文件。

---

## 🙏 致谢

- [FFmpeg](https://ffmpeg.org/)：永远的神。
- [arthenica/ffmpeg-kit](https://github.com/arthenica/ffmpeg-kit)：省去了自己编译 FFmpeg 的麻烦。
- 所有付费转码 App 的“激励”：你们要是不那么贵，我也不会自己写。🤪

---

> **最后更新**：2026年3月11日  
> **项目地址
**：[https://github.com/xumous/android-media-transcoder-example](https://github.com/xumous/android-media-transcoder-example)  
> **如有问题**：欢迎提 Issue，但可能不会马上修，毕竟写着玩儿的～