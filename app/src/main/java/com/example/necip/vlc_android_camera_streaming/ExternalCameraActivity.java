package com.example.necip.vlc_android_camera_streaming;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ExternalCameraActivity extends AppCompatActivity {

    RadioButton r_b_ip_http, r_b_ip_rtsp;
    EditText address1_input, address2_input, address3_input, address4_input, port_input, username_input, password_input, command_input, width_input, height_input;

    Spinner resolution_spinner, resolution_spinner_command;
    int width = 640;
    int height = 480;
    String ip_command = "/1";
    TextView txt_option_1, txt_option_2, txt_weblink;
    LinearLayout linearLayout_option_1, linearLayout_option_2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_external_camera);

        txt_option_1 = findViewById(R.id.txt_option_1);
        txt_option_2 = findViewById(R.id.txt_option_2);

        linearLayout_option_1 = findViewById(R.id.linearLayout_option_1);
        linearLayout_option_2 = findViewById(R.id.linearLayout_option_2);
        linearLayout_option_1.setVisibility(View.VISIBLE);
        linearLayout_option_2.setVisibility(View.GONE);

        r_b_ip_http = findViewById(R.id.r_b_ip_http);
        r_b_ip_rtsp = findViewById(R.id.r_b_ip_rtsp);

        address1_input = findViewById(R.id.address1_input);
        address2_input = findViewById(R.id.address2_input);
        address3_input = findViewById(R.id.address3_input);
        address4_input = findViewById(R.id.address4_input);
        port_input = findViewById(R.id.port_input);
        username_input = findViewById(R.id.username_input);
        password_input = findViewById(R.id.password_input);

        width_input = findViewById(R.id.width_input);
        height_input = findViewById(R.id.height_input);
        command_input = findViewById(R.id.command_input);

        txt_weblink = findViewById(R.id.txt_weblink);
        txt_weblink.setMovementMethod(LinkMovementMethod.getInstance());
        txt_weblink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://camlytics.com/cameras"));
                startActivity(intent);
            }
        });

        ArrayAdapter<CharSequence> adapter =
                ArrayAdapter.createFromResource(this, R.array.resolution_array_res,
                        android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        resolution_spinner = findViewById(R.id.resolution_spinner);
        resolution_spinner.setAdapter(adapter);
        resolution_spinner.setSelection(adapter.getCount() - 1);
        resolution_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View viw, int arg2, long arg3) {
                Spinner spinner = (Spinner) parent;
                String item = (String) spinner.getSelectedItem();
                if (item.equals("1920x1080")) {
                    width = 1920;
                    height = 1080;
                } else if (item.equals("1080x1920")) {
                    width = 1080;
                    height = 1920;
                } else if (item.equals("1280x720")) {
                    width = 1280;
                    height = 720;
                } else if (item.equals("720x1280")) {
                    width = 720;
                    height = 1280;
                } else if (item.equals("800x600")) {
                    width = 800;
                    height = 600;
                } else if (item.equals("600x800")) {
                    width = 600;
                    height = 800;
                } else if (item.equals("640x480")) {
                    width = 640;
                    height = 480;
                } else if (item.equals("480x640")) {
                    width = 480;
                    height = 640;
                } else if (item.equals("320x240")) {
                    width = 320;
                    height = 240;
                } else if (item.equals("240x320")) {
                    width = 240;
                    height = 320;
                } else if (item.equals("176x144")) {
                    width = 176;
                    height = 144;
                } else if (item.equals("144x176")) {
                    width = 144;
                    height = 176;
                }
                width_input.setText(String.valueOf(width));
                height_input.setText(String.valueOf(height));

            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


        ArrayAdapter<CharSequence> adapter2 =
                ArrayAdapter.createFromResource(this, R.array.command_array_res,
                        android.R.layout.simple_spinner_item);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        resolution_spinner_command = findViewById(R.id.resolution_spinner_command);
        resolution_spinner_command.setAdapter(adapter2);
        resolution_spinner_command.setSelection(adapter2.getCount() - 1);
        resolution_spinner_command.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View viw, int arg2, long arg3) {
                Spinner spinner2 = (Spinner) parent;
                String item2 = (String) spinner2.getSelectedItem();
                if (item2.equals("/snap.jpg")) {
                    ip_command = "/snap.jpg";
                } else if (item2.equals("/0")) {
                    ip_command = "/0";
                } else if (item2.equals("/1")) {
                    ip_command = "/1";
                } else if (item2.equals("/2")) {
                    ip_command = "/2";
                } else if (item2.equals("/cam1/onvif-h264")) {
                    ip_command = "/cam1/onvif-h264";
                } else if (item2.equals("/cam/realmonitor")) {
                    ip_command = "/cam/realmonitor";
                } else if (item2.equals("/Onvif/live/1/1")) {
                    ip_command = "/Onvif/live/1/1";
                } else if (item2.equals("/ch0_0.h264")) {
                    ip_command = "/ch0_0.h264";
                } else if (item2.equals(" ")) {
                    ip_command = "";
                }
                command_input.setText(String.valueOf(ip_command));

            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


        Button startButton = (Button)findViewById(R.id.buttonStart);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String s;
                s = width_input.getText().toString();
                if (!"".equals(s))
                    width = Integer.parseInt(s);
                s = height_input.getText().toString();
                if (!"".equals(s))
                    height = Integer.parseInt(s);

                s = command_input.getText().toString();
                if (!"".equals(s))
                    ip_command = s;

                Intent intent = new Intent(ExternalCameraActivity.this, VideoActivity.class);
                EditText textRTSP = (EditText)findViewById(R.id.textRTSPUrl);
                intent.putExtra(VideoActivity.RTSP_URL, textRTSP.getText().toString());
                intent.putExtra(VideoActivity.width_URL, width);
                intent.putExtra(VideoActivity.height_URL, height);
                intent.putExtra(VideoActivity.command_URL, ip_command);
                Toast.makeText(getApplicationContext(), width + "-" + height, Toast.LENGTH_LONG).show();

                startActivity(intent);
                //startActivityForResult(intent, REQUEST_SETTINGS);

            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        //Retrieve data from preference:
        SharedPreferences prefs = getSharedPreferences("SAVED_VALUES", MODE_PRIVATE);
        //port_input.setText(prefs.getInt("port", Integer.parseInt(port_input.getText().toString())));
        username_input.setText(prefs.getString("username", username_input.getText().toString()));
        password_input.setText(prefs.getString("password", username_input.getText().toString()));
        try {
            width_input.setText(prefs.getInt("widthg", Integer.parseInt(width_input.getText().toString())));
            height_input.setText(prefs.getInt("heightg", Integer.parseInt(height_input.getText().toString())));
        } catch (Exception e) {}

        command_input.setText(prefs.getString("command", command_input.getText().toString()));
    }

    @Override
    protected void onPause() {
        super.onPause();

        //Setting values in Preference:
        SharedPreferences preferences = getSharedPreferences("SAVED_VALUES", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();  // Put the values from the UI
        //editor.putInt("port", Integer.parseInt(port_input.getText().toString()));
        editor.putString("username", username_input.getText().toString()); // value to store
        editor.putString("password", password_input.getText().toString());
        editor.putInt("widthg", Integer.parseInt(width_input.getText().toString()));
        editor.putInt("heightg", Integer.parseInt(height_input.getText().toString()));
        editor.putString("command", command_input.getText().toString());
        editor.apply();
    }

    public void TextOptionClick(View v) {
        switch (v.getId()) {
            case R.id.txt_option_1:
                linearLayout_option_1.setVisibility(View.VISIBLE);
                linearLayout_option_2.setVisibility(View.GONE);
                break;
            case R.id.txt_option_2:
                linearLayout_option_1.setVisibility(View.GONE);
                linearLayout_option_2.setVisibility(View.VISIBLE);
                break;
        }

    }

    public void linearLayoutTappedS(View view) {
        try {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}