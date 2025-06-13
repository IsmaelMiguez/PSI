package es.udc.psi.pacman;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import es.udc.psi.pacman.data.models.Puntuacion;

public class RankingAdapter extends RecyclerView.Adapter<RankingAdapter.RankingViewHolder> {
    
    private List<Puntuacion> puntuaciones = new ArrayList<>();
    private String currentUserId;
    private int currentSortType = 0; // 0 = puntuación, 1 = tiempo
    private OnUserPositionFoundListener listener;
    
    public interface OnUserPositionFoundListener {
        void onUserPositionFound(int position);
    }
    
    public RankingAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }
    
    public void setOnUserPositionFoundListener(OnUserPositionFoundListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public RankingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ranking, parent, false);
        return new RankingViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull RankingViewHolder holder, int position) {
        Puntuacion puntuacion = puntuaciones.get(position);
        
        // Mostrar posición
        holder.tvPosition.setText(String.valueOf(position + 1));
        
        // Nombre del jugador
        holder.tvPlayerName.setText(puntuacion.getNombreJugador());
        
        // Puntos
        holder.tvScore.setText(String.valueOf(puntuacion.getPuntos()));
        
        // Formatear tiempo
        int minutes = puntuacion.getDuracionPartida() / 60;
        int seconds = puntuacion.getDuracionPartida() % 60;
        holder.tvTime.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
        
        // Formatear fecha
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        holder.tvDate.setText(sdf.format(puntuacion.getFechaRegistro().toDate()));
        
        // Mostrar estado de completado
        holder.tvCompleted.setText(puntuacion.isPartidaCompletada() ? "✓" : "✗");
        holder.tvCompleted.setTextColor(puntuacion.isPartidaCompletada() ? 
                Color.GREEN : Color.RED);
        
        // Resaltar si es el usuario actual
        boolean isCurrentUser = currentUserId != null && currentUserId.equals(puntuacion.getIdJugador());
        
        if (isCurrentUser) {
            // Color de fondo naranja claro
            holder.itemView.setBackgroundColor(Color.parseColor("#FFE0B2")); // Naranja claro
            
            // Texto en color oscuro para mejor contraste
            holder.tvPlayerName.setTextColor(Color.parseColor("#E65100")); // Naranja oscuro
            holder.tvPosition.setTextColor(Color.parseColor("#E65100"));
            holder.tvScore.setTextColor(Color.parseColor("#E65100"));
            holder.tvTime.setTextColor(Color.parseColor("#E65100"));
            holder.tvDate.setTextColor(Color.parseColor("#BF360C"));
            
            // Añadir un borde más visible
            holder.itemView.setPadding(8, 8, 8, 8);
            
            // Notificar la posición del usuario encontrado
            if (listener != null) {
                listener.onUserPositionFound(position);
            }
        } else {
            // Restaurar colores normales
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
            holder.itemView.setPadding(12, 12, 12, 12);
            
            int defaultTextColor = ContextCompat.getColor(holder.itemView.getContext(), android.R.color.black);
            holder.tvPlayerName.setTextColor(defaultTextColor);
            holder.tvPosition.setTextColor(defaultTextColor);
            holder.tvScore.setTextColor(defaultTextColor);
            holder.tvTime.setTextColor(defaultTextColor);
            holder.tvDate.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.darker_gray));
        }
    }
    
    @Override
    public int getItemCount() {
        return puntuaciones.size();
    }
    
    public void updateData(List<Puntuacion> newPuntuaciones, int sortType) {
        this.puntuaciones = newPuntuaciones;
        this.currentSortType = sortType;
        notifyDataSetChanged();
    }
    
    public int getCurrentSortType() {
        return currentSortType;
    }
    
    public int findUserPosition() {
        if (currentUserId == null) return -1;
        
        for (int i = 0; i < puntuaciones.size(); i++) {
            if (currentUserId.equals(puntuaciones.get(i).getIdJugador())) {
                return i;
            }
        }
        return -1;
    }
    
    static class RankingViewHolder extends RecyclerView.ViewHolder {
        TextView tvPosition, tvPlayerName, tvScore, tvTime, tvDate, tvCompleted;
        
        public RankingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPosition = itemView.findViewById(R.id.tvPosition);
            tvPlayerName = itemView.findViewById(R.id.tvPlayerName);
            tvScore = itemView.findViewById(R.id.tvScore);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvCompleted = itemView.findViewById(R.id.tvCompleted);
        }
    }
}