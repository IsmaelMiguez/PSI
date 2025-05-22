package es.udc.psi.pacman.data.models;

import com.google.firebase.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class Jugador {
    private String id;            // ID de usuario generado por Firebase Auth
    private String nombre;
    private String email;
    private Timestamp fechaRegistro;
    private Timestamp ultimaConexion;

    // Constructor vacío necesario para Firestore
    public Jugador() {}

    public Jugador(String id, String nombre, String email) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.fechaRegistro = Timestamp.now();
        this.ultimaConexion = Timestamp.now();
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Timestamp getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(Timestamp fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public Timestamp getUltimaConexion() { return ultimaConexion; }
    public void setUltimaConexion(Timestamp ultimaConexion) { this.ultimaConexion = ultimaConexion; }

    // Método para convertir a Map para Firestore
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("nombre", nombre);
        map.put("email", email);
        map.put("fechaRegistro", fechaRegistro);
        map.put("ultimaConexion", ultimaConexion);
        return map;
    }
}