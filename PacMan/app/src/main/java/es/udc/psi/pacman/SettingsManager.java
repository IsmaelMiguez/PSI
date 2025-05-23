package es.udc.psi.pacman;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Vibrator;

/**
 * Clase para gestionar configuraciones globales de la aplicación.
 */
public class SettingsManager {
    private static final String PREFS_NAME = "PacManPrefs";
    private static SettingsManager instance;
    private SharedPreferences preferences;
    private Context context;

    private SettingsManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SettingsManager getInstance(Context context) {
        if (instance == null) {
            instance = new SettingsManager(context);
        }
        return instance;
    }

    /**
     * Obtiene el volumen de música establecido (0-100)
     */
    public int getMusicVolume() {
        return preferences.getInt("musicVolume", 100);
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
     * Comprueba si los efectos de sonido están habilitados
     */
    public boolean areSoundEffectsEnabled() {
        return preferences.getBoolean("soundEffectsEnabled", true);
    }

    /**
     * Comprueba si la vibración está habilitada
     */
    public boolean isVibrationEnabled() {
        return preferences.getBoolean("vibrationEnabled", true);
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

    /**
     * Obtiene el tipo de control (buttons o swipe)
     */
    public String getControlType() {
        return preferences.getString("controlType", "buttons");
    }

    /**
     * Comprueba si el control es por botones
     */
    public boolean isButtonControl() {
        return "buttons".equals(getControlType());
    }

    /**
     * Obtiene el nivel de dificultad (easy, normal, hard)
     */
    public String getDifficulty() {
        return preferences.getString("difficulty", "normal");
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