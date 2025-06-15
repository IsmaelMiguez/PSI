package es.udc.psi.pacman.data.models;

import com.google.firebase.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class Partida {
    private String id;              // ID generado por Firestore
    private Timestamp fecha;
    private int duracion;           // Duración en segundos
    private int nivel;
    private String modoJuego;       // "clasico" o "cooperativo"
    private int numeroParticipantes;
    private boolean completada;     // Si se completó el nivel o se acabaron las vidas

    // Constructor vacío necesario para Firestore
    public Partida() {}

    public Partida(int duracion, int nivel, String modoJuego, int numeroParticipantes, boolean completada) {
        this.fecha = Timestamp.now();
        this.duracion = duracion;
        this.nivel = nivel;
        this.modoJuego = modoJuego;
        this.numeroParticipantes = numeroParticipantes;
        this.completada = completada;
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

    public int getNumeroParticipantes() { return numeroParticipantes; }
    public void setNumeroParticipantes(int numeroParticipantes) { this.numeroParticipantes = numeroParticipantes; }

    public boolean isCompletada() { return completada; }
    public void setCompletada(boolean completada) { this.completada = completada; }

    // Método para convertir a Map para Firestore
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("fecha", fecha);
        map.put("duracion", duracion);
        map.put("nivel", nivel);
        map.put("modoJuego", modoJuego);
        map.put("numeroParticipantes", numeroParticipantes);
        map.put("completada", completada);
        return map;
    }
}