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
            // Crear datos de la partida
            Map<String, Object> partidaData = new HashMap<>();
            partidaData.put("fechaInicio", new Timestamp(new Date(startTime)));
            partidaData.put("fechaFin", new Timestamp(new Date(endTime)));
            partidaData.put("duracionPartida", duration);
            partidaData.put("modoJuego", modoJuego);
            partidaData.put("nivel", nivel);
            partidaData.put("partidaCompletada", completed);
            partidaData.put("numeroJugadores", playerScores.size());
            
            // Crear lista de puntuaciones
            List<Map<String, Object>> puntuacionesList = new ArrayList<>();
            
            for (PlayerScore playerScore : playerScores) {
                Map<String, Object> puntuacionData = new HashMap<>();
                puntuacionData.put("idJugador", playerScore.playerId);
                puntuacionData.put("nombreJugador", playerScore.playerName);
                puntuacionData.put("puntos", playerScore.score);
                puntuacionData.put("vidas", playerScore.lives);
                puntuacionData.put("fechaRegistro", new Timestamp(new Date()));
                puntuacionData.put("modoJuego", modoJuego);
                puntuacionData.put("nivel", nivel);
                puntuacionData.put("duracionPartida", duration);
                puntuacionData.put("partidaCompletada", completed);
                
                puntuacionesList.add(puntuacionData);
            }
            
            // Guardar en Firestore usando FirestoreManager
            FirestoreManager firestoreManager = new FirestoreManager();
            
            // Primero guardar la partida
            Partida partida = new Partida(duration, nivel, modoJuego, playerScores.size(), completed);
            firestoreManager.guardarPartida(partida)
                    .addOnSuccessListener(documentReference -> {
                        String partidaId = documentReference.getId();
                        Log.d(TAG, "Partida guardada con ID: " + partidaId);
                        
                        // Luego guardar las puntuaciones usando el nuevo método
                        for (Map<String, Object> puntuacionData : puntuacionesList) {
                            String idJugador = (String) puntuacionData.get("idJugador");
                            String nombreJugador = (String) puntuacionData.get("nombreJugador");
                            int puntos = (Integer) puntuacionData.get("puntos");
                            String modoJuego = (String) puntuacionData.get("modoJuego");
                            int nivel = (Integer) puntuacionData.get("nivel");
                            int duracionPartida = (Integer) puntuacionData.get("duracionPartida");
                            boolean partidaCompletada = (Boolean) puntuacionData.get("partidaCompletada");
                            
                            Puntuacion puntuacion = new Puntuacion(idJugador, partidaId, puntos, 
                                    nombreJugador, modoJuego, duracionPartida, nivel, partidaCompletada);
                            
                            // Usar el nuevo método que mantiene solo la mejor puntuación
                            firestoreManager.guardarMejorPuntuacion(puntuacion)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Mejor puntuación guardada para " + nombreJugador);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error guardando mejor puntuación", e);
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