package com.example.necip.vlc_android_camera_streaming;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;
import android.view.TextureView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

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

public class VideoActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {

    public final static String TAG = "VideoActivity";

    public static final String RTSP_URL = "rtspurl";
    public static final String width_URL = "width";
    public static final String height_URL = "height";
    public static final String command_URL = "ip_command";


    private TextureView textureView;

    private ImageView imageView;

    // media player
    private LibVLC libvlc;
    private MediaPlayer mMediaPlayer = null;
    private final static int VideoSizeChanged = -1;


    private String rtspUrl;
    private int width = 176;
    private int height = 144;
    private String ip_command;

    String s;

    boolean flag;
    Bitmap bitmapframe, b, b1, b2;
    Mat imageMat, frame;

    Mat avg = null;
    Mat firstframe;
    Mat grayMat;
    Mat accWght;
    Mat cnvrtScal;
    Mat frameDelta;
    Mat cannyEdges;
    Mat thresh;

    List<MatOfPoint> contourList;
    Rect rect;
    ArrayList<Rect> arr;        //xvalues, yvalues, w, h
    List<Integer> xvalues = new ArrayList<>();
    List<Integer> motion = new ArrayList<>();
    //List<Integer> xvalues;
    //List<Integer> motion;

    private SparseArray<Face> mFaces;
    FaceDetector detector;
    Frame frame2;
    int giris = 0;
    int cikis = 0;

    Spinner resolution_spinner_cv;


    final Handler handler = new Handler();
    final int updateFreqMs = 30; //30; 1000// call update every 1000 ms 1000/fps

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        //SharedPreferences preferences = getSharedPreferences("SAVED_VALUES", MODE_PRIVATE);
        //width = preferences.getInt("width", width);
        //height = preferences.getInt("height", height);


        // Get URL
        Intent intent = getIntent();
        rtspUrl = intent.getExtras().getString(RTSP_URL);
        width = intent.getExtras().getInt(width_URL, width);
        height = intent.getExtras().getInt(height_URL, height);
        ip_command = intent.getExtras().getString(command_URL);


        Log.d(TAG, "Playing back " + rtspUrl);

        textureView = findViewById(R.id.textureView);
        textureView.setSurfaceTextureListener(this);

        imageView = findViewById(R.id.imageView);

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
                if (item2.equals("Motion Detection 1")) {
                    ip_command = "1";
                } else if (item2.equals("Motion Detection 2")) {
                    ip_command = "2";
                } else if (item2.equals("Face Detection")) {
                    ip_command = "3";
                } else if (item2.equals("No Scanning")) {
                    ip_command = "4";
                }
                //command_input.setText(String.valueOf(ip_command));

            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        isStoragePermissionGranted();
    }

    public boolean isStoragePermissionGranted() {
        String TAG = "Storage Permission";
        if (Build.VERSION.SDK_INT >= 23) {
            if (this.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "Permission is granted");
                return true;
            } else {
                Log.v(TAG, "Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission is granted");
            return true;
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

    @Override
    protected void onResume() {
        super.onResume();
        //createPlayer(rtspUrl);

        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            Toast.makeText(this, "Internal OpenCV library not found. Using OpenCV Manager for initialization", Toast.LENGTH_SHORT).show();
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            Toast.makeText(this, "OpenCV library found inside package. Using it!", Toast.LENGTH_SHORT).show();

        }


        //Retrieve data from preference:
        SharedPreferences prefs = getSharedPreferences("SAVED_VALUES", MODE_PRIVATE);
        String myedittext = prefs.getString("myedittext", "necip");

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

    private void createPlayer(String rtspUrl) {
        releasePlayer();
        try {
            ArrayList<String> options = new ArrayList<String>();
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
            textureView.setVisibility(View.INVISIBLE);     // !!!!!!!!!!!!!!!!! (find alternative)


            Media m = new Media(libvlc, Uri.parse(rtspUrl));
            mMediaPlayer.setMedia(m);
            mMediaPlayer.play();
            m.setHWDecoderEnabled(true, false);
            m.addOption(":network-caching=150");
            m.addOption(":clock-jitter=0");
            m.addOption(":clock-synchro=0");

            Toast.makeText(this, "android sürüm: "+android.os.Build.VERSION.SDK_INT, Toast.LENGTH_SHORT).show();

            //set first frame
            firstframe = new Mat();
            grayMat = new Mat();
            accWght = new Mat();
            cnvrtScal = new Mat();
            frameDelta = new Mat();
            cannyEdges = new Mat();
            thresh = new Mat();

            arr = new ArrayList<>();


        } catch (Exception e) {
            Toast.makeText(this, "Error creating player!", Toast.LENGTH_SHORT).show();
        }
    }

    public void scan(View v) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    UpdateCannyEdge();
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
        textureView.setVisibility(View.VISIBLE);
        b = textureView.getBitmap(width,height);
        textureView.setVisibility(View.INVISIBLE);
        // Bitmap to Mat
        imageMat = new Mat(b.getHeight(), b.getWidth(), CvType.CV_8UC1);
        b1 = b.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(b1, imageMat);

        //OPENCV 1
        OpenCV_1();

        //OPENCV 2
        //OpenCV_2();

        // Mat to Bitmap
        b2 = Bitmap.createBitmap(imageMat.cols(), imageMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(imageMat, b2);
        // setImage
        imageView.setVisibility(View.VISIBLE);  //??
        runOnUiThread(new Runnable() {  //buna gerek var mı
            @Override
            public void run() {
                imageView.setImageBitmap(b2);
            }
        });

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
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        releasePlayer();

        //Setting values in Preference:
        SharedPreferences preferences = getSharedPreferences("SAVED_VALUES", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();  // Put the values from the UI
        editor.putString("myedittext", s); // value to store
        editor.apply();

        handler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    public void releasePlayer() {
        if (libvlc == null)
            return;
        mMediaPlayer.stop();
        final IVLCVout vout = mMediaPlayer.getVLCVout();
        vout.detachViews();
        //holder = null;
        libvlc.release();
        libvlc = null;

        Log.d(TAG, "HEY releasePlayer");
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        try {
            createPlayer(rtspUrl);
        } catch (Exception e) {
            Toast.makeText(this, "???????????????", Toast.LENGTH_SHORT).show();
        }
        Log.d(TAG, "HEY onSurfaceTextureAvailable");
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        Toast.makeText(this, "HEY onSurfaceTextureUpdated", Toast.LENGTH_SHORT).show();
    }
}