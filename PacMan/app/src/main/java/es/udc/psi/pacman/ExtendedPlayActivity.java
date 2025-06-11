package es.udc.psi.pacman;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.util.Log;
import com.google.android.material.snackbar.Snackbar;

public class ExtendedPlayActivity extends Activity {

    private static final String TAG = "PACMAN_NET";
    private static ExtendedPlayActivity instance;
    public static ExtendedPlayActivity get() { return instance; }

    private DrawingViewMulti drawing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;

        startService(new Intent(this, UdpServerService.class));

        drawing = new DrawingViewMulti(this);
        setContentView(drawing);

        String ip = NetUtils.getWifiIPv4(this);
        View root = findViewById(android.R.id.content);
        Snackbar.make(root,"Conéctate a "+ip+":55555",
                Snackbar.LENGTH_INDEFINITE).show();
        Log.i(TAG, "[SCREEN] IP publicada " + ip);
    }

    @Override protected void onResume() {              //  <<< CAMBIO
        super.onResume();
        if (drawing != null) drawing.resume();
    }

    @Override protected void onPause() {
        if (drawing != null) drawing.pause();
        super.onPause();
    }

    @Override protected void onDestroy() {
        if (drawing != null) drawing.pause();
        stopService(new Intent(this, UdpServerService.class));
        super.onDestroy();
    }

    /* callbacks del servidor UDP */
    public void onRemoteDirection(int player, int dir) {
        Log.i(TAG, "[SCREEN] Dir jugador "+player+" = "+dir);
        drawing.setNextDirection(player, dir);
    }

    public void onPlayersChanged(int count) {
        Log.i(TAG, "[SCREEN] Conectados "+count+" jugadores");
        drawing.resizePlayers(count);
    }
}
