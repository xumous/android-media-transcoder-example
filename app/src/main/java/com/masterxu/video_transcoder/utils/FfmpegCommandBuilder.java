package com.masterxu.video_transcoder.utils;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FFmpeg 命令构建器
 * 根据用户选项构建 FFmpeg 命令字符串
 */
public class FfmpegCommandBuilder {

    // 视频编码器映射
    public static final String[] VIDEO_ENCODERS = {
            "libx264",      // H.264
            "libx265",      // H.265/HEVC
            "libvpx-vp9",   // VP9
            "libaom-av1",   // AV1
            "mpeg4",        // MPEG-4
            "copy"          // 复制
    };

    public static final String[] VIDEO_ENCODER_NAMES = {
            "H.264",
            "HEVC/H.265",
            "VP9",
            "AV1",
            "MPEG-4",
            "复制 (不重新编码)"
    };

    // 音频编码器映射
    public static final String[] AUDIO_ENCODERS = {
            "aac",          // AAC
            "libmp3lame",   // MP3
            "ac3",          // AC3
            "flac",         // FLAC
            "libopus",      // Opus
            "libvorbis",    // Vorbis
            "pcm_s16le",    // PCM
            "copy"          // 复制
    };

    public static final String[] AUDIO_ENCODER_NAMES = {
            "AAC",
            "MP3",
            "AC3",
            "FLAC",
            "Opus",
            "Vorbis",
            "PCM",
            "复制 (不重新编码)"
    };

    // 视频格式映射
    public static final String[] VIDEO_FORMATS = {
            "mp4",
            "mkv",
            "mov",
            "avi",
            "webm",
            "flv",
            "ts"
    };

    public static final String[] VIDEO_FORMAT_NAMES = {
            "MP4",
            "MKV",
            "MOV",
            "AVI",
            "WebM",
            "FLV",
            "TS"
    };

    // 音频格式映射
    public static final String[] AUDIO_FORMATS = {
            "mp3",
            "aac",
            "flac",
            "wav",
            "ogg",
            "m4a",
            "wma"
    };

    public static final String[] AUDIO_FORMAT_NAMES = {
            "MP3",
            "AAC",
            "FLAC",
            "WAV",
            "OGG",
            "M4A",
            "WMA"
    };

    // 图片格式映射
    public static final String[] IMAGE_FORMATS = {
            "jpg",
            "png",
            "gif",
            "bmp",
            "tiff",
            "webp"
    };

    public static final String[] IMAGE_FORMAT_NAMES = {
            "JPEG",
            "PNG",
            "GIF",
            "BMP",
            "TIFF",
            "WebP"
    };

    // 采样率选项
    public static final String[] SAMPLE_RATES = {
            "44100",
            "48000",
            "96000",
            "192000"
    };

    // 格式名到 FFmpeg 输出格式名的映射
    private static final Map<String, String> FORMAT_TO_FFMPEG_MUXER = new HashMap<>();

    static {
        // 视频
        FORMAT_TO_FFMPEG_MUXER.put("mp4", "mp4");
        FORMAT_TO_FFMPEG_MUXER.put("mkv", "matroska");
        FORMAT_TO_FFMPEG_MUXER.put("mov", "mov");
        FORMAT_TO_FFMPEG_MUXER.put("avi", "avi");
        FORMAT_TO_FFMPEG_MUXER.put("webm", "webm");
        FORMAT_TO_FFMPEG_MUXER.put("flv", "flv");
        FORMAT_TO_FFMPEG_MUXER.put("ts", "mpegts");
        // 音频
        FORMAT_TO_FFMPEG_MUXER.put("mp3", "mp3");
        FORMAT_TO_FFMPEG_MUXER.put("aac", "adts");      // AAC 通常用 ADTS 或 MP4，这里用 ADTS 确保纯音频文件
        FORMAT_TO_FFMPEG_MUXER.put("flac", "flac");
        FORMAT_TO_FFMPEG_MUXER.put("wav", "wav");
        FORMAT_TO_FFMPEG_MUXER.put("ogg", "ogg");
        FORMAT_TO_FFMPEG_MUXER.put("m4a", "mp4");       // M4A 实质是 MP4
        FORMAT_TO_FFMPEG_MUXER.put("wma", "asf");        // WMA 对应 ASF 容器
        // 图片
        FORMAT_TO_FFMPEG_MUXER.put("jpg", "mjpeg");      // JPEG 编码对应 mjpeg 复用器
        FORMAT_TO_FFMPEG_MUXER.put("jpeg", "mjpeg");
        FORMAT_TO_FFMPEG_MUXER.put("png", "image2");     // PNG 可通过 image2 输出
        FORMAT_TO_FFMPEG_MUXER.put("gif", "gif");
        FORMAT_TO_FFMPEG_MUXER.put("bmp", "image2");     // BMP 也使用 image2
        FORMAT_TO_FFMPEG_MUXER.put("tiff", "image2");    // TIFF 也使用 image2
        FORMAT_TO_FFMPEG_MUXER.put("webp", "webp");
    }

    /**
     * 获取 FFmpeg 可识别的输出格式名
     */
    private static String getFfmpegFormat(String userFormat) {
        String ffFormat = FORMAT_TO_FFMPEG_MUXER.get(userFormat);
        return ffFormat != null ? ffFormat : userFormat; // 默认返回原值，部分格式可直接使用
    }

    /**
     * 构建视频转码命令
     */
    public static String buildVideoCommand(VideoOptions options) {
        List<String> cmd = new ArrayList<>();

        // 输入
        cmd.add("-i");
        cmd.add("fd:" + options.inputFd);

        // 视频编码器
        cmd.add("-c:v");
        cmd.add(options.videoEncoder);

        // 如果编码器不是 copy，添加视频参数
        if (!"copy".equals(options.videoEncoder)) {
            // 尺寸
            if (!TextUtils.isEmpty(options.videoSize)) {
                cmd.add("-s");
                cmd.add(options.videoSize);
            }

            // 码率或 CRF
            if (options.useCrf) {
                cmd.add("-crf");
                cmd.add(options.crfValue);
                cmd.add("-b:v");
                cmd.add("0");  // CRF 模式下禁用固定码率
            } else {
                cmd.add("-b:v");
                cmd.add(options.videoBitrate);
            }

            // 帧率
            if (!TextUtils.isEmpty(options.frameRate)) {
                cmd.add("-r");
                cmd.add(options.frameRate);
            }

            // 宽高比
            if (!TextUtils.isEmpty(options.aspectRatio)) {
                cmd.add("-aspect");
                cmd.add(options.aspectRatio);
            }

            // 关键帧间隔
            if (!TextUtils.isEmpty(options.keyframeInterval)) {
                cmd.add("-g");
                cmd.add(options.keyframeInterval);
            }

            // 视频滤镜（旋转、翻转）
            List<String> filters = new ArrayList<>();

            if (options.rotation != 0) {
                switch (options.rotation) {
                    case 90:
                        filters.add("transpose=1");
                        break;
                    case 180:
                        filters.add("transpose=2,transpose=2");
                        break;
                    case 270:
                        filters.add("transpose=2");
                        break;
                }
            }

            if (options.hFlip) {
                filters.add("hflip");
            }

            if (options.vFlip) {
                filters.add("vflip");
            }

            if (!filters.isEmpty()) {
                cmd.add("-vf");
                cmd.add(String.join(",", filters));
            }

            // 二次编码
            if (options.twoPass) {
                cmd.add("-pass");
                cmd.add("2");
            }
        }

        // 音频编码器
        cmd.add("-c:a");
        cmd.add(options.audioEncoder);

        // 如果音频编码器不是 copy，添加音频参数
        if (!"copy".equals(options.audioEncoder)) {
            cmd.add("-ar");
            cmd.add(options.sampleRate);

            cmd.add("-b:a");
            cmd.add(options.audioBitrate);

            cmd.add("-ac");
            cmd.add(options.channels);

            // 音量调整
            if (!"100".equals(options.volume)) {
                try {
                    float vol = Float.parseFloat(options.volume) / 100f;
                    cmd.add("-af");
                    cmd.add("volume=" + vol);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // 保留所有流
        if (options.mapAllStreams) {
            cmd.add("-map");
            cmd.add("0");
        }

        // 覆盖输出文件
        cmd.add("-y");

        // 明确指定输出格式（关键修复）
        cmd.add("-f");
        cmd.add(getFfmpegFormat(options.format));

        // 输出
        cmd.add("fd:" + options.outputFd);

        return String.join(" ", cmd);
    }

    /**
     * 构建音频转码命令
     */
    public static String buildAudioCommand(AudioOptions options) {
        List<String> cmd = new ArrayList<>();

        cmd.add("-i");
        cmd.add("fd:" + options.inputFd);

        cmd.add("-c:a");
        cmd.add(options.encoder);

        cmd.add("-ar");
        cmd.add(options.sampleRate);

        cmd.add("-b:a");
        cmd.add(options.bitrate);

        cmd.add("-ac");
        cmd.add(options.channels);

        // 音量调整
        if (!"100".equals(options.volume)) {
            try {
                float vol = Float.parseFloat(options.volume) / 100f;
                cmd.add("-af");
                cmd.add("volume=" + vol);
            } catch (NumberFormatException ignored) {
            }
        }

        // 仅音频流
        cmd.add("-vn");

        cmd.add("-y");

        // 明确指定输出格式
        cmd.add("-f");
        cmd.add(getFfmpegFormat(options.format));

        cmd.add("fd:" + options.outputFd);

        return String.join(" ", cmd);
    }

    /**
     * 构建图片转码命令
     */
    public static String buildImageCommand(ImageOptions options) {
        List<String> cmd = new ArrayList<>();

        cmd.add("-i");
        cmd.add("fd:" + options.inputFd);

        // 根据格式指定编码器
        String encoder;
        switch (options.format) {
            case "jpg":
            case "jpeg":
                encoder = "mjpeg";
                break;
            case "png":
                encoder = "png";
                break;
            case "gif":
                encoder = "gif";
                break;
            case "bmp":
                encoder = "bmp";
                break;
            case "tiff":
                encoder = "tiff";
                break;
            case "webp":
                encoder = "webp";
                break;
            default:
                encoder = "mjpeg";
                break;
        }
        cmd.add("-c:v");
        cmd.add(encoder);

        // JPEG 质量
        if (options.format.equals("jpg") || options.format.equals("jpeg")) {
            cmd.add("-q:v");
            cmd.add(String.valueOf(options.quality));
        }

        // PNG 压缩级别
        if (options.format.equals("png")) {
            cmd.add("-compression_level");
            cmd.add("6");
        }

        cmd.add("-y");

        // 明确指定输出格式
        cmd.add("-f");
        // 图片格式可能直接使用编码器名作为复用器名，但部分需要映射
        String outFormat = getFfmpegFormat(options.format);
        cmd.add(outFormat);

        cmd.add("fd:" + options.outputFd);

        return String.join(" ", cmd);
    }

    // 以下索引方法保持不变...
    public static int getVideoEncoderIndex(String encoder) {
        for (int i = 0; i < VIDEO_ENCODERS.length; i++) {
            if (VIDEO_ENCODERS[i].equals(encoder)) {
                return i;
            }
        }
        return 1;
    }

    public static int getAudioEncoderIndex(String encoder) {
        for (int i = 0; i < AUDIO_ENCODERS.length; i++) {
            if (AUDIO_ENCODERS[i].equals(encoder)) {
                return i;
            }
        }
        return 0;
    }

    public static int getVideoFormatIndex(String format) {
        for (int i = 0; i < VIDEO_FORMATS.length; i++) {
            if (VIDEO_FORMATS[i].equals(format)) {
                return i;
            }
        }
        return 0;
    }

    public static int getAudioFormatIndex(String format) {
        for (int i = 0; i < AUDIO_FORMATS.length; i++) {
            if (AUDIO_FORMATS[i].equals(format)) {
                return i;
            }
        }
        return 0;
    }

    public static int getImageFormatIndex(String format) {
        for (int i = 0; i < IMAGE_FORMATS.length; i++) {
            if (IMAGE_FORMATS[i].equals(format)) {
                return i;
            }
        }
        return 0;
    }

    /**
     * 视频转码选项
     */
    public static class VideoOptions {
        public String inputFd;              // 输入文件描述符
        public String outputFd;             // 输出文件描述符
        public String format = "mp4";       // 输出格式
        public String videoEncoder = "libx265";  // 视频编码器
        public String videoSize = "";       // 视频尺寸 (如 1920x1080)
        public String videoBitrate = "16000k";   // 视频码率
        public boolean useCrf = false;      // 使用 CRF
        public String crfValue = "23";      // CRF 值
        public String frameRate = "";       // 帧率
        public String aspectRatio = "";     // 宽高比
        public boolean twoPass = false;     // 二次编码
        public String keyframeInterval = ""; // 关键帧间隔
        public int rotation = 0;            // 旋转角度 (0, 90, 180, 270)
        public boolean hFlip = false;       // 水平翻转
        public boolean vFlip = false;       // 垂直翻转

        // 音频选项
        public String audioEncoder = "aac"; // 音频编码器
        public String sampleRate = "48000"; // 采样率
        public String audioBitrate = "320k"; // 音频码率
        public String channels = "2";       // 声道数
        public String volume = "100";       // 音量 (百分比)
        public boolean mapAllStreams = true; // 保留所有流
    }

    /**
     * 音频转码选项
     */
    public static class AudioOptions {
        public String inputFd;
        public String outputFd;
        public String format = "mp3";
        public String encoder = "libmp3lame";
        public String sampleRate = "44100";
        public String bitrate = "320k";
        public String channels = "2";
        public String volume = "100";
    }

    /**
     * 图片转码选项
     */
    public static class ImageOptions {
        public String inputFd;
        public String outputFd;
        public String format = "jpg";
        public int quality = 90;            // JPEG 质量 1-100
    }
}