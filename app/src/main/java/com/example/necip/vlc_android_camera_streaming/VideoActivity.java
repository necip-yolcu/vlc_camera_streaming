package com.example.necip.vlc_android_camera_streaming;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VideoActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener, CameraBridgeViewBase.CvCameraViewListener2 {

    public final static String TAG = "VideoActivity";

    private TextureView textureView;
    private ImageView imageView;
    ToggleButton tgl_btn_cam_switch;
    ImageButton img_btn_cam_switch;


    // media player
    private LibVLC libvlc;
    private MediaPlayer mMediaPlayer = null;

    Boolean from_external, from_internal;

    private String rtspUrl = "rtsp";
    private int width = 600;
    private int height = 144;

    String scan_command;

    boolean img_btn, flag;
    Bitmap b, b1, b2;

    Mat avg = null;
    Mat imageMat, firstframe, grayMat, accWght, cnvrtScal, frameDelta, cannyEdges, thresh;


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

    Spinner resolution_spinner_cv;
    LinearLayout video_linearLayout, linear_layout_of_texture;
    ImageView imageView_walking_standing;

    // android camera
    CameraBridgeViewBase cameraBridgeViewBase;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final int WRITE_EXTERNAL_STORAGE_PERMISSION_CODE = 200;

    final Handler handler = new Handler();
    final int updateFreqMs = 30; //30; 1000// call update every 1000 ms 1000/fps

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);


        video_linearLayout = findViewById(R.id.video_linearLayout);
        linear_layout_of_texture = findViewById(R.id.linear_layout_of_texture);
        imageView_walking_standing = findViewById(R.id.imageView_walking_standing);

        textureView = findViewById(R.id.textureView);
        //textureView.setSurfaceTextureListener(this);
        //textureView.setSurfaceTextureListener(null);

        imageView = findViewById(R.id.imageView);

        img_btn = true;
        img_btn_cam_switch = findViewById(R.id.img_btn_cam_switch);
        img_btn_cam_switch.setOnClickListener(view -> {
            if (img_btn) {
                img_btn = false;
                cameraBridgeViewBase.disableView();
                cameraBridgeViewBase.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
                cameraBridgeViewBase.enableView();
            } else {
                img_btn = true;
                cameraBridgeViewBase.disableView();
                cameraBridgeViewBase.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
                cameraBridgeViewBase.enableView();
            }
        });

        tgl_btn_cam_switch = findViewById(R.id.tgl_btn_cam_switch);
        tgl_btn_cam_switch.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                cameraBridgeViewBase.disableView();
                cameraBridgeViewBase.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
                cameraBridgeViewBase.enableView();
            } else {
                cameraBridgeViewBase.disableView();
                cameraBridgeViewBase.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
                cameraBridgeViewBase.enableView();
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
                    case "Motion Detection 1":
                        scan_command = "1";
                        break;
                    case "Motion Detection 2":
                        scan_command = "2";
                        break;
                    case "Face Detection":
                        scan_command = "3";
                        break;
                    case "No Scanning":
                        scan_command = "4";
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


        ///android camera CV
        //System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        cameraBridgeViewBase = (JavaCameraView)findViewById(R.id.myAndroidCameraView);
        //cameraBridgeViewBase.setAlpha(0);

        /*
        baseLoaderCallback = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                super.onManagerConnected(status);
                switch(status){
                    case BaseLoaderCallback.SUCCESS:
                        cameraBridgeViewBase.enableView();
                        break;
                    default:
                        super.onManagerConnected(status);
                        break;
                }
            }
        };
         */

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

                tgl_btn_cam_switch.setVisibility(View.GONE);


            } else if (str_data.equals("From MainActivity Internal-Button")) {
                Log.d("here", "4_internal");
                from_external = false;
                from_internal = true;
                cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
                cameraBridgeViewBase.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
                cameraBridgeViewBase.setCvCameraViewListener(this);
                cameraBridgeViewBase.enableView();

                textureView.setSurfaceTextureListener(null);
                textureView.setVisibility(View.GONE);

                imageView.setVisibility(View.GONE);

                tgl_btn_cam_switch.setVisibility(View.VISIBLE);


                cameraBridgeViewBase.setScaleX(-1);
                cameraBridgeViewBase.setScaleY(-1);


            }
        }

        /*
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            rtspUrl = extras.getString(rtspUrl);
            Toast.makeText(this, "URL: " + rtspUrl, Toast.LENGTH_SHORT).show();
        }
         */

        SharedPreferences preferences = getSharedPreferences("SAVED_VALUES", MODE_PRIVATE);
        width = preferences.getInt("width", width);
        height = preferences.getInt("height", height);


        //PERMISSION
        checkCameraPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_CODE);
        checkStoragePermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE_PERMISSION_CODE);

        // Check if Google Play Services is installed and its version is at least 20.12.14
        /// On Android 8.1 Go devices ML Kit requires at least version 20.12.14 to be able to download models properly without a reboot
        int result = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this, 201214000);
        if (result != ConnectionResult.SUCCESS) {
            if (GoogleApiAvailability.getInstance().isUserResolvableError(result)) {
                GoogleApiAvailability.getInstance().getErrorDialog(this, result, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            }
        }

    }

    /*
    @Override
    protected void onPause() {
        super.onPause();
        if(cameraBridgeViewBase!=null){
            cameraBridgeViewBase.disableView();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug())
            Toast.makeText(getApplicationContext(), "There is a problem in OpenCV", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(getApplicationContext(), "it works", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraBridgeViewBase != null) {
            cameraBridgeViewBase.disableView();
        }
    }
     */

    public void checkCameraPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(VideoActivity.this, permission) == PackageManager.PERMISSION_DENIED)
            if (ActivityCompat.shouldShowRequestPermissionRationale(VideoActivity.this, permission)) {
                Log.v(TAG, "Permission is revoked");
            } else {
                Log.v(TAG, "Permission is granted");
                ActivityCompat.requestPermissions(VideoActivity.this, new String[]{permission}, requestCode);
            }
        else
            Log.v(TAG,"Permission is granted");
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
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(VideoActivity.this, "Kamera izni verildi", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(VideoActivity.this, "Kamera izni reddedildi", Toast.LENGTH_SHORT).show();
        } else if (requestCode == WRITE_EXTERNAL_STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(VideoActivity.this, "Depolama izni verildi", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(VideoActivity.this, "Depolama izni reddedildi", Toast.LENGTH_SHORT).show();
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

            /*
            //set first frame
            firstframe = new Mat();
            grayMat = new Mat();
            accWght = new Mat();
            cnvrtScal = new Mat();
            frameDelta = new Mat();
            cannyEdges = new Mat();
            thresh = new Mat();

            arr = new ArrayList<>();
             */


        } catch (Exception e) {
            Toast.makeText(this, "Error creating player!", Toast.LENGTH_SHORT).show();
        }
    }

    public void scan(View v) {
        Log.d("here", "scan");
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    if (from_external) {
                        //cameraBridgeViewBase.disableView();
                        Log.d("here", "from external");
                        UpdateCannyEdge();
                    } else if (from_internal) {
                        //cameraBridgeViewBase.enableView();
                        Log.d("here", "from internal");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Oynatma hatası!", Toast.LENGTH_SHORT).show();
                }
                handler.postDelayed(this, updateFreqMs);
            }
        }, updateFreqMs);

    }

    public void UpdateCannyEdge() {
        //b = textureView.getBitmap(textureView.getWidth(),textureView.getHeight());
        b = textureView.getBitmap(width, height);
        Log.d("here", "UpdateCannyEdge: " + width + "-" + height + b);

        // Bitmap to Mat
        imageMat = new Mat(b.getHeight(), b.getWidth(), CvType.CV_8UC1);
        b1 = b.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(b1, imageMat);


        //OPENCV 1
        OpenCV_1();

        //OPENCV 2
        //OpenCV_2();

        // Mat to Bitmap
        MatToBitmap_setImageBitmap();

        /* frame kaybolmasıok donuyor
        b = textureView.getBitmap(textureView.getWidth(),textureView.getHeight());
        if (!detector.isOperational()) {
            //Handle contingency
        } else {
            frame2 = new Frame.Builder().setBitmap(b).build();
            mFaces = detector.detect(frame2);
            detector.release();
        }
        //invalidate();
        runOnUiThread(new Runnable() {  //buna gerek var mı
            @Override
            public void run() {
                imageView.setImageBitmap(b);
            }
        });
         */

    }

    public void MatToBitmap_setImageBitmap(){
        b2 = Bitmap.createBitmap(imageMat.cols(), imageMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(imageMat, b2);
        // setImage
        imageView.setVisibility(View.VISIBLE);  //??

        runOnUiThread(() -> imageView.setImageBitmap(b2));   //buna gerek var mı
    }

    @SuppressLint({"SetTextI18n"})
    private void OpenCV_1() {
        flag = true;
        contourList = new ArrayList<>(); //A list to store all the contours

        Imgproc.cvtColor(imageMat, grayMat, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(grayMat, grayMat, new Size(21,21), 0);
        //Imgproc.Canny(imageMat, cannyEdges, 10, 100);

        //avg = grayMat.clone();
        if (avg == null) {
            avg = grayMat.clone();
            avg.convertTo(avg, CvType.CV_32F);
        }

        Imgproc.accumulateWeighted(grayMat, avg, 0.5);
        Core.convertScaleAbs(avg, cnvrtScal);
        //compute difference between first frame and current frame
        /////Core.absdiff(firstframe, currentframe, frameDelta);
        Core.absdiff(grayMat, cnvrtScal, frameDelta);

        Imgproc.threshold(frameDelta, thresh, 5, 255, Imgproc.THRESH_BINARY);
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
                if((find_maj[0] == 1) && find_maj[1] >= 15)
                    //if(find_maj[0] == 1)
                    giris += 1;
                else
                    cikis += 1;
            }
            xvalues.clear();
            motion.clear();
        }

        Imgproc.line(imageMat, new Point(imageMat.width()/2, 0), new Point(imageMat.width()/2, imageMat.height()), new Scalar(255, 0, 0), 2);   //frame.width()/2 = 160, frame.height() = 720
        Imgproc.putText(imageMat, String.format("Giris: %s", giris), new Point(imageMat.width()/2 + 10, 40), Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0, 255, 0), 2);
        Imgproc.putText(imageMat, String.format("Cikis: %s", cikis), new Point(10, 40), Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0, 0, 255), 2);

        runOnUiThread(() -> Update_Text(giris));

    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void Update_Text(int giris) {
        txt_current.setText(String.valueOf(giris));
        txt_to_enter.setText(String.valueOf(kapasite - giris));

        if (giris > 3) {
            video_linearLayout.setBackgroundColor(Color.RED);
            imageView_walking_standing.setImageDrawable(getResources().getDrawable(R.drawable.standing));
        }
    }

    private void OpenCV_2() {
        Imgproc.resize(imageMat, imageMat, new Size(500, 500));
        Mat gray = new Mat();
        Imgproc.cvtColor(imageMat, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(gray, gray, new Size(21, 21), 0);

        if (firstframe == null) {
            firstframe = gray;
            //continue;
        }

        Mat frameDelta = new Mat();
        Core.absdiff(firstframe, gray, frameDelta);
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
        firstframe = gray;
    }

    private int[] majority(List<Integer> motion) {
        Map<Integer, Integer> myMap = new HashMap<>();
        int[] maximum = new int[]{0,0};

        for (int n : motion) {
            if (myMap.containsKey(n))
                myMap.put(n, myMap.get(n) + 1);
            else
                myMap.put(n, 1);

            if (myMap.get(n) > maximum[1]){
                maximum[0] = n;
                maximum[1] = myMap.get(n);
            }
        }
        return maximum;
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
    }

    @Override
    protected void onResume() {
        Log.d("here", "onResume");
        super.onResume();

        if (!OpenCVLoader.initDebug())
            Toast.makeText(getApplicationContext(), "There is a problem in OpenCV", Toast.LENGTH_SHORT).show();
        else {
            Toast.makeText(getApplicationContext(), "it works", Toast.LENGTH_SHORT).show();

            //set first frame
            firstframe = new Mat();
            grayMat = new Mat();
            accWght = new Mat();
            cnvrtScal = new Mat();
            frameDelta = new Mat();
            cannyEdges = new Mat();
            thresh = new Mat();
            arr = new ArrayList<>();

        }


        ///baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);

        /*
        if(textureView == null) {
            textureView = findViewById(R.id.textureView); //????
            linear_layout_of_texture.addView(textureView);
        }

         */


        /*
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                UpdateCannyEdge();
                handler.postDelayed(this, updateFreqMs);
            }
        }, updateFreqMs);
         */

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
    public void onCameraViewStarted(int width, int height) { //320-240
        //andr_mat = new Mat(width, height, CvType.CV_8UC4);
        Log.d("here", "onCameraViewStarted: " + width + "-" + height);
    }

    @Override
    public void onCameraViewStopped() {
        Log.d("here", "onCameraViewStopped");
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        if (imageMat != null)
            imageMat.release();
        imageMat = inputFrame.rgba().t();
        Core.flip(imageMat, imageMat, 1);
        Imgproc.resize(imageMat, imageMat, inputFrame.rgba().size()); //new Size(width:.. , height: ..)
        Log.d("here", "onCameraFrame: " + inputFrame.rgba().size());


        OpenCV_1();

        inputFrame.rgba().release();  //buna gerek var mı???

        return imageMat;

    }

    /*    for bitmap
    private Bitmap rotateBitmap(Bitmap bitmap){
        Matrix rotateRight = new Matrix();
        rotateRight.preRotate(90);

        float[] mirrorY = { -1, 0, 0, 0, 1, 0, 0, 0, 1};
        rotateRight = new Matrix();
        Matrix matrixMirrorY = new Matrix();
        matrixMirrorY.setValues(mirrorY);

        rotateRight.postConcat(matrixMirrorY);

        rotateRight.preRotate(270);


        final Bitmap rImg= Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), rotateRight, true);
        return rImg;
    }

     */

}