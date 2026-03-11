package com.masterxu.video_transcoder.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
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
 * 视频转码选项对话框
 */
public class VideoOptionsDialog extends Dialog {

    public static final int REQUEST_CODE_INPUT_FILE = 2001;
    public static final int REQUEST_CODE_OUTPUT_DIR = 2002;

    private Context context;
    private OnVideoOptionsConfirmedListener listener;
    private ActivityResultLauncher<Intent> filePickerLauncher;

    // UI 组件
    private TextView tvInputFile;
    private Button btnOpenFile;
    private Spinner spinnerFormat;
    private Spinner spinnerVideoEncoder;
    private EditText etVideoSize;
    private EditText etVideoBitrate;
    private CheckBox cbUseCrf;
    private EditText etCrfValue;
    private LinearLayout layoutBitrate;
    private EditText etFrameRate;
    private EditText etAspectRatio;
    private CheckBox cbTwoPass;
    private EditText etKeyframeInterval;
    private Spinner spinnerRotation;
    private CheckBox cbHFlip;
    private CheckBox cbVFlip;
    private Spinner spinnerAudioEncoder;
    private Spinner spinnerSampleRate;
    private EditText etAudioBitrate;
    private EditText etChannels;
    private EditText etVolume;
    private CheckBox cbMapAllStreams;
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

    public VideoOptionsDialog(@NonNull Context context,
                              ActivityResultLauncher<Intent> filePickerLauncher) {
        super(context);
        this.context = context;
        this.filePickerLauncher = filePickerLauncher;
    }

    public void setOnVideoOptionsConfirmedListener(OnVideoOptionsConfirmedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_video_options); // 确保使用正确的布局
        // 设置对话框宽度填满屏幕
        if (getWindow() != null) {
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        setTitle("视频转码");

        initViews();
        setupSpinners();
        setupListeners();
        setDefaultValues();
    }

    private void initViews() {
        tvInputFile = findViewById(R.id.tv_input_file);
        btnOpenFile = findViewById(R.id.btn_open_file);
        spinnerFormat = findViewById(R.id.spinner_format);
        spinnerVideoEncoder = findViewById(R.id.spinner_video_encoder);
        etVideoSize = findViewById(R.id.et_video_size);
        etVideoBitrate = findViewById(R.id.et_video_bitrate);
        cbUseCrf = findViewById(R.id.cb_use_crf);
        etCrfValue = findViewById(R.id.et_crf_value);
        layoutBitrate = findViewById(R.id.layout_bitrate);
        etFrameRate = findViewById(R.id.et_frame_rate);
        etAspectRatio = findViewById(R.id.et_aspect_ratio);
        cbTwoPass = findViewById(R.id.cb_two_pass);
        etKeyframeInterval = findViewById(R.id.et_keyframe_interval);
        spinnerRotation = findViewById(R.id.spinner_rotation);
        cbHFlip = findViewById(R.id.cb_h_flip);
        cbVFlip = findViewById(R.id.cb_v_flip);
        spinnerAudioEncoder = findViewById(R.id.spinner_audio_encoder);
        spinnerSampleRate = findViewById(R.id.spinner_sample_rate);
        etAudioBitrate = findViewById(R.id.et_audio_bitrate);
        etChannels = findViewById(R.id.et_channels);
        etVolume = findViewById(R.id.et_volume);
        cbMapAllStreams = findViewById(R.id.cb_map_all_streams);
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
                android.R.layout.simple_spinner_item, FfmpegCommandBuilder.VIDEO_FORMAT_NAMES);
        formatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFormat.setAdapter(formatAdapter);

        // 视频编码器
        ArrayAdapter<String> videoEncoderAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, FfmpegCommandBuilder.VIDEO_ENCODER_NAMES);
        videoEncoderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVideoEncoder.setAdapter(videoEncoderAdapter);

        // 旋转
        String[] rotations = {"不旋转", "90°", "180°", "270°"};
        ArrayAdapter<String> rotationAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, rotations);
        rotationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRotation.setAdapter(rotationAdapter);

        // 音频编码器
        ArrayAdapter<String> audioEncoderAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, FfmpegCommandBuilder.AUDIO_ENCODER_NAMES);
        audioEncoderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAudioEncoder.setAdapter(audioEncoderAdapter);

        // 采样率
        String[] sampleRates = {"44100 Hz", "48000 Hz", "96000 Hz", "192000 Hz"};
        ArrayAdapter<String> sampleRateAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, sampleRates);
        sampleRateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSampleRate.setAdapter(sampleRateAdapter);
    }

    private void setupListeners() {
        btnOpenFile.setOnClickListener(v -> openInputFile());

        cbUseCrf.setOnCheckedChangeListener((buttonView, isChecked) -> {
            layoutBitrate.setEnabled(!isChecked);
            etVideoBitrate.setEnabled(!isChecked);
            etCrfValue.setEnabled(isChecked);
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
        spinnerFormat.setSelection(0); // MP4
        spinnerVideoEncoder.setSelection(1); // HEVC/H.265
        etVideoSize.setText("");
        etVideoBitrate.setText("16000k");
        cbUseCrf.setChecked(false);
        etCrfValue.setText("23");
        etCrfValue.setEnabled(false);
        layoutBitrate.setEnabled(true);
        etVideoBitrate.setEnabled(true);
        etFrameRate.setText("");
        etAspectRatio.setText("");
        cbTwoPass.setChecked(false);
        etKeyframeInterval.setText("");
        spinnerRotation.setSelection(0);
        cbHFlip.setChecked(false);
        cbVFlip.setChecked(false);
        spinnerAudioEncoder.setSelection(0); // AAC
        spinnerSampleRate.setSelection(1); // 48000
        etAudioBitrate.setText("320k");
        etChannels.setText("2");
        etVolume.setText("100");
        cbMapAllStreams.setChecked(true);
        rgOutputLocation.check(R.id.rb_source_dir);
    }

    private void openInputFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("video/*");
        String[] mimeTypes = {"video/mp4", "video/x-matroska", "video/quicktime",
                "video/x-msvideo", "video/webm", "video/x-flv", "video/mp2t"};
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

        // 构建选项
        FfmpegCommandBuilder.VideoOptions options = new FfmpegCommandBuilder.VideoOptions();
        options.format = FfmpegCommandBuilder.VIDEO_FORMATS[spinnerFormat.getSelectedItemPosition()];
        options.videoEncoder = FfmpegCommandBuilder.VIDEO_ENCODERS[spinnerVideoEncoder.getSelectedItemPosition()];
        options.videoSize = etVideoSize.getText().toString().trim();
        options.videoBitrate = etVideoBitrate.getText().toString().trim();
        options.useCrf = cbUseCrf.isChecked();
        options.crfValue = etCrfValue.getText().toString().trim();
        options.frameRate = etFrameRate.getText().toString().trim();
        options.aspectRatio = etAspectRatio.getText().toString().trim();
        options.twoPass = cbTwoPass.isChecked();
        options.keyframeInterval = etKeyframeInterval.getText().toString().trim();
        options.rotation = spinnerRotation.getSelectedItemPosition() * 90;
        options.hFlip = cbHFlip.isChecked();
        options.vFlip = cbVFlip.isChecked();
        options.audioEncoder = FfmpegCommandBuilder.AUDIO_ENCODERS[spinnerAudioEncoder.getSelectedItemPosition()];
        options.sampleRate = FfmpegCommandBuilder.SAMPLE_RATES[spinnerSampleRate.getSelectedItemPosition()];
        options.audioBitrate = etAudioBitrate.getText().toString().trim();
        options.channels = etChannels.getText().toString().trim();
        options.volume = etVolume.getText().toString().trim();
        options.mapAllStreams = cbMapAllStreams.isChecked();

        // 创建任务
        TaskItem task = new TaskItem(TaskItem.TaskType.VIDEO);
        task.setInputUri(inputUri.toString());
        task.setDisplayName(inputFileName);
        task.setInputSize(inputFileSize);

        // 确定输出 URI
        Uri outputUri = null;
        if (rbSelectDir.isChecked() && outputDirUri != null) {
            // 使用已选择的目录
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
            // 尝试输出到源文件目录
            String outputFileName = FileUtils.generateOutputFileName(inputFileName, options.format);
            DocumentFile sourceFile = DocumentFile.fromSingleUri(context, inputUri);
            if (sourceFile != null && sourceFile.getParentFile() != null) {
                DocumentFile outputFile = sourceFile.getParentFile().createFile(
                        FileUtils.getMimeType(outputFileName), outputFileName);
                if (outputFile != null) {
                    outputUri = outputFile.getUri();
                }
            }

            // 如果源目录创建失败，自动切换到选择目录模式
            if (outputUri == null) {
                Toast.makeText(context, "无法在源目录创建文件，请手动选择输出目录", Toast.LENGTH_LONG).show();
                rgOutputLocation.check(R.id.rb_select_dir); // 切换单选按钮
                btnSelectOutputDir.setEnabled(true);
                selectOutputDirectory(); // 弹出目录选择器
                // 注意：这里不能继续执行，需等待用户选择目录后再次点击确认
                return;
            }
        }

        task.setOutputUri(outputUri.toString());

        // 构建命令
        options.inputFd = "input";
        options.outputFd = "output";
        String command = FfmpegCommandBuilder.buildVideoCommand(options);
        task.setCommand(command);

        if (listener != null) {
            listener.onConfirmed(task, startImmediately);
        }

        dismiss();
    }

    public interface OnVideoOptionsConfirmedListener {
        void onConfirmed(TaskItem task, boolean startImmediately);
    }
}
