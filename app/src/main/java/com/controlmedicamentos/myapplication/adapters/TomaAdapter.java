package com.controlmedicamentos.myapplication.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.controlmedicamentos.myapplication.R;
import com.controlmedicamentos.myapplication.models.Toma;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter para mostrar el historial de tomas de un medicamento
 */
public class TomaAdapter extends RecyclerView.Adapter<TomaAdapter.TomaViewHolder> {

    private Context context;
    private List<Toma> tomas;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat timeFormat;

    /**
     * Constructor del adapter
     * @param context Contexto de la aplicación
     * @param tomas Lista de tomas a mostrar
     */
    public TomaAdapter(Context context, List<Toma> tomas) {
        this.context = context;
        this.tomas = tomas;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public TomaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_toma, parent, false);
        return new TomaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TomaViewHolder holder, int position) {
        Toma toma = tomas.get(position);
        holder.bind(toma);
    }

    @Override
    public int getItemCount() {
        return tomas != null ? tomas.size() : 0;
    }

    /**
     * Actualiza la lista de tomas y notifica al adapter
     * @param nuevasTomas Nueva lista de tomas
     */
    public void actualizarTomas(List<Toma> nuevasTomas) {
        this.tomas = nuevasTomas;
        notifyDataSetChanged();
    }

    class TomaViewHolder extends RecyclerView.ViewHolder {
        private View viewEstado;
        private TextView tvFechaHora;
        private TextView tvEstado;
        private TextView tvObservaciones;

        public TomaViewHolder(@NonNull View itemView) {
            super(itemView);
            viewEstado = itemView.findViewById(R.id.viewEstado);
            tvFechaHora = itemView.findViewById(R.id.tvFechaHora);
            tvEstado = itemView.findViewById(R.id.tvEstado);
            tvObservaciones = itemView.findViewById(R.id.tvObservaciones);
        }

        public void bind(Toma toma) {
            if (toma == null) {
                return;
            }

            // Configurar fecha y hora
            Date fechaMostrar = toma.getFechaHoraTomada() != null 
                ? toma.getFechaHoraTomada() 
                : toma.getFechaHoraProgramada();
            
            if (fechaMostrar != null) {
                String fechaStr = dateFormat.format(fechaMostrar);
                String horaStr = timeFormat.format(fechaMostrar);
                tvFechaHora.setText(fechaStr + " " + horaStr);
            } else {
                tvFechaHora.setText("Fecha no disponible");
            }

            // Configurar estado
            Toma.EstadoToma estado = toma.getEstado();
            if (estado == null) {
                estado = Toma.EstadoToma.PENDIENTE;
            }

            String estadoTexto = obtenerTextoEstado(estado);
            tvEstado.setText(estadoTexto);

            // Configurar color del indicador según el estado
            int colorEstado = obtenerColorEstado(estado);
            viewEstado.setBackgroundColor(ContextCompat.getColor(context, colorEstado));

            // Mostrar observaciones si existen
            if (toma.getObservaciones() != null && !toma.getObservaciones().isEmpty()) {
                tvObservaciones.setText(toma.getObservaciones());
                tvObservaciones.setVisibility(View.VISIBLE);
            } else {
                tvObservaciones.setVisibility(View.GONE);
            }
        }

        private String obtenerTextoEstado(Toma.EstadoToma estado) {
            switch (estado) {
                case TOMADA:
                    return context.getString(R.string.take_status_taken);
                case PERDIDA:
                    return context.getString(R.string.take_status_missed);
                case PENDIENTE:
                    return context.getString(R.string.take_status_pending);
                case PROXIMA:
                    return context.getString(R.string.take_status_upcoming);
                default:
                    return context.getString(R.string.take_status_pending);
            }
        }

        private int obtenerColorEstado(Toma.EstadoToma estado) {
            switch (estado) {
                case TOMADA:
                    return R.color.barra_tomada;
                case PERDIDA:
                    return R.color.barra_perdida;
                case PENDIENTE:
                    return R.color.barra_pendiente;
                case PROXIMA:
                    return R.color.barra_proxima;
                default:
                    return R.color.barra_pendiente;
            }
        }
    }
}

