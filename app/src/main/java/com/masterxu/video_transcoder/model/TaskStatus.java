package com.masterxu.video_transcoder.model;

/**
 * 任务状态枚举
 */
public enum TaskStatus {
    PENDING("等待中", 0xFF9E9E9E),      // 灰色
    RUNNING("运行中", 0xFF2196F3),      // 蓝色
    COMPLETED("已完成", 0xFF4CAF50),    // 绿色
    FAILED("失败", 0xFFF44336),         // 红色
    CANCELLED("已取消", 0xFFFF9800);    // 橙色

    private final String displayName;
    private final int color;

    TaskStatus(String displayName, int color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getColor() {
        return color;
    }
}
