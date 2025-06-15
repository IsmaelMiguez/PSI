package es.udc.psi.pacman;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
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
    private TextView tvSortIndicator;
    
    private FirestoreManager firestoreManager;
    private RankingAdapter adapter;
    private String currentUserId;
    private FirebaseFirestore db; // Agregar esta línea
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranking);
        
        initViews();
        setupTabs();
        
        firestoreManager = new FirestoreManager();
        db = FirebaseFirestore.getInstance(); // Inicializar aquí
        
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
        tvSortIndicator = findViewById(R.id.tvSortIndicator);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }
    
    private void setupTabs() {
        TabLayout.Tab clasicoTab = tabLayout.newTab().setText("🎮 Clásico");
        TabLayout.Tab cooperativoTab = tabLayout.newTab().setText("👥 Cooperativo");

        tabLayout.addTab(clasicoTab);
        tabLayout.addTab(cooperativoTab);
        
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                loadRanking(tab.getPosition(), 0); // Por puntuación por defecto
                updateSortIndicator(0); // NUEVO
            }
            
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Alternar entre puntuación y tiempo
                int currentSort = adapter != null ? adapter.getCurrentSortType() : 0;
                int newSort = currentSort == 0 ? 1 : 0; // 0 = puntuación, 1 = tiempo
                loadRanking(tab.getPosition(), newSort);
                updateSortIndicator(newSort);
            }
        });
    }

    private void updateSortIndicator(int sortType) {
        if (tvSortIndicator == null) return;
        
        if (sortType == 0) {
            // Ordenado por puntuación
            tvSortIndicator.setText(R.string.ordenado_por_puntuacion);
            tvSortIndicator.setBackgroundColor(getColor(R.color.sort_indicator_background));
            tvSortIndicator.setTextColor(getColor(R.color.sort_indicator_text));
        } else {
            // Ordenado por tiempo
            tvSortIndicator.setText(R.string.ordenado_por_tiempo);
            tvSortIndicator.setBackgroundColor(getColor(R.color.sort_indicator_time_background));
            tvSortIndicator.setTextColor(getColor(R.color.sort_indicator_time_text));
        }
    }

    private void handleQueryError(Exception e) {
        if (e.getMessage() != null) {
            if (e.getMessage().contains("requires an index")) {
                Toast.makeText(this, "Ordenación por tiempo no disponible. Mostrando por puntuación.", Toast.LENGTH_LONG).show();
                Log.w(TAG, "Índices aún no están listos, usando fallback");
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

        updateSortIndicator(sortType);
        
        Task<QuerySnapshot> query = getQueryForModeAndSort(gameMode, sortType);
        
        query.addOnSuccessListener(queryDocumentSnapshots -> {
            List<Puntuacion> puntuaciones = new ArrayList<>();
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                Puntuacion puntuacion = document.toObject(Puntuacion.class);
                puntuacion.setId(document.getId());
                puntuaciones.add(puntuacion);
                
                // Agregar log para debug
                Log.d(TAG, "Puntuación cargada: " + puntuacion.getNombreJugador() + 
                        ", puntos=" + puntuacion.getPuntos() + 
                        ", completada=" + puntuacion.isPartidaCompletada() +
                        ", modo=" + puntuacion.getModoJuego());
            }
            
            // Si estamos ordenando por tiempo, ordenar manualmente en el cliente
            if (sortType == 1) {
                puntuaciones.sort((p1, p2) -> {
                    // Primero por duración (ascendente)
                    int compareTime = Integer.compare(p1.getDuracionPartida(), p2.getDuracionPartida());
                    if (compareTime != 0) {
                        return compareTime;
                    }
                    // Si tienen el mismo tiempo, por puntuación (descendente)
                    return Integer.compare(p2.getPuntos(), p1.getPuntos());
                });
            }
            
            Log.d(TAG, "Cargadas " + puntuaciones.size() + " puntuaciones únicas (modo=" + gameMode + ", sort=" + sortType + ")");
            
            if (adapter == null) {
                adapter = new RankingAdapter(currentUserId);
                
                // Configurar listener para scroll automático
                adapter.setOnUserPositionFoundListener(new RankingAdapter.OnUserPositionFoundListener() {
                    @Override
                    public void onUserPositionFound(int position) {
                        if (recyclerView != null && position >= 0) {
                            recyclerView.smoothScrollToPosition(position);
                        }
                    }
                });
                
                recyclerView.setAdapter(adapter);
            }
            
            // Como ahora cada usuario tiene solo una puntuación por modo, no necesitamos filtrar
            adapter.updateData(puntuaciones, sortType);
            
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            
            // Scroll automático al usuario actual
            scrollToCurrentUser(puntuaciones);

            // Asegurar que el indicador se actualice al finalizar la carga
            updateSortIndicator(sortType);
            
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error al cargar ranking", e);
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            
            // Manejo específico para errores de índices
            if (e.getMessage() != null && e.getMessage().contains("requires an index")) {
                Log.w(TAG, "Índice requerido no disponible, intentando consulta alternativa");
                
                // Intentar una consulta más simple como fallback
                loadRankingFallback(gameMode, sortType);
            } else {
                handleQueryError(e);
            }
        });
    }

    private void loadRankingFallback(int gameMode, int sortType) {
        // Consulta simplificada sin ordenación compleja
        Task<QuerySnapshot> fallbackQuery;
        String modoJuego = gameMode == 0 ? "clasico" : "cooperativo";

        updateSortIndicator(sortType);
        
        Log.d(TAG, "Usando fallback para modo=" + modoJuego + ", sortType=" + sortType);
        
        if (sortType == 1) {
            // Para ordenación por tiempo, traer TODAS las partidas (no solo completadas)
            fallbackQuery = db.collection("puntuaciones")
                    .whereEqualTo("modoJuego", modoJuego)
                    .get();
        } else {
            // Para puntuación, usar la consulta original que funciona
            fallbackQuery = getQueryForModeAndSort(gameMode, 0); // Siempre por puntuación
        }
        
        fallbackQuery.addOnSuccessListener(queryDocumentSnapshots -> {
            List<Puntuacion> puntuaciones = new ArrayList<>();
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                Puntuacion puntuacion = document.toObject(Puntuacion.class);
                puntuacion.setId(document.getId());
                puntuaciones.add(puntuacion);
                
                // Log para debug
                Log.d(TAG, "Fallback - Puntuación cargada: " + puntuacion.getNombreJugador() + 
                        ", puntos=" + puntuacion.getPuntos() + 
                        ", completada=" + puntuacion.isPartidaCompletada() +
                        ", tiempo=" + puntuacion.getDuracionPartida());
            }
            
            // Ordenar manualmente en el cliente
            if (sortType == 1) {
                // Ordenar por tiempo, pero priorizar partidas completadas
                puntuaciones.sort((p1, p2) -> {
                    // Primero, partidas completadas van antes que incompletas
                    if (p1.isPartidaCompletada() && !p2.isPartidaCompletada()) {
                        return -1;
                    } else if (!p1.isPartidaCompletada() && p2.isPartidaCompletada()) {
                        return 1;
                    }
                    
                    // Si ambas tienen el mismo estado de completado, ordenar por tiempo
                    int compareTime = Integer.compare(p1.getDuracionPartida(), p2.getDuracionPartida());
                    if (compareTime != 0) {
                        return compareTime;
                    }
                    // Si tienen el mismo tiempo, por puntuación
                    return Integer.compare(p2.getPuntos(), p1.getPuntos());
                });
            } else {
                puntuaciones.sort((p1, p2) -> {
                    int compareScore = Integer.compare(p2.getPuntos(), p1.getPuntos());
                    if (compareScore != 0) {
                        return compareScore;
                    }
                    return Integer.compare(p1.getDuracionPartida(), p2.getDuracionPartida());
                });
            }
            
            Log.d(TAG, "Fallback: Cargadas " + puntuaciones.size() + " puntuaciones (después de ordenar)");
            
            if (adapter == null) {
                adapter = new RankingAdapter(currentUserId);
                adapter.setOnUserPositionFoundListener(position -> {
                    if (recyclerView != null && position >= 0) {
                        recyclerView.smoothScrollToPosition(position);
                    }
                });
                recyclerView.setAdapter(adapter);
            }
            
            adapter.updateData(puntuaciones, sortType);
            
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            
            scrollToCurrentUser(puntuaciones);
            
            // Mostrar mensaje informativo
            Toast.makeText(this, "Cargado con ordenación alternativa", Toast.LENGTH_SHORT).show();

            // Confirmar indicador al finalizar fallback
            updateSortIndicator(sortType);
            
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error en consulta fallback", e);
            handleQueryError(e);
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
        });
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
        if (currentUserId == null || puntuaciones.isEmpty()) return;
        
        // Buscar posición del usuario actual
        for (int i = 0; i < puntuaciones.size(); i++) {
            if (currentUserId.equals(puntuaciones.get(i).getIdJugador())) {
                final int userPosition = i;
                recyclerView.postDelayed(() -> {
                    if (recyclerView != null) {
                        recyclerView.smoothScrollToPosition(userPosition);
                        Log.d(TAG, "Scroll automático a posición " + userPosition + " del usuario actual");
                    }
                }, 300);
                break;
            }
        }
    }
}