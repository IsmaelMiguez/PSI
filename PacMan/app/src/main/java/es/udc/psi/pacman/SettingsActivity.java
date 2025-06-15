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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import es.udc.psi.pacman.data.FirestoreManager;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";
    
    // UI Components
    private TextView tvUserInfo;
    private Button btnLogout, btnDeleteAccount, btnBack;
    private ProgressBar progressBar;
    private SeekBar sbMusicVolume;
    private Switch switchMusic, switchSoundEffects, switchVibration;
    private RadioGroup rgControlType, rgDifficulty;
    private RadioButton rbButtons, rbSwipe, rbEasy, rbNormal, rbHard;

    private RadioGroup rgLanguage;
    private RadioButton rbSpanish, rbEnglish;
    
    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    
    // Settings Manager
    private SettingsManager settingsManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        // Inicializar SettingsManager
        settingsManager = SettingsManager.getInstance(this);
        
        // Inicializar Firebase Auth y Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        // Iniciar vistas
        initViews();
        
        // Verificar si hay usuario logueado
        if (mAuth.getCurrentUser() == null) {
            startLoginActivity();
            return;
        }
        
        // Configurar listeners
        setUpListeners();
        
        // Cargar configuraciones guardadas
        loadSettings();

        // Mostrar información del usuario
        displayUserInfo();
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
        switchVibration = findViewById(R.id.switchVibration);
        
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

        // Language section
        rgLanguage = findViewById(R.id.rgLanguage);
        rbSpanish = findViewById(R.id.rbSpanish);
        rbEnglish = findViewById(R.id.rbEnglish);
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
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
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
        
        // Listener para cambios en el tipo de control
        rgControlType.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isButtons = checkedId == R.id.rbButtons;
            Log.d(TAG, "Control type changed to: " + (isButtons ? "buttons" : "swipe"));
        });
    }
    
    private void loadSettings() {
        // Cargar configuraciones usando SettingsManager
        int musicVolume = settingsManager.getMusicVolume();
        boolean musicEnabled = settingsManager.isMusicEnabled();
        boolean soundEffectsEnabled = settingsManager.areSoundEffectsEnabled();
        boolean vibrationEnabled = settingsManager.isVibrationEnabled();
        boolean isButtonControl = settingsManager.isButtonControl();
        String difficulty = settingsManager.getDifficulty();

        String language = settingsManager.getLanguage();
        
        // Aplicar configuraciones a la UI
        sbMusicVolume.setProgress(musicVolume);
        switchMusic.setChecked(musicEnabled);
        switchSoundEffects.setChecked(soundEffectsEnabled);
        switchVibration.setChecked(vibrationEnabled);
        
        // Control type
        if (isButtonControl) {
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

        // Language
        switch (language) {
            case "en":
                rbEnglish.setChecked(true);
                break;
            default:
                rbSpanish.setChecked(true);
                break;
        }

    }
    
    private void saveSettings() {
        // Guardar configuraciones usando SettingsManager
        settingsManager.setMusicVolume(sbMusicVolume.getProgress());
        settingsManager.setMusicEnabled(switchMusic.isChecked());
        settingsManager.setSoundEffectsEnabled(switchSoundEffects.isChecked());
        settingsManager.setVibrationEnabled(switchVibration.isChecked());
        
        // Control type
        String controlType = rbButtons.isChecked() ? "buttons" : "swipe";
        settingsManager.setControlType(controlType);
        
        // Difficulty
        String difficulty;
        if (rbEasy.isChecked()) {
            difficulty = "easy";
        } else if (rbHard.isChecked()) {
            difficulty = "hard";
        } else {
            difficulty = "normal";
        }
        settingsManager.setDifficulty(difficulty);

        // Language
        String language = rbEnglish.isChecked() ? "en" : "es";
        String currentLanguage = settingsManager.getLanguage();
        
        if (!language.equals(currentLanguage)) {
            settingsManager.setLanguage(language);
            // Recrear la actividad para aplicar el nuevo idioma
            recreate();
            return;
        }
        
        Log.d(TAG, "Settings saved - Control type: " + controlType + ", Language: " + language);
        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
    }
    
    private void displayUserInfo() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            if (user.isAnonymous()) {
                tvUserInfo.setText(getString(R.string.guest_user_info));
                btnDeleteAccount.setVisibility(View.GONE);
            } else {
                String displayName = user.getDisplayName();
                String email = user.getEmail();
                tvUserInfo.setText(getString(R.string.user_info, displayName, email));
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
        
        if (!user.isAnonymous()) {
            builder.setPositiveButton(getString(R.string.confirm), (dialog, which) -> {
                showGameDataDeletionDialog();
            });
        } else {
            builder.setPositiveButton(getString(R.string.confirm), (dialog, which) -> {
                showGameDataDeletionDialog();
            });
        }
        
        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
            dialog.cancel();
        });
        
        builder.show();
    }

    private void showGameDataDeletionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Eliminar datos de juego");
        builder.setMessage("¿Deseas eliminar también todas tus partidas y tu posición en los rankings?\n\nEsta acción no se puede deshacer.");
        
        builder.setPositiveButton("Sí, eliminar todo", (dialog, which) -> {
            if (mAuth.getCurrentUser() != null && !mAuth.getCurrentUser().isAnonymous()) {
                showReauthenticateDialog(true);
            } else {
                deleteAccountWithGameData(true);
            }
        });
        
        builder.setNegativeButton("No, solo la cuenta", (dialog, which) -> {
            if (mAuth.getCurrentUser() != null && !mAuth.getCurrentUser().isAnonymous()) {
                showReauthenticateDialog(false);
            } else {
                deleteAccountWithGameData(false);
            }
        });
        
        builder.setNeutralButton("Cancelar", (dialog, which) -> {
            dialog.cancel();
        });
        
        builder.show();
    }
    
    private void showReauthenticateDialog(boolean deleteGameData) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.isAnonymous()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.reauthenticate));
        builder.setMessage(getString(R.string.enter_password_to_delete));

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint(getString(R.string.password));
        builder.setView(input);

        builder.setPositiveButton(getString(R.string.confirm), (dialog, which) -> {
            String password = input.getText().toString().trim();
            if (!password.isEmpty()) {
                reauthenticateAndDelete(password, deleteGameData);
            } else {
                Toast.makeText(this, getString(R.string.password_required), Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
            dialog.cancel();
        });

        builder.show();
    }

    private void reauthenticateAndDelete(String password, boolean deleteGameData) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        
        String email = user.getEmail();
        if (email == null) {
            Toast.makeText(this, "Error: No se pudo obtener el email del usuario", Toast.LENGTH_SHORT).show();
            return;
        }
        
        progressBar.setVisibility(View.VISIBLE);
        
        AuthCredential credential = EmailAuthProvider.getCredential(email, password);
        
        user.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        deleteAccountWithGameData(deleteGameData);
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, getString(R.string.error_auth_failed) + ": " + 
                                task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void reauthenticateUser(String email, String password) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        
        progressBar.setVisibility(View.VISIBLE);
        
        AuthCredential credential = EmailAuthProvider.getCredential(email, password);
        
        user.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        deleteAccount();
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, getString(R.string.error_auth_failed) + ": " + 
                                task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void deleteAccount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        
        String userId = user.getUid();
        progressBar.setVisibility(View.VISIBLE);
        
        db.collection("users").document(userId)
                .delete()
                .addOnCompleteListener(userDataTask -> {
                    user.delete()
                            .addOnCompleteListener(authTask -> {
                                progressBar.setVisibility(View.GONE);
                                
                                if (authTask.isSuccessful()) {
                                    Toast.makeText(SettingsActivity.this,
                                            getString(R.string.account_deleted),
                                            Toast.LENGTH_SHORT).show();
                                    startLoginActivity();
                                } else {
                                    Toast.makeText(SettingsActivity.this,
                                            getString(R.string.error_delete_account) + ": " +
                                                    authTask.getException().getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                });
    }

    private void deleteAccountWithGameData(boolean deleteGameData) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        
        progressBar.setVisibility(View.VISIBLE);
        
        if (deleteGameData) {
            eliminarDatosDeJuegoCompletamente(user.getUid());
        } else {
            proceedWithAccountDeletion(user);
        }
    }

    private void eliminarDatosDeJuegoCompletamente(String userId) {
        db.collection("puntuaciones")
                .whereEqualTo("idJugador", userId)
                .get()
                .addOnCompleteListener(puntuacionesTask -> {
                    if (puntuacionesTask.isSuccessful()) {
                        Log.d(TAG, "Encontradas " + puntuacionesTask.getResult().size() + " puntuaciones");
                        eliminarPuntuacionesIndividualmente(puntuacionesTask.getResult().getDocuments(), userId);
                    } else {
                        Log.w(TAG, "Error obteniendo puntuaciones", puntuacionesTask.getException());
                        proceedWithAccountDeletion(mAuth.getCurrentUser());
                    }
                });
    }

    private void eliminarPuntuacionesIndividualmente(List<DocumentSnapshot> puntuaciones, String userId) {
        AtomicInteger pendingDeletions = new AtomicInteger(puntuaciones.size());
        Set<String> partidasClasicas = new HashSet<>();
        
        if (puntuaciones.isEmpty()) {
            eliminarPartidasClasicas(partidasClasicas, userId);
            return;
        }
        
        for (DocumentSnapshot puntuacion : puntuaciones) {
            String modoJuego = puntuacion.getString("modoJuego");
            String idPartida = puntuacion.getString("idPartida");
            if ("clasico".equals(modoJuego) && idPartida != null) {
                partidasClasicas.add(idPartida);
            }
            
            puntuacion.getReference().delete()
                    .addOnCompleteListener(deleteTask -> {
                        if (deleteTask.isSuccessful()) {
                            Log.d(TAG, "Puntuación eliminada: " + puntuacion.getId());
                        } else {
                            Log.w(TAG, "Error eliminando puntuación: " + puntuacion.getId());
                        }
                        
                        if (pendingDeletions.decrementAndGet() == 0) {
                            eliminarPartidasClasicas(partidasClasicas, userId);
                        }
                    });
        }
    }

    private void eliminarPartidasClasicas(Set<String> partidasClasicas, String userId) {
        AtomicInteger pendingDeletions = new AtomicInteger(partidasClasicas.size());
        
        Log.d(TAG, "Eliminando " + partidasClasicas.size() + " partidas clásicas");
        
        if (partidasClasicas.isEmpty()) {
            proceedWithAccountDeletion(mAuth.getCurrentUser());
            return;
        }
        
        for (String partidaId : partidasClasicas) {
            db.collection("partidas").document(partidaId)
                    .delete()
                    .addOnCompleteListener(deleteTask -> {
                        if (deleteTask.isSuccessful()) {
                            Log.d(TAG, "Partida eliminada: " + partidaId);
                        } else {
                            Log.w(TAG, "Error eliminando partida: " + partidaId);
                        }
                        
                        if (pendingDeletions.decrementAndGet() == 0) {
                            proceedWithAccountDeletion(mAuth.getCurrentUser());
                        }
                    });
        }
    }

    private void proceedWithAccountDeletion(FirebaseUser user) {
        if (user == null) {
            progressBar.setVisibility(View.GONE);
            return;
        }
        
        String userId = user.getUid();
        
        db.collection("users").document(userId)
                .delete()
                .addOnCompleteListener(userDataTask -> {
                    if (userDataTask.isSuccessful()) {
                        Log.d(TAG, "User data deleted from users collection");
                    } else {
                        Log.w(TAG, "Error deleting user data from users collection", userDataTask.getException());
                    }
                    
                    user.delete()
                            .addOnCompleteListener(authTask -> {
                                progressBar.setVisibility(View.GONE);
                                
                                if (authTask.isSuccessful()) {
                                    Toast.makeText(SettingsActivity.this,
                                            getString(R.string.account_deleted),
                                            Toast.LENGTH_SHORT).show();
                                    startLoginActivity();
                                } else {
                                    Toast.makeText(SettingsActivity.this,
                                            getString(R.string.error_delete_account) + ": " +
                                                    authTask.getException().getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
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