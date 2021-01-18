package com.example.necip.vlc_android_camera_streaming;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static android.content.Context.INPUT_METHOD_SERVICE;
import static android.content.Context.MODE_PRIVATE;
import static android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK;
import static android.hardware.Camera.open;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link VideoFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class VideoFragment extends Fragment implements TextureView.SurfaceTextureListener, CameraBridgeViewBase.CvCameraViewListener2 {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;


    public final static String TAG = "VideoActivity";

    private TextureView textureView;
    private ImageView imageView;
    ImageButton img_btn_cam_switch, img_btn_cam_res;
    FaceDetectorOptions highAccuracyOpts, realTimeOpts, realTimeMultiOpts;
    android.graphics.Rect bounds;

    // media player
    private LibVLC libvlc;
    private MediaPlayer mMediaPlayer = null;

    Boolean from_external, from_internal;

    private String rtspUrl = "rtsp";
    private int width = 600;
    private int height = 144;

    EditText edt1, edt2, txt_capacity;

    AtomicInteger cam_index_no;
    int[] checkedItem;
    List<ArrayAdapter<String>> lst_of_array_list;
    ArrayAdapter<String> arrayAdapter;
    ArrayList<Integer> arrayListForWidth, arrayListForHeight;

    boolean img_btn1, flag, motion_detection, face_detection_1, face_detection_2, no_scanning;
    Bitmap b, b1, b2;

    Mat avg;
    Mat imageMat, firstframe, grayMat, accWght, cnvrtScal, frameDelta, cannyEdges, thresh, dilate;


    List<MatOfPoint> contourList;
    Rect rect;
    ArrayList<Rect> arr;        //xvalues, yvalues, w, h
    List<Integer> xvalues = new ArrayList<>();
    List<Integer> motion = new ArrayList<>();
    //List<Integer> xvalues;
    //List<Integer> motion;

    int giris = 0;
    int cikis = 0;
    int kapasite = 3;
    TextView txt_current, txt_to_enter;

    Spinner resolution_spinner_cv;
    LinearLayout video_linearLayout, linear_layout_of_texture;
    ImageView imageView_walking_standing;

    private CascadeClassifier cascadeClassifier;
    MatOfRect haarcascade_first;
    Rect[] haarcascadeArray_first;


    final Handler handler = new Handler();
    final int updateFreqMs = 30; //30; 1000// call update every 1000 ms 1000/fps

    // android camera
    private static final int CAMERA_PERMISSION_CODE = 100;
    CameraBridgeViewBase cameraBridgeViewBase;
    BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(getActivity()) {
        @Override
        public void onManagerConnected(int status) {
            super.onManagerConnected(status);
            switch(status){
                case BaseLoaderCallback.SUCCESS:
                    try {
                        if (!OpenCVLoader.initDebug())
                            OpenCVLoader.initDebug();

                        InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_default);
                        File cascadeDir = getActivity().getDir("haarcascade_frontalface_default", MODE_PRIVATE);
                        File mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_default.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1)
                            os.write(buffer, 0, bytesRead);
                        is.close();
                        os.close();
                        // Load the cascade classifier
                        cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e("OpenCVActivity", "Error loading cascade", e);
                    }
                    cameraBridgeViewBase.enableView();
                    break;
                case BaseLoaderCallback.INCOMPATIBLE_MANAGER_VERSION:
                    super.onManagerConnected(status);
                    Log.e("OpenCVActivity", "INCOMPATIBLE_MANAGER_VERSION");
                    break;
                case BaseLoaderCallback.INIT_FAILED:
                    super.onManagerConnected(status);
                    Log.e("OpenCVActivity", "INIT_FAILED");
                    break;
                case BaseLoaderCallback.INSTALL_CANCELED:
                    super.onManagerConnected(status);
                    Log.e("OpenCVActivity", "INSTALL_CANCELED");
                    break;
                case BaseLoaderCallback.MARKET_ERROR:
                    super.onManagerConnected(status);
                    Log.e("OpenCVActivity", "MARKET_ERROR");
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };



    public VideoFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment VideoFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static VideoFragment newInstance(String param1, String param2) {
        VideoFragment fragment = new VideoFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Objects.requireNonNull(getActivity()).getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_video, container, false);

        video_linearLayout = view.findViewById(R.id.video_linearLayout);
        linear_layout_of_texture = view.findViewById(R.id.linear_layout_of_texture);
        imageView_walking_standing = view.findViewById(R.id.imageView_walking_standing);

        textureView = view.findViewById(R.id.textureView);

        imageView = view.findViewById(R.id.imageView);

        ///android camera CV
        //System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        cameraBridgeViewBase = (JavaCameraView) view.findViewById(R.id.myAndroidCameraView);

        img_btn_cam_switch = view.findViewById(R.id.img_btn_cam_switch);

        img_btn_cam_res = view.findViewById(R.id.img_btn_cam_res);

        resolution_spinner_cv = view.findViewById(R.id.resolution_spinner_cv);

        txt_capacity = view.findViewById(R.id.txt_capacity);
        txt_current = view.findViewById(R.id.txt_current);
        txt_to_enter = view.findViewById(R.id.txt_to_enter);

        return view;
    }

    @SuppressLint({"ResourceType", "UseCompatLoadingForDrawables"})
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        video_linearLayout.setOnClickListener(view12 -> {
            try {
                InputMethodManager inputMethodManager = (InputMethodManager) Objects.requireNonNull(VideoFragment.this.getActivity()).getSystemService(INPUT_METHOD_SERVICE);
                assert inputMethodManager != null;
                inputMethodManager.hideSoftInputFromWindow(VideoFragment.this.getActivity().getCurrentFocus().getWindowToken(), 0);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        //PERMISSION
        checkCameraPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_CODE);

        camera_allow();

        cam_index_no = new AtomicInteger();
        img_btn1 = true;

        img_btn_cam_switch.setOnClickListener(view1 -> {
            if (img_btn1) {
                cameraBridgeViewBase.disableView();
                cameraBridgeViewBase.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
                cameraBridgeViewBase.enableView();
                img_btn1 = false;

                releaseMat();
                startMat();

                //cam_index_no.set(0);
                //cam_index_no.set(CAMERA_FACING_FRONT); //CAMERA_FACING_FRONT
            } else {
                cameraBridgeViewBase.disableView();
                cameraBridgeViewBase.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
                cameraBridgeViewBase.enableView();
                img_btn1 = true;

                releaseMat();
                startMat();

                //cam_index_no.set(1);
                //cam_index_no.set(CAMERA_FACING_BACK); //CAMERA_FACING_BACK
            }
        });

        checkedItem = new int[]{1}; //arrayAdapter.getCount() - 1; son olan

        img_btn_cam_res.setOnClickListener(view1 -> {
            //dialog appear
            img_btn_cam_res.setImageDrawable(getResources().getDrawable(R.drawable.setting_purple_48));

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogCustom);
            //AlertDialog.Builder builder = new AlertDialog.Builder(VideoActivity.this);
            builder.setCancelable(false);
            builder.setTitle(R.string.img_btn_cam_res_title);
            float dH = 0.5f;

            if (from_external) {
                LinearLayout layout0 = new LinearLayout(getActivity());
                layout0.setOrientation(LinearLayout.VERTICAL);

                LinearLayout layout = new LinearLayout(getActivity());
                layout.setLayoutParams(new LinearLayout.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT));
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setGravity(Gravity.CENTER);

                Spinner spnnr = new Spinner(getActivity());
                spnnr.setLayoutParams(new LinearLayout.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT));
                spnnr.setId(78);
                layout.addView(spnnr);

                LinearLayout layout2 = new LinearLayout(getActivity());
                layout2.setLayoutParams(new LinearLayout.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT));
                layout2.setOrientation(LinearLayout.HORIZONTAL);
                layout2.setGravity(Gravity.CENTER);

                edt1 = new EditText(getActivity());
                edt1.setLayoutParams(new LinearLayout.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT));
                edt1.setInputType(InputType.TYPE_CLASS_NUMBER);
                layout2.addView(edt1);

                TextView tx1 = new TextView(getActivity());
                tx1.setLayoutParams(new LinearLayout.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT));
                tx1.setText("x");
                layout2.addView(tx1);

                edt2 = new EditText(getActivity());
                edt2.setLayoutParams(new LinearLayout.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT));
                edt2.setInputType(InputType.TYPE_CLASS_NUMBER);
                layout2.addView(edt2);

                layout0.addView(layout);
                layout0.addView(layout2);

                builder.setView(layout0);

                ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(), R.array.resolution_array_res, android.R.layout.simple_spinner_item);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spnnr.setAdapter(adapter);
                spnnr.setSelection(adapter.getCount() - 1);
                spnnr.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(AdapterView<?> parent, View viw, int arg2, long arg3) {
                        Spinner spinner = (Spinner) parent;
                        String item = (String) spinner.getSelectedItem();
                        switch (item) {
                            case "1920x1080 (16:9)":
                                width = 1920; height = 1080;
                                break;
                            case "1280x720 (16:9)":
                                width = 1280; height = 720;
                                break;
                            case "1024x576 (16:9)":
                                width = 1024; height = 576;
                                break;
                            case "960x540 (16:9)":
                                width = 960; height = 540;
                                break;
                            case "848x480 (16:9)":
                                width = 848; height = 480;
                                break;
                            case "720x405 (16:9)":
                                width = 720; height = 405;
                                break;
                            case "640x360 (16:9)":
                                width = 640; height = 360;
                                break;
                            case "480x270 (16:9)":
                                width = 480; height = 270;
                                break;
                            case "320x180 (16:9)":
                                width = 320; height = 180;
                                break;
                            case "240x135 (16:9)":
                                width = 240; height = 135;
                                break;
                            case "160x90 (16:9)":
                                width = 160; height = 90;
                                break;

                            case "1920x1440 (4:3)":
                                width = 1920; height = 1440;
                                break;
                            case "1440x1080 (4:3)":
                                width = 1440; height = 1080;
                                break;
                            case "1280x960 (4:3)":
                                width = 1280; height = 960;
                                break;
                            case "1024x768 (4:3)":
                                width = 1024; height = 768;
                                break;
                            case "960x720 (4:3)":
                                width = 960; height = 720;
                                break;
                            case "800x600 (4:3)":
                                width = 800; height = 600;
                                break;
                            case "720x540 (4:3)":
                                width = 720; height = 540;
                                break;
                            case "640x480 (4:3)":
                                width = 640; height = 480;
                                break;
                            case "480x360 (4:3)":
                                width = 480; height = 360;
                                break;
                            case "320x240 (4:3)":
                                width = 320; height = 240;
                                break;
                            case "160x120 (4:3)":
                                width = 160; height = 120;
                                break;
                        }
                        edt1.setText(String.valueOf(width));
                        edt2.setText(String.valueOf(height));
                    }

                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });


                builder.setPositiveButton(R.string.img_btn_cam_res_pos_btn, (dialogInterface, i) -> img_btn_cam_res.setImageDrawable(getResources().getDrawable(R.drawable.setting_white_48)));

                dH = 0.25f;

            } else if (from_internal) {

                Log.d("acamf", lst_of_array_list.size() + "");
                Log.d("acamf", cam_index_no + "");
                try {
                    //builder.setSingleChoiceItems(arrayAdapter, checkedItem[0], new DialogInterface.OnClickListener() {
                    builder.setSingleChoiceItems(lst_of_array_list.get(cam_index_no.get()), checkedItem[0], (dialogInterface, i) -> {
                        checkedItem[0] = i;
                        cameraBridgeViewBase.setMaxFrameSize(arrayListForWidth.get(i), arrayListForHeight.get(i));
                        cameraBridgeViewBase.disableView();
                        //avg.release();
                        //avg = new Mat();
                        releaseMat();
                        startMat();
                        cameraBridgeViewBase.enableView();
                        Toast.makeText(getActivity(), "Çözünürlük:  " +
                                arrayListForWidth.get(i) + "x" + arrayListForHeight.get(i), Toast.LENGTH_LONG).show();
                        dialogInterface.dismiss();
                        img_btn_cam_res.setImageDrawable(getResources().getDrawable(R.drawable.setting_white_48));
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
                dH = 0.5f;
            }

            builder.setNegativeButton(R.string.img_btn_cam_res_neg_btn, (dialogInterface, i) -> {
                img_btn_cam_res.setImageDrawable(getResources().getDrawable(R.drawable.setting_white_48));
            });
            AlertDialog dialog = builder.create();
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#809b9e90")));
            dialog.show();
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#500099"));

            // make alert dialog fill 50% of screen
            DisplayMetrics displayMetrics = new DisplayMetrics();
            Objects.requireNonNull(getActivity()).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int displayWidth = displayMetrics.widthPixels;
            int displayHeight = displayMetrics.heightPixels;
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(dialog.getWindow().getAttributes());
            int dialogWindowWidth = (int) (displayWidth * 0.5f);
            int dialogWindowHeight = (int) (displayHeight * dH);
            layoutParams.width = dialogWindowWidth;
            layoutParams.height = dialogWindowHeight;
            dialog.getWindow().setAttributes(layoutParams);

        });

        ArrayAdapter<CharSequence> adapter3 =
                ArrayAdapter.createFromResource(getActivity(), R.array.cv_array_res,
                        android.R.layout.simple_spinner_item);
        adapter3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        resolution_spinner_cv.setAdapter(adapter3);
        resolution_spinner_cv.setSelection(adapter3.getCount() - 1);
        resolution_spinner_cv.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @SuppressLint("NonConstantResourceId")
            public void onItemSelected(AdapterView<?> parent, View viw, int arg2, long arg3) {
                Spinner spinner2 = (Spinner) parent;
                String item2 = (String) spinner2.getSelectedItem();
                if (item2.equals(getContext().getResources().getString(R.string.motion_detect))) {
                    no_scanning = false;
                    face_detection_1 = false;
                    face_detection_2 = false;
                    motion_detection = true;

                    releaseMat();
                    startMat();
                    giris = 0;  cikis = 0;
                } else if (item2.equals(getContext().getResources().getString(R.string.face_detect_1))) {
                    no_scanning = false;
                    motion_detection = false;
                    face_detection_1 = true;
                    face_detection_2 = false;
                    face_builder();

                    releaseMat();
                    startMat();
                    giris = 0;  cikis = 0;
                } else if (item2.equals(getContext().getResources().getString(R.string.face_detect_2))) {
                    no_scanning = false;
                    motion_detection = false;
                    face_detection_1 = false;
                    face_detection_2 = true;
                    face_builder();

                    releaseMat();
                    startMat();
                    giris = 0;  cikis = 0;
                } else if (item2.equals(getContext().getResources().getString(R.string.no_scanning))) {
                    motion_detection = false;
                    face_detection_1 = false;
                    face_detection_2 = false;
                    no_scanning = true;

                    releaseMat();
                    startMat();
                    giris = 0;  cikis = 0;
                    Update_Text(giris);
                }
                /*
                switch (item2) {
                    case getContext().getResources().getString(R.string.motion_detect):
                        no_scanning = false;
                        face_detection_1 = false;
                        face_detection_2 = false;
                        motion_detection = true;

                        releaseMat();
                        startMat();
                        giris = 0;  cikis = 0;
                        break;
                    case "Yüz Algılama 1":
                        no_scanning = false;
                        motion_detection = false;
                        face_detection_1 = true;
                        face_detection_2 = false;
                        face_builder();

                        releaseMat();
                        startMat();
                        giris = 0;  cikis = 0;
                        break;
                    case "Yüz Algılama 2":
                        no_scanning = false;
                        motion_detection = false;
                        face_detection_1 = false;
                        face_detection_2 = true;
                        face_builder();

                        releaseMat();
                        startMat();
                        giris = 0;  cikis = 0;
                        break;
                    case "Tarama Yok":
                        motion_detection = false;
                        face_detection_1 = false;
                        face_detection_2 = false;
                        no_scanning = true;

                        releaseMat();
                        startMat();
                        giris = 0;  cikis = 0;
                        Update_Text(giris);
                        break;
                }
                 */
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        txt_capacity.setText(String.valueOf(kapasite));
        txt_capacity.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void afterTextChanged(Editable editable) {
                try {
                    kapasite = Integer.parseInt(txt_capacity.getText().toString());
                    Update_Text(giris);
                    //(kapasite >= 0) ? txt_capacity.setError(null) : txt_capacity.setError("0'dan küçük olamaz!");
                } catch (Exception ignored) {}
            }
        });

        txt_current.setText(String.valueOf(giris));
        txt_to_enter.setText(String.valueOf(kapasite - giris));


        // Get URL
        Intent intent = Objects.requireNonNull(getActivity()).getIntent();
        if (intent != null) {
            String str_data = intent.getExtras().getString("Source");
            if (str_data.equals("From External Activity")) {
                rtspUrl = intent.getExtras().getString(rtspUrl);
                from_external = true;
                from_internal = false;
                cameraBridgeViewBase.setCvCameraViewListener((CameraBridgeViewBase.CvCameraViewListener2) null);
                cameraBridgeViewBase.disableView();
                cameraBridgeViewBase.setVisibility(SurfaceView.GONE);

                textureView.setSurfaceTextureListener((TextureView.SurfaceTextureListener) this);
                textureView.setVisibility(View.VISIBLE);

                imageView.setVisibility(View.VISIBLE);

                img_btn_cam_switch.setVisibility(View.GONE);
                //img_btn_cam_res.setVisibility(View.GONE);


                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            UpdateExternal();
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(getActivity(), "Oynatma hatası!", Toast.LENGTH_SHORT).show();
                        }
                        handler.postDelayed(this, updateFreqMs);
                    }
                }, updateFreqMs);


            } else if (str_data.equals("From MainActivity Internal-Button")) {
                from_external = false;
                from_internal = true;
                cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
                cameraBridgeViewBase.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
                cameraBridgeViewBase.setCvCameraViewListener(this);
                cameraBridgeViewBase.setMaxFrameSize(320, 240);
                //cameraBridgeViewBase.setMaxFrameSize(480, 320);
                cameraBridgeViewBase.enableView();

                textureView.setSurfaceTextureListener(null);
                textureView.setVisibility(View.GONE);

                imageView.setVisibility(View.GONE);

                img_btn_cam_switch.setVisibility(View.VISIBLE);
                img_btn_cam_res.setVisibility(View.VISIBLE);


            }
        }

        SharedPreferences preferences = Objects.requireNonNull(getActivity()).getSharedPreferences("SAVED_VALUES", MODE_PRIVATE);
        width = preferences.getInt("width", width);
        height = preferences.getInt("height", height);
        //

        super.onViewCreated(view, savedInstanceState);
    }

    private void releaseMat() {
        firstframe.release();
        avg.release();
        grayMat.release();
        accWght.release();
        cnvrtScal.release();
        frameDelta.release();
        cannyEdges.release();
        thresh.release();
        dilate.release();
        haarcascade_first.release();
    }

    private void startMat() {
        firstframe = new Mat();
        avg = new Mat();
        grayMat = new Mat();    //CvType.CV_8UC1 (default for Mat)
        accWght = new Mat();
        cnvrtScal = new Mat();
        frameDelta = new Mat();
        cannyEdges = new Mat();
        thresh = new Mat();
        dilate = new Mat();
        arr = new ArrayList<>();
        haarcascade_first = new MatOfRect();
    }

    public void checkCameraPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(Objects.requireNonNull(getActivity()), permission) == PackageManager.PERMISSION_DENIED)
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), permission)) {
                Log.v(TAG, "Permission is revoked");
            } else {
                Log.v(TAG, "Permission is granted 1");
                ActivityCompat.requestPermissions(getActivity(), new String[]{permission}, requestCode);
            }
        else
            Log.v(TAG,"Permission is granted 2");
         /*
        if (Build.VERSION.SDK_INT >= 23) {
            if (this.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "Permission is granted");

            } else {
                Log.v(TAG, "Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission is granted");
        }
         */
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getActivity(), "Kamera izni verildi", Toast.LENGTH_SHORT).show();
                camera_allow();
            }
            else
                Toast.makeText(getActivity(), "Kamera izni reddedildi", Toast.LENGTH_SHORT).show();
        }
    }

    public void camera_allow() {
        try {

            lst_of_array_list = new ArrayList<>();

            arrayListForWidth = new ArrayList<>();
            arrayListForHeight = new ArrayList<>();
            List list;
            for (int i = 0; i<2; i++) {  //index=0 means CAMERA__BACK, =1 means _FRONT
                int index = ((i == 0) ? CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT);
                Camera cam_index;
                try {
                    cam_index = open(i);
                } catch (Exception e) {
                    cam_index = open();
                }
                if (cam_index.getParameters().getSupportedVideoSizes() != null || cam_index.getParameters().getSupportedPreviewSizes() != null) {
                    if (cam_index.getParameters().getSupportedVideoSizes() != null)
                        list = cam_index.getParameters().getSupportedVideoSizes();
                    else
                        list = cam_index.getParameters().getSupportedPreviewSizes();
                    arrayAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.select_dialog_singlechoice) {
                        @Override
                        public View getView(int position, View convertView, ViewGroup parent) {
                            TextView textView = (TextView) super.getView(position, convertView, parent);
                            textView.setTextColor(Color.WHITE);
                            return textView;
                        }
                    };
                    for (Object e_list : list) {
                        arrayAdapter.addAll(((Camera.Size) e_list).width + "x" + ((Camera.Size) e_list).height);
                        arrayListForWidth.add(((Camera.Size) e_list).width);
                        arrayListForHeight.add(((Camera.Size) e_list).height);
                        Log.d("camera1: ", ((Camera.Size) e_list).width + " x " + ((Camera.Size) e_list).height);
                    }
                    lst_of_array_list.add(i, arrayAdapter);
                }
            }
        } catch (Exception e) {
            Log.d("here",  "camera is closed or need permission");
        }
    }

    private void createPlayer(String rtspUrl) {
        releasePlayer();
        try {
            ArrayList<String> options = new ArrayList<>();
            options.add("--file-caching=2000");
            options.add("-vvv"); // verbosity

            libvlc = new LibVLC(Objects.requireNonNull(getActivity()), options);
            ///holder.setKeepScreenOn(true);

            // Create media player
            mMediaPlayer = new MediaPlayer(libvlc);

            // Set up video output
            final IVLCVout vout = mMediaPlayer.getVLCVout();
            mMediaPlayer.setScale(0);
            vout.detachViews();
            vout.setVideoView(textureView);
            vout.setWindowSize(textureView.getWidth(), textureView.getHeight());
            //vout.setWindowSize(width, height);
            vout.attachViews();
            textureView.setKeepScreenOn(true);
            textureView.setAlpha(0); //işe yarar mı bilmiyorum,  andr.cam works

            Media m = new Media(libvlc, Uri.parse(rtspUrl));
            mMediaPlayer.setMedia(m);
            mMediaPlayer.play();
            m.setHWDecoderEnabled(true, false);
            m.addOption(":network-caching=150");
            m.addOption(":clock-jitter=0");
            m.addOption(":clock-synchro=0");

            //Toast.makeText(this, "android sürüm: "+android.os.Build.VERSION.SDK_INT, Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(getActivity(), "Error creating player!", Toast.LENGTH_SHORT).show();
        }
    }

    public void face_builder() {
        // High-accuracy landmark detection and face classification
        highAccuracyOpts =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .build();

        // Real-time contour detection
        realTimeOpts =
                new FaceDetectorOptions.Builder()
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                        .build();

        // Real-time contour detection of multiple faces
        realTimeMultiOpts =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setContourMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                        .build();

        // For face detection, you should use an image with dimensions of at least 480x360 pixels.
        // If you are detecting faces in real time, capturing frames at this minimum resolution can help reduce latency.
    }

    public void face_detect_android_build(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        FaceDetector detector = FaceDetection.getClient(realTimeOpts);

        //Task<List<Face>> result = detector.process(image....)
        detector.process(image).addOnSuccessListener(new OnSuccessListener<List<Face>>() {
            @Override
            public void onSuccess(List<Face> faces) {

                Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        for (Face face : faces) {
                            bounds = face.getBoundingBox();
                            float rotY = face.getHeadEulerAngleY();  // Head is rotated to the right rotY degrees
                            float rotZ = face.getHeadEulerAngleZ();  // Head is tilted sideways rotZ degrees

                            // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
                            // nose available):
                            FaceLandmark leftEar = face.getLandmark(FaceLandmark.LEFT_EAR);
                            if (leftEar != null) {
                                PointF leftEarPos = leftEar.getPosition();
                            }

                            //// If contour detection was enabled:
                            //List<PointF> leftEyeContour = face.getContour(FaceContour.LEFT_EYE).getPoints();
                            //List<PointF> upperLipBottomContour = face.getContour(FaceContour.UPPER_LIP_BOTTOM).getPoints();

                            // If classification was enabled:
                            if (face.getSmilingProbability() != null) {
                                float smileProb = face.getSmilingProbability();
                            }
                            if (face.getRightEyeOpenProbability() != null) {
                                float rightEyeOpenProb = face.getRightEyeOpenProbability();
                            }

                            // If face tracking was enabled:
                            if (face.getTrackingId() != null) {
                                int id = face.getTrackingId();
                            }

                            //openCV_per_face(imageMat, bounds);
                        }

                        //OpenCV_3();
                        MatToBitmap_setImageBitmap();

                    }
                });

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("here", "failure");
                //Task failed
            }
        });

    }

    public void UpdateExternal() {
        //b = textureView.getBitmap(textureView.getWidth(),textureView.getHeight());
        try {
            b = textureView.getBitmap(Integer.parseInt(edt1.getText().toString()), Integer.parseInt(edt2.getText().toString()));
        } catch (Exception e) {
            b = textureView.getBitmap(width, height);
        }
        Log.d("here", "UpdateExternal: " + width + "-" + height + b);

        if (imageMat != null)
            imageMat.release();

        // Bitmap to Mat
        imageMat = new Mat(b.getHeight(), b.getWidth(), CvType.CV_8UC1);
        b1 = b.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(b1, imageMat);

        if (motion_detection) {
            OpenCV_motion_detection();
            MatToBitmap_setImageBitmap();
        }

        if (face_detection_1) {
            //face_detect(b);
            OpenCV_face_detect_haarcascade();
            MatToBitmap_setImageBitmap();
        }

        if (face_detection_2) {
            //face_detect(b);
            OpenCV_face_detect_haarcascade_2();
            MatToBitmap_setImageBitmap();
        }

        if (no_scanning)
            MatToBitmap_setImageBitmap();

    }

    public void MatToBitmap_setImageBitmap(){
        b2 = Bitmap.createBitmap(imageMat.cols(), imageMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(imageMat, b2);
        // setImage
        imageView.setVisibility(View.VISIBLE);  //??

        Objects.requireNonNull(getActivity()).runOnUiThread(() -> imageView.setImageBitmap(b2));   //buna gerek var mı
    }

    public void OpenCV_face_detect_haarcascade() {
        flag = true;
        Imgproc.cvtColor(imageMat, grayMat, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(grayMat, grayMat, new Size(21,21), 0);

        MatOfRect haarcascade = new MatOfRect();
        if (cascadeClassifier != null) {
            Log.d("face", "2");
            cascadeClassifier.detectMultiScale(grayMat, haarcascade, 1.1, 3, 3, new Size(30,30));
        }

        Rect[] haarcascadeArray = haarcascade.toArray();

        for (Rect rect1 : haarcascadeArray) {
            int xd1 = (int) rect1.tl().x;
            int yd1 = (int) rect1.tl().y;
            int xd2 = (int) rect1.br().x;
            int yd2 = (int) rect1.br().y;
            xvalues.add(xd1);

            int center_x = (xd1 + xd2)/2;
            int center_y = (yd1 + yd2)/2;
            Imgproc.rectangle(imageMat, rect1.tl(), rect1.br(), new Scalar(0,0,255), 2);
            //Imgproc.drawMarker(imageMat, new Point(center_x, center_y), new Scalar(255,0,0));
            //Rect roi = new Rect(xd1, yd1, xd2-xd1, yd2-yd1);
            flag = false;
        }

        OpenCV_counting(xvalues, flag);
    }

    public void OpenCV_face_detect_haarcascade_2() {
        Imgproc.cvtColor(imageMat, grayMat, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(grayMat, grayMat, new Size(21,21), 0);
        if (avg.empty() || avg == null)      //(avg == null || avg.empty()
            avg = grayMat.clone();

        // what if avg && grayMat has 0x0
        if (haarcascade_first.empty()) {
            avg = grayMat.clone();
            cascadeClassifier.detectMultiScale(avg, haarcascade_first, 1.1, 3, 3, new Size(30,30));
        }
        haarcascadeArray_first = haarcascade_first.toArray();
        int center_x_first=0, center_y_first=0;
        for (Rect rect1 : haarcascadeArray_first) {
            center_x_first = (int) ((rect1.tl().x + rect1.br().x)/2);
            center_y_first = (int) ((rect1.tl().y + rect1.br().y)/2);
            //Imgproc.rectangle(imageMat, rect1.tl(), rect1.br(), new Scalar(0,255,255), 2);
        }

        MatOfRect haarcascade = new MatOfRect();
        if (cascadeClassifier != null)
            cascadeClassifier.detectMultiScale(grayMat, haarcascade, 1.1, 3, 3, new Size(30,30));

        Rect[] haarcascadeArray = haarcascade.toArray();
        int xd1=0, yd1, xd2=0, yd2, center_x=0, center_y=0;
        for (Rect rect1 : haarcascadeArray) {
            xd1 = (int) rect1.tl().x;
            yd1 = (int) rect1.tl().y;
            xd2 = (int) rect1.br().x;
            yd2 = (int) rect1.br().y;

            center_x = (xd1 + xd2)/2;
            center_y = (yd1 + yd2)/2;
            Imgproc.rectangle(imageMat, rect1.tl(), rect1.br(), new Scalar(0,0,255), 2);
        }

        if ((center_x != 0) && (center_x_first != 0) && (xd1 != 0) && (xd2 != 0)) {
            Imgproc.line(imageMat, new Point(center_x_first, center_y_first), new Point(center_x, center_y), new Scalar(0, 0, 255), 2);
            if ((center_x_first < imageMat.width()/2) && (center_x > imageMat.width()/2)) {
                giris++;
                haarcascade_first = haarcascade;
            } else if ((center_x_first > imageMat.width()/2) && (center_x < imageMat.width()/2)) {
                cikis++;
                haarcascade_first = haarcascade;
            }

            if (xd2 > imageMat.width()-imageMat.width()/20)
                haarcascade_first = new MatOfRect();
            else if (xd1 < imageMat.width()/20)
                haarcascade_first = new MatOfRect();
        }

        Imgproc.line(imageMat, new Point(imageMat.width()/2.0, 0), new Point(imageMat.width()/2.0, imageMat.height()), new Scalar(255, 0, 0), 2);   //frame.width()/2 = 160, frame.height() = 720
        Imgproc.putText(imageMat, String.format(getString(R.string.input_no) + "%s", giris), new Point(imageMat.width()/2.0 + 10, 40), Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0, 255, 255), 2);
        Imgproc.putText(imageMat, String.format(getString(R.string.output_no) + "%s", cikis), new Point(10, 40), Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(255, 255, 0), 2);

        Update_Text(giris);
    }

    @SuppressLint({"SetTextI18n"})
    private void OpenCV_motion_detection() {
        flag = true;
        contourList = new ArrayList<>(); //A list to store all the contours

        Imgproc.cvtColor(imageMat, grayMat, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(grayMat, grayMat, new Size(21,21), 0);
        //Imgproc.Canny(imageMat, cannyEdges, 10, 100);

        if (avg.empty() || avg == null) {
            assert avg != null;
            grayMat.convertTo(avg, CvType.CV_32F);
            if (avg.width()==0 && avg.height()==0)
                Log.e("avg", avg.size() + "-" + grayMat.size());
        } else {              // we couldnt use <continue> in above if statement, that's why <else> is best option
            Imgproc.accumulateWeighted(grayMat, avg, 0.5);
            Core.convertScaleAbs(avg, cnvrtScal);
            Core.absdiff(grayMat, cnvrtScal, frameDelta);
            ///compute difference between first frame and current frame
            ///Core.absdiff(grayMat, avg, frameDelta); //üstteki 3 satır veya bu.

            Imgproc.threshold(frameDelta, thresh, 5, 255, Imgproc.THRESH_BINARY); //thresh:25
            Imgproc.dilate(thresh, dilate, new Mat(), new Point(-1, -1), 2);
            Imgproc.findContours(dilate, contourList, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);


            for (int i=0; i < contourList.size(); i++) {
                if (Imgproc.contourArea(contourList.get(i)) > 5000) {
                    rect = Imgproc.boundingRect(contourList.get(i));
                    arr.add(rect);
                    xvalues.add(rect.x);
                    Imgproc.rectangle(imageMat, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0,0,255), 2);
                    flag = false;
                }
            }

            OpenCV_counting(xvalues, flag);
        }

    }

    public void OpenCV_counting(List<Integer> xvalues, boolean flag) {
        int no_x = xvalues.size();
        if (no_x > 2) {
            int difference = xvalues.get(no_x - 1) - xvalues.get(no_x - 2);
            if (difference > 0)
                motion.add(1);
            else
                motion.add(0);
        }

        if (flag) {
            if (no_x > 5) {
                int[] find_maj = majority(motion);
                //if((find_maj[0] == 1) && find_maj[1] >= 15)
                if(find_maj[0] == 1)
                    giris += 1;
                else
                    cikis += 1;
            }
            xvalues.clear();
            motion.clear();
        }

        Imgproc.line(imageMat, new Point(imageMat.width()/20.0, 0), new Point(imageMat.width()/20.0, imageMat.height()), new Scalar(255, 255, 0), 2);   //frame.width()/2 = 160, frame.height() = 720
        Imgproc.line(imageMat, new Point(imageMat.width()-imageMat.width()/20.0, 0), new Point(imageMat.width()-imageMat.width()/20.0, imageMat.height()), new Scalar(0, 255, 255), 2);   //frame.width()/2 = 160, frame.height() = 720
        Imgproc.putText(imageMat, String.format(getString(R.string.input_no) + "%s", giris), new Point(10, 60), Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0, 255, 255), 2);
        Imgproc.putText(imageMat, String.format(getString(R.string.output_no) + "%s", cikis), new Point(10, 40), Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(255, 255, 0), 2);

        Update_Text(giris);
    }

    private int[] majority(List<Integer> motion) {
        Map<Integer, Integer> myMap = new HashMap<>();
        int[] maximum = new int[]{0,0};

        for (int n : motion) {
            //if (myMap.get(n) == null) continue;  //not sure about that ..
            if (myMap.containsKey(n)) {
                myMap.put(n, myMap.get(n) + 1);
            } else
                myMap.put(n, 1);

            if (myMap.get(n) > maximum[1]){
                maximum[0] = n;
                maximum[1] = myMap.get(n);
            }
        }
        return maximum;
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void Update_Text(int giris) {

        Objects.requireNonNull(getActivity()).runOnUiThread(() -> {
            txt_current.setText(String.valueOf(giris));
            txt_to_enter.setText(String.valueOf(kapasite - giris));

            if (giris > kapasite) {
                video_linearLayout.setBackgroundColor(Color.RED);
                imageView_walking_standing.setImageDrawable(getResources().getDrawable(R.drawable.standing));
            } else {
                video_linearLayout.setBackgroundColor(Color.parseColor("#009900"));
                imageView_walking_standing.setImageDrawable(getResources().getDrawable(R.drawable.walking));
            }
        });
    }

    private void OpenCV_2() {
        Imgproc.cvtColor(imageMat, grayMat, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(grayMat, grayMat, new Size(21, 21), 0);

        if (firstframe == null) {
            firstframe = grayMat;
            //continue;
        }

        Core.absdiff(firstframe, grayMat, frameDelta);
        Imgproc.threshold(frameDelta, thresh, 25, 255, Imgproc.THRESH_BINARY);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();

        Imgproc.findContours(thresh, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        Imgproc.dilate(thresh, dilate, new Mat(), new Point(-1, -1), 2);

        for (MatOfPoint mf : contours) {

            if (Imgproc.contourArea(mf) < 2000) {
                continue;
            }
            Imgproc.drawContours(imageMat, contours, contours.indexOf(mf), new Scalar(0, 255, 255));
            Imgproc.fillConvexPoly(imageMat, mf, new Scalar(0, 255, 255));
            Rect r = Imgproc.boundingRect(mf);
            Imgproc.rectangle(imageMat, r.tl(), r.br(), new Scalar(0, 255, 0), 2);
        }
        firstframe = grayMat;
    }

    private  void OpenCV_3() {
        if (bounds != null)
            Imgproc.rectangle(imageMat, new Point(bounds.left, bounds.top), new Point(bounds.left + bounds.width(), bounds.top + bounds.height()), new Scalar(0, 0, 255), 2);
    }

    @Override
    public void onPause() {
        Log.d("here", "onPause");
        super.onPause();
        if (from_external)
            releasePlayer();

        if(textureView != null) {
            linear_layout_of_texture.removeView(textureView);
            textureView = null;
        }

        if(cameraBridgeViewBase!=null)
            cameraBridgeViewBase.disableView();

        handler.removeCallbacksAndMessages(null);

        //releaseMat();           //not sure
    }

    @Override
    public void onResume() {
        Log.d("here", "onResume");
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Log.e("here", "There is a problem in OpenCV");
            if (getActivity() != null)
                OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_3_0, getActivity(), baseLoaderCallback); //OPENCV_VERSION_3_3_0(arada hata) //OPENCV_VERSION_2_4_11(eskisi) //3_4_0 (hata)
        } else {
            Toast.makeText(getActivity(), "it works", Toast.LENGTH_SHORT).show();
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            //cameraBridgeViewBase.enableView();

            startMat();
        }
    }

    @Override
    public void onDestroy() {
        Log.d("here", "onDestroy");
        super.onDestroy();
        //if (from_external)
        //    releasePlayer();

        if (cameraBridgeViewBase != null)
            cameraBridgeViewBase.disableView();
    }

    public void releasePlayer() {
        Log.d("here", "releasePlayer");
        if (libvlc == null)
            return;
        mMediaPlayer.stop();
        final IVLCVout vout = mMediaPlayer.getVLCVout();
        vout.detachViews();
        //holder = null;
        libvlc.release();
        libvlc = null;

    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        //textureView.getSurfaceTexture().setDefaultBufferSize(width, height);
        Log.d("here", "onSurfaceTextureAvailable: " + width + height);
        try {
            if (from_external)
                createPlayer(rtspUrl);
        } catch (Exception e) {
            Toast.makeText(getActivity(), "???????????????", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.d("here", "onSurfaceTextureSizeChanged");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.d("here", "onSurfaceTextureDestroyed");
        //textureView.getSurfaceTexture().setDefaultBufferSize(0, 0);
        //return true;
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        Log.d("here", "onSurfaceTextureUpdated");
    }


    //AFTER THAT POINT --> ANDROID CAMERA
    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.d("here", "onCameraViewStarted: " + width + "-" + height);
        imageMat = new Mat();
    }

    @Override
    public void onCameraViewStopped() {
        Log.d("here", "onCameraViewStopped");
        // imageMat.release(); bu veya aşağıdaki 3 satır
        if(imageMat != null) {
            imageMat.release();
        }
        imageMat = null;
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        if (imageMat != null)
            imageMat.release();
        imageMat = inputFrame.rgba().t();
        if (img_btn1)
            Core.flip(imageMat, imageMat, 1);
        else
            Core.flip(imageMat, imageMat, -1);
        Imgproc.resize(imageMat, imageMat, inputFrame.rgba().size()); //new Size(width:.. , height: ..)
        Log.d("here", "onCameraFrame: " + inputFrame.rgba().size());


        if (motion_detection)
            OpenCV_motion_detection();

        if (face_detection_1)
            OpenCV_face_detect_haarcascade();

        if (face_detection_2)
            OpenCV_face_detect_haarcascade_2();

        if (no_scanning) {
            Log.d("here", "no scanning: ");
        }

        inputFrame.rgba().release();  //buna gerek var mı??? zaten yukerıda release oluyor

        return imageMat;
    }

}