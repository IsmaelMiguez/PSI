package es.udc.psi.pacman;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class ExtendedPlayActivity extends Activity {

    private static ExtendedPlayActivity instance;
    private DrawingViewMulti drawing;

    public static ExtendedPlayActivity get() { return instance; }

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        instance = this;

        // Arranca el servidor UDP en segundo plano
        startService(new Intent(this, UdpServerService.class));

        drawing = new DrawingViewMulti(this);
        setContentView(drawing);
    }

    /** Recibido desde el servicio cuando un mando envía dirección */
    public void onRemoteDirection(int playerId, int dir) {
        drawing.setNextDirection(playerId, dir);
    }

    @Override protected void onDestroy() {
        stopService(new Intent(this, UdpServerService.class));
        super.onDestroy();
    }
}
