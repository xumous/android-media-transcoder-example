package com.masterxu.video_transcoder.viewmodel;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.masterxu.video_transcoder.model.TaskItem;
import com.masterxu.video_transcoder.model.TaskStatus;
import com.masterxu.video_transcoder.worker.FfmpegTaskRunner;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 任务视图模型
 * 管理任务列表、线程池和任务执行
 */
public class TaskViewModel extends AndroidViewModel {

    private static final String PREFS_NAME = "video_transcoder_settings";
    private static final String KEY_THREAD_COUNT = "thread_count";
    private static final int DEFAULT_THREAD_COUNT = 5;
    private static final int MAX_THREAD_COUNT = 10;
    private static final int MIN_THREAD_COUNT = 1;

    private final MutableLiveData<List<TaskItem>> taskList = new MutableLiveData<>(new ArrayList<>());
    private final SharedPreferences prefs;

    // 线程池和并发控制
    private ExecutorService executorService;
    private Semaphore semaphore;
    private int currentThreadCount;

    public TaskViewModel(@NonNull Application application) {
        super(application);
        prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        currentThreadCount = getThreadCount();
        initThreadPool();
    }

    /**
     * 初始化线程池
     */
    private void initThreadPool() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }

        semaphore = new Semaphore(currentThreadCount);
        executorService = new ThreadPoolExecutor(
                currentThreadCount,
                currentThreadCount,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>()
        );
    }

    /**
     * 获取任务列表 LiveData
     */
    public LiveData<List<TaskItem>> getTaskList() {
        return taskList;
    }

    /**
     * 获取当前任务列表
     */
    public List<TaskItem> getCurrentTaskList() {
        return taskList.getValue();
    }

    /**
     * 添加任务
     */
    public void addTask(TaskItem task) {
        List<TaskItem> currentList = taskList.getValue();
        if (currentList != null) {
            currentList.add(task);
            taskList.postValue(new ArrayList<>(currentList));
        }
    }

    /**
     * 移除选中的任务
     */
    public void removeSelectedTasks() {
        List<TaskItem> currentList = taskList.getValue();
        if (currentList != null) {
            List<TaskItem> newList = new ArrayList<>();
            for (TaskItem task : currentList) {
                if (!task.isSelected()) {
                    newList.add(task);
                } else if (task.getStatus() == TaskStatus.RUNNING && task.getSession() != null) {
                    // 如果任务正在运行，先取消
                    task.getSession().cancel();
                }
            }
            taskList.postValue(newList);
        }
    }

    /**
     * 清空所有任务
     */
    public void clearAllTasks() {
        List<TaskItem> currentList = taskList.getValue();
        if (currentList != null) {
            // 取消所有运行中的任务
            for (TaskItem task : currentList) {
                if (task.getStatus() == TaskStatus.RUNNING && task.getSession() != null) {
                    task.getSession().cancel();
                }
            }
            taskList.postValue(new ArrayList<>());
        }
    }

    /**
     * 开始所有等待中的任务
     */
    public void startAllPendingTasks(Context context) {
        List<TaskItem> currentList = taskList.getValue();
        if (currentList != null) {
            for (TaskItem task : currentList) {
                if (task.getStatus() == TaskStatus.PENDING) {
                    startTask(context, task);
                }
            }
        }
    }

    /**
     * 开始单个任务
     */
    public void startTask(Context context, TaskItem task) {
        if (task.getStatus() != TaskStatus.PENDING) {
            return;
        }

        executorService.execute(() -> {
            try {
                semaphore.acquire();
                FfmpegTaskRunner runner = new FfmpegTaskRunner(context, task, this);
                runner.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                task.setStatus(TaskStatus.FAILED);
                task.setErrorMessage("任务被中断");
                updateTask(task);
            } finally {
                semaphore.release();
            }
        });
    }

    /**
     * 停止所有运行中的任务
     */
    public void stopAllRunningTasks() {
        List<TaskItem> currentList = taskList.getValue();
        if (currentList != null) {
            for (TaskItem task : currentList) {
                if (task.getStatus() == TaskStatus.RUNNING && task.getSession() != null) {
                    task.getSession().cancel();
                    task.setStatus(TaskStatus.CANCELLED);
                    task.setEndTime(System.currentTimeMillis());
                }
            }
            taskList.postValue(new ArrayList<>(currentList));
        }
    }

    /**
     * 更新任务状态
     */
    public void updateTask(TaskItem task) {
        List<TaskItem> currentList = taskList.getValue();
        if (currentList != null) {
            int index = currentList.indexOf(task);
            if (index >= 0) {
                currentList.set(index, task);
                taskList.postValue(new ArrayList<>(currentList));
            }
        }
    }

    /**
     * 获取线程数
     */
    public int getThreadCount() {
        return prefs.getInt(KEY_THREAD_COUNT, DEFAULT_THREAD_COUNT);
    }

    /**
     * 设置线程数
     */
    public void setThreadCount(int count) {
        int newCount = Math.max(MIN_THREAD_COUNT, Math.min(MAX_THREAD_COUNT, count));
        prefs.edit().putInt(KEY_THREAD_COUNT, newCount).apply();

        if (newCount != currentThreadCount) {
            currentThreadCount = newCount;
            initThreadPool();
        }
    }

    /**
     * 重置为默认线程数
     */
    public void resetThreadCount() {
        setThreadCount(DEFAULT_THREAD_COUNT);
    }

    /**
     * 获取默认线程数
     */
    public int getDefaultThreadCount() {
        return DEFAULT_THREAD_COUNT;
    }

    /**
     * 获取最大线程数
     */
    public int getMaxThreadCount() {
        return MAX_THREAD_COUNT;
    }

    /**
     * 获取最小线程数
     */
    public int getMinThreadCount() {
        return MIN_THREAD_COUNT;
    }

    /**
     * 获取运行中的任务数量
     */
    public int getRunningTaskCount() {
        List<TaskItem> currentList = taskList.getValue();
        int count = 0;
        if (currentList != null) {
            for (TaskItem task : currentList) {
                if (task.getStatus() == TaskStatus.RUNNING) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * 获取等待中的任务数量
     */
    public int getPendingTaskCount() {
        List<TaskItem> currentList = taskList.getValue();
        int count = 0;
        if (currentList != null) {
            for (TaskItem task : currentList) {
                if (task.getStatus() == TaskStatus.PENDING) {
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
