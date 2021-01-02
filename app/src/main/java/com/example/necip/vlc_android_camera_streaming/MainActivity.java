package com.example.necip.vlc_android_camera_streaming;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @SuppressLint("NonConstantResourceId")
    public void btnClickNextActivity(View v) {
        switch (v.getId()) {
            case R.id.img_btn_internal:
                Intent intent = new Intent(this, VideoActivity.class);
                //Intent intent = new Intent(this, FaceDetectionAct.class);
                intent.putExtra("Source", "From MainActivity Internal-Button");
                startActivity(intent);
                break;
            case R.id.img_btn_external:
                Intent intent2 = new Intent(this, ExternalCameraActivity.class);
                //Intent intent2 = new Intent(this, FaceDetectionAct.class);
                startActivity(intent2);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + v.getId());
        }
    }
}