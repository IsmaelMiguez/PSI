package es.udc.psi.pacman;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.util.Log;

import java.util.Locale;

/**
 * Clase para gestionar configuraciones globales de la aplicación.
 * Fuente única de verdad para todas las configuraciones.
 */
public class SettingsManager {
    private static final String TAG = "SettingsManager";
    private static final String PREFS_NAME = "PacManPrefs";
    private static final String HELP_PREFS_NAME = "info"; // Para compatibilidad con HelpActivity
    private static SettingsManager instance;
    private SharedPreferences preferences;
    private SharedPreferences helpPreferences; // Para sincronización
    private Context context;

    private SettingsManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.helpPreferences = context.getSharedPreferences(HELP_PREFS_NAME, Context.MODE_PRIVATE);

        // Aplicar idioma guardado al inicializar
        applyLanguage();
    }

    public static synchronized SettingsManager getInstance(Context context) {
        if (instance == null) {
            instance = new SettingsManager(context);
        }
        return instance;
    }

        // === MÉTODOS DE IDIOMA ===
    
    /**
     * Obtiene el idioma configurado
     */
    public String getLanguage() {
        return preferences.getString("language", "es"); // Español por defecto
    }
    
    /**
     * Establece el idioma de la aplicación
     */
    public void setLanguage(String languageCode) {
        preferences.edit().putString("language", languageCode).apply();
        applyLanguage();
        Log.d(TAG, "Language set to: " + languageCode);
    }
    
    /**
     * Aplica el idioma configurado
     */
    private void applyLanguage() {
        String languageCode = getLanguage();
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        
        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }
    
    /**
     * Obtiene el contexto con el idioma aplicado
     */
    public Context getLocalizedContext() {
        String languageCode = getLanguage();
        Locale locale = new Locale(languageCode);
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        return context.createConfigurationContext(config);
    }

    // === MÉTODOS DE AUDIO ===
    
    /**
     * Obtiene el volumen de música establecido (0-100)
     */
    public int getMusicVolume() {
        return preferences.getInt("musicVolume", 100);
    }
    
    /**
     * Establece el volumen de música (0-100)
     */
    public void setMusicVolume(int volume) {
        preferences.edit().putInt("musicVolume", volume).apply();
        Log.d(TAG, "Music volume set to: " + volume);
    }

    /**
     * Aplica el volumen a un MediaPlayer
     */
    public void applyMusicVolume(MediaPlayer player) {
        if (player != null) {
            float volume = getMusicVolume() / 100f;
            player.setVolume(volume, volume);
        }
    }

    /**
     * Comprueba si la música está habilitada
     */
    public boolean isMusicEnabled() {
        return preferences.getBoolean("musicEnabled", true);
    }
    
    /**
     * Establece si la música está habilitada
     */
    public void setMusicEnabled(boolean enabled) {
        preferences.edit().putBoolean("musicEnabled", enabled).apply();
        Log.d(TAG, "Music enabled set to: " + enabled);
    }

    /**
     * Comprueba si los efectos de sonido están habilitados
     */
    public boolean areSoundEffectsEnabled() {
        return preferences.getBoolean("soundEffectsEnabled", true);
    }
    
    /**
     * Establece si los efectos de sonido están habilitados
     */
    public void setSoundEffectsEnabled(boolean enabled) {
        preferences.edit().putBoolean("soundEffectsEnabled", enabled).apply();
        Log.d(TAG, "Sound effects enabled set to: " + enabled);
    }

    // === MÉTODOS DE VIBRACIÓN ===
    
    /**
     * Comprueba si la vibración está habilitada
     */
    public boolean isVibrationEnabled() {
        return preferences.getBoolean("vibrationEnabled", true);
    }
    
    /**
     * Establece si la vibración está habilitada
     */
    public void setVibrationEnabled(boolean enabled) {
        preferences.edit().putBoolean("vibrationEnabled", enabled).apply();
        Log.d(TAG, "Vibration enabled set to: " + enabled);
    }

    /**
     * Ejecuta una vibración si está habilitada
     * @param milliseconds duración de la vibración
     */
    public void vibrate(long milliseconds) {
        if (isVibrationEnabled()) {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(milliseconds);
            }
        }
    }

    // === MÉTODOS DE CONTROLES ===

    /**
     * Obtiene el tipo de control (buttons o swipe) - con sincronización automática
     */
    public String getControlType() {
        String controlType = preferences.getString("controlType", null);
        
        if (controlType == null) {
            // Si no existe, revisar en el sistema de HelpActivity para migración
            boolean useButtonControls = helpPreferences.getBoolean("use_button_controls", false);
            controlType = useButtonControls ? "buttons" : "swipe";
            
            // Migrar al sistema principal
            setControlType(controlType);
            Log.d(TAG, "Migrated control type from HelpActivity: " + controlType);
        }
        
        return controlType;
    }

    /**
     * Comprueba si el control es por botones - con sincronización automática
     */
    public boolean isButtonControl() {
        return "buttons".equals(getControlType());
    }

    /**
     * Establece el tipo de control y sincroniza automáticamente
     */
    public void setControlType(String controlType) {
        // Guardar en sistema principal
        preferences.edit().putString("controlType", controlType).apply();
        
        // Sincronizar con sistema de HelpActivity para compatibilidad
        boolean isButtons = "buttons".equals(controlType);
        helpPreferences.edit().putBoolean("use_button_controls", isButtons).apply();
        
        Log.d(TAG, "Control type set to: " + controlType + " (isButtons: " + isButtons + ")");
    }

    // === MÉTODOS DE DIFICULTAD ===

    /**
     * Obtiene el nivel de dificultad (easy, normal, hard)
     */
    public String getDifficulty() {
        return preferences.getString("difficulty", "normal");
    }
    
    /**
     * Establece el nivel de dificultad
     */
    public void setDifficulty(String difficulty) {
        preferences.edit().putString("difficulty", difficulty).apply();
        Log.d(TAG, "Difficulty set to: " + difficulty);
    }

    /**
     * Obtiene un multiplicador de dificultad (0.8 para fácil, 1.0 para normal, 1.3 para difícil)
     */
    public float getDifficultyMultiplier() {
        switch (getDifficulty()) {
            case "easy":
                return 0.8f;
            case "hard":
                return 1.3f;
            default:
                return 1.0f;
        }
    }
}