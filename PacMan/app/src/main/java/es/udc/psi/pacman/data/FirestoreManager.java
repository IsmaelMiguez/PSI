package es.udc.psi.pacman.data;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.List;

import es.udc.psi.pacman.data.models.Jugador;
import es.udc.psi.pacman.data.models.Partida;
import es.udc.psi.pacman.data.models.Puntuacion;

public class FirestoreManager {
    private FirebaseFirestore db;

    // Nombres de colecciones
    private static final String COLECCION_JUGADORES = "jugadores";
    private static final String COLECCION_PARTIDAS = "partidas";
    private static final String COLECCION_PUNTUACIONES = "puntuaciones";

    public FirestoreManager() {
        db = FirebaseFirestore.getInstance();
    }

    // MÉTODOS PARA JUGADORES
    public Task<DocumentReference> guardarJugador(Jugador jugador) {
        return db.collection(COLECCION_JUGADORES)
                .add(jugador.toMap());
    }

    public Task<Void> actualizarJugador(String id, Jugador jugador) {
        return db.collection(COLECCION_JUGADORES)
                .document(id)
                .update(jugador.toMap());
    }

    // MÉTODOS PARA PARTIDAS
    public Task<DocumentReference> guardarPartida(Partida partida) {
        return db.collection(COLECCION_PARTIDAS)
                .add(partida.toMap());
    }

    // MÉTODOS PARA PUNTUACIONES
    public Task<Void> guardarMejorPuntuacion(Puntuacion nuevaPuntuacion) {
        return obtenerMejorPuntuacionUsuario(nuevaPuntuacion.getIdJugador(), nuevaPuntuacion.getModoJuego())
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot result = task.getResult();
                        
                        if (result.isEmpty()) {
                            // No hay puntuaciones anteriores, guardar directamente
                            return db.collection(COLECCION_PUNTUACIONES)
                                    .add(nuevaPuntuacion.toMap())
                                    .continueWith(addTask -> null);
                        } else {
                            // Existe una puntuación anterior, comparar
                            Puntuacion mejorAnterior = result.getDocuments().get(0).toObject(Puntuacion.class);
                            
                            if (nuevaPuntuacion.getPuntos() > mejorAnterior.getPuntos()) {
                                // La nueva puntuación es mejor, reemplazar
                                WriteBatch batch = db.batch();
                                
                                // Agregar nueva puntuación
                                DocumentReference nuevaRef = db.collection(COLECCION_PUNTUACIONES).document();
                                batch.set(nuevaRef, nuevaPuntuacion.toMap());
                                
                                // Eliminar puntuación anterior
                                batch.delete(result.getDocuments().get(0).getReference());
                                
                                return batch.commit();
                            } else {
                                // La puntuación anterior es mejor o igual, no guardar
                                return Tasks.forResult(null);
                            }
                        }
                    } else {
                        throw task.getException();
                    }
                });
    }

    // Guardar partida completa con todas las puntuaciones en una transacción
    public Task<Void> guardarPartidaCompleta(Partida partida, List<Puntuacion> puntuaciones) {
        WriteBatch batch = db.batch();
        
        // Agregar la partida
        DocumentReference partidaRef = db.collection(COLECCION_PARTIDAS).document();
        partida.setId(partidaRef.getId());
        batch.set(partidaRef, partida.toMap());
        
        // Agregar todas las puntuaciones
        for (Puntuacion puntuacion : puntuaciones) {
            puntuacion.setIdPartida(partidaRef.getId());
            DocumentReference puntuacionRef = db.collection(COLECCION_PUNTUACIONES).document();
            batch.set(puntuacionRef, puntuacion.toMap());
        }
        
        return batch.commit();
    }

    // RANKINGS
    
    // Obtener ranking clásico por puntuación
    public Task<QuerySnapshot> obtenerRankingClasicoPorPuntuacion(int limite) {
        return db.collection(COLECCION_PUNTUACIONES)
                .whereEqualTo("modoJuego", "clasico")
                .orderBy("puntos", Query.Direction.DESCENDING)
                .orderBy("duracionPartida", Query.Direction.ASCENDING)
                .get();
    }

    // Obtener ranking clásico por tiempo (solo partidas completadas)
    public Task<QuerySnapshot> obtenerRankingClasicoPorTiempo(int limite) {
        return db.collection(COLECCION_PUNTUACIONES)
                .whereEqualTo("modoJuego", "clasico")
                .whereEqualTo("partidaCompletada", true)
                .orderBy("duracionPartida", Query.Direction.ASCENDING)
                .orderBy("puntos", Query.Direction.DESCENDING)
                .get();
    }

    // Obtener ranking cooperativo por puntuación
    public Task<QuerySnapshot> obtenerRankingCooperativoPorPuntuacion(int limite) {
        return db.collection(COLECCION_PUNTUACIONES)
                .whereEqualTo("modoJuego", "cooperativo")
                .orderBy("puntos", Query.Direction.DESCENDING)
                .orderBy("duracionPartida", Query.Direction.ASCENDING)
                .get();
    }

    // Obtener ranking cooperativo por tiempo (solo partidas completadas)
    public Task<QuerySnapshot> obtenerRankingCooperativoPorTiempo(int limite) {
        return db.collection(COLECCION_PUNTUACIONES)
                .whereEqualTo("modoJuego", "cooperativo")
                .whereEqualTo("partidaCompletada", true)
                .orderBy("duracionPartida", Query.Direction.ASCENDING)
                .orderBy("puntos", Query.Direction.DESCENDING)
                .get();
    }

    // Método para obtener la mejor puntuación existente de un usuario en un modo específico
    public Task<QuerySnapshot> obtenerMejorPuntuacionUsuario(String idJugador, String modoJuego) {
        return db.collection(COLECCION_PUNTUACIONES)
                .whereEqualTo("idJugador", idJugador)
                .whereEqualTo("modoJuego", modoJuego)
                .orderBy("puntos", Query.Direction.DESCENDING)
                .limit(1)
                .get();
    }

    // Método para eliminar puntuaciones anteriores de un usuario en un modo específico
    public Task<Void> eliminarPuntuacionesAnteriores(String idJugador, String modoJuego, String excluirId) {
        return db.collection(COLECCION_PUNTUACIONES)
                .whereEqualTo("idJugador", idJugador)
                .whereEqualTo("modoJuego", modoJuego)
                .get()
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        WriteBatch batch = db.batch();
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            if (!doc.getId().equals(excluirId)) {
                                batch.delete(doc.getReference());
                            }
                        }
                        return batch.commit();
                    }
                    return Tasks.forException(task.getException());
                });
    }

    // Obtener ranking global por puntuación
    public Task<QuerySnapshot> obtenerRankingGlobal(int limite) {
        return db.collection(COLECCION_PUNTUACIONES)
                .orderBy("puntos", Query.Direction.DESCENDING)
                .orderBy("duracionPartida", Query.Direction.ASCENDING)
                .limit(limite)
                .get();
    }

    // Obtener puntuaciones de un jugador específico
    public Task<QuerySnapshot> obtenerPuntuacionesJugador(String idJugador) {
        return db.collection(COLECCION_PUNTUACIONES)
                .whereEqualTo("idJugador", idJugador)
                .orderBy("puntos", Query.Direction.DESCENDING)
                .get();
    }

    // Obtener jugadores de una partida
    public Task<QuerySnapshot> obtenerJugadoresPorPartida(String idPartida) {
        return db.collection(COLECCION_PUNTUACIONES)
                .whereEqualTo("idPartida", idPartida)
                .orderBy("puntos", Query.Direction.DESCENDING)
                .get();
    }
}