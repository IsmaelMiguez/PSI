package es.udc.psi.pacman.data;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        
        // Habilitar persistencia offline para mejor rendimiento
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        
        try {
            db.setFirestoreSettings(settings);
        } catch (IllegalStateException e) {
            // Los settings ya fueron configurados, esto es normal
            Log.d("FirestoreManager", "Settings de Firestore ya configurados");
        }
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
                            Log.d("FirestoreManager", "Primera puntuación para " + nuevaPuntuacion.getNombreJugador() + 
                                    " en modo " + nuevaPuntuacion.getModoJuego());
                            return db.collection(COLECCION_PUNTUACIONES)
                                    .add(nuevaPuntuacion.toMap())
                                    .continueWith(addTask -> null);
                        } else {
                            // Existe una puntuación anterior, comparar
                            DocumentSnapshot mejorAnteriorDoc = result.getDocuments().get(0);
                            Puntuacion mejorAnterior = mejorAnteriorDoc.toObject(Puntuacion.class);
                            
                            Log.d("FirestoreManager", "Comparando puntuaciones para " + nuevaPuntuacion.getNombreJugador() + 
                                    ": nueva=" + nuevaPuntuacion.getPuntos() + ", anterior=" + mejorAnterior.getPuntos());
                            
                            if (esPuntuacionMejor(nuevaPuntuacion, mejorAnterior)) {
                                // La nueva puntuación es mejor, reemplazar
                                Log.d("FirestoreManager", "Nueva puntuación es mejor, reemplazando");
                                WriteBatch batch = db.batch();
                                
                                // Agregar nueva puntuación
                                DocumentReference nuevaRef = db.collection(COLECCION_PUNTUACIONES).document();
                                batch.set(nuevaRef, nuevaPuntuacion.toMap());
                                
                                // Eliminar puntuación anterior
                                batch.delete(mejorAnteriorDoc.getReference());
                                
                                return batch.commit();
                            } else {
                                // La puntuación anterior es mejor o igual, no guardar la nueva
                                Log.d("FirestoreManager", "Puntuación anterior es mejor, no se guarda la nueva");
                                return Tasks.forResult(null);
                            }
                        }
                    } else {
                        throw task.getException();
                    }
                });
    }

    // Método auxiliar para determinar si una puntuación es mejor que otra
    private boolean esPuntuacionMejor(Puntuacion nueva, Puntuacion anterior) {
        // Criterio principal: mayor puntuación
        if (nueva.getPuntos() > anterior.getPuntos()) {
            return true;
        }
        
        // Si tienen la misma puntuación, criterio secundario: menor tiempo (solo si ambas están completadas)
        if (nueva.getPuntos() == anterior.getPuntos()) {
            if (nueva.isPartidaCompletada() && anterior.isPartidaCompletada()) {
                return nueva.getDuracionPartida() < anterior.getDuracionPartida();
            } else if (nueva.isPartidaCompletada() && !anterior.isPartidaCompletada()) {
                // Preferir partidas completadas
                return true;
            }
        }
        
        return false;
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

    // Método para eliminar todos los datos de un usuario
    public Task<Void> eliminarTodosDatosUsuario(String idJugador) {
        return db.collection(COLECCION_PUNTUACIONES)
                .whereEqualTo("idJugador", idJugador)
                .get()
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        WriteBatch batch = db.batch();
                        Set<String> partidasClasicasAEliminar = new HashSet<>();
                        
                        Log.d("FirestoreManager", "Encontradas " + task.getResult().size() + " puntuaciones para eliminar");
                        
                        // 1. Eliminar todas las puntuaciones del usuario y recopilar partidas clásicas
                        for (DocumentSnapshot puntuacionDoc : task.getResult().getDocuments()) {
                            Log.d("FirestoreManager", "Eliminando puntuación: " + puntuacionDoc.getId());
                            batch.delete(puntuacionDoc.getReference());
                            
                            // Si es una partida clásica, agregar a la lista para eliminar
                            String modoJuego = puntuacionDoc.getString("modoJuego");
                            String idPartida = puntuacionDoc.getString("idPartida");
                            if ("clasico".equals(modoJuego) && idPartida != null) {
                                partidasClasicasAEliminar.add(idPartida);
                                Log.d("FirestoreManager", "Partida clásica marcada para eliminar: " + idPartida);
                            }
                        }
                        
                        // 2. Eliminar las partidas clásicas directamente (ya tenemos los IDs)
                        for (String idPartida : partidasClasicasAEliminar) {
                            DocumentReference partidaRef = db.collection(COLECCION_PARTIDAS).document(idPartida);
                            batch.delete(partidaRef);
                            Log.d("FirestoreManager", "Eliminando partida clásica: " + idPartida);
                        }
                        
                        Log.d("FirestoreManager", "Batch preparado: " + 
                                task.getResult().size() + " puntuaciones + " + 
                                partidasClasicasAEliminar.size() + " partidas clásicas");
                        
                        return batch.commit();
                    } else {
                        Log.e("FirestoreManager", "Error al obtener puntuaciones: " + task.getException().getMessage());
                        return Tasks.forException(task.getException());
                    }
                });
    }

    // RANKINGS
    
    // Obtener ranking clásico por puntuación
    public Task<QuerySnapshot> obtenerRankingClasicoPorPuntuacion(int limite) {
        return db.collection(COLECCION_PUNTUACIONES)
                .whereEqualTo("modoJuego", "clasico")
                .orderBy("puntos", Query.Direction.DESCENDING)
                .get();
    }

    // Obtener ranking clásico por tiempo (solo partidas completadas)
    public Task<QuerySnapshot> obtenerRankingClasicoPorTiempo(int limite) {
        // Cambiar para incluir todas las partidas, no solo completadas
        return db.collection(COLECCION_PUNTUACIONES)
                .whereEqualTo("modoJuego", "clasico")
                .orderBy("duracionPartida", Query.Direction.ASCENDING)
                .get();
    }

    // Obtener ranking cooperativo por puntuación
    public Task<QuerySnapshot> obtenerRankingCooperativoPorPuntuacion(int limite) {
        return db.collection(COLECCION_PUNTUACIONES)
                .whereEqualTo("modoJuego", "cooperativo")
                .orderBy("puntos", Query.Direction.DESCENDING)
                .get();
    }

    // Obtener ranking cooperativo por tiempo (solo partidas completadas)
    public Task<QuerySnapshot> obtenerRankingCooperativoPorTiempo(int limite) {
        return db.collection(COLECCION_PUNTUACIONES)
                .whereEqualTo("modoJuego", "cooperativo")
                .orderBy("duracionPartida", Query.Direction.ASCENDING)
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