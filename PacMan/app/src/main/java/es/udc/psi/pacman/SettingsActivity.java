package es.udc.psi.pacman;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";
    private static final String PREFS_NAME = "PacManPrefs";
    
    // UI Components
    private TextView tvUserInfo;
    private Button btnLogout, btnDeleteAccount, btnBack;
    private ProgressBar progressBar;
    private SeekBar sbMusicVolume;
    private Switch switchMusic, switchSoundEffects, switchVibration;
    private RadioGroup rgControlType, rgDifficulty;
    private RadioButton rbButtons, rbSwipe, rbEasy, rbNormal, rbHard;
    private Switch controlSwitch;  // Control switch de HelpActivity
    
    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    
    // Preferences
    private SharedPreferences settings;
    private SharedPreferences controlPrefs;  // Para el switch de controles de HelpActivity
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        // Inicializar Firebase Auth y Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        // Inicializar SharedPreferences
        settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        controlPrefs = getSharedPreferences("info", MODE_PRIVATE);  // Preferencias de Help
        
        // Vincular vistas
        initViews();
        
        // Verificar si hay usuario logueado
        if (mAuth.getCurrentUser() == null) {
            // Redirigir al login si no hay usuario
            startLoginActivity();
            return;
        }
        
        // Configurar listeners
        setUpListeners();
        
        // Cargar configuraciones guardadas
        loadSettings();
        
        // Mostrar información del usuario
        displayUserInfo();
        
        // Configurar el switch de control de la sección de ayuda
        setupHelpControls();
    }
    
    private void initViews() {
        // User section
        tvUserInfo = findViewById(R.id.tvUserInfo);
        btnLogout = findViewById(R.id.btnLogout);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
        btnBack = findViewById(R.id.btnBack);
        progressBar = findViewById(R.id.progressBar);
        
        // Audio section
        sbMusicVolume = findViewById(R.id.sbMusicVolume);
        switchMusic = findViewById(R.id.switchMusic);
        switchSoundEffects = findViewById(R.id.switchSoundEffects);
        
        // Controls section
        rgControlType = findViewById(R.id.rgControlType);
        rbButtons = findViewById(R.id.rbButtons);
        rbSwipe = findViewById(R.id.rbSwipe);
        switchVibration = findViewById(R.id.switchVibration);
        
        // Difficulty section
        rgDifficulty = findViewById(R.id.rgDifficulty);
        rbEasy = findViewById(R.id.rbEasy);
        rbNormal = findViewById(R.id.rbNormal);
        rbHard = findViewById(R.id.rbHard);
        
        // Help section
        controlSwitch = findViewById(R.id.controlswitch);
    }
    
    private void setUpListeners() {
        btnLogout.setOnClickListener(v -> logoutUser());
        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountConfirmation());
        btnBack.setOnClickListener(v -> {
            saveSettings();
            finish();
        });
        
        // Easter egg: vibrar al cambiar el switch de vibración
        switchVibration.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null && vibrator.hasVibrator()) {
                    vibrator.vibrate(200);
                }
            }
        });
        
        // Ajustar volumen de música en tiempo real
        sbMusicVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && MainActivity.getPlayer() != null) {
                    float volume = progress / 100f;
                    MainActivity.getPlayer().setVolume(volume, volume);
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // No se necesita implementación
            }
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // No se necesita implementación
            }
        });
        
        // Pausar/reanudar música
        switchMusic.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (MainActivity.getPlayer() != null) {
                if (isChecked) {
                    MainActivity.getPlayer().start();
                } else {
                    MainActivity.getPlayer().pause();
                }
            }
        });
    }

    private void setupHelpControls() {
        // Cargar preferencia guardada
        boolean isButtonMode = controlPrefs.getBoolean("use_button_controls", false);
        controlSwitch.setChecked(isButtonMode);

        // Configurar listener
        controlSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = controlPrefs.edit();
            editor.putBoolean("use_button_controls", isChecked);
            editor.apply();
        });
    }
    
    private void loadSettings() {
        // Cargar configuraciones de SharedPreferences
        int musicVolume = settings.getInt("musicVolume", 100);
        boolean musicEnabled = settings.getBoolean("musicEnabled", true);
        boolean soundEffectsEnabled = settings.getBoolean("soundEffectsEnabled", true);
        boolean vibrationEnabled = settings.getBoolean("vibrationEnabled", true);
        String controlType = settings.getString("controlType", "buttons");
        String difficulty = settings.getString("difficulty", "normal");
        
        // Aplicar configuraciones a la UI
        sbMusicVolume.setProgress(musicVolume);
        switchMusic.setChecked(musicEnabled);
        switchSoundEffects.setChecked(soundEffectsEnabled);
        switchVibration.setChecked(vibrationEnabled);
        
        // Control type
        if ("buttons".equals(controlType)) {
            rbButtons.setChecked(true);
        } else {
            rbSwipe.setChecked(true);
        }
        
        // Difficulty
        switch (difficulty) {
            case "easy":
                rbEasy.setChecked(true);
                break;
            case "hard":
                rbHard.setChecked(true);
                break;
            default:
                rbNormal.setChecked(true);
                break;
        }
    }
    
    private void saveSettings() {
        SharedPreferences.Editor editor = settings.edit();
        
        // Guardar configuraciones actuales
        editor.putInt("musicVolume", sbMusicVolume.getProgress());
        editor.putBoolean("musicEnabled", switchMusic.isChecked());
        editor.putBoolean("soundEffectsEnabled", switchSoundEffects.isChecked());
        editor.putBoolean("vibrationEnabled", switchVibration.isChecked());
        
        // Control type
        String controlType = rbButtons.isChecked() ? "buttons" : "swipe";
        editor.putString("controlType", controlType);
        
        // Difficulty
        String difficulty;
        if (rbEasy.isChecked()) {
            difficulty = "easy";
        } else if (rbHard.isChecked()) {
            difficulty = "hard";
        } else {
            difficulty = "normal";
        }
        editor.putString("difficulty", difficulty);
        
        editor.apply();
        
        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
    }
    
    private void displayUserInfo() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            if (user.isAnonymous()) {
                // Si es usuario invitado
                tvUserInfo.setText(getString(R.string.guest_user_info));
                
                // Ocultar botón de eliminar cuenta para invitados
                btnDeleteAccount.setVisibility(View.GONE);
            } else {
                // Si es usuario registrado
                String displayName = user.getDisplayName();
                String email = user.getEmail();
                
                tvUserInfo.setText(getString(R.string.user_info, displayName, email));
                
                // Mostrar botón de eliminar cuenta para usuarios registrados
                btnDeleteAccount.setVisibility(View.VISIBLE);
            }
        }
    }
    
    private void logoutUser() {
        progressBar.setVisibility(View.VISIBLE);
        mAuth.signOut();
        Toast.makeText(this, getString(R.string.logged_out), Toast.LENGTH_SHORT).show();
        startLoginActivity();
    }
    
    private void showDeleteAccountConfirmation() {
        FirebaseUser user = mAuth.getCurrentUser();
        
        if (user == null) return;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.delete_account));
        builder.setMessage(getString(R.string.confirm_delete_account));
        
        // Si es un usuario por email, solicitar reautenticación
        if (!user.isAnonymous()) {
            builder.setPositiveButton(getString(R.string.confirm), (dialog, which) -> {
                showReauthenticateDialog();
            });
        } else {
            // Para usuarios invitados, eliminar directamente
            builder.setPositiveButton(getString(R.string.confirm), (dialog, which) -> {
                deleteAccount();
            });
        }
        
        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
            dialog.cancel();
        });
        
        builder.show();
    }
    
    private void showReauthenticateDialog() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) return;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.reauth_required));
        
        // Configurar un campo para la contraseña
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint(getString(R.string.password));
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);
        layout.addView(input);
        
        builder.setView(layout);
        
        builder.setPositiveButton(getString(R.string.confirm), (dialog, which) -> {
            String password = input.getText().toString().trim();
            if (!password.isEmpty()) {
                reauthenticateUser(user.getEmail(), password);
            }
        });
        
        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
            dialog.cancel();
        });
        
        builder.show();
    }
    
    private void reauthenticateUser(String email, String password) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        
        progressBar.setVisibility(View.VISIBLE);
        
        AuthCredential credential = EmailAuthProvider.getCredential(email, password);
        
        user.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Reautenticación exitosa, proceder a eliminar la cuenta
                        deleteAccount();
                    } else {
                        // Error en la reautenticación
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, getString(R.string.error_auth_failed) + ": " + 
                                task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void deleteAccount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        
        progressBar.setVisibility(View.VISIBLE);
        
        // Eliminar primero la cuenta de autenticación
        user.delete()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Eliminar los datos de Firestore después de eliminar la autenticación
                        // (Esto puede fallar si las reglas no lo permiten, pero el usuario ya estará eliminado)
                        try {
                            db.collection("users").document(user.getUid())
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "User data deleted from Firestore");
                                    })
                                    .addOnFailureListener(e -> {
                                        // Esto podría fallar, pero no bloqueamos el proceso
                                        Log.w(TAG, "Error deleting user data", e);
                                    });
                        } catch (Exception e) {
                            Log.w(TAG, "Error trying to delete user data", e);
                        }

                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(SettingsActivity.this,
                                getString(R.string.account_deleted),
                                Toast.LENGTH_SHORT).show();
                        startLoginActivity();
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(SettingsActivity.this,
                                getString(R.string.error_delete_account) + ": " +
                                        task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void startLoginActivity() {
        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
    
    @Override
    public void onBackPressed() {
        saveSettings();
        super.onBackPressed();
    }
}