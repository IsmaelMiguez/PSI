package es.udc.psi.pacman.data;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

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
    public Task<DocumentReference> guardarPuntuacion(Puntuacion puntuacion) {
        return db.collection(COLECCION_PUNTUACIONES)
                .add(puntuacion.toMap());
    }

    // Obtener ranking global
    public Task<QuerySnapshot> obtenerRankingGlobal(int limite) {
        return db.collection(COLECCION_PUNTUACIONES)
                .orderBy("puntos", Query.Direction.DESCENDING)
                .limit(limite)
                .get();
    }

    // Obtener ranking por nivel
    public Task<QuerySnapshot> obtenerRankingPorNivel(int nivel, int limite) {
        return db.collection(COLECCION_PUNTUACIONES)
                .whereEqualTo("nivel", nivel)
                .orderBy("puntos", Query.Direction.DESCENDING)
                .limit(limite)
                .get();
    }

    // Obtener puntuaciones de un jugador
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