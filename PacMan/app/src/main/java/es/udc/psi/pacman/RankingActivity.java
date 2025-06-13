package es.udc.psi.pacman;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.udc.psi.pacman.data.FirestoreManager;
import es.udc.psi.pacman.data.models.Puntuacion;

public class RankingActivity extends AppCompatActivity {
    private static final String TAG = "RankingActivity";
    
    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    
    private FirestoreManager firestoreManager;
    private RankingAdapter adapter;
    private String currentUserId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranking);
        
        initViews();
        setupTabs();
        
        firestoreManager = new FirestoreManager();
        
        // Obtener ID del usuario actual
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            Log.d(TAG, "Usuario actual ID: " + currentUserId);
            
            // Para debug: mostrar también el email/displayName
            if (currentUser.isAnonymous()) {
                Log.d(TAG, "Usuario anónimo detectado");
            } else {
                Log.d(TAG, "Usuario registrado: " + currentUser.getEmail());
            }
        } else {
            Log.w(TAG, "No hay usuario autenticado");
            // Opcional: redirigir al login
            finish();
            return;
        }
        
        // Cargar ranking clásico por puntuación por defecto
        loadRanking(0, 0);
    }
    
    private void initViews() {
        tabLayout = findViewById(R.id.tabLayout);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RankingAdapter(currentUserId);
        recyclerView.setAdapter(adapter);
    }
    
    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Clásico"));
        tabLayout.addTab(tabLayout.newTab().setText("Cooperativo"));
        
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                loadRanking(tab.getPosition(), 0); // Por puntuación por defecto
            }
            
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Alternar entre puntuación y tiempo
                int currentSort = adapter.getCurrentSortType();
                int newSort = currentSort == 0 ? 1 : 0; // 0 = puntuación, 1 = tiempo
                loadRanking(tab.getPosition(), newSort);
            }
        });
    }

    private void handleQueryError(Exception e) {
        if (e.getMessage() != null) {
            if (e.getMessage().contains("requires an index")) {
                Toast.makeText(this, "Los índices aún se están construyendo. Intenta en unos minutos.", Toast.LENGTH_LONG).show();
                Log.w(TAG, "Índices aún no están listos");
            } else if (e.getMessage().contains("PERMISSION_DENIED")) {
                Toast.makeText(this, "No tienes permisos para acceder a los datos", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error de permisos");
            } else {
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error general: " + e.getMessage());
            }
        } else {
            Toast.makeText(this, "Error desconocido cargando ranking", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error sin mensaje", e);
        }
    }

    private Task<QuerySnapshot> getQueryForModeAndSort(int gameMode, int sortType) {
        switch (gameMode) {
            case 0: // Clásico
                if (sortType == 0) {
                    return firestoreManager.obtenerRankingClasicoPorPuntuacion(100);
                } else {
                    return firestoreManager.obtenerRankingClasicoPorTiempo(100);
                }
            case 1: // Cooperativo
                if (sortType == 0) {
                    return firestoreManager.obtenerRankingCooperativoPorPuntuacion(100);
                } else {
                    return firestoreManager.obtenerRankingCooperativoPorTiempo(100);
                }
            default:
                // Por defecto, ranking clásico por puntuación
                return firestoreManager.obtenerRankingClasicoPorPuntuacion(100);
        }
    }
    
    private void loadRanking(int gameMode, int sortType) {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        
        Task<QuerySnapshot> query = getQueryForModeAndSort(gameMode, sortType);
        
        query.addOnSuccessListener(queryDocumentSnapshots -> {
            List<Puntuacion> puntuaciones = new ArrayList<>();
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                Puntuacion puntuacion = document.toObject(Puntuacion.class);
                puntuacion.setId(document.getId());
                puntuaciones.add(puntuacion);
            }
            
            if (adapter == null) {
                adapter = new RankingAdapter(currentUserId);
                
                // Configurar listener para scroll automático
                adapter.setOnUserPositionFoundListener(new RankingAdapter.OnUserPositionFoundListener() {
                    @Override
                    public void onUserPositionFound(int position) {
                        // Hacer scroll hasta la posición del usuario con un pequeño delay
                        recyclerView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                scrollToUserPosition(position);
                            }
                        }, 100); // Delay de 100ms para asegurar que el layout esté completo
                    }
                });
                
                recyclerView.setAdapter(adapter);
            }
            
            adapter.updateData(puntuaciones, sortType);
            
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            
            // Buscar y hacer scroll a la posición del usuario después de un breve delay
            recyclerView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scrollToUserIfFound();
                }
            }, 200);
            
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error al cargar ranking", e);
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            Toast.makeText(this, "Error al cargar el ranking: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
        });
    }
    
    private void handleRankingResponse(QuerySnapshot querySnapshot, int sortType) {
        progressBar.setVisibility(View.GONE);
        
        if (querySnapshot != null) {
            Log.d(TAG, "Procesando " + querySnapshot.size() + " documentos");
            
            List<Puntuacion> todasLasPuntuaciones = new ArrayList<>();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                Puntuacion puntuacion = doc.toObject(Puntuacion.class);
                if (puntuacion != null) {
                    puntuacion.setId(doc.getId());
                    todasLasPuntuaciones.add(puntuacion);
                    Log.d(TAG, "Puntuación: " + puntuacion.getNombreJugador() + 
                            " - " + puntuacion.getPuntos() + " pts - " + 
                            puntuacion.getDuracionPartida() + "s");
                }
            }
            
            // Filtrar para mantener solo la mejor puntuación por usuario
            List<Puntuacion> mejoresPuntuaciones = filtrarMejoresPorUsuario(todasLasPuntuaciones, sortType);
            
            if (mejoresPuntuaciones.isEmpty()) {
                Toast.makeText(this, "No hay datos para mostrar", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "No se encontraron puntuaciones");
            } else {
                adapter.updateData(mejoresPuntuaciones, sortType);
                scrollToCurrentUser(mejoresPuntuaciones);
                Log.d(TAG, "Ranking actualizado correctamente con " + mejoresPuntuaciones.size() + " entradas únicas");
            }
        } else {
            Toast.makeText(this, "Error cargando ranking", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "QuerySnapshot es null");
        }
    }
    
    private List<Puntuacion> filtrarMejoresPorUsuario(List<Puntuacion> puntuaciones, int sortType) {
        Map<String, Puntuacion> mejoresPorUsuario = new HashMap<>();
        
        for (Puntuacion puntuacion : puntuaciones) {
            String userId = puntuacion.getIdJugador();
            
            if (!mejoresPorUsuario.containsKey(userId)) {
                mejoresPorUsuario.put(userId, puntuacion);
            } else {
                Puntuacion actual = mejoresPorUsuario.get(userId);
                
                // Comparar según el tipo de ordenamiento
                boolean esMejor = false;
                if (sortType == 0) { // Por puntuación
                    esMejor = puntuacion.getPuntos() > actual.getPuntos() ||
                             (puntuacion.getPuntos() == actual.getPuntos() && 
                              puntuacion.getDuracionPartida() < actual.getDuracionPartida());
                } else { // Por tiempo (solo partidas completadas)
                    if (puntuacion.isPartidaCompletada() && actual.isPartidaCompletada()) {
                        esMejor = puntuacion.getDuracionPartida() < actual.getDuracionPartida() ||
                                 (puntuacion.getDuracionPartida() == actual.getDuracionPartida() && 
                                  puntuacion.getPuntos() > actual.getPuntos());
                    } else if (puntuacion.isPartidaCompletada() && !actual.isPartidaCompletada()) {
                        esMejor = true;
                    }
                }
                
                if (esMejor) {
                    mejoresPorUsuario.put(userId, puntuacion);
                }
            }
        }
        
        List<Puntuacion> resultado = new ArrayList<>(mejoresPorUsuario.values());
        
        // Ordenar el resultado final
        if (sortType == 0) { // Por puntuación
            resultado.sort((p1, p2) -> {
                int puntosComparison = Integer.compare(p2.getPuntos(), p1.getPuntos());
                if (puntosComparison == 0) {
                    return Integer.compare(p1.getDuracionPartida(), p2.getDuracionPartida());
                }
                return puntosComparison;
            });
        } else { // Por tiempo
            resultado.sort((p1, p2) -> {
                // Solo considerar partidas completadas para ranking por tiempo
                if (!p1.isPartidaCompletada() && !p2.isPartidaCompletada()) {
                    return Integer.compare(p2.getPuntos(), p1.getPuntos());
                } else if (!p1.isPartidaCompletada()) {
                    return 1;
                } else if (!p2.isPartidaCompletada()) {
                    return -1;
                } else {
                    int tiempoComparison = Integer.compare(p1.getDuracionPartida(), p2.getDuracionPartida());
                    if (tiempoComparison == 0) {
                        return Integer.compare(p2.getPuntos(), p1.getPuntos());
                    }
                    return tiempoComparison;
                }
            });
        }
        
        return resultado;
    }

    private void scrollToUserIfFound() {
        if (adapter != null && currentUserId != null) {
            int userPosition = adapter.findUserPosition();
            if (userPosition != -1) {
                scrollToUserPosition(userPosition);
            }
        }
    }

    private void scrollToUserPosition(int position) {
        if (recyclerView != null && position >= 0 && position < adapter.getItemCount()) {
            // Calcular la posición óptima para centrar el item del usuario
            LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
            if (layoutManager != null) {
                // Scroll suave hasta la posición del usuario
                recyclerView.smoothScrollToPosition(position);
                
                // Después de un pequeño delay, centrar mejor la vista
                recyclerView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Intentar centrar el item en la pantalla
                        View targetView = layoutManager.findViewByPosition(position);
                        if (targetView != null) {
                            int offset = (recyclerView.getHeight() / 2) - (targetView.getHeight() / 2);
                            layoutManager.scrollToPositionWithOffset(position, offset);
                        }
                    }
                }, 500);
            }
            
            Log.d(TAG, "Haciendo scroll hasta la posición del usuario: " + position);
        }
    }
    
    private void scrollToCurrentUser(List<Puntuacion> puntuaciones) {
        if (currentUserId == null) return;
        
        for (int i = 0; i < puntuaciones.size(); i++) {
            if (currentUserId.equals(puntuaciones.get(i).getIdJugador())) {
                // Hacer scroll suave hasta la posición del usuario
                recyclerView.smoothScrollToPosition(i);
                Log.d(TAG, "Scroll a posición del usuario: " + i);
                break;
            }
        }
    }
}