package com.example.necip.vlc_android_camera_streaming;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
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
import java.util.concurrent.atomic.AtomicInteger;

import static android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK;
import static android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT;

public class VideoActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener, CameraBridgeViewBase.CvCameraViewListener2 {

    public final static String TAG = "VideoActivity";

    private Mat[] buf = null;

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


    AtomicInteger cam_index_no;
    int[] checkedItem;
    List<ArrayAdapter<String>> lst_of_array_list;
    ArrayAdapter<String> arrayAdapter;
    ArrayList<Integer> arrayListForWidth, arrayListForHeight;

    boolean img_btn1, flag, motion_detection, face_detection_1, face_detection_2, no_scanning;
    Bitmap b, b1, b2;

    Mat avg, avg2;
    Mat imageMat, firstframe, grayMat, grayMat2, accWght, cnvrtScal, frameDelta, cannyEdges, thresh;


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
    TextView txt_capacity, txt_current, txt_to_enter;

    Spinner resolution_spinner_cv, resolution_spinner;
    LinearLayout video_linearLayout, linear_layout_of_texture;
    ImageView imageView_walking_standing;

    private CascadeClassifier cascadeClassifier;
    MatOfRect haarcascade_first;
    Rect[] haarcascadeArray_first;

    // android camera
    CameraBridgeViewBase cameraBridgeViewBase;
    BaseLoaderCallback baseLoaderCallback;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final int WRITE_EXTERNAL_STORAGE_PERMISSION_CODE = 200;

    final Handler handler = new Handler();
    final int updateFreqMs = 30; //30; 1000// call update every 1000 ms 1000/fps

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);


        video_linearLayout = findViewById(R.id.video_linearLayout);
        linear_layout_of_texture = findViewById(R.id.linear_layout_of_texture);
        imageView_walking_standing = findViewById(R.id.imageView_walking_standing);


        textureView = findViewById(R.id.textureView);

        imageView = findViewById(R.id.imageView);

        //PERMISSION
        checkCameraPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_CODE);
        //checkStoragePermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE_PERMISSION_CODE);

        // Check if Google Play Services is installed and its version is at least 20.12.14
        /// On Android 8.1 Go devices ML Kit requires at least version 20.12.14 to be able to download models properly without a reboot
        /*
        int result = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this, 201214000);
        if (result != ConnectionResult.SUCCESS) {
            if (GoogleApiAvailability.getInstance().isUserResolvableError(result)) {
                GoogleApiAvailability.getInstance().getErrorDialog(this, result, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            }
        }*/

        ///android camera CV
        //System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        cameraBridgeViewBase = (JavaCameraView)findViewById(R.id.myAndroidCameraView);
        //cameraBridgeViewBase.setAlpha(0);

        baseLoaderCallback = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                super.onManagerConnected(status);
                switch(status){
                    case BaseLoaderCallback.SUCCESS:
                        try {
                            //if (!OpenCVLoader.initDebug())
                            //    OpenCVLoader.initDebug();

                            InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_default);
                            File cascadeDir = getDir("haarcascade_frontalface_default", Context.MODE_PRIVATE);
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
                    default:
                        super.onManagerConnected(status);
                        break;
                }
            }
        };

        camera_allow();

        cam_index_no = new AtomicInteger();
        img_btn1 = true;
        img_btn_cam_switch = findViewById(R.id.img_btn_cam_switch);
        img_btn_cam_switch.setOnClickListener(view -> {
            if (img_btn1) {
                cameraBridgeViewBase.disableView();
                cameraBridgeViewBase.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
                cameraBridgeViewBase.enableView();
                img_btn1 = false;


                firstframe.release();
                avg.release();
                avg2.release();
                grayMat.release();
                grayMat2.release();
                accWght.release();
                cnvrtScal.release();
                frameDelta.release();
                cannyEdges.release();
                thresh.release();
                //haarcascade_first.release();

                firstframe = new Mat();
                avg = new Mat();
                avg2 = new Mat();
                grayMat = new Mat();    //CvType.CV_8UC1 (default for Mat)
                grayMat2 = new Mat();
                accWght = new Mat();
                cnvrtScal = new Mat();
                frameDelta = new Mat();
                cannyEdges = new Mat();
                thresh = new Mat();
                arr = new ArrayList<>();
                //haarcascade_first = new MatOfRect();

                cam_index_no.set(0);
                //cam_index_no.set(CAMERA_FACING_FRONT); //CAMERA_FACING_FRONT
            } else {
                cameraBridgeViewBase.disableView();
                cameraBridgeViewBase.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
                cameraBridgeViewBase.enableView();
                img_btn1 = true;

                firstframe.release();
                avg.release();
                avg2.release();
                grayMat.release();
                grayMat2.release();
                accWght.release();
                cnvrtScal.release();
                frameDelta.release();
                cannyEdges.release();
                thresh.release();
                //haarcascade_first.release();

                firstframe = new Mat();
                avg = new Mat();
                avg2 = new Mat();
                grayMat = new Mat();
                grayMat2 = new Mat();
                accWght = new Mat();
                cnvrtScal = new Mat();
                frameDelta = new Mat();
                cannyEdges = new Mat();
                thresh = new Mat();
                arr = new ArrayList<>();
                //haarcascade_first = new MatOfRect();

                cam_index_no.set(1);
                //cam_index_no.set(CAMERA_FACING_BACK); //CAMERA_FACING_BACK
            }

        });

        checkedItem = new int[]{1}; //arrayAdapter.getCount() - 1; son olan
        img_btn_cam_res = findViewById(R.id.img_btn_cam_res);
        img_btn_cam_res.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("UseCompatLoadingForDrawables")
            @Override
            public void onClick(View view) {
                //dialog appear
                img_btn_cam_res.setImageDrawable(getResources().getDrawable(R.drawable.setting_purple_48));

                AlertDialog.Builder builder = new AlertDialog.Builder(VideoActivity.this, R.style.AlertDialogCustom);
                //AlertDialog.Builder builder = new AlertDialog.Builder(VideoActivity.this);
                builder.setCancelable(false);
                builder.setTitle("Çözünürlük Seçiniz");
                Log.d("acam", lst_of_array_list + "");
                try {
                    builder.setSingleChoiceItems(lst_of_array_list.get(cam_index_no.get()), checkedItem[0], new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            checkedItem[0] = i;
                            cameraBridgeViewBase.setMaxFrameSize(arrayListForWidth.get(i), arrayListForHeight.get(i));
                            cameraBridgeViewBase.disableView();
                            cameraBridgeViewBase.enableView();
                            Toast.makeText(VideoActivity.this, "Çözünürlük:  " +
                                    arrayListForWidth.get(i) + "x" + arrayListForHeight.get(i), Toast.LENGTH_LONG).show();
                            dialogInterface.dismiss();
                            img_btn_cam_res.setImageDrawable(getResources().getDrawable(R.drawable.setting_white_48));
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
                builder.setNegativeButton("İptal", (dialogInterface, i) -> {
                    img_btn_cam_res.setImageDrawable(getResources().getDrawable(R.drawable.setting_white_48));
                });
                AlertDialog dialog = builder.create();
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#809b9e90")));
                dialog.show();
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#500099"));

                // make alert dialog fill 50% of screen
                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                int displayWidth = displayMetrics.widthPixels;
                int displayHeight = displayMetrics.heightPixels;
                WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
                layoutParams.copyFrom(dialog.getWindow().getAttributes());
                int dialogWindowWidth = (int) (displayWidth * 0.5f);
                int dialogWindowHeight = (int) (displayHeight * 0.5f);
                layoutParams.width = dialogWindowWidth;
                layoutParams.height = dialogWindowHeight;
                dialog.getWindow().setAttributes(layoutParams);
            }

        });

        ArrayAdapter<CharSequence> adapter3 =
                ArrayAdapter.createFromResource(this, R.array.cv_array_res,
                        android.R.layout.simple_spinner_item);
        adapter3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        resolution_spinner_cv = findViewById(R.id.resolution_spinner_cv);
        resolution_spinner_cv.setAdapter(adapter3);
        resolution_spinner_cv.setSelection(adapter3.getCount() - 1);
        resolution_spinner_cv.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View viw, int arg2, long arg3) {
                Spinner spinner2 = (Spinner) parent;
                String item2 = (String) spinner2.getSelectedItem();
                switch (item2) {
                    case "Hareket Algılama":
                        Log.d("herec", "MotionDetect clicked");
                        no_scanning = false;
                        face_detection_1 = false;
                        face_detection_2 = false;
                        motion_detection = true;

                        //imageMat.release();
                        //giris = 0;  cikis = 0;
                        //Update_Text(giris);
                        break;
                    case "Yüz Algılama 1":
                        Log.d("herec", "FaceDetect 1 clicked");
                        no_scanning = false;
                        motion_detection = false;
                        face_detection_1 = true;
                        face_detection_2 = false;
                        face_builder();

                        //imageMat.release();
                        //giris = 0;  cikis = 0;
                        //Update_Text(giris);
                        break;
                    case "Yüz Algılama 2":
                        Log.d("herec", "FaceDetect 2 clicked");
                        no_scanning = false;
                        motion_detection = false;
                        face_detection_1 = false;
                        face_detection_2 = true;
                        face_builder();

                        //imageMat.release();
                        //giris = 0;  cikis = 0;
                        //Update_Text(giris);
                        break;
                    case "Tarama Yok":
                        Log.d("herec", "No scannning clicked");
                        motion_detection = false;
                        face_detection_1 = false;
                        face_detection_2 = false;
                        no_scanning = true;

                        //imageMat.release();
                        //giris = 0;  cikis = 0;
                        //Update_Text(giris);
                        break;
                }
                //command_input.setText(String.valueOf(ip_command));
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        txt_capacity = findViewById(R.id.txt_capacity);
        txt_capacity.setText(String.valueOf(kapasite));
        txt_current = findViewById(R.id.txt_current);
        txt_current.setText(String.valueOf(giris));
        txt_to_enter = findViewById(R.id.txt_to_enter);
        txt_to_enter.setText(String.valueOf(kapasite - giris));


        // Get URL
        Intent intent = getIntent();
        Log.d("here", "1");
        if (intent != null) {
            Log.d("here", "2");
            String str_data = intent.getExtras().getString("Source");
            if (str_data.equals("From External Activity")) {
                rtspUrl = intent.getExtras().getString(rtspUrl);
                Log.d("here", "3_1_external: "+ rtspUrl);
                from_external = true;
                from_internal = false;
                cameraBridgeViewBase.setCvCameraViewListener((CameraBridgeViewBase.CvCameraViewListener2) null);
                cameraBridgeViewBase.disableView();
                cameraBridgeViewBase.setVisibility(SurfaceView.GONE);

                textureView.setSurfaceTextureListener(this);
                textureView.setVisibility(View.VISIBLE);

                imageView.setVisibility(View.VISIBLE);

                img_btn_cam_switch.setVisibility(View.GONE);
                img_btn_cam_res.setVisibility(View.GONE);


                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            UpdateExternal();
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(getApplicationContext(), "Oynatma hatası!", Toast.LENGTH_SHORT).show();
                        }
                        handler.postDelayed(this, updateFreqMs);
                    }
                }, updateFreqMs);


            } else if (str_data.equals("From MainActivity Internal-Button")) {
                Log.d("here", "4_internal");
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

        SharedPreferences preferences = getSharedPreferences("SAVED_VALUES", MODE_PRIVATE);
        width = preferences.getInt("width", width);
        height = preferences.getInt("height", height);
        //

    }

    public void checkCameraPermission(String permission, int requestCode) {
        /*
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED)
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                Log.v(TAG, "Permission is revoked");
            } else {
                Log.v(TAG, "Permission is granted");
                ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
            }
        else
            Log.v(TAG,"Permission is granted");
         */
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
    }

    public void checkStoragePermission(String permission, int requestCode) {
        String TAG = "Storage Permission";
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
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Kamera izni verildi", Toast.LENGTH_SHORT).show();
                camera_allow();
            }
            else
                Toast.makeText(this, "Kamera izni reddedildi", Toast.LENGTH_SHORT).show();
        }
        /*else if (requestCode == WRITE_EXTERNAL_STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(VideoActivity.this, "Depolama izni verildi", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(VideoActivity.this, "Depolama izni reddedildi", Toast.LENGTH_SHORT).show();
        }*/
    }

    public void camera_allow() {
        try {
            Log.d("acama", "open");

            lst_of_array_list = new ArrayList<>();

            arrayListForWidth = new ArrayList<>();
            arrayListForHeight = new ArrayList<>();
            List list;
            int index;
            for (int i = 0; i<2; i++) {  //index=0 means CAMERA__BACK, =1 means _FRONT
                index = ((i == 0) ? CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT);
                Camera cam_index = Camera.open(index);
                if (cam_index.getParameters().getSupportedVideoSizes() != null) {
                    arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice) {
                        @Override
                        public View getView(int position, View convertView, ViewGroup parent) {
                            TextView textView = (TextView) super.getView(position, convertView, parent);
                            textView.setTextColor(Color.WHITE);
                            return textView;
                        }
                    };
                    list = cam_index.getParameters().getSupportedVideoSizes();
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
            Log.d("acama",  "camera is closed or need permission");
        }
    }

    public void saved_bmp(View v) {
        b = textureView.getBitmap(textureView.getWidth(),textureView.getHeight());

        // Bitmap to Mat
        imageMat = new Mat(b.getHeight(), b.getWidth(), CvType.CV_8UC1);
        b1 = b.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(b1, imageMat);
        Imgproc.line(imageMat, new Point(imageMat.width()/2, 0), new Point(imageMat.width()/2, imageMat.height()), new Scalar(0, 255, 0), 2);   //frame.width()/2 = 160, frame.height() = 720
        // Mat to Bitmap
        b2 = Bitmap.createBitmap(imageMat.cols(), imageMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(imageMat, b2);
        imageView.setImageBitmap(b2);


        /*
        try {
            frame = Utils.loadResource(getApplicationContext(), R.drawable.foto1);
        } catch (IOException e) { e.printStackTrace(); }

        final Bitmap bmp1 = Bitmap.createBitmap(frame.cols(), frame.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(frame, bmp1);
        imageView.setVisibility(View.VISIBLE);


        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/saved_images");
        if (!myDir.exists()) {
            myDir.mkdirs();
        }
        Random generator = new Random();
        int n = 10000;
        n = generator.nextInt(n);
        String fname = "Image-"+ n +".jpg";
        File file = new File (myDir, fname);
        if (file.exists ())
            file.delete ();
        try {
            file.createNewFile(); // if file already exists will do nothing
            FileOutputStream out = new FileOutputStream(file);
            Toast.makeText(getApplicationContext(), "2", Toast.LENGTH_LONG).show();
            b.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
            Toast.makeText(getApplicationContext(), "kaydedildi", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "hata: " + e, Toast.LENGTH_LONG).show();
        }
         */
    }

    private void createPlayer(String rtspUrl) {
        Log.d("here", "createPlayer()");
        releasePlayer();
        try {
            ArrayList<String> options = new ArrayList<>();
            options.add("--file-caching=2000");
            options.add("-vvv"); // verbosity

            libvlc = new LibVLC(this, options);
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
            Toast.makeText(this, "Error creating player!", Toast.LENGTH_SHORT).show();
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

                runOnUiThread(new Runnable() {
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

                        OpenCV_3();
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
        b = textureView.getBitmap(width, height);
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

        runOnUiThread(() -> imageView.setImageBitmap(b2));   //buna gerek var mı
    }

    public void OpenCV_face_detect_haarcascade_2() {
        Imgproc.cvtColor(imageMat, grayMat2, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(grayMat2, grayMat2, new Size(21,21), 0);
        if (avg2.empty() || avg2 == null)      //(avg == null || avg.empty()
            avg2 = grayMat2.clone();

        if (haarcascade_first.empty()) {
            avg2 = grayMat2.clone();
            cascadeClassifier.detectMultiScale(avg2, haarcascade_first, 1.1, 3, 3, new Size(30,30));
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
            cascadeClassifier.detectMultiScale(grayMat2, haarcascade, 1.1, 3, 3, new Size(30,30));

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
        Imgproc.putText(imageMat, String.format("Giris: %s", giris), new Point(imageMat.width()/2.0 + 10, 40), Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0, 255, 255), 2);
        Imgproc.putText(imageMat, String.format("Cikis: %s", cikis), new Point(10, 40), Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(255, 255, 0), 2);

        Update_Text(giris);
    }

    public void OpenCV_face_detect_haarcascade() {
        flag = true;
        Log.d("face", "1");
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

    @SuppressLint({"SetTextI18n"})
    private void OpenCV_motion_detection2() {
        flag = true;
        contourList = new ArrayList<>(); //A list to store all the contours

        Imgproc.cvtColor(imageMat, grayMat, Imgproc.COLOR_BGR2GRAY);
        //Imgproc.cvtColor(imageMat, grayMat, Imgproc.COLOR_RGBA2GRAY);
        Imgproc.GaussianBlur(grayMat, grayMat, new Size(5,5), 0);    //size(5,5),5   //size(21,21),0
        Imgproc.Canny(grayMat, cannyEdges, 10, 100); //10,100   //  75,200     //75,150

        if (buf == null || buf[0].size() != imageMat.size()) {
            if (buf == null)
                buf = new Mat[1];
            if (buf[0] != null) {
                buf[0].release();
                buf[0] = null;
            }
            buf[0] = new Mat(imageMat.size(), CvType.CV_8UC1);
            buf[0] = Mat.zeros(imageMat.size(), CvType.CV_8UC1);

        }


        //Imgproc.threshold(frameDelta, thresh, 5, 255, Imgproc.THRESH_BINARY); //thresh:25
        //Imgproc.dilate(thresh, thresh, new Mat(), new Point(-1, -1), 2);
        Imgproc.findContours(cannyEdges, contourList, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);       //RETR_EXTERNAL, RETR_LIST

        for (int i=0; i < contourList.size(); i++) {
            if (Imgproc.contourArea(contourList.get(i)) > 5000) {    //5000
                Log.d("motion_contour ", "feys");
                rect = Imgproc.boundingRect(contourList.get(i));
                arr.add(rect);
                xvalues.add(rect.x);
                Imgproc.rectangle(imageMat, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0,0,255), 2);
                flag = false;

            }
        }

        OpenCV_counting(xvalues, flag);
    }

    @SuppressLint({"SetTextI18n"})
    private void OpenCV_motion_detection() {
        flag = true;
        contourList = new ArrayList<>(); //A list to store all the contours

        Imgproc.cvtColor(imageMat, grayMat, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(grayMat, grayMat, new Size(21,21), 0);
        //Imgproc.Canny(imageMat, cannyEdges, 10, 100);

        if (avg.empty() || avg == null) {
            grayMat.convertTo(avg, CvType.CV_32F);
            Log.d("avg", "her");
        }

        Imgproc.accumulateWeighted(grayMat, avg, 0.5);
        Core.convertScaleAbs(avg, cnvrtScal);
        Core.absdiff(grayMat, cnvrtScal, frameDelta);
        ///compute difference between first frame and current frame
        ///Core.absdiff(grayMat, avg, frameDelta); //üstteki 3 satır veya bu.

        Imgproc.threshold(frameDelta, thresh, 5, 255, Imgproc.THRESH_BINARY); //thresh:25
        Imgproc.dilate(thresh, thresh, new Mat(), new Point(-1, -1), 2);
        Imgproc.findContours(thresh, contourList, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);


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
        Imgproc.putText(imageMat, String.format("Giris: %s", giris), new Point(10, 60), Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0, 255, 255), 2);
        Imgproc.putText(imageMat, String.format("Cikis: %s", cikis), new Point(10, 40), Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(255, 255, 0), 2);

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

        runOnUiThread(() -> {
            txt_current.setText(String.valueOf(giris));
            txt_to_enter.setText(String.valueOf(kapasite - giris));

            if (giris > 3) {
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

        Mat frameDelta = new Mat();
        Core.absdiff(firstframe, grayMat, frameDelta);
        Mat thresh = new Mat();
        Imgproc.threshold(frameDelta, thresh, 25, 255, Imgproc.THRESH_BINARY);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();

        Imgproc.findContours(thresh, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        Imgproc.dilate(thresh, thresh, new Mat(), new Point(-1, -1), 2);

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
    protected void onPause() {
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


        imageMat.release();
        firstframe.release();
        avg.release();
        avg2.release();
        grayMat.release();
        grayMat2.release();
        accWght.release();
        cnvrtScal.release();
        frameDelta.release();
        cannyEdges.release();
        thresh.release();
        arr.clear();
        haarcascade_first.release();

    }

    @Override
    protected void onResume() {
        Log.d("here", "onResume");
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Toast.makeText(getApplicationContext(), "There is a problem in OpenCV", Toast.LENGTH_SHORT).show();
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, baseLoaderCallback); //OPENCV_VERSION_3_3_0 //OPENCV_VERSION_2_4_11(eskisi)
        }
        else {
            Toast.makeText(getApplicationContext(), "it works", Toast.LENGTH_SHORT).show();
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            //cameraBridgeViewBase.enableView();

            //set first frame
            //imageMat = new Mat(CvType.CV_8UC4);
            firstframe = new Mat();
            avg = new Mat();
            avg2 = new Mat();
            grayMat = new Mat();
            grayMat2 = new Mat();
            accWght = new Mat();
            cnvrtScal = new Mat();
            frameDelta = new Mat();
            cannyEdges = new Mat();
            thresh = new Mat();
            arr = new ArrayList<>();
            haarcascade_first = new MatOfRect();
        }

    }

    @Override
    protected void onDestroy() {
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
        Log.d("here", "onSurfaceTextureAvailable");
        //textureView.getSurfaceTexture().setDefaultBufferSize(width, height);
        Log.d("here", "onSurfaceTextureAvailable: " + width + height);
        try {
            if (from_external) {
                Log.d("here", "3_2: " + rtspUrl);
                createPlayer(rtspUrl);
            }
        } catch (Exception e) {
            Toast.makeText(this, "???????????????", Toast.LENGTH_SHORT).show();
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
        //1 imageMat.release(); bu veya aşağıdaki 3 satır
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
        }

        inputFrame.rgba().release();  //buna gerek var mı???

        return imageMat;
    }

}