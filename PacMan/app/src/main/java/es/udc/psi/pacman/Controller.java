package es.udc.psi.pacman;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Controller extends AppCompatActivity {

    private static final String TAG = "PacmanController";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_controller);

        // Aplica el padding para el sistema (status bar, navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.controller), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializa botones y asigna eventos
        Button btnUp = findViewById(R.id.btnUp);
        Button btnDown = findViewById(R.id.btnDown);
        Button btnLeft = findViewById(R.id.btnLeft);
        Button btnRight = findViewById(R.id.btnRight);

        btnUp.setOnClickListener(v -> move("UP"));
        btnDown.setOnClickListener(v -> move("DOWN"));
        btnLeft.setOnClickListener(v -> move("LEFT"));
        btnRight.setOnClickListener(v -> move("RIGHT"));
    }

    private void move(String direction) {
        // Aquí podrías comunicarte con el motor del juego o enviar eventos
        Log.d(TAG, "Direction pressed: " + direction);
    }
}
