package com.controlmedicamentos.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.button.MaterialButton;
import com.controlmedicamentos.myapplication.adapters.HistorialAdapter;
import com.controlmedicamentos.myapplication.models.Medicamento;
import com.controlmedicamentos.myapplication.services.AuthService;
import com.controlmedicamentos.myapplication.services.FirebaseService;
import com.controlmedicamentos.myapplication.utils.NetworkUtils;
import android.content.Intent;
import java.util.ArrayList;
import java.util.List;

public class HistorialActivity extends AppCompatActivity {

    private BarChart chartAdherencia;
    private RecyclerView rvTratamientosConcluidos;
    private TextView tvEstadisticasGenerales;
    private MaterialButton btnVolver;
    // Botones de navegación
    private MaterialButton btnNavHome, btnNavNuevaMedicina, btnNavBotiquin, btnNavAjustes;
    private HistorialAdapter adapter;
    private List<Medicamento> tratamientosConcluidos = new ArrayList<>();
    private AuthService authService;
    private FirebaseService firebaseService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Ocultar ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        setContentView(R.layout.activity_historial);

        // Inicializar servicios
        authService = new AuthService();
        firebaseService = new FirebaseService();

        // Verificar autenticación
        if (!authService.isUserLoggedIn()) {
            finish();
            return;
        }

        inicializarVistas();
        configurarGrafico();
        configurarRecyclerView();
        cargarDatos();
        configurarListeners();
        configurarNavegacion();
    }

    private void inicializarVistas() {
        chartAdherencia = findViewById(R.id.chartAdherencia);
        rvTratamientosConcluidos = findViewById(R.id.rvTratamientosConcluidos);
        tvEstadisticasGenerales = findViewById(R.id.tvEstadisticasGenerales);
        btnVolver = findViewById(R.id.btnVolver);
        
        // Botones de navegación
        btnNavHome = findViewById(R.id.btnNavHome);
        btnNavNuevaMedicina = findViewById(R.id.btnNavNuevaMedicina);
        btnNavBotiquin = findViewById(R.id.btnNavBotiquin);
        btnNavAjustes = findViewById(R.id.btnNavAjustes);
    }

    private void configurarGrafico() {
        // Configurar el gráfico de barras
        chartAdherencia.getDescription().setEnabled(false);
        chartAdherencia.setDrawGridBackground(false);
        chartAdherencia.setDrawBarShadow(false);
        chartAdherencia.setDrawValueAboveBar(true);

        // Configurar eje X
        XAxis xAxis = chartAdherencia.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        // Configurar eje Y
        chartAdherencia.getAxisLeft().setDrawGridLines(true);
        chartAdherencia.getAxisRight().setEnabled(false);

        // Configurar leyenda
        chartAdherencia.getLegend().setEnabled(false);

        // Configurar animación
        chartAdherencia.animateY(1000);
    }

    private void configurarRecyclerView() {
        adapter = new HistorialAdapter(this, tratamientosConcluidos);
        rvTratamientosConcluidos.setLayoutManager(new LinearLayoutManager(this));
        rvTratamientosConcluidos.setAdapter(adapter);
    }

    private void cargarDatos() {
        // Verificar conexión a internet
        if (!NetworkUtils.isNetworkAvailable(this)) {
            tvEstadisticasGenerales.setText("No hay conexión a internet");
            return;
        }

        // Cargar todos los medicamentos desde Firebase
        firebaseService.obtenerMedicamentos(new FirebaseService.FirestoreListCallback() {
            @Override
            public void onSuccess(List<?> result) {
                List<Medicamento> todosLosMedicamentos = (List<Medicamento>) result;
                
                // Calcular estadísticas
                int totalMedicamentos = todosLosMedicamentos.size();
                int medicamentosActivos = 0;
                int medicamentosPausados = 0;

                for (Medicamento medicamento : todosLosMedicamentos) {
                    if (medicamento.isActivo() && !medicamento.isPausado()) {
                        medicamentosActivos++;
                    }
                    if (medicamento.isPausado()) {
                        medicamentosPausados++;
                    }
                }

                // Mostrar estadísticas generales (simplificadas por ahora)
                tvEstadisticasGenerales.setText(String.format(
                        "Medicamentos Activos: %d\nMedicamentos Pausados: %d\nTotal Medicamentos: %d",
                        medicamentosActivos,
                        medicamentosPausados,
                        totalMedicamentos
                ));

                // Cargar gráfico de adherencia por medicamento
                cargarGraficoAdherencia(todosLosMedicamentos);

                // Cargar tratamientos concluidos (pausados)
                tratamientosConcluidos = new ArrayList<>();
                for (Medicamento medicamento : todosLosMedicamentos) {
                    if (medicamento.isPausado()) {
                        tratamientosConcluidos.add(medicamento);
                    }
                }
                adapter.actualizarMedicamentos(tratamientosConcluidos);
            }

            @Override
            public void onError(Exception exception) {
                tvEstadisticasGenerales.setText("Error al cargar datos");
            }
        });
    }

    private void cargarGraficoAdherencia(List<Medicamento> medicamentos) {
        if (medicamentos.isEmpty()) {
            return;
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        // Por ahora mostramos un gráfico simplificado
        // TODO: Calcular adherencia real desde las tomas en Firebase
        for (int i = 0; i < medicamentos.size() && i < 5; i++) { // Máximo 5 medicamentos en el gráfico
            Medicamento medicamento = medicamentos.get(i);
            // Valor temporal - se calculará desde las tomas reales más adelante
            float adherencia = 85.0f; // Placeholder
            entries.add(new BarEntry(i, adherencia));
            labels.add(medicamento.getNombre());
        }

        BarDataSet dataSet = new BarDataSet(entries, "Adherencia (%)");
        dataSet.setColor(getResources().getColor(R.color.primary));
        dataSet.setValueTextSize(12f);

        BarData barData = new BarData(dataSet);
        chartAdherencia.setData(barData);
        chartAdherencia.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chartAdherencia.invalidate();
    }

    private void configurarListeners() {
        if (btnVolver != null) {
            btnVolver.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
    }
    
    private void configurarNavegacion() {
        if (btnNavHome != null) {
            btnNavHome.setOnClickListener(v -> {
                Intent intent = new Intent(HistorialActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            });
        }
        
        if (btnNavNuevaMedicina != null) {
            btnNavNuevaMedicina.setOnClickListener(v -> {
                Intent intent = new Intent(HistorialActivity.this, NuevaMedicinaActivity.class);
                startActivity(intent);
            });
        }
        
        if (btnNavBotiquin != null) {
            btnNavBotiquin.setOnClickListener(v -> {
                Intent intent = new Intent(HistorialActivity.this, BotiquinActivity.class);
                startActivity(intent);
                finish();
            });
        }
        
        if (btnNavAjustes != null) {
            btnNavAjustes.setOnClickListener(v -> {
                Intent intent = new Intent(HistorialActivity.this, AjustesActivity.class);
                startActivity(intent);
                finish();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarDatos(); // Recargar datos al volver
    }
}