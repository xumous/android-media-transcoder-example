# android-media-transcoder-example

A comprehensive Android media transcoder supporting video, audio, and image format conversion. Leverages hardware
acceleration via ffmpeg-kit-full-gpl on Android for efficient processing. Handles common formats such as MP4 for video;
MP3 for audio; and JPEG for images. Ideal for learning Android media processing or building file converter apps.

# Android Media Transcoder - Video/Audio/Image Transcoder Example

[![Java](https://img.shields.io/badge/language-Java-blue.svg)](https://www.java.com/)
[![FFmpegKit](https://img.shields.io/badge/FFmpegKit-6.0--2-red.svg)](https://github.com/arthenica/ffmpeg-kit)
[![API](https://img.shields.io/badge/API-28%2B-brightgreen.svg)](https://developer.android.com/about/versions/pie)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

> **Just for Fun** – Since you can't really run FFmpeg via command line on phones, and most transcoding apps on the
> market ask for payments or VIP memberships, I decided to build my own.  
> As long as it runs, it's fine. Bug fixes are on a "when I feel like it" basis. Compatibility might be a bit iffy, but
> if you're willing to tinker, it might just work.

---

## ✨ Features

* **Supports Three Major Media Types**: Video, audio, and image transcoding. Covers almost all common formats.
* **Based on FFmpeg**: Uses `ffmpeg-kit-full-gpl` under the hood for powerful decoding/encoding.
* **Multi-threaded Task Queue**: Configure concurrent tasks (1~10), tasks are automatically queued.
* **Real-time Progress Display**: Shows transcoding progress, output file size, and size change percentage (↑
  increase / ↓ decrease) directly in the task list.
* **Detailed Parameter Adjustments**:
    * **Video**: Format (MP4/MKV/MOV…), Encoder (H.264/HEVC/VP9…), Resolution, Bitrate/CRF, Framerate, Aspect Ratio,
      2-Pass Encoding, Rotate/Flip.
    * **Audio**: Format (MP3/AAC/FLAC…), Encoder, Sample Rate, Bitrate, Channels, Volume Adjustment.
    * **Image**: Format (JPEG/PNG/GIF…), JPEG Quality Adjustment.
* **Smart Output Path**: Choose to save output in the source file directory, or manually select any folder (SAF
  support).
* **Task Management**: Add, remove selected, clear list, stop all, start all – simple and straightforward.
* **Global Settings**: Adjust the number of concurrent threads to prevent your phone from lagging.
* **Permission-Friendly (?)** : Requires a fair number of permissions, but it's all for file reading/writing.

---

## 🧰 Tech Stack

- **Language**: Java
- **Minimum SDK**: 28 (Android 9)
- **FFmpeg Binding**: [ffmpeg-kit-full-gpl](https://github.com/arthenica/ffmpeg-kit) v6.0-2
- **UI**: Material Design 3 (via Material Components)
- **Architecture Components**: ViewModel + LiveData + RecyclerView
- **File Access**: Storage Access Framework (SAF) + File Descriptor Passthrough

---

## 📁 Project Structure

```
com.masterxu.video_transcoder
│
├── MainActivity.java                 // Main UI
├── model
│   ├── TaskItem.java                 // Task Entity
│   └── TaskStatus.java               // Status Enum
├── viewmodel
│   └── TaskViewModel.java            // Task Management + Thread Pool
├── adapter
│   └── TaskListAdapter.java          // List Adapter
├── dialog
│   ├── GlobalSettingsDialog.java     // Global Settings Dialog
│   ├── VideoOptionsDialog.java       // Video Options Dialog
│   ├── AudioOptionsDialog.java       // Audio Options Dialog
│   └── ImageOptionsDialog.java       // Image Options Dialog
├── utils
│   ├── FfmpegCommandBuilder.java     // FFmpeg Command Builder
│   ├── FileUtils.java                // SAF File Utilities
│   └── PermissionHelper.java         // Permission Request Helper
└── worker
    └── FfmpegTaskRunner.java         // Thread for Executing Transcoding Tasks
```

---

## ⚙️ Global Settings

Click the menu item **"Global Settings"** in the top right corner:

- **Thread Count**: Number of tasks running simultaneously (1~10), default is 5.  
  Note: Setting this too high might cause your phone to overheat or lag. Adjust based on your device's performance.

---

## 🛠 Build & Run

### Requirements

- Android Studio Flamingo or newer
- Gradle 7.4+
- Android SDK 34

### Steps

1. Clone the repository
   ```bash
   git clone https://github.com/xumous/android-media-transcoder-example.git
   ```
2. Open the project in Android Studio and wait for Gradle to sync.
3. Connect a device or start an emulator, then click Run.

### Dependencies

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

## ⚠️ Important Notes

1. **Permissions**: As mentioned, please grant all necessary storage permissions in system settings, especially "All
   files access" on Android 11+.
2. **Transcoding Performance**: FFmpeg is CPU-intensive. Long-duration, high-load transcoding may lead to overheating
   and rapid battery drain.
3. **Format Compatibility**: Some encoders may require hardware support (e.g., hardware-accelerated encoding). If they
   fail, the process will fall back to software encoding automatically.
4. **Canceling Tasks**: Clicking "Stop All" or removing a running task will terminate the FFmpeg process immediately,
   but any data already written will not be deleted.
5. **Known Bugs**: The progress bar might occasionally act up, and size change calculations could be off. But hey, "as
   long as it runs" 😅.

---

## ❓ FAQ

### Q: Why does it always say "Cannot create file in source directory"?

A: Due to Android SAF restrictions, some directories (like DCIM, Downloads) may not have direct write permissions.
Please choose **"Select Directory"** and manually pick a folder where your app has write access (e.g., a dedicated
folder you created).

### Q: Transcoding starts but gets stuck at 0%?

A: The input file might be corrupted, or FFmpeg might not be able to decode it. Check the Logcat logs (search
for `FfmpegTaskRunner`) for detailed errors. It could also be that the maximum number of concurrent tasks is reached;
just wait for others to finish.

### Q: Can it run in the background?

A: Currently, no Service is implemented. Tasks will be forcefully stopped if you exit the app. A foreground service
might be considered in future versions.

### Q: Does it support GPU acceleration?

A: FFmpegKit full-gpl includes hardware-accelerated encoders like NVENC, VAAPI, etc. However, hardware acceleration
support on Android depends on the device's codecs. If your device supports it, you could manually add encoders
like `h264_mediacodec` in the code, though they are not included in the default encoder list.

### Q: Why can't some players open the generated video?

A: This might be due to incompatibility between the encoder and the container (e.g., putting HEVC video into an AVI
container). Stick to mainstream combinations (like H.264 + MP4, HEVC + MKV).

---

## 📄 License

The code in this project is licensed under the **GNU General Public License v3.0**.  
Due to the use of [ffmpeg-kit-full-gpl](https://github.com/arthenica/ffmpeg-kit) (GPL version), the entire project must
comply with the GPL license.  
See the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgements

- [FFmpeg](https://ffmpeg.org/): The GOAT.
- [arthenica/ffmpeg-kit](https://github.com/arthenica/ffmpeg-kit): Saved me the trouble of compiling FFmpeg myself.
- All paid transcoding apps for the "motivation": If you weren't so expensive, I wouldn't have built this myself. 🤪

---

> **Last Updated**: March 11, 2026  
> **Project URL
**: [https://github.com/xumous/android-media-transcoder-example](https://github.com/xumous/android-media-transcoder-example)  
> **Issues**: Feel free to open an issue, but don't expect an immediate fix. It's just for fun～