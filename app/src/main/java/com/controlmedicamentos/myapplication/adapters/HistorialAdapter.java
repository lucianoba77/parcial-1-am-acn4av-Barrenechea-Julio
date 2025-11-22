package com.controlmedicamentos.myapplication.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.controlmedicamentos.myapplication.R;
import com.controlmedicamentos.myapplication.models.Medicamento;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class HistorialAdapter extends RecyclerView.Adapter<HistorialAdapter.HistorialViewHolder> {

    private Context context;
    private List<Medicamento> medicamentos;
    private SimpleDateFormat dateFormat;

    public HistorialAdapter(Context context, List<Medicamento> medicamentos) {
        this.context = context;
        this.medicamentos = medicamentos;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public HistorialViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_historial, parent, false);
        return new HistorialViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistorialViewHolder holder, int position) {
        Medicamento medicamento = medicamentos.get(position);
        holder.bind(medicamento);
    }

    @Override
    public int getItemCount() {
        return medicamentos != null ? medicamentos.size() : 0;
    }

    public void actualizarMedicamentos(List<Medicamento> nuevosMedicamentos) {
        this.medicamentos = nuevosMedicamentos != null ? nuevosMedicamentos : new java.util.ArrayList<>();
        notifyDataSetChanged();
    }

    class HistorialViewHolder extends RecyclerView.ViewHolder {
        private MaterialCardView cardMedicamento;
        private ImageView ivIcono;
        private TextView tvNombre;
        private TextView tvPresentacion;
        private TextView tvFechaInicio;
        private TextView tvFechaFin;
        private TextView tvDuracion;
        private TextView tvEstado;

        public HistorialViewHolder(@NonNull View itemView) {
            super(itemView);
            cardMedicamento = itemView.findViewById(R.id.cardMedicamento);
            ivIcono = itemView.findViewById(R.id.ivIcono);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            tvPresentacion = itemView.findViewById(R.id.tvPresentacion);
            tvFechaInicio = itemView.findViewById(R.id.tvFechaInicio);
            tvFechaFin = itemView.findViewById(R.id.tvFechaFin);
            tvDuracion = itemView.findViewById(R.id.tvDuracion);
            tvEstado = itemView.findViewById(R.id.tvEstado);
        }

        public void bind(Medicamento medicamento) {
            // Configurar ícono
            ivIcono.setImageResource(medicamento.getIconoPresentacion());

            // Configurar nombre
            tvNombre.setText(medicamento.getNombre());

            // Configurar presentación
            tvPresentacion.setText(medicamento.getPresentacion());

            // Configurar fechas
            if (medicamento.getFechaInicioTratamiento() != null) {
                tvFechaInicio.setText("Inicio: " + dateFormat.format(medicamento.getFechaInicioTratamiento()));
            } else {
                tvFechaInicio.setText("Inicio: No disponible");
            }

            if (medicamento.getFechaVencimiento() != null) {
                tvFechaFin.setText("Fin: " + dateFormat.format(medicamento.getFechaVencimiento()));
            } else {
                tvFechaFin.setText("Fin: No disponible");
            }

            // Configurar duración
            if (medicamento.getDiasTratamiento() > 0) {
                tvDuracion.setText("Duración: " + medicamento.getDiasTratamiento() + " días");
            } else {
                tvDuracion.setText("Duración: Crónico");
            }

            // Configurar estado
            if (medicamento.estaVencido()) {
                tvEstado.setText("Vencido");
                tvEstado.setTextColor(context.getColor(R.color.error));
            } else if (medicamento.isPausado()) {
                tvEstado.setText("Completado");
                tvEstado.setTextColor(context.getColor(R.color.success));
            } else {
                tvEstado.setText("Activo");
                tvEstado.setTextColor(context.getColor(R.color.primary));
            }

            // Configurar color de fondo
            cardMedicamento.setCardBackgroundColor(medicamento.getColor());
        }
    }
}
