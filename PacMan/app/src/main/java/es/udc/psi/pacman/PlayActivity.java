package es.udc.psi.pacman;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

public class PlayActivity extends Activity {
    static PlayActivity activity;
    private SharedPreferences sharedPreferences;
    private DrawingView drawingView;
    private Globals globals;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("info", MODE_PRIVATE);
        boolean useButtons = prefs.getBoolean("use_button_controls", false);

        if (useButtons) {
            setContentView(R.layout.activity_play_with_controls); // Este tendrá cruceta + DrawingView
            drawingView = findViewById(R.id.drawingView);
            setupControlButtons();
        } else {
            drawingView = new DrawingView(this);
            setContentView(drawingView);
        }

        activity = this;
        globals = Globals.getInstance();
        int temp = prefs.getInt("high_score", 0);
        globals.setHighScore(temp);
    }

    private void setupControlButtons() {
        findViewById(R.id.btnUp).setOnClickListener(v -> drawingView.setNextDirection(0));
        findViewById(R.id.btnRight).setOnClickListener(v -> drawingView.setNextDirection(1));
        findViewById(R.id.btnDown).setOnClickListener(v -> drawingView.setNextDirection(2));
        findViewById(R.id.btnLeft).setOnClickListener(v -> drawingView.setNextDirection(3));
    }



    @Override
    protected void onPause() {
        Log.i("info", "onPause");
        super.onPause();
        drawingView.pause();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("high_score", globals.getHighScore());
        editor.apply();
        // Removed MediaPlayer pause logic
    }

    @Override
    protected void onResume() {
        Log.i("info", "onResume");
        super.onResume();
        drawingView.resume();
        // Removed MediaPlayer start logic
    }

    public static PlayActivity getInstance() {
        return activity;
    }

}
