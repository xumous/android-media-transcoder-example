package com.masterxu.video_transcoder.worker;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.Statistics;
import com.arthenica.ffmpegkit.StatisticsCallback;
import com.masterxu.video_transcoder.model.TaskItem;
import com.masterxu.video_transcoder.model.TaskStatus;
import com.masterxu.video_transcoder.utils.FileUtils;
import com.masterxu.video_transcoder.viewmodel.TaskViewModel;

/**
 * FFmpeg 任务执行器
 * 实际执行转码任务
 */
public class FfmpegTaskRunner implements Runnable {

    private static final String TAG = "FfmpegTaskRunner";

    private final Context context;
    private final TaskItem task;
    private final TaskViewModel viewModel;

    private ParcelFileDescriptor inputPfd;
    private ParcelFileDescriptor outputPfd;

    public FfmpegTaskRunner(Context context, TaskItem task, TaskViewModel viewModel) {
        this.context = context.getApplicationContext();
        this.task = task;
        this.viewModel = viewModel;
    }

    @Override
    public void run() {
        try {
            // 更新任务状态为运行中
            task.setStatus(TaskStatus.RUNNING);
            task.setStartTime(System.currentTimeMillis());
            task.setProgress(0);
            viewModel.updateTask(task);

            // 打开文件描述符
            if (!openFileDescriptors()) {
                handleError("无法打开文件描述符");
                return;
            }

            // 获取 fd 数字
            int inputFd = FileUtils.getFdNumber(inputPfd);
            int outputFd = FileUtils.getFdNumber(outputPfd);

            // 构建命令（替换 fd 占位符）
            String command = task.getCommand();
            command = command.replace("fd:input", "/proc/self/fd/" + inputFd);
            command = command.replace("fd:output", "/proc/self/fd/" + outputFd);

            Log.d(TAG, "执行命令: " + command);

            // 执行 FFmpeg 命令
            FFmpegSession session = FFmpegKit.executeAsync(command,
                    completeSession -> {
                        // 完成回调
                        handleCompletion(completeSession);
                    },
                    log -> {
                        // 日志回调（可选）
                        Log.v(TAG, log.getMessage());
                    },
                    statistics -> {
                        // 统计回调（进度更新）
                        handleStatistics(statistics);
                    }
            );

            // 保存会话引用以便取消
            task.setSession(session);

        } catch (Exception e) {
            Log.e(TAG, "任务执行异常", e);
            handleError("执行异常: " + e.getMessage());
        }
    }

    /**
     * 打开输入输出文件描述符
     */
    private boolean openFileDescriptors() {
        try {
            // 打开输入文件
            Uri inputUri = Uri.parse(task.getInputUri());
            inputPfd = FileUtils.openInputFile(context, inputUri);
            if (inputPfd == null) {
                return false;
            }

            // 打开输出文件
            Uri outputUri = Uri.parse(task.getOutputUri());
            outputPfd = FileUtils.openOutputFile(context, outputUri);
            if (outputPfd == null) {
                FileUtils.closeQuietly(inputPfd);
                return false;
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "打开文件描述符失败", e);
            closeFileDescriptors();
            return false;
        }
    }

    /**
     * 关闭文件描述符
     */
    private void closeFileDescriptors() {
        FileUtils.closeQuietly(inputPfd);
        FileUtils.closeQuietly(outputPfd);
    }

    /**
     * 处理 FFmpeg 统计信息（进度更新）
     */
    private void handleStatistics(Statistics statistics) {
        if (statistics == null || task.getInputSize() <= 0) {
            return;
        }

        // 获取已处理的大小
        long processedSize = statistics.getSize();

        // 计算进度
        int progress = (int) ((processedSize * 100) / task.getInputSize());
        progress = Math.max(0, Math.min(100, progress));

        // 更新任务进度
        task.setProgress(progress);
        viewModel.updateTask(task);
    }

    /**
     * 处理任务完成
     */
    private void handleCompletion(FFmpegSession session) {
        try {
            ReturnCode returnCode = session.getReturnCode();

            if (ReturnCode.isSuccess(returnCode)) {
                // 成功
                task.setStatus(TaskStatus.COMPLETED);
                task.setProgress(100);

                // 获取输出文件大小
                try {
                    Uri outputUri = Uri.parse(task.getOutputUri());
                    long outputSize = FileUtils.getFileSize(context, outputUri);
                    task.setOutputSize(outputSize);
                } catch (Exception e) {
                    Log.w(TAG, "无法获取输出文件大小", e);
                }

            } else if (ReturnCode.isCancel(returnCode)) {
                // 被取消
                task.setStatus(TaskStatus.CANCELLED);
            } else {
                // 失败
                task.setStatus(TaskStatus.FAILED);
                String error = session.getOutput();
                if (error == null || error.isEmpty()) {
                    error = "返回码: " + returnCode;
                }
                task.setErrorMessage(error);
            }

        } catch (Exception e) {
            Log.e(TAG, "处理完成时出错", e);
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage("处理完成时出错: " + e.getMessage());
        } finally {
            task.setEndTime(System.currentTimeMillis());
            task.setSession(null);
            closeFileDescriptors();
            viewModel.updateTask(task);
        }
    }

    /**
     * 处理错误
     */
    private void handleError(String errorMessage) {
        task.setStatus(TaskStatus.FAILED);
        task.setErrorMessage(errorMessage);
        task.setEndTime(System.currentTimeMillis());
        closeFileDescriptors();
        viewModel.updateTask(task);
    }
}
