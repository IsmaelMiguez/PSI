package es.udc.psi.pacman;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.udc.psi.pacman.data.FirestoreManager;
import es.udc.psi.pacman.data.models.Partida;
import es.udc.psi.pacman.data.models.Puntuacion;

public class GameSession {
    private static final String TAG = "GameSession";
    
    private long startTime;
    private String modoJuego; // "clasico" o "cooperativo"
    private int nivel;
    private FirestoreManager firestoreManager;
    private List<PlayerScore> playerScores;
    
    public static class PlayerScore {
        public String playerId;
        public String playerName;
        public int score;
        public int lives;
        
        public PlayerScore(String playerId, String playerName, int score, int lives) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.score = score;
            this.lives = lives;
        }
    }
    
    public GameSession(String modoJuego, int nivel) {
        this.startTime = System.currentTimeMillis();
        this.modoJuego = modoJuego;
        this.nivel = nivel;
        this.firestoreManager = new FirestoreManager();
        this.playerScores = new ArrayList<>();
        
        // Agregar el jugador principal
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String playerName = currentUser.getDisplayName();
            if (playerName == null || playerName.isEmpty()) {
                playerName = currentUser.isAnonymous() ? "Invitado" : currentUser.getEmail();
            }
            addPlayer(currentUser.getUid(), playerName, 0, 3);
        }
    }
    
    public void addPlayer(String playerId, String playerName, int score, int lives) {
        playerScores.add(new PlayerScore(playerId, playerName, score, lives));
    }
    
    public void updatePlayerScore(String playerId, int newScore, int lives) {
        for (PlayerScore player : playerScores) {
            if (player.playerId.equals(playerId)) {
                player.score = newScore;
                player.lives = lives;
                break;
            }
        }
    }
    
    public void updateMainPlayerScore(int newScore, int lives) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String playerId = currentUser.getUid();
            String playerName = currentUser.getDisplayName();
            
            if (playerName == null || playerName.isEmpty()) {
                playerName = currentUser.isAnonymous() ? "Invitado" : "Jugador";
            }
            
            // Actualizar o añadir jugador principal
            boolean found = false;
            for (PlayerScore playerScore : playerScores) {
                if (playerScore.playerId.equals(playerId)) {
                    playerScore.score = newScore;
                    playerScore.lives = lives;
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                addPlayer(playerId, playerName, newScore, lives);
            }
            
            Log.d(TAG, "Puntuación actualizada para " + playerName + ": " + newScore + " puntos, " + lives + " vidas");
        }
    }

    public void endGame(boolean completed) {
        long endTime = System.currentTimeMillis();
        int duration = (int) ((endTime - startTime) / 1000); // duración en segundos
        
        Log.d(TAG, "Finalizando partida - Completada: " + completed + ", Duración: " + duration + "s");
        
        if (playerScores.isEmpty()) {
            Log.w(TAG, "No hay puntuaciones para guardar");
            return;
        }
        
        // Obtener información del usuario actual
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "No hay usuario autenticado");
            return;
        }
        
        try {
            FirestoreManager firestoreManager = new FirestoreManager();
            
            // Crear y guardar la partida primero
            Partida partida = new Partida(duration, nivel, modoJuego, playerScores.size(), completed);
            firestoreManager.guardarPartida(partida)
                    .addOnSuccessListener(documentReference -> {
                        String partidaId = documentReference.getId();
                        Log.d(TAG, "Partida guardada con ID: " + partidaId);
                        
                        // Procesar cada jugador y guardar/actualizar su mejor puntuación
                        for (PlayerScore playerScore : playerScores) {
                            Log.d(TAG, "Puntuación actualizada para " + playerScore.playerName + 
                                    ": " + playerScore.score + " puntos, " + playerScore.lives + " vidas");
                            
                            // Crear puntuación para este jugador
                            Puntuacion puntuacion = new Puntuacion(
                                    playerScore.playerId,
                                    partidaId,
                                    playerScore.score,
                                    playerScore.playerName,
                                    modoJuego,
                                    duration,
                                    nivel,
                                    completed
                            );
                            
                            // Guardar solo si es su mejor puntuación
                            firestoreManager.guardarMejorPuntuacion(puntuacion)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Mejor puntuación procesada para " + playerScore.playerName);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error procesando mejor puntuación para " + playerScore.playerName, e);
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error guardando partida", e);
                    });
                    
        } catch (Exception e) {
            Log.e(TAG, "Error al finalizar partida", e);
        }
    }
    
    public int getDuration() {
        return (int) ((System.currentTimeMillis() - startTime) / 1000);
    }
    
    public String getModoJuego() {
        return modoJuego;
    }
    
    public int getNivel() {
        return nivel;
    }
}