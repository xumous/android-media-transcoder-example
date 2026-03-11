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
 * 音频转码选项对话框
 */
public class AudioOptionsDialog extends Dialog {

    private Context context;
    private OnAudioOptionsConfirmedListener listener;
    private ActivityResultLauncher<Intent> filePickerLauncher;

    // UI 组件
    private TextView tvInputFile;
    private Button btnOpenFile;
    private Spinner spinnerFormat;
    private Spinner spinnerEncoder;
    private Spinner spinnerSampleRate;
    private EditText etBitrate;
    private EditText etChannels;
    private EditText etVolume;
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

    public AudioOptionsDialog(@NonNull Context context,
                              ActivityResultLauncher<Intent> filePickerLauncher) {
        super(context);
        this.context = context;
        this.filePickerLauncher = filePickerLauncher;
    }

    public void setOnAudioOptionsConfirmedListener(OnAudioOptionsConfirmedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_audio_options);
        setTitle("音频转码");

        initViews();
        setupSpinners();
        setupListeners();
        setDefaultValues();
    }

    private void initViews() {
        tvInputFile = findViewById(R.id.tv_input_file);
        btnOpenFile = findViewById(R.id.btn_open_file);
        spinnerFormat = findViewById(R.id.spinner_format);
        spinnerEncoder = findViewById(R.id.spinner_encoder);
        spinnerSampleRate = findViewById(R.id.spinner_sample_rate);
        etBitrate = findViewById(R.id.et_bitrate);
        etChannels = findViewById(R.id.et_channels);
        etVolume = findViewById(R.id.et_volume);
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
                android.R.layout.simple_spinner_item, FfmpegCommandBuilder.AUDIO_FORMAT_NAMES);
        formatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFormat.setAdapter(formatAdapter);

        // 编码器
        ArrayAdapter<String> encoderAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, FfmpegCommandBuilder.AUDIO_ENCODER_NAMES);
        encoderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEncoder.setAdapter(encoderAdapter);

        // 采样率
        String[] sampleRates = {"44100 Hz", "48000 Hz", "96000 Hz", "192000 Hz"};
        ArrayAdapter<String> sampleRateAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, sampleRates);
        sampleRateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSampleRate.setAdapter(sampleRateAdapter);
    }

    private void setupListeners() {
        btnOpenFile.setOnClickListener(v -> openInputFile());

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
        spinnerFormat.setSelection(0); // MP3
        spinnerEncoder.setSelection(1); // MP3 (libmp3lame)
        spinnerSampleRate.setSelection(0); // 44100
        etBitrate.setText("320k");
        etChannels.setText("2");
        etVolume.setText("100");
        rgOutputLocation.check(R.id.rb_source_dir);
    }

    private void openInputFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        String[] mimeTypes = {"audio/mpeg", "audio/aac", "audio/flac", "audio/wav",
                "audio/ogg", "audio/mp4", "audio/x-ms-wma"};
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

        FfmpegCommandBuilder.AudioOptions options = new FfmpegCommandBuilder.AudioOptions();
        options.format = FfmpegCommandBuilder.AUDIO_FORMATS[spinnerFormat.getSelectedItemPosition()];
        options.encoder = FfmpegCommandBuilder.AUDIO_ENCODERS[spinnerEncoder.getSelectedItemPosition()];
        options.sampleRate = FfmpegCommandBuilder.SAMPLE_RATES[spinnerSampleRate.getSelectedItemPosition()];
        options.bitrate = etBitrate.getText().toString().trim();
        options.channels = etChannels.getText().toString().trim();
        options.volume = etVolume.getText().toString().trim();

        TaskItem task = new TaskItem(TaskItem.TaskType.AUDIO);
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
        String command = FfmpegCommandBuilder.buildAudioCommand(options);
        task.setCommand(command);

        if (listener != null) {
            listener.onConfirmed(task, startImmediately);
        }

        dismiss();
    }

    public interface OnAudioOptionsConfirmedListener {
        void onConfirmed(TaskItem task, boolean startImmediately);
    }
}
