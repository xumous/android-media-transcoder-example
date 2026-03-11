package com.masterxu.video_transcoder.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import com.masterxu.video_transcoder.R;
import com.masterxu.video_transcoder.model.TaskItem;
import com.masterxu.video_transcoder.utils.FfmpegCommandBuilder;
import com.masterxu.video_transcoder.utils.FileUtils;

/**
 * 图片转码选项对话框
 */
public class ImageOptionsDialog extends Dialog {

    private Context context;
    private OnImageOptionsConfirmedListener listener;
    private ActivityResultLauncher<Intent> filePickerLauncher;

    // UI 组件
    private TextView tvInputFile;
    private Button btnOpenFile;
    private Spinner spinnerFormat;
    private TextView tvQualityLabel;
    private SeekBar seekBarQuality;
    private TextView tvQualityValue;
    private RadioGroup rgOutputLocation;
    private RadioButton rbSourceDir;
    private RadioButton rbSelectDir;
    private TextView tvOutputDir;
    private Button btnSelectOutputDir;
    private Button btnCancel;
    private Button btnDefault;
    private Button btnOk;
    private Button btnOkAndStart;

    // 数据
    private Uri inputUri;
    private Uri outputDirUri;
    private String inputFileName;
    private long inputFileSize;

    public ImageOptionsDialog(@NonNull Context context,
                              ActivityResultLauncher<Intent> filePickerLauncher) {
        super(context);
        this.context = context;
        this.filePickerLauncher = filePickerLauncher;
    }

    public void setOnImageOptionsConfirmedListener(OnImageOptionsConfirmedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_image_options);
        setTitle("图片转码");

        initViews();
        setupSpinners();
        setupListeners();
        setDefaultValues();
    }

    private void initViews() {
        tvInputFile = findViewById(R.id.tv_input_file);
        btnOpenFile = findViewById(R.id.btn_open_file);
        spinnerFormat = findViewById(R.id.spinner_format);
        tvQualityLabel = findViewById(R.id.tv_quality_label);
        seekBarQuality = findViewById(R.id.seekbar_quality);
        tvQualityValue = findViewById(R.id.tv_quality_value);
        rgOutputLocation = findViewById(R.id.rg_output_location);
        rbSourceDir = findViewById(R.id.rb_source_dir);
        rbSelectDir = findViewById(R.id.rb_select_dir);
        tvOutputDir = findViewById(R.id.tv_output_dir);
        btnSelectOutputDir = findViewById(R.id.btn_select_output_dir);
        btnCancel = findViewById(R.id.btn_cancel);
        btnDefault = findViewById(R.id.btn_default);
        btnOk = findViewById(R.id.btn_ok);
        btnOkAndStart = findViewById(R.id.btn_ok_and_start);
    }

    private void setupSpinners() {
        // 格式
        ArrayAdapter<String> formatAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, FfmpegCommandBuilder.IMAGE_FORMAT_NAMES);
        formatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFormat.setAdapter(formatAdapter);

        // 格式选择改变时更新质量控件状态
        spinnerFormat.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                updateQualityControlState();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }

    private void setupListeners() {
        btnOpenFile.setOnClickListener(v -> openInputFile());

        seekBarQuality.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvQualityValue.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        rgOutputLocation.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_select_dir) {
                btnSelectOutputDir.setEnabled(true);
                if (outputDirUri == null) {
                    selectOutputDirectory();
                }
            } else {
                btnSelectOutputDir.setEnabled(false);
            }
        });

        btnSelectOutputDir.setOnClickListener(v -> selectOutputDirectory());

        btnCancel.setOnClickListener(v -> dismiss());

        btnDefault.setOnClickListener(v -> setDefaultValues());

        btnOk.setOnClickListener(v -> confirm(false));

        btnOkAndStart.setOnClickListener(v -> confirm(true));
    }

    private void setDefaultValues() {
        spinnerFormat.setSelection(0); // JPEG
        seekBarQuality.setProgress(90);
        tvQualityValue.setText("90");
        rgOutputLocation.check(R.id.rb_source_dir);
        updateQualityControlState();
    }

    private void updateQualityControlState() {
        int position = spinnerFormat.getSelectedItemPosition();
        String format = FfmpegCommandBuilder.IMAGE_FORMATS[position];
        boolean isJpeg = "jpg".equals(format) || "jpeg".equals(format);

        tvQualityLabel.setEnabled(isJpeg);
        seekBarQuality.setEnabled(isJpeg);
        tvQualityValue.setEnabled(isJpeg);
    }

    private void openInputFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        String[] mimeTypes = {"image/jpeg", "image/png", "image/gif", "image/bmp",
                "image/tiff", "image/webp"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        filePickerLauncher.launch(intent);
    }

    private void selectOutputDirectory() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        filePickerLauncher.launch(intent);
    }

    public void handleInputFileResult(Uri uri) {
        if (uri != null) {
            inputUri = uri;
            inputFileName = FileUtils.getFileName(context, uri);
            inputFileSize = FileUtils.getFileSize(context, uri);
            tvInputFile.setText(inputFileName);
        }
    }

    public void handleOutputDirResult(Uri uri) {
        if (uri != null) {
            outputDirUri = uri;
            String dirName = DocumentFile.fromTreeUri(context, uri).getName();
            tvOutputDir.setText(dirName);
            tvOutputDir.setVisibility(View.VISIBLE);
        }
    }

    private void confirm(boolean startImmediately) {
        if (inputUri == null) {
            Toast.makeText(context, "请先选择输入文件", Toast.LENGTH_SHORT).show();
            return;
        }

        FfmpegCommandBuilder.ImageOptions options = new FfmpegCommandBuilder.ImageOptions();
        options.format = FfmpegCommandBuilder.IMAGE_FORMATS[spinnerFormat.getSelectedItemPosition()];
        options.quality = seekBarQuality.getProgress();

        TaskItem task = new TaskItem(TaskItem.TaskType.IMAGE);
        task.setInputUri(inputUri.toString());
        task.setDisplayName(inputFileName);
        task.setInputSize(inputFileSize);

        Uri outputUri = null;
        if (rbSelectDir.isChecked() && outputDirUri != null) {
            String outputFileName = FileUtils.generateOutputFileName(inputFileName, options.format);
            DocumentFile outputFile = FileUtils.createFileInDirectory(context, outputDirUri,
                    outputFileName, FileUtils.getMimeType(outputFileName));
            if (outputFile != null) {
                outputUri = outputFile.getUri();
            } else {
                Toast.makeText(context, "无法在选定目录创建输出文件", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            String outputFileName = FileUtils.generateOutputFileName(inputFileName, options.format);
            DocumentFile sourceFile = DocumentFile.fromSingleUri(context, inputUri);
            if (sourceFile != null && sourceFile.getParentFile() != null) {
                DocumentFile outputFile = sourceFile.getParentFile().createFile(
                        FileUtils.getMimeType(outputFileName), outputFileName);
                if (outputFile != null) {
                    outputUri = outputFile.getUri();
                }
            }
            if (outputUri == null) {
                Toast.makeText(context, "无法在源目录创建文件，请手动选择输出目录", Toast.LENGTH_LONG).show();
                rgOutputLocation.check(R.id.rb_select_dir);
                btnSelectOutputDir.setEnabled(true);
                selectOutputDirectory();
                return;
            }
        }

        task.setOutputUri(outputUri.toString());

        options.inputFd = "input";
        options.outputFd = "output";
        String command = FfmpegCommandBuilder.buildImageCommand(options);
        task.setCommand(command);

        if (listener != null) {
            listener.onConfirmed(task, startImmediately);
        }

        dismiss();
    }

    public interface OnImageOptionsConfirmedListener {
        void onConfirmed(TaskItem task, boolean startImmediately);
    }
}
