package com.masterxu.video_transcoder;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.masterxu.video_transcoder.adapter.TaskListAdapter;
import com.masterxu.video_transcoder.dialog.AudioOptionsDialog;
import com.masterxu.video_transcoder.dialog.GlobalSettingsDialog;
import com.masterxu.video_transcoder.dialog.ImageOptionsDialog;
import com.masterxu.video_transcoder.dialog.VideoOptionsDialog;
import com.masterxu.video_transcoder.model.TaskItem;
import com.masterxu.video_transcoder.utils.PermissionHelper;
import com.masterxu.video_transcoder.viewmodel.TaskViewModel;

/**
 * 主界面
 * 管理任务列表和全局操作
 */
public class MainActivity extends AppCompatActivity {

    private TaskViewModel viewModel;
    private TaskListAdapter adapter;

    // UI 组件
    private RecyclerView recyclerView;
    private Button btnAddTask;
    private Button btnRemoveSelected;
    private Button btnClearAll;
    private Button btnStopAll;
    private Button btnStartAll;

    // 对话框
    private VideoOptionsDialog videoDialog;
    private AudioOptionsDialog audioDialog;
    private ImageOptionsDialog imageDialog;

    // 文件选择器
    private ActivityResultLauncher<Intent> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化 ViewModel
        viewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        // 初始化文件选择器
        initFilePickerLauncher();

        // 初始化对话框
        initDialogs();

        // 初始化视图
        initViews();

        // 检查权限
        checkPermissions();

        // 观察任务列表变化
        observeTasks();
    }

    private void initFilePickerLauncher() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            // 持久化权限
                            int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                            getContentResolver().takePersistableUriPermission(uri, takeFlags);

                            // 根据当前对话框类型处理结果
                            handleFilePickerResult(uri);
                        }
                    }
                }
        );
    }

    private void handleFilePickerResult(Uri uri) {
        // 检查是哪个对话框打开的
        if (videoDialog != null && videoDialog.isShowing()) {
            // 判断是文件选择还是目录选择
            if (uri.toString().contains("tree")) {
                videoDialog.handleOutputDirResult(uri);
            } else {
                videoDialog.handleInputFileResult(uri);
            }
        } else if (audioDialog != null && audioDialog.isShowing()) {
            if (uri.toString().contains("tree")) {
                audioDialog.handleOutputDirResult(uri);
            } else {
                audioDialog.handleInputFileResult(uri);
            }
        } else if (imageDialog != null && imageDialog.isShowing()) {
            if (uri.toString().contains("tree")) {
                imageDialog.handleOutputDirResult(uri);
            } else {
                imageDialog.handleInputFileResult(uri);
            }
        }
    }

    private void initDialogs() {
        videoDialog = new VideoOptionsDialog(this, filePickerLauncher);
        videoDialog.setOnVideoOptionsConfirmedListener((task, startImmediately) -> {
            viewModel.addTask(task);
            if (startImmediately) {
                viewModel.startTask(this, task);
            }
        });

        audioDialog = new AudioOptionsDialog(this, filePickerLauncher);
        audioDialog.setOnAudioOptionsConfirmedListener((task, startImmediately) -> {
            viewModel.addTask(task);
            if (startImmediately) {
                viewModel.startTask(this, task);
            }
        });

        imageDialog = new ImageOptionsDialog(this, filePickerLauncher);
        imageDialog.setOnImageOptionsConfirmedListener((task, startImmediately) -> {
            viewModel.addTask(task);
            if (startImmediately) {
                viewModel.startTask(this, task);
            }
        });
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_view);
        btnAddTask = findViewById(R.id.btn_add_task);
        btnRemoveSelected = findViewById(R.id.btn_remove_selected);
        btnClearAll = findViewById(R.id.btn_clear_all);
        btnStopAll = findViewById(R.id.btn_stop_all);
        btnStartAll = findViewById(R.id.btn_start_all);

        // 设置 RecyclerView
        adapter = new TaskListAdapter();
        adapter.setOnTaskClickListener(new TaskListAdapter.OnTaskClickListener() {
            @Override
            public void onTaskClick(TaskItem task) {
                // 切换选中状态
                task.setSelected(!task.isSelected());
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onTaskLongClick(TaskItem task) {
                // 显示任务详情
                showTaskDetails(task);
            }

            @Override
            public void onOutputPathClick(TaskItem task) {
                // 打开输出文件
                openOutputFile(task);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // 添加任务按钮
        btnAddTask.setOnClickListener(this::showAddTaskMenu);

        // 移除选中
        btnRemoveSelected.setOnClickListener(v -> {
            viewModel.removeSelectedTasks();
        });

        // 清空列表
        btnClearAll.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("确认清空")
                    .setMessage("确定要清空所有任务吗？")
                    .setPositiveButton("确定", (dialog, which) -> viewModel.clearAllTasks())
                    .setNegativeButton("取消", null)
                    .show();
        });

        // 停止所有
        btnStopAll.setOnClickListener(v -> {
            viewModel.stopAllRunningTasks();
        });

        // 开始所有
        btnStartAll.setOnClickListener(v -> {
            viewModel.startAllPendingTasks(this);
        });
    }

    private void showAddTaskMenu(View anchor) {
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        popupMenu.getMenuInflater().inflate(R.menu.menu_add_task, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_add_video) {
                videoDialog.show();
                return true;
            } else if (itemId == R.id.action_add_audio) {
                audioDialog.show();
                return true;
            } else if (itemId == R.id.action_add_image) {
                imageDialog.show();
                return true;
            }
            return false;
        });

        popupMenu.show();
    }

    private void observeTasks() {
        viewModel.getTaskList().observe(this, tasks -> {
            adapter.setTasks(tasks);

            // 更新按钮状态
            boolean hasTasks = tasks != null && !tasks.isEmpty();
            btnRemoveSelected.setEnabled(hasTasks);
            btnClearAll.setEnabled(hasTasks);
            btnStartAll.setEnabled(viewModel.getPendingTaskCount() > 0);
            btnStopAll.setEnabled(viewModel.getRunningTaskCount() > 0);
        });
    }

    private void checkPermissions() {
        if (!PermissionHelper.hasStoragePermission(this)) {
            PermissionHelper.requestStoragePermission(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PermissionHelper.REQUEST_CODE_STORAGE) {
            if (PermissionHelper.handlePermissionResult(requestCode, permissions, grantResults)) {
                Toast.makeText(this, "权限已授予", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "需要存储权限才能使用此应用", Toast.LENGTH_LONG).show();
                if (!PermissionHelper.shouldShowStorageRationale(this)) {
                    // 用户选择了"不再询问"，引导去设置
                    new AlertDialog.Builder(this)
                            .setTitle("需要权限")
                            .setMessage("请在设置中授予存储权限")
                            .setPositiveButton("去设置", (dialog, which) -> {
                                PermissionHelper.openAppSettings(this);
                            })
                            .setNegativeButton("取消", null)
                            .show();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            showGlobalSettings();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showGlobalSettings() {
        GlobalSettingsDialog dialog = new GlobalSettingsDialog(this, viewModel);
        dialog.setOnSettingsChangedListener(threadCount -> {
            Toast.makeText(this, "线程数已设置为: " + threadCount, Toast.LENGTH_SHORT).show();
        });
        dialog.show();
    }

    private void showTaskDetails(TaskItem task) {
        StringBuilder details = new StringBuilder();
        details.append("文件名: ").append(task.getDisplayName()).append("\n");
        details.append("类型: ").append(task.getTaskType().getDisplayName()).append("\n");
        details.append("状态: ").append(task.getStatus().getDisplayName()).append("\n");
        details.append("进度: ").append(task.getProgress()).append("%\n");
        details.append("输入大小: ").append(TaskItem.formatFileSize(task.getInputSize())).append("\n");
        if (task.getOutputSize() > 0) {
            details.append("输出大小: ").append(TaskItem.formatFileSize(task.getOutputSize())).append("\n");
        }
        if (task.getCommand() != null) {
            details.append("命令: ").append(task.getCommand()).append("\n");
        }
        if (task.getErrorMessage() != null) {
            details.append("错误: ").append(task.getErrorMessage()).append("\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("任务详情")
                .setMessage(details.toString())
                .setPositiveButton("确定", null)
                .show();
    }

    private void openOutputFile(TaskItem task) {
        if (task.getOutputUri() == null || task.getOutputUri().isEmpty()) {
            Toast.makeText(this, "输出文件不存在", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri uri = Uri.parse(task.getOutputUri());
            Intent intent = new Intent(Intent.ACTION_VIEW);

            String mimeType = getContentResolver().getType(uri);
            if (mimeType == null) {
                mimeType = "*/*";
            }

            intent.setDataAndType(uri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "没有应用可以打开此文件", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "无法打开文件: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 停止所有运行中的任务
        viewModel.stopAllRunningTasks();
    }
}
