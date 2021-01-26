package com.example.necip.vlc_android_camera_streaming;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    SharedPreferences prefs;
    String language = "en"; //default lang. is English
    int saved_no=1;
    Locale myLocale;

    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("SAVED_VALUES_Language", MODE_PRIVATE);
        language = prefs.getString("USER_LANGUAGE", language);
        switch_lang(language);
    }

    @SuppressLint("NonConstantResourceId")
    public void btnClickNextActivity(View v) {
        switch (v.getId()) {
            case R.id.img_btn_internal:
                Intent intent = new Intent(this, Video_LogPages.class);
                intent.putExtra("Source", "From MainActivity Internal-Button");
                startActivity(intent);
                break;
            case R.id.img_btn_external:
                Intent intent2 = new Intent(this, ExternalCameraActivity.class);
                startActivity(intent2);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + v.getId());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.info_item) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            //builder.setCancelable(false);
            builder.setTitle(R.string.app_name);
            builder.setMessage(getString(R.string.version_alert) + BuildConfig.VERSION_NAME);
            builder.setPositiveButton(R.string.send_email_alert, (dialogInterface, i) -> {
                Intent emailIntent = new Intent(Intent.ACTION_SEND);
                emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"ncpyolcu@gmail.com"});
                emailIntent.setType("text/html");
                emailIntent.setPackage("com.google.android.gm");
                startActivity(Intent.createChooser(emailIntent, "Send mail"));
            });
            builder.setNegativeButton(R.string.negative_button_alert_item, (dialogInterface, i) -> {});
            AlertDialog dialog = builder.create();
            //dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#809b9e90")));
            dialog.show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#99FF99"));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#FF0000"));

        } else if (item.getItemId() == R.id.language_item) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            //builder.setCancelable(false);
            builder.setTitle(R.string.switch_lang_item);
            ArrayAdapter<CharSequence> langarrayAdapter = ArrayAdapter.createFromResource(this,
                    R.array.languages, android.R.layout.select_dialog_singlechoice);
            builder.setSingleChoiceItems(langarrayAdapter, saved_no, ((dialogInterface, i) -> {
                saved_no = i;

                //myLocale = new Locale(langarrayAdapter.getItem(i));
                language = (String) langarrayAdapter.getItem(i);

                switch_lang(language);

                Intent refresh = new Intent(this, MainActivity.class);
                startActivity(refresh);
                finish();


                Toast.makeText(getApplicationContext(), language, Toast.LENGTH_SHORT).show();
                dialogInterface.dismiss();
            }));
            builder.setNegativeButton(R.string.cancel_lang_switch_neg, (dialogInterface, i) -> {});
            AlertDialog dialog = builder.create();
            dialog.show();
            //dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#99FF99"));
            //dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#FF0000"));

            //Toast.makeText(getApplicationContext(), R.string.switch_lang_item, Toast.LENGTH_SHORT).show();
        }

        return super.onOptionsItemSelected(item);
    }

    public void switch_lang(String language) {
        myLocale = new Locale(language);
        Locale.setDefault(myLocale);
        Configuration conf = getResources().getConfiguration();
        conf.locale = myLocale;
        getResources().updateConfiguration(conf, getResources().getDisplayMetrics());
    }

    /**
     * when app restarted or maximized
     */
    @Override
    protected void onResume() {
        super.onResume();

        //Retrieve data from preference:
        //prefs = getSharedPreferences("SAVED_VALUES_Language", MODE_PRIVATE);

        //language = prefs.getString("USER_LANGUAGE", language);
        saved_no = prefs.getInt("saved_nmb", saved_no);

    }

    /**
     * when app closed or minimized
     */
    @Override
    protected void onPause() {
        super.onPause();

        //Setting values in Preference:
        SharedPreferences preferences = getSharedPreferences("SAVED_VALUES_Language", MODE_PRIVATE);
        @SuppressLint("CommitPrefEdits") SharedPreferences.Editor editor = preferences.edit();  // Put the values from the UI

        editor.putString("USER_LANGUAGE", language);
        editor.putInt("saved_nmb", saved_no);
        editor.apply();
    }
}