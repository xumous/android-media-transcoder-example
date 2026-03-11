package com.masterxu.video_transcoder.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import androidx.documentfile.provider.DocumentFile;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * 文件工具类
 * 处理 SAF (Storage Access Framework) 相关操作
 */
public class FileUtils {

    /**
     * 从 URI 获取文件名
     */
    public static String getFileName(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }

    /**
     * 从 URI 获取文件大小
     */
    public static long getFileSize(Context context, Uri uri) {
        long size = 0;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (index >= 0) {
                        size = cursor.getLong(index);
                    }
                }
            }
        }
        return size;
    }

    /**
     * 获取文件描述符 (ParcelFileDescriptor)
     */
    public static ParcelFileDescriptor getFileDescriptor(Context context, Uri uri, String mode)
            throws FileNotFoundException {
        return context.getContentResolver().openFileDescriptor(uri, mode);
    }

    /**
     * 获取文件描述符的 fd 数字
     */
    public static int getFdNumber(ParcelFileDescriptor pfd) {
        return pfd.getFd();
    }

    /**
     * 打开输入文件并获取 fd 数字
     */
    public static ParcelFileDescriptor openInputFile(Context context, Uri uri)
            throws FileNotFoundException {
        return getFileDescriptor(context, uri, "r");
    }

    /**
     * 打开输出文件并获取 fd 数字
     */
    public static ParcelFileDescriptor openOutputFile(Context context, Uri uri)
            throws FileNotFoundException {
        return getFileDescriptor(context, uri, "rw");
    }

    /**
     * 在指定目录下创建新文件
     */
    public static DocumentFile createFileInDirectory(Context context, Uri treeUri,
                                                     String fileName, String mimeType) {
        DocumentFile pickedDir = DocumentFile.fromTreeUri(context, treeUri);
        if (pickedDir != null && pickedDir.isDirectory()) {
            return pickedDir.createFile(mimeType, fileName);
        }
        return null;
    }

    /**
     * 获取文件的 MIME 类型
     */
    public static String getMimeType(String fileName) {
        String extension = getFileExtension(fileName).toLowerCase();
        switch (extension) {
            case "mp4":
                return "video/mp4";
            case "mkv":
                return "video/x-matroska";
            case "mov":
                return "video/quicktime";
            case "avi":
                return "video/x-msvideo";
            case "webm":
                return "video/webm";
            case "flv":
                return "video/x-flv";
            case "ts":
                return "video/mp2t";
            case "mp3":
                return "audio/mpeg";
            case "aac":
                return "audio/aac";
            case "flac":
                return "audio/flac";
            case "wav":
                return "audio/wav";
            case "ogg":
                return "audio/ogg";
            case "m4a":
                return "audio/mp4";
            case "wma":
                return "audio/x-ms-wma";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "tiff":
            case "tif":
                return "image/tiff";
            case "webp":
                return "image/webp";
            default:
                return "*/*";
        }
    }

    /**
     * 获取文件扩展名
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    /**
     * 移除文件扩展名
     */
    public static String removeExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return fileName;
        }
        return fileName.substring(0, fileName.lastIndexOf("."));
    }

    /**
     * 更改文件扩展名
     */
    public static String changeExtension(String fileName, String newExtension) {
        String baseName = removeExtension(fileName);
        return baseName + "." + newExtension;
    }

    /**
     * 格式化文件大小
     */
    public static String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * 检查 URI 是否可读写
     */
    public static boolean canReadWrite(Context context, Uri uri) {
        try {
            ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "rw");
            if (pfd != null) {
                pfd.close();
                return true;
            }
        } catch (IOException e) {
            // 无法读写
        }
        return false;
    }

    /**
     * 获取文件描述符的大小
     */
    public static long getFdSize(ParcelFileDescriptor pfd) {
        try {
            return pfd.getStatSize();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 关闭 ParcelFileDescriptor（安全地）
     */
    public static void closeQuietly(ParcelFileDescriptor pfd) {
        if (pfd != null) {
            try {
                pfd.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * 生成输出文件名
     */
    public static String generateOutputFileName(String inputFileName, String outputFormat) {
        String baseName = removeExtension(inputFileName);
        return baseName + "_converted." + outputFormat;
    }

    /**
     * 生成带时间戳的输出文件名
     */
    public static String generateTimestampedFileName(String inputFileName, String outputFormat) {
        String baseName = removeExtension(inputFileName);
        String timestamp = String.valueOf(System.currentTimeMillis());
        return baseName + "_" + timestamp + "." + outputFormat;
    }
}
