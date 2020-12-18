package com.example.necip.vlc_android_camera_streaming;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

public class ExternalCameraActivity extends AppCompatActivity {

    RadioGroup port_group;
    RadioButton r_b_ip_http, r_b_ip_rtsp;
    EditText textRTSP, protocol_input, address1_input, address2_input, address3_input, address4_input, port_input, username_input, password_input, command_input, width_input, height_input;

    Spinner resolution_spinner, command_spinner;
    int width;// = 640;
    int height;// = 480;
    String ip_command;// = "/1";
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    SwitchCompat switch1, switch2;
    TextView txt_weblink;
    LinearLayout linearLayout_option_1, linearLayout_option_2;

    @SuppressLint({"SetTextI18n", "ResourceType"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_external_camera);

        setTitle("Harici Kamera");

        switch1 = findViewById(R.id.switch1);
        switch2 = findViewById(R.id.switch2);

        linearLayout_option_1 = findViewById(R.id.linearLayout_option_1);
        linearLayout_option_2 = findViewById(R.id.linearLayout_option_2);
        linearLayout_option_1.setVisibility(View.GONE);
        linearLayout_option_2.setVisibility(View.GONE);

        textRTSP = findViewById(R.id.textRTSPUrl);


        r_b_ip_http = findViewById(R.id.r_b_ip_http);
        r_b_ip_rtsp = findViewById(R.id.r_b_ip_rtsp);

        protocol_input = findViewById(R.id.protocol_input);
        protocol_input.setText("rtsp");
        port_group = findViewById(R.id.port_radiogroup);
        port_group.setOnCheckedChangeListener((group, checkedId) -> {
            if(checkedId == R.id.r_b_ip_http){
                protocol_input.setText("http");
            }else if(checkedId == R.id.r_b_ip_rtsp){
                protocol_input.setText("rtsp");
            } else {
                protocol_input.setText(protocol_input.getText().toString());
            }
        });

        address1_input = findViewById(R.id.address1_input);
        address1_input.setText(String.valueOf(192));
        address2_input = findViewById(R.id.address2_input);
        address2_input.setText(String.valueOf(168));
        address3_input = findViewById(R.id.address3_input);
        address3_input.setText(String.valueOf(0));
        address4_input = findViewById(R.id.address4_input);
        address4_input.setText(String.valueOf(180));
        port_input = findViewById(R.id.port_input);
        port_input.setText(String.valueOf(80));
        username_input = findViewById(R.id.username_input);
        username_input.setText("kullanıcı");
        password_input = findViewById(R.id.password_input);
        password_input.setText("şifre");
        width_input = findViewById(R.id.width_input);
        width_input.setText(String.valueOf(640));
        height_input = findViewById(R.id.height_input);
        height_input.setText(String.valueOf(480));
        command_input = findViewById(R.id.command_input);
        //command_input.setText("/1");

        txt_weblink = findViewById(R.id.txt_weblink);
        txt_weblink.setMovementMethod(LinkMovementMethod.getInstance());
        txt_weblink.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://camlytics.com/cameras"));
            startActivity(intent);
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
                switch (item) {
                    case "1920x1080":
                        width = 1920;
                        height = 1080;
                        break;
                    case "1080x1920":
                        width = 1080;
                        height = 1920;
                        break;
                    case "1280x720":
                        width = 1280;
                        height = 720;
                        break;
                    case "720x1280":
                        width = 720;
                        height = 1280;
                        break;
                    case "800x600":
                        width = 800;
                        height = 600;
                        break;
                    case "600x800":
                        width = 600;
                        height = 800;
                        break;
                    case "640x480":
                        width = 640;
                        height = 480;
                        break;
                    case "480x640":
                        width = 480;
                        height = 640;
                        break;
                    case "320x240":
                        width = 320;
                        height = 240;
                        break;
                    case "240x320":
                        width = 240;
                        height = 320;
                        break;
                    case "176x144":
                        width = 176;
                        height = 144;
                        break;
                    case "144x176":
                        width = 144;
                        height = 176;
                        break;
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
        command_spinner = findViewById(R.id.command_spinner);
        command_spinner.setAdapter(adapter2);
        command_spinner.setSelection(adapter2.getCount() - 1);
        command_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View viw, int arg2, long arg3) {
                Spinner spinner2 = (Spinner) parent;
                String item2 = (String) spinner2.getSelectedItem();
                switch (item2) {
                    case "snap.jpg (for photo)":
                        ip_command = "snap.jpg";
                        break;
                    case "0":
                        ip_command = "0";
                        break;
                    case "1":
                        ip_command = "1";
                        break;
                    case "2":
                        ip_command = "2";
                        break;
                    case "cam1/onvif-h264":
                        ip_command = "cam1/onvif-h264";
                        break;
                    case "cam/realmonitor":
                        ip_command = "cam/realmonitor";
                        break;
                    case "Onvif/live/1/1":
                        ip_command = "Onvif/live/1/1";
                        break;
                    case "ch0_0.h264":
                        ip_command = "ch0_0.h264";
                        break;
                }
                command_input.setText(String.valueOf(ip_command));
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

    }

    /**
     * when app restarted or maximized
     */
    @Override
    protected void onResume() {
        super.onResume();

        //Retrieve data from preference:
        SharedPreferences prefs = getSharedPreferences("SAVED_VALUES", MODE_PRIVATE);

        // 1st option
        textRTSP.setText(prefs.getString("rtsp1", textRTSP.getText().toString()));

        //2dn option
        protocol_input.setText(prefs.getString("protocol", protocol_input.getText().toString()));
        if (protocol_input.getText().toString().equals("http")) {
            r_b_ip_http.setChecked(true);
        } else if (protocol_input.getText().toString().equals("rtsp")) {
            r_b_ip_rtsp.setChecked(true);
        } else {
            r_b_ip_http.setChecked(false);
            r_b_ip_rtsp.setChecked(false);
        }
        address1_input.setText(String.valueOf(prefs.getInt("address 1", Integer.parseInt(address1_input.getText().toString()))));
        address2_input.setText(String.valueOf(prefs.getInt("address 2", Integer.parseInt(address2_input.getText().toString()))));
        address3_input.setText(String.valueOf(prefs.getInt("address 3", Integer.parseInt(address3_input.getText().toString()))));
        address4_input.setText(String.valueOf(prefs.getInt("address 4", Integer.parseInt(address4_input.getText().toString()))));
        port_input.setText(String.valueOf(prefs.getInt("port", Integer.parseInt(port_input.getText().toString()))));
        username_input.setText(prefs.getString("username", username_input.getText().toString()));
        password_input.setText(prefs.getString("password", username_input.getText().toString()));
        command_input.setText(prefs.getString("command", command_input.getText().toString()));
        ip_command = command_input.getText().toString();
        width_input.setText(String.valueOf(prefs.getInt("width", Integer.parseInt(width_input.getText().toString()))));
        width = Integer.parseInt(width_input.getText().toString());
        height_input.setText(String.valueOf(prefs.getInt("height", Integer.parseInt(height_input.getText().toString()))));
        height = Integer.parseInt(height_input.getText().toString());

    }

    /**
     * when app closed or minimized
     */
    @Override
    protected void onPause() {
        super.onPause();

        if ("".equals(protocol_input.getText().toString()))
            protocol_input.setText("");
        if ("".equals(address1_input.getText().toString()))
            address1_input.setText(String.valueOf(192));
        if ("".equals(address2_input.getText().toString()))
            address2_input.setText(String.valueOf(168));
        if ("".equals(address3_input.getText().toString()))
            address3_input.setText(String.valueOf(0));
        if ("".equals(address4_input.getText().toString()))
            address4_input.setText(String.valueOf(180));
        if ("".equals(port_input.getText().toString()))
            port_input.setText(String.valueOf(80));
        if ("".equals(username_input.getText().toString()))
            username_input.setText("");
        if ("".equals(password_input.getText().toString()))
            password_input.setText("");
        if ("".equals(width_input.getText().toString()))
            width_input.setText(String.valueOf(640));
        if ("".equals(height_input.getText().toString()))
            height_input.setText(String.valueOf(480));
        if ("".equals(command_input.getText().toString()))
            command_input.setText("");


        //Setting values in Preference:
        SharedPreferences preferences = getSharedPreferences("SAVED_VALUES", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();  // Put the values from the UI

        // 1st option
        editor.putString("rtsp1", textRTSP.getText().toString()); // value to store

        //2dn option
        editor.putString("protocol", protocol_input.getText().toString());
        editor.putInt("address 1", Integer.parseInt(address1_input.getText().toString()));
        editor.putInt("address 2", Integer.parseInt(address2_input.getText().toString()));
        editor.putInt("address 3", Integer.parseInt(address3_input.getText().toString()));
        editor.putInt("address 4", Integer.parseInt(address4_input.getText().toString()));
        editor.putInt("port", Integer.parseInt(port_input.getText().toString()));
        editor.putString("username", username_input.getText().toString()); // value to store
        editor.putString("password", password_input.getText().toString());
        editor.putString("command", command_input.getText().toString());
        editor.putInt("width", Integer.parseInt(width_input.getText().toString()));
        editor.putInt("height", Integer.parseInt(height_input.getText().toString()));
        editor.apply();
    }

    @SuppressLint("NonConstantResourceId")
    public void SwitchClick(View v) {
        switch (v.getId()) {
            case R.id.switch1:
                if (switch1.isChecked()) {
                    linearLayout_option_1.setVisibility(View.VISIBLE);
                    linearLayout_option_2.setVisibility(View.GONE);
                } else {
                    linearLayout_option_1.setVisibility(View.GONE);
                }
                switch2.setChecked(false);
                break;

            case R.id.switch2:
                if (switch2.isChecked()) {
                    linearLayout_option_1.setVisibility(View.GONE);
                    linearLayout_option_2.setVisibility(View.VISIBLE);
                } else {
                    linearLayout_option_2.setVisibility(View.GONE);
                }
                switch1.setChecked(false);
                break;
        }

    }

    public void onClickPLAY(View v) {
        Intent intent = new Intent(this, VideoActivity.class);
        intent.putExtra("Source", "From External Activity");

        StringBuilder sb = new StringBuilder();
        String s_dot = ".";
        String s_colon = ":";
        String s_slash = "/";
        String s_at = "@";
        String URL;

        if (switch1.isChecked()) {
            intent.putExtra("rtsp", textRTSP.getText().toString());
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);

        } else if (switch2.isChecked()) {
            sb.append(protocol_input.getText().toString());
            sb.append(s_colon);
            sb.append(s_slash).append(s_slash);
            sb.append(username_input.getText().toString());
            sb.append(s_colon);
            sb.append(password_input.getText().toString());
            sb.append(s_at);
            sb.append(address1_input.getText().toString());
            sb.append(s_dot);
            sb.append(address2_input.getText().toString());
            sb.append(s_dot);
            sb.append(address3_input.getText().toString());
            sb.append(s_dot);
            sb.append(address4_input.getText().toString());
            sb.append(s_colon);
            sb.append(port_input.getText().toString());
            sb.append(s_slash);
            sb.append(command_input.getText().toString());
            URL = new String(sb);

            intent.putExtra("rtsp", URL);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);

        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(ExternalCameraActivity.this);
            //builder.setCancelable(false);
            builder.setTitle("Uyarı!!");
            builder.setMessage("Lütfen yukarıdaki seçeneklerden birini seçiniz");
            builder.setPositiveButton("Tamam", (dialogInterface, i) -> {});
            builder.show();
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