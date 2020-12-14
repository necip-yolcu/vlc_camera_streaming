package com.example.necip.vlc_android_camera_streaming;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class InternalCameraActivity extends AppCompatActivity {

    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_internal_camera);

        intent = new Intent(this, VideoActivity.class);
    }

    public void CameraOpen(View v) {
        switch (v.getId()) {
            case R.id.btn_frontcamera:
                //some front camera open process
                break;
            case R.id.btn_backcamera:
                //some back camera open process
                break;
        }
        startActivity(intent);
    }
}