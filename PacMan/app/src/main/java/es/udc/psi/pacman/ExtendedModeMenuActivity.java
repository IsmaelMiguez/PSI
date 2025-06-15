package es.udc.psi.pacman;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

public class ExtendedModeMenuActivity extends AppCompatActivity {

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_extended_mode_menu);
    }

    // Botón “Usar como PANTALLA”
    public void startAsHost(View v) {
        startActivity(new Intent(this, ExtendedPlayActivity.class));
    }

    // Botón “Usar como MANDO”
    public void startAsController(View v) {
        // Pide la IP del host con un diálogo sencillo.
        IpDialog.show(this, ip ->
                startActivity(new Intent(this, Controller.class)
                        .putExtra("HOST_IP", ip)));
    }
}
