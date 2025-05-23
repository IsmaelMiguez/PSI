package es.udc.psi.pacman;

import android.media.MediaPlayer;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static MediaPlayer player;
    private TextView tvWelcome;
    private FirebaseAuth mAuth;
    private boolean isGuest = false;
    private Button btnSettings;
    private SettingsManager settingsManager;

    // Method to start activity for Play button
    public void showPlayScreen(View view) {
        Intent playIntent = new Intent(this, PlayActivity.class);
        startActivity(playIntent);
    }

    // Method to start activity for Settings button
    public void showSettingsScreen() {
        Intent settingsIntent = new Intent(this, SettingsActivity.class);
        startActivity(settingsIntent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Inicializar Settings Manager
        settingsManager = SettingsManager.getInstance(this);
        
        // Inicializar Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        
        // Configurar música
        player = MediaPlayer.create(this, R.raw.pacman_song);
        
        // Aplicar volumen según configuración
        settingsManager.applyMusicVolume(player);
        
        player.setLooping(true);
        
        // Solo reproducir si la música está habilitada
        if (settingsManager.isMusicEnabled()) {
            player.start();
        }
        
        // Configurar botón de configuración
        btnSettings = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(v -> showSettingsScreen());
        
        // Verificar estado del usuario
        checkUserStatus();
    }

    private void checkUserStatus() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Verificar si es un usuario invitado
            if (currentUser.isAnonymous()) {
                isGuest = true;
                // Podríamos añadir un TextView en el layout para mostrar mensaje de invitado
                // tvWelcome.setText("Modo Invitado");
            } else {
                // Obtener datos del usuario desde Firestore
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("users").document(currentUser.getUid())
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful() && task.getResult() != null) {
                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {
                                    // Podríamos mostrar el nombre del usuario
                                    String username = document.getString("username");
                                    // tvWelcome.setText("Bienvenido, " + username);
                                }
                            }
                        });
            }
        } else {
            // Si no hay usuario, redirigir a LoginActivity
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    private void signOut() {
        mAuth.signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    public static MediaPlayer getPlayer() {
        return player;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (player != null) {
            player.pause();
        }
    }

    @Override
    public void onResume() {
        Log.i("info", "MainActivity onResume");
        super.onResume();
        
        if (player != null && settingsManager.isMusicEnabled()) {
            // Aplicar volumen según configuración
            settingsManager.applyMusicVolume(player);
            player.start();
        }
        
        // Verificar estado del usuario cuando se regresa a la actividad
        checkUserStatus();
    }
}