package com.example.necip.vlc_android_camera_streaming;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class Video_LogPages extends AppCompatActivity {

    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_log_pages);

        BottomNavigationView nav_view = findViewById(R.id.nav_view);

        ViewPager viewPager = findViewById(R.id.viewPager);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        nav_view.getMenu().findItem(R.id.live_item).setChecked(true);
                        break;
                    case 1:
                        nav_view.getMenu().findItem(R.id.log_item).setChecked(true);
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        nav_view.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.live_item:
                    viewPager.setCurrentItem(0);
                    break;
                case R.id.log_item:
                    viewPager.setCurrentItem(1);
                    break;
            }
            return true;
        });



        ViewPagerAdapter mViewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(mViewPagerAdapter);

    }
}
