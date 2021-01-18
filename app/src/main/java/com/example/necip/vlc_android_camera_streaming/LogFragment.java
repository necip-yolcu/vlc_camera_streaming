package com.example.necip.vlc_android_camera_streaming;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Path;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import android.os.Handler;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static android.content.Context.INPUT_METHOD_SERVICE;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LogFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LogFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    LinearLayout linear_layout_of_fragment_log;
    TextView txt_log;
    StringBuffer output;
    ScrollView scrollView;
    RadioGroup group_items;
    MenuItem searchMenuItem, debug_item, pause_item;
    SearchView searchViewEditText;

    boolean pause_clicked = true;
    String logcatcode;

    Handler handler = new Handler();

    public LogFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment LogFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static LogFragment newInstance(String param1, String param2) {
        LogFragment fragment = new LogFragment();
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_log, container, false);

        linear_layout_of_fragment_log = view.findViewById(R.id.linear_layout_of_fragment_log);

        txt_log = view.findViewById(R.id.txt_log);
        txt_log.setMovementMethod(new ScrollingMovementMethod());

        scrollView = view.findViewById(R.id.scrollView);

        group_items = view.findViewById(R.id.group_items);

        setHasOptionsMenu(true);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        linear_layout_of_fragment_log.setOnClickListener(view12 -> {
            try {
                InputMethodManager inputMethodManager = (InputMethodManager) Objects.requireNonNull(this.getActivity()).getSystemService(INPUT_METHOD_SERVICE);
                assert inputMethodManager != null;
                inputMethodManager.hideSoftInputFromWindow(this.getActivity().getCurrentFocus().getWindowToken(), 0);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });



        try {
            Runtime.getRuntime().exec("logcat -c");
        } catch (IOException e) { e.printStackTrace(); }

        output = new StringBuffer();


        logcatcode = "-d *:I";
        handlerX(logcatcode);
    }

    public void handlerX(String logcatCode) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                output = new StringBuffer();
                logcat(logcatCode, output);
                txt_log.post(() -> {
                    txt_log.setText("");
                    txt_log.setText(output.toString());
                });

                scrollView.post(() -> {
                    scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                });

                handler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    public void logcat(String code, StringBuffer outputLog) {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("logcat " + code);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            /*
            StringBuilder log=new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                log.append(line + "\n");
            }
             */

            int read;
            char[] buffer = new char[4096];
            while ((read = bufferedReader.read(buffer)) > 0)
                outputLog.append(buffer, 0, read);

            bufferedReader.close();
            process.waitFor();

            // arada bilgi kaçıyo
            //Runtime.getRuntime().exec("logcat -c"); // process biterken logcat temizle, zaten yeni log açılırken temizlikten itibaren aldığı için sorun olmayacaktır

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (null != process)
                process.destroy();
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.my_menu_log_fragment, menu);

        debug_item = menu.findItem(R.id.debug_item);
        debug_item.setChecked(true);

        this.pause_item = menu.findItem(R.id.pause_thread_item);

        searchMenuItem = menu.findItem(R.id.search_item);
        searchViewEditText = (SearchView) searchMenuItem.getActionView();
        SearchManager searchManager = (SearchManager) Objects.requireNonNull(getActivity()).getSystemService(Context.SEARCH_SERVICE);
        if (searchViewEditText != null) {
            searchViewEditText.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
            searchViewEditText.setIconifiedByDefault(true);
            searchViewEditText.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    if (query != null)
                        setHighLightedText(txt_log, query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    handler.removeCallbacksAndMessages(null);
                    pause_item.setIcon(android.R.drawable.ic_media_play);
                    pause_clicked = false;
                    if (!newText.equals("")) {
                        pause_item.setEnabled(false);
                        pause_item.getIcon().setAlpha(130);
                    } else {
                        pause_item.setEnabled(true);
                        pause_item.getIcon().setAlpha(255);
                    }
                    return true;
                }
            });
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    public void setHighLightedText(TextView tv, String textToHighlight) {
        String tvt = tv.getText().toString();
        int ofe = tvt.indexOf(textToHighlight, 0);
        Spannable wordToSpan = new SpannableString(tv.getText());
        for (int i = 0; i < tvt.length() && ofe != -1; i = ofe + 1) {
            ofe = tvt.indexOf(textToHighlight, i);
            if (ofe == -1)
                break;
            else {
                // set color here
                wordToSpan.setSpan(new BackgroundColorSpan(0xFFFFFF00), ofe, ofe + textToHighlight.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                tv.setText(wordToSpan, TextView.BufferType.SPANNABLE);
            }
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.share_item:
                handler.removeCallbacksAndMessages(null);
                this.pause_item.setIcon(android.R.drawable.ic_media_play);
                pause_clicked = false;
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, output.toString());
                startActivity(Intent.createChooser(sharingIntent, getString(R.string.sharing_intent_title))); //Share via
                break;
            case R.id.pause_thread_item:
                if (pause_clicked) {
                    handler.removeCallbacksAndMessages(null);
                    this.pause_item.setIcon(android.R.drawable.ic_media_play);
                    pause_clicked = false;
                } else {
                    this.pause_item.setIcon(android.R.drawable.ic_media_pause);
                    handlerX(logcatcode);
                    pause_clicked = true;
                }
                break;
            case R.id.verbose_item:
                Toast.makeText(getContext(), "Verbose", Toast.LENGTH_SHORT).show();
                handler.removeCallbacksAndMessages(null);
                this.pause_item.setIcon(android.R.drawable.ic_media_pause);
                pause_clicked = false;
                logcatcode = "-d *:V";
                handlerX(logcatcode);
                item.setChecked(true);
                break;
            case R.id.debug_item:
                Toast.makeText(getContext(), "Debug", Toast.LENGTH_SHORT).show();
                handler.removeCallbacksAndMessages(null);
                this.pause_item.setIcon(android.R.drawable.ic_media_pause);
                pause_clicked = false;
                logcatcode = "-d *:D";
                handlerX(logcatcode);
                item.setChecked(true);
                break;
            case R.id.info_item:
                Toast.makeText(getContext(), "Info", Toast.LENGTH_SHORT).show();
                handler.removeCallbacksAndMessages(null);
                this.pause_item.setIcon(android.R.drawable.ic_media_pause);
                pause_clicked = false;
                logcatcode = "-d *:I";
                handlerX(logcatcode);
                item.setChecked(true);
                break;
            case R.id.warning_item:
                Toast.makeText(getContext(), "Warn", Toast.LENGTH_SHORT).show();
                handler.removeCallbacksAndMessages(null);
                this.pause_item.setIcon(android.R.drawable.ic_media_pause);
                pause_clicked = false;
                logcatcode = "-d *:W";
                handlerX(logcatcode);
                item.setChecked(true);
                break;
            case R.id.error_item:
                Toast.makeText(getContext(), "Error", Toast.LENGTH_SHORT).show();
                handler.removeCallbacksAndMessages(null);
                this.pause_item.setIcon(android.R.drawable.ic_media_pause);
                pause_clicked = false;
                logcatcode = "-d *:E";
                handlerX(logcatcode);
                item.setChecked(true);
                break;
            case R.id.clear_item:
                Toast.makeText(getContext(), "Clear Logs", Toast.LENGTH_SHORT).show();
                handler.removeCallbacksAndMessages(null);

                this.pause_item.setIcon(android.R.drawable.ic_media_play);
                pause_clicked = false;

                try {
                    Runtime.getRuntime().exec("logcat -c");
                    Runtime.getRuntime().exec("logcat -c, --clear");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                txt_log.post(() -> txt_log.setText(""));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacksAndMessages(null);
    }
}