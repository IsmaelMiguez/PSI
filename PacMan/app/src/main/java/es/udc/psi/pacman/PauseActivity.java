package es.udc.psi.pacman;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class PauseActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pause);
        // Removed MediaPlayer start logic
    }

    // Method to start activity for Help button
    public void showSettingsScreen(View view) {
        Intent helpIntent = new Intent(this, SettingsActivity.class);
        startActivity(helpIntent);
    }

    // Method to start activity for Play button
    public void showPlayScreen(View view) {
        Intent playIntent = new Intent(this, PlayActivity.class);
        startActivity(playIntent);
        PlayActivity.getInstance().finish();
        this.finish();
    }

    // Method to resume the game
    public void resumeGame(View view) {
        Intent resumeIntent = new Intent(this, PlayActivity.class);
        resumeIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(resumeIntent);
        this.finish();
    }

}
