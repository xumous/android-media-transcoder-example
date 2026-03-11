package com.masterxu.video_transcoder.adapter;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.masterxu.video_transcoder.R;
import com.masterxu.video_transcoder.model.TaskItem;
import com.masterxu.video_transcoder.model.TaskStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * 任务列表适配器
 */
public class TaskListAdapter extends RecyclerView.Adapter<TaskListAdapter.TaskViewHolder> {

    private List<TaskItem> tasks = new ArrayList<>();
    private OnTaskClickListener listener;

    public void setOnTaskClickListener(OnTaskClickListener listener) {
        this.listener = listener;
    }

    public void setTasks(List<TaskItem> tasks) {
        this.tasks = tasks != null ? tasks : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        TaskItem task = tasks.get(position);
        holder.bind(task);
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    public interface OnTaskClickListener {
        void onTaskClick(TaskItem task);

        void onTaskLongClick(TaskItem task);

        void onOutputPathClick(TaskItem task);
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {

        private final CheckBox cbSelect;
        private final ImageView ivType;
        private final TextView tvFileName;
        private final TextView tvStatus;
        private final ProgressBar progressBar;
        private final TextView tvProgress;
        private final TextView tvInputSize;
        private final TextView tvOutputSize;
        private final TextView tvSizeChange;
        private final TextView tvOutputPath;
        private final TextView tvDuration;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            cbSelect = itemView.findViewById(R.id.cb_select);
            ivType = itemView.findViewById(R.id.iv_type);
            tvFileName = itemView.findViewById(R.id.tv_file_name);
            tvStatus = itemView.findViewById(R.id.tv_status);
            progressBar = itemView.findViewById(R.id.progress_bar);
            tvProgress = itemView.findViewById(R.id.tv_progress);
            tvInputSize = itemView.findViewById(R.id.tv_input_size);
            tvOutputSize = itemView.findViewById(R.id.tv_output_size);
            tvSizeChange = itemView.findViewById(R.id.tv_size_change);
            tvOutputPath = itemView.findViewById(R.id.tv_output_path);
            tvDuration = itemView.findViewById(R.id.tv_duration);
        }

        void bind(TaskItem task) {
            // 选择框
            cbSelect.setChecked(task.isSelected());
            cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                task.setSelected(isChecked);
            });

            // 类型图标
            int typeIcon;
            switch (task.getTaskType()) {
                case AUDIO:
                    typeIcon = R.drawable.ic_audio;
                    break;
                case IMAGE:
                    typeIcon = R.drawable.ic_image;
                    break;
                case VIDEO:
                default:
                    typeIcon = R.drawable.ic_video;
                    break;
            }
            ivType.setImageResource(typeIcon);

            // 文件名
            tvFileName.setText(task.getDisplayName());

            // 状态
            tvStatus.setText(task.getStatus().getDisplayName());
            tvStatus.setTextColor(task.getStatus().getColor());

            // 进度条
            if (task.getStatus() == TaskStatus.RUNNING) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(task.getProgress());
                tvProgress.setVisibility(View.VISIBLE);
                tvProgress.setText(task.getProgress() + "%");
            } else if (task.getStatus() == TaskStatus.COMPLETED) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(100);
                tvProgress.setVisibility(View.VISIBLE);
                tvProgress.setText("100%");
            } else {
                progressBar.setVisibility(View.GONE);
                tvProgress.setVisibility(View.GONE);
            }

            // 文件大小
            tvInputSize.setText("输入: " + TaskItem.formatFileSize(task.getInputSize()));
            if (task.getOutputSize() > 0) {
                tvOutputSize.setVisibility(View.VISIBLE);
                tvOutputSize.setText("输出: " + TaskItem.formatFileSize(task.getOutputSize()));
            } else {
                tvOutputSize.setVisibility(View.GONE);
            }

            // 大小变化
            if (task.getStatus() == TaskStatus.COMPLETED && task.getOutputSize() > 0) {
                tvSizeChange.setVisibility(View.VISIBLE);
                tvSizeChange.setText(task.getFormattedSizeChange());
                double change = task.getSizeChangePercent();
                if (change > 0) {
                    tvSizeChange.setTextColor(0xFFF44336); // 红色 - 增大
                } else if (change < 0) {
                    tvSizeChange.setTextColor(0xFF4CAF50); // 绿色 - 减小
                } else {
                    tvSizeChange.setTextColor(0xFF9E9E9E); // 灰色 - 不变
                }
            } else {
                tvSizeChange.setVisibility(View.GONE);
            }

            // 输出路径
            if (task.getOutputUri() != null && !task.getOutputUri().isEmpty()) {
                tvOutputPath.setVisibility(View.VISIBLE);
                tvOutputPath.setText("点击打开输出文件");
                tvOutputPath.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onOutputPathClick(task);
                    }
                });
            } else {
                tvOutputPath.setVisibility(View.GONE);
            }

            // 运行时长
            if (task.getStatus() == TaskStatus.RUNNING ||
                    task.getStatus() == TaskStatus.COMPLETED ||
                    task.getStatus() == TaskStatus.FAILED ||
                    task.getStatus() == TaskStatus.CANCELLED) {
                long duration = task.getDuration();
                if (duration > 0) {
                    tvDuration.setVisibility(View.VISIBLE);
                    tvDuration.setText(formatDuration(duration));
                } else {
                    tvDuration.setVisibility(View.GONE);
                }
            } else {
                tvDuration.setVisibility(View.GONE);
            }

            // 错误信息
            if (task.getStatus() == TaskStatus.FAILED && task.getErrorMessage() != null) {
                tvStatus.setText(task.getStatus().getDisplayName() + ": " + task.getErrorMessage());
            }

            // 点击事件
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTaskClick(task);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onTaskLongClick(task);
                    return true;
                }
                return false;
            });
        }

        private String formatDuration(long millis) {
            long seconds = millis / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;

            if (hours > 0) {
                return String.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60);
            } else {
                return String.format("%02d:%02d", minutes, seconds % 60);
            }
        }
    }
}
