package com.example.necip.vlc_android_camera_streaming;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class ViewPagerAdapter extends FragmentPagerAdapter {
    public ViewPagerAdapter(@NonNull FragmentManager fm) {
        super(fm);
    }

    /**
     *
     * @param tabPosition
     * @return tabs
     */
    @NonNull
    @Override
    public Fragment getItem(int tabPosition) {
        switch(tabPosition) {
            case 0:
                VideoFragment videoFragment = new VideoFragment();
                return videoFragment;
            case 1:
                return new LogFragment();
            default:
                return null;
        }
    }

    /**
     *
     * @return number of tab which is written above
     */
    @Override
    public int getCount() {
        return 2;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return "Live";
            case 1:
                return "Logs";
            default:
                return null;
        }
    }
}