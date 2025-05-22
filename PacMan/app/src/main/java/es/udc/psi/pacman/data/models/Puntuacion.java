package es.udc.psi.pacman.data.models;

import com.google.firebase.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class Puntuacion {
    private String id;              // ID generado por Firestore
    private String idJugador;
    private String idPartida;
    private int puntos;
    private Timestamp fechaRegistro;
    private int posicion;
    private String nombreJugador;   // Agregado para facilitar la visualización

    // Constructor vacío necesario para Firestore
    public Puntuacion() {}

    public Puntuacion(String idJugador, String idPartida, int puntos, String nombreJugador) {
        this.idJugador = idJugador;
        this.idPartida = idPartida;
        this.puntos = puntos;
        this.fechaRegistro = Timestamp.now();
        this.nombreJugador = nombreJugador;
        // La posición se calculará en el servidor
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIdJugador() { return idJugador; }
    public void setIdJugador(String idJugador) { this.idJugador = idJugador; }

    public String getIdPartida() { return idPartida; }
    public void setIdPartida(String idPartida) { this.idPartida = idPartida; }

    public int getPuntos() { return puntos; }
    public void setPuntos(int puntos) { this.puntos = puntos; }

    public Timestamp getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(Timestamp fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public int getPosicion() { return posicion; }
    public void setPosicion(int posicion) { this.posicion = posicion; }

    public String getNombreJugador() { return nombreJugador; }
    public void setNombreJugador(String nombreJugador) { this.nombreJugador = nombreJugador; }

    // Método para convertir a Map para Firestore
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("idJugador", idJugador);
        map.put("idPartida", idPartida);
        map.put("puntos", puntos);
        map.put("fechaRegistro", fechaRegistro);
        map.put("posicion", posicion);
        map.put("nombreJugador", nombreJugador);
        return map;
    }
}