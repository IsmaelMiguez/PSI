package es.udc.psi.pacman;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;

public class HelpActivity extends Activity {
    
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        MainActivity.getPlayer().start();
        
        // Ya no hay switch de controles aquí - toda la configuración está centralizada en SettingsActivity
    }

    @Override
    protected void onPause() {
        super.onPause();
        MainActivity.getPlayer().pause();
    }
}