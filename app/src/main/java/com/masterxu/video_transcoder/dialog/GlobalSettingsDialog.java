package com.masterxu.video_transcoder.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.masterxu.video_transcoder.R;
import com.masterxu.video_transcoder.viewmodel.TaskViewModel;

/**
 * 全局设置对话框
 */
public class GlobalSettingsDialog extends Dialog {

    private TaskViewModel viewModel;
    private OnSettingsChangedListener listener;

    private SeekBar seekBarThreadCount;
    private TextView tvThreadCount;
    private Button btnReset;
    private Button btnDefault;
    private Button btnCancel;
    private Button btnApply;
    private Button btnOk;

    private int currentThreadCount;
    private int tempThreadCount;

    public GlobalSettingsDialog(@NonNull Context context, TaskViewModel viewModel) {
        super(context);
        this.viewModel = viewModel;
        this.currentThreadCount = viewModel.getThreadCount();
        this.tempThreadCount = currentThreadCount;
    }

    public void setOnSettingsChangedListener(OnSettingsChangedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_global_settings);

        setTitle("全局设置");

        initViews();
        setupListeners();
        updateUI();
    }

    private void initViews() {
        seekBarThreadCount = findViewById(R.id.seekbar_thread_count);
        tvThreadCount = findViewById(R.id.tv_thread_count);
        btnReset = findViewById(R.id.btn_reset);
        btnDefault = findViewById(R.id.btn_default);
        btnCancel = findViewById(R.id.btn_cancel);
        btnApply = findViewById(R.id.btn_apply);
        btnOk = findViewById(R.id.btn_ok);

        // 设置 SeekBar 范围
        seekBarThreadCount.setMax(viewModel.getMaxThreadCount() - viewModel.getMinThreadCount());
        seekBarThreadCount.setProgress(currentThreadCount - viewModel.getMinThreadCount());
    }

    private void setupListeners() {
        seekBarThreadCount.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tempThreadCount = progress + viewModel.getMinThreadCount();
                updateThreadCountDisplay();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        btnReset.setOnClickListener(v -> {
            tempThreadCount = currentThreadCount;
            seekBarThreadCount.setProgress(tempThreadCount - viewModel.getMinThreadCount());
            updateThreadCountDisplay();
        });

        btnDefault.setOnClickListener(v -> {
            tempThreadCount = viewModel.getDefaultThreadCount();
            seekBarThreadCount.setProgress(tempThreadCount - viewModel.getMinThreadCount());
            updateThreadCountDisplay();
        });

        btnCancel.setOnClickListener(v -> dismiss());

        btnApply.setOnClickListener(v -> {
            applySettings();
        });

        btnOk.setOnClickListener(v -> {
            applySettings();
            dismiss();
        });
    }

    private void updateUI() {
        updateThreadCountDisplay();
    }

    private void updateThreadCountDisplay() {
        tvThreadCount.setText(String.valueOf(tempThreadCount));
    }

    private void applySettings() {
        if (tempThreadCount != currentThreadCount) {
            currentThreadCount = tempThreadCount;
            viewModel.setThreadCount(currentThreadCount);
            if (listener != null) {
                listener.onSettingsChanged(currentThreadCount);
            }
        }
    }

    public interface OnSettingsChangedListener {
        void onSettingsChanged(int threadCount);
    }
}
