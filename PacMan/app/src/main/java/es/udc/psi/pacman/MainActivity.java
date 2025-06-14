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

import es.udc.psi.pacman.data.FirestoreManager;
import es.udc.psi.pacman.data.models.Puntuacion;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static MediaPlayer player;
    private TextView tvWelcome;
    private FirebaseAuth mAuth;
    private boolean isGuest = false;
    private Button btnSettings;
    private SettingsManager settingsManager;
    private Button btnRankings;

    // Method to start activity for Play button
    public void showPlayScreen(View view) {
        Intent playIntent = new Intent(this, PlayActivity.class);
        startActivity(playIntent);
    }

    public void showExtendedMenu(View v){
        startActivity(new Intent(this, ExtendedModeMenuActivity.class));
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
        
        // Inicializar elementos de la UI
        tvWelcome = findViewById(R.id.tvWelcome);
        btnSettings = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(v -> showSettingsScreen());
        
        btnRankings = findViewById(R.id.btnRankings);
        btnRankings.setOnClickListener(v -> showRankingsScreen());
        
        // Verificar estado del usuario
        checkUserStatus();
    }

    private void showRankingsScreen() {
        Intent intent = new Intent(this, RankingActivity.class);
        startActivity(intent);
    }

    private void checkUserStatus() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Primero mostrar un mensaje genérico mientras cargamos datos
            tvWelcome.setText(getString(R.string.loading_user_data));
            
            // Verificar si es un usuario invitado
            if (currentUser.isAnonymous()) {
                isGuest = true;
                tvWelcome.setText(getString(R.string.welcome_guest));
                Log.d(TAG, "Usuario anónimo identificado");
            } else {
                // Para un usuario autenticado, primero intentamos mostrar la información local disponible
                String displayName = currentUser.getDisplayName();
                String email = currentUser.getEmail();
                
                // Mostrar nombre por defecto mientras se carga Firestore
                if (displayName != null && !displayName.isEmpty()) {
                    tvWelcome.setText(getString(R.string.welcome_user, displayName));
                    Log.d(TAG, "Usando displayName: " + displayName);
                } else if (email != null) {
                    tvWelcome.setText(getString(R.string.welcome_user, email));
                    Log.d(TAG, "Usando email: " + email);
                }
                
                // Obtener datos del usuario desde Firestore
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("users").document(currentUser.getUid())
                        .get()
                        .addOnSuccessListener(document -> {
                            if (document.exists()) {
                                // Mostrar el nombre del usuario de Firestore
                                String username = document.getString("username");
                                Log.d(TAG, "Datos de Firestore: username=" + username);
                                
                                if (username != null && !username.isEmpty()) {
                                    tvWelcome.setText(getString(R.string.welcome_user, username));
                                    Log.d(TAG, "Mensaje actualizado con username de Firestore");
                                }
                            } else {
                                Log.d(TAG, "No se encontraron datos del usuario en Firestore");
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error al obtener datos de usuario", e);
                        });
            }
        } else {
            // Si no hay usuario, redirigir a LoginActivity
            Log.d(TAG, "No hay usuario autenticado, redirigiendo a login");
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