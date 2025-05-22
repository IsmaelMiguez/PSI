package es.udc.psi.pacman.data.models;

import com.google.firebase.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class Partida {
    private String id;              // ID generado por Firestore
    private Timestamp fecha;
    private int duracion;
    private int nivel;
    private String modoJuego;

    // Constructor vacío necesario para Firestore
    public Partida() {}

    public Partida(int duracion, int nivel, String modoJuego) {
        this.fecha = Timestamp.now();
        this.duracion = duracion;
        this.nivel = nivel;
        this.modoJuego = modoJuego;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Timestamp getFecha() { return fecha; }
    public void setFecha(Timestamp fecha) { this.fecha = fecha; }

    public int getDuracion() { return duracion; }
    public void setDuracion(int duracion) { this.duracion = duracion; }

    public int getNivel() { return nivel; }
    public void setNivel(int nivel) { this.nivel = nivel; }

    public String getModoJuego() { return modoJuego; }
    public void setModoJuego(String modoJuego) { this.modoJuego = modoJuego; }

    // Método para convertir a Map para Firestore
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("fecha", fecha);
        map.put("duracion", duracion);
        map.put("nivel", nivel);
        map.put("modoJuego", modoJuego);
        return map;
    }
}