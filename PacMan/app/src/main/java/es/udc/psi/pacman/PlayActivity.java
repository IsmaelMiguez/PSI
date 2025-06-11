package es.udc.psi.pacman;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

public class PlayActivity extends Activity {
    private static PlayActivity activity;
    private DrawingView drawingView;
    private Globals globals;

    private static final String PREFS = "info";
    private boolean useButtonControls;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        useButtonControls = prefs.getBoolean("use_button_controls", false);

        if (useButtonControls) {
            setContentView(R.layout.activity_play_with_controls);
            drawingView = findViewById(R.id.drawingView);
            setupControlButtons();
        } else {
            drawingView = new DrawingView(this);
            setContentView(drawingView);
        }

        activity = this;
        globals  = Globals.getInstance();
        globals.setHighScore(prefs.getInt("high_score", 0));
    }

    private void setupControlButtons() {
        findViewById(R.id.btnUp)   .setOnClickListener(v -> drawingView.setNextDirection(0));
        findViewById(R.id.btnRight).setOnClickListener(v -> drawingView.setNextDirection(1));
        findViewById(R.id.btnDown) .setOnClickListener(v -> drawingView.setNextDirection(2));
        findViewById(R.id.btnLeft) .setOnClickListener(v -> drawingView.setNextDirection(3));
    }


    @Override
    protected void onPause() {
        Log.i("info", "onPause");
        super.onPause();

        if (drawingView != null) drawingView.pause();

        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        sp.edit()
                .putInt   ("high_score", globals.getHighScore())
                .putBoolean("use_button_controls", useButtonControls)
                .apply();
    }

    @Override
    protected void onResume() {
        Log.i("info", "onResume");
        super.onResume();

        if (drawingView != null) drawingView.resume();
    }


    @Override
    protected void onDestroy() {
        if (drawingView != null) drawingView.pause();
        super.onDestroy();
    }


    public static PlayActivity getInstance() { return activity; }
}