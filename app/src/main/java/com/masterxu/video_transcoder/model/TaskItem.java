package com.masterxu.video_transcoder.model;

import android.net.Uri;

import com.arthenica.ffmpegkit.FFmpegSession;

import java.util.UUID;

/**
 * 任务实体类
 * 包含任务的所有信息和状态
 */
public class TaskItem {
    private final String id;                  // 唯一ID
    private String inputUri;                  // 输入文件URI字符串
    private String outputUri;                 // 输出文件URI字符串
    private String displayName;               // 显示文件名
    private TaskStatus status;                // 任务状态
    private int progress;                     // 进度 0-100
    private long inputSize;                   // 输入文件大小（字节）
    private long outputSize;                  // 输出文件大小（字节）
    private String command;                   // FFmpeg命令
    private long startTime;                   // 开始时间
    private long endTime;                     // 结束时间
    private String errorMessage;              // 错误信息
    private boolean selected;                 // 是否被选中
    private TaskType taskType;                // 任务类型

    // 运行时对象（不序列化）
    private transient FFmpegSession session;  // FFmpeg会话
    private transient Object inputFd;         // 输入文件描述符
    private transient Object outputFd;        // 输出文件描述符

    public TaskItem() {
        this.id = UUID.randomUUID().toString();
        this.status = TaskStatus.PENDING;
        this.progress = 0;
        this.selected = false;
    }

    public TaskItem(TaskType taskType) {
        this();
        this.taskType = taskType;
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

    // Getters and Setters

    public String getId() {
        return id;
    }

    public String getInputUri() {
        return inputUri;
    }

    public void setInputUri(String inputUri) {
        this.inputUri = inputUri;
    }

    public String getOutputUri() {
        return outputUri;
    }

    public void setOutputUri(String outputUri) {
        this.outputUri = outputUri;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = Math.max(0, Math.min(100, progress));
    }

    public long getInputSize() {
        return inputSize;
    }

    public void setInputSize(long inputSize) {
        this.inputSize = inputSize;
    }

    public long getOutputSize() {
        return outputSize;
    }

    public void setOutputSize(long outputSize) {
        this.outputSize = outputSize;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    public FFmpegSession getSession() {
        return session;
    }

    public void setSession(FFmpegSession session) {
        this.session = session;
    }

    public Object getInputFd() {
        return inputFd;
    }

    public void setInputFd(Object inputFd) {
        this.inputFd = inputFd;
    }

    public Object getOutputFd() {
        return outputFd;
    }

    public void setOutputFd(Object outputFd) {
        this.outputFd = outputFd;
    }

    /**
     * 获取文件大小变化百分比
     *
     * @return 百分比值，正值表示增大，负值表示减小
     */
    public double getSizeChangePercent() {
        if (inputSize <= 0 || outputSize <= 0) {
            return 0;
        }
        return ((double) (outputSize - inputSize) / inputSize) * 100;
    }

    /**
     * 获取格式化的大小变化字符串
     */
    public String getFormattedSizeChange() {
        double percent = getSizeChangePercent();
        if (percent > 0) {
            return String.format("↑ %.1f%%", percent);
        } else if (percent < 0) {
            return String.format("↓ %.1f%%", Math.abs(percent));
        } else {
            return "→ 0%";
        }
    }

    /**
     * 获取运行时长（毫秒）
     */
    public long getDuration() {
        if (startTime <= 0) {
            return 0;
        }
        long end = endTime > 0 ? endTime : System.currentTimeMillis();
        return end - startTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskItem taskItem = (TaskItem) o;
        return id.equals(taskItem.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * 任务类型枚举
     */
    public enum TaskType {
        VIDEO("视频"),
        AUDIO("音频"),
        IMAGE("图片");

        private final String displayName;

        TaskType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
