package es.udc.psi.pacman;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.widget.Switch;
import android.content.SharedPreferences;

public class HelpActivity extends Activity {
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        MainActivity.getPlayer().start();

        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch controlSwitch = findViewById(R.id.controlswitch);
        SharedPreferences prefs = getSharedPreferences("info", MODE_PRIVATE);
        boolean isButtonMode = prefs.getBoolean("use_button_controls", false);
        controlSwitch.setChecked(isButtonMode);

        controlSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("use_button_controls", isChecked);
            editor.apply();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        MainActivity.getPlayer().pause();
    }

}