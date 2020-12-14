package com.example.necip.vlc_android_camera_streaming;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void btnClickNextActivity(View v) {
        switch (v.getId()) {
            case R.id.btn_internal:
                Intent intent = new Intent(this, InternalCameraActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_external:
                Intent intent2 = new Intent(this, ExternalCameraActivity.class);
                startActivity(intent2);
                break;
        }
    }
}