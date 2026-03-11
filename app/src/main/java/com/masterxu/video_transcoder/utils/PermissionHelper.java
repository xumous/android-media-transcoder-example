package com.masterxu.video_transcoder.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限帮助类
 * 处理运行时权限请求
 */
public class PermissionHelper {

    public static final int REQUEST_CODE_STORAGE = 1001;
    public static final int REQUEST_CODE_MANAGE_STORAGE = 1002;

    /**
     * 检查是否有所需的存储权限
     */
    public static boolean hasStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 使用新的媒体权限
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context,
                            Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context,
                            Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10-12
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            // Android 9 及以下
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * 请求存储权限
     */
    public static void requestStoragePermission(Activity activity) {
        List<String> permissions = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO);
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO);
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            // Android 12 及以下
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }

        ActivityCompat.requestPermissions(activity,
                permissions.toArray(new String[0]),
                REQUEST_CODE_STORAGE);
    }

    /**
     * 检查是否需要显示权限请求理由
     */
    public static boolean shouldShowStorageRationale(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.READ_MEDIA_VIDEO) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(activity,
                            Manifest.permission.READ_MEDIA_AUDIO) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(activity,
                            Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            return ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    /**
     * 检查是否有管理所有文件的权限 (Android 11+)
     */
    public static boolean hasManageStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return true;
    }

    /**
     * 请求管理所有文件的权限
     */
    public static void requestManageStoragePermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                intent.setData(uri);
                activity.startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE);
            } catch (Exception e) {
                // 如果上面的 intent 不可用，尝试通用设置
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                activity.startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE);
            }
        }
    }

    /**
     * 处理权限请求结果
     */
    public static boolean handlePermissionResult(int requestCode, String[] permissions,
                                                 int[] grantResults) {
        if (requestCode == REQUEST_CODE_STORAGE) {
            if (grantResults.length > 0) {
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * 获取权限名称（用于显示）
     */
    public static String getPermissionDisplayName(String permission) {
        switch (permission) {
            case Manifest.permission.READ_EXTERNAL_STORAGE:
                return "读取存储";
            case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                return "写入存储";
            case Manifest.permission.READ_MEDIA_VIDEO:
                return "读取视频";
            case Manifest.permission.READ_MEDIA_AUDIO:
                return "读取音频";
            case Manifest.permission.READ_MEDIA_IMAGES:
                return "读取图片";
            default:
                return permission;
        }
    }

    /**
     * 打开应用设置页面
     */
    public static void openAppSettings(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        activity.startActivity(intent);
    }
}
