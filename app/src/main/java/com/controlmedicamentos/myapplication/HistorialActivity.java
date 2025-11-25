package com.controlmedicamentos.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.controlmedicamentos.myapplication.adapters.HistorialAdapter;
import com.controlmedicamentos.myapplication.models.AdherenciaIntervalo;
import com.controlmedicamentos.myapplication.models.AdherenciaResumen;
import com.controlmedicamentos.myapplication.models.Medicamento;
import com.controlmedicamentos.myapplication.models.Toma;
import com.controlmedicamentos.myapplication.services.AuthService;
import com.controlmedicamentos.myapplication.services.FirebaseService;
import com.controlmedicamentos.myapplication.utils.AdherenciaCalculator;
import com.controlmedicamentos.myapplication.utils.NetworkUtils;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class HistorialActivity extends AppCompatActivity {

    private BarChart chartAdherencia;
    private RecyclerView rvTratamientosConcluidos;
    private TextView tvEstadisticasGenerales;
    private MaterialButton btnVolver;
    // Botones de navegación
    private MaterialButton btnNavHome, btnNavNuevaMedicina, btnNavBotiquin, btnNavAjustes;
    private HistorialAdapter adapter;
    private List<Medicamento> tratamientosConcluidos = new ArrayList<>();
    private List<Medicamento> todosLosMedicamentos = new ArrayList<>();
    private List<Toma> tomasUsuario = new ArrayList<>();
    private AuthService authService;
    private FirebaseService firebaseService;

    // Plan de adherencia
    private TextInputLayout tilMedicamentosAdherencia;
    private AutoCompleteTextView actvMedicamentosAdherencia;
    private TextView tvResumenPlanAdherencia;
    private TextView tvEmptyPlanAdherencia;
    private LinearLayout layoutPlanCharts;
    private BarChart chartAdherenciaSemanal;
    private BarChart chartAdherenciaMensual;
    private View cardPlanAdherencia;
    private List<Medicamento> medicamentosPlan = new ArrayList<>();
    private ArrayAdapter<String> planAdapter;

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
        configurarGraficos();
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

        tilMedicamentosAdherencia = findViewById(R.id.tilMedicamentosAdherencia);
        actvMedicamentosAdherencia = findViewById(R.id.actvMedicamentosAdherencia);
        tvResumenPlanAdherencia = findViewById(R.id.tvResumenPlanAdherencia);
        tvEmptyPlanAdherencia = findViewById(R.id.tvEmptyPlanAdherencia);
        layoutPlanCharts = findViewById(R.id.layoutPlanCharts);
        chartAdherenciaSemanal = findViewById(R.id.chartAdherenciaSemanal);
        chartAdherenciaMensual = findViewById(R.id.chartAdherenciaMensual);
        cardPlanAdherencia = findViewById(R.id.cardPlanAdherencia);
    }

    private void configurarGraficos() {
        configurarBarChart(chartAdherencia);
        configurarBarChart(chartAdherenciaSemanal);
        configurarBarChart(chartAdherenciaMensual);
    }

    private void configurarBarChart(BarChart chart) {
        if (chart == null) {
            return;
        }
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setDrawBarShadow(false);
        chart.setDrawValueAboveBar(true);
        chart.setNoDataText(getString(R.string.adherence_plan_empty));
        chart.setScaleEnabled(false);
        chart.getAxisLeft().setAxisMaximum(100f);
        chart.getAxisLeft().setAxisMinimum(0f);
        chart.getAxisLeft().setDrawGridLines(true);
        chart.getAxisRight().setEnabled(false);
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        chart.getLegend().setEnabled(false);
        chart.animateY(800);
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
                todosLosMedicamentos = result != null
                    ? (List<Medicamento>) result
                    : new ArrayList<>();
                cargarTomasUsuario();
            }

            @Override
            public void onError(Exception exception) {
                tvEstadisticasGenerales.setText("Error al cargar datos");
            }
        });
    }

    private void cargarTomasUsuario() {
        firebaseService.obtenerTomasUsuario(new FirebaseService.FirestoreListCallback() {
            @Override
            public void onSuccess(List<?> result) {
                tomasUsuario = result != null ? (List<Toma>) result : new ArrayList<>();
                procesarInformacion();
            }

            @Override
            public void onError(Exception exception) {
                tvEstadisticasGenerales.setText("Error al obtener tomas del usuario");
            }
        });
    }

    private void procesarInformacion() {
        if (todosLosMedicamentos.isEmpty()) {
            tvEstadisticasGenerales.setText(getString(R.string.loading_statistics));
            return;
        }

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

        tvEstadisticasGenerales.setText(String.format(
            Locale.getDefault(),
            "Medicamentos Activos: %d\nMedicamentos Pausados: %d\nTotal Medicamentos: %d",
            medicamentosActivos,
            medicamentosPausados,
            totalMedicamentos
        ));

        List<AdherenciaResumen> resumenes = new ArrayList<>();
        tratamientosConcluidos = new ArrayList<>();

        for (Medicamento medicamento : todosLosMedicamentos) {
            List<Toma> tomasMedicamento = AdherenciaCalculator.filtrarTomasPorMedicamento(
                tomasUsuario,
                medicamento.getId());
            boolean esOcasional = medicamento.getTomasDiarias() == 0;
            if (esOcasional && tomasMedicamento.isEmpty()) {
                continue; // Ocasionales solo aparecen si tuvieron tomas
            }

            resumenes.add(AdherenciaCalculator.calcularResumenGeneral(medicamento, tomasMedicamento));

            if (medicamento.isPausado()) {
                tratamientosConcluidos.add(medicamento);
            }
        }

        adapter.actualizarMedicamentos(tratamientosConcluidos);
        cargarGraficoAdherencia(resumenes);
        configurarPlanAdherencia();
    }

    private void cargarGraficoAdherencia(List<AdherenciaResumen> resumenes) {
        if (resumenes == null || resumenes.isEmpty()) {
            chartAdherencia.clear();
            chartAdherencia.invalidate();
            return;
        }

        Collections.sort(resumenes, Comparator.comparing(AdherenciaResumen::getPorcentaje).reversed());
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int limite = Math.min(5, resumenes.size());
        for (int i = 0; i < limite; i++) {
            AdherenciaResumen resumen = resumenes.get(i);
            entries.add(new BarEntry(i, resumen.getPorcentaje()));
            labels.add(resumen.getMedicamentoNombre());
        }

        BarDataSet dataSet = new BarDataSet(entries, "Adherencia (%)");
        dataSet.setColor(getResources().getColor(R.color.primary));
        dataSet.setValueTextSize(12f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);
        chartAdherencia.setData(barData);
        chartAdherencia.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chartAdherencia.invalidate();
    }

    private void configurarPlanAdherencia() {
        medicamentosPlan.clear();

        for (Medicamento medicamento : todosLosMedicamentos) {
            if (medicamento.getTomasDiarias() > 0 && medicamento.isActivo()) {
                medicamentosPlan.add(medicamento);
            }
        }

        if (medicamentosPlan.isEmpty()) {
            cardPlanAdherencia.setVisibility(View.GONE);
            return;
        }

        cardPlanAdherencia.setVisibility(View.VISIBLE);
        List<String> nombres = new ArrayList<>();
        for (Medicamento medicamento : medicamentosPlan) {
            nombres.add(medicamento.getNombre());
        }

        planAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nombres);
        actvMedicamentosAdherencia.setAdapter(planAdapter);
        actvMedicamentosAdherencia.setText("");
        actvMedicamentosAdherencia.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < medicamentosPlan.size()) {
                actualizarPlanAdherencia(medicamentosPlan.get(position));
            }
        });

        // Seleccionar el primer medicamento por defecto
        if (!medicamentosPlan.isEmpty()) {
            actvMedicamentosAdherencia.setText(medicamentosPlan.get(0).getNombre(), false);
            actualizarPlanAdherencia(medicamentosPlan.get(0));
        }
    }

    private void actualizarPlanAdherencia(Medicamento medicamento) {
        if (medicamento == null) {
            tvResumenPlanAdherencia.setText(getString(R.string.adherence_plan_summary_placeholder));
            tvEmptyPlanAdherencia.setVisibility(View.VISIBLE);
            layoutPlanCharts.setVisibility(View.GONE);
            return;
        }

        List<Toma> tomasMedicamento = AdherenciaCalculator.filtrarTomasPorMedicamento(
            tomasUsuario,
            medicamento.getId());

        AdherenciaResumen resumen = AdherenciaCalculator.calcularResumenGeneral(medicamento, tomasMedicamento);
        int porcentaje = Math.round(resumen.getPorcentaje());
        tvResumenPlanAdherencia.setText(getString(
            R.string.adherence_plan_summary,
            porcentaje,
            resumen.getTomasRealizadas(),
            resumen.getTomasEsperadas()
        ));

        List<AdherenciaIntervalo> datosSemanales = AdherenciaCalculator.calcularAdherenciaSemanal(medicamento, tomasMedicamento);
        List<AdherenciaIntervalo> datosMensuales = AdherenciaCalculator.calcularAdherenciaMensual(medicamento, tomasMedicamento);

        boolean sinDatos = datosSemanales.isEmpty() && datosMensuales.isEmpty();
        tvEmptyPlanAdherencia.setVisibility(sinDatos ? View.VISIBLE : View.GONE);
        layoutPlanCharts.setVisibility(sinDatos ? View.GONE : View.VISIBLE);

        actualizarChartIntervalos(chartAdherenciaSemanal, datosSemanales);
        actualizarChartIntervalos(chartAdherenciaMensual, datosMensuales);
    }

    private void actualizarChartIntervalos(BarChart chart, List<AdherenciaIntervalo> datos) {
        if (chart == null) return;

        if (datos == null || datos.isEmpty()) {
            chart.clear();
            chart.invalidate();
            return;
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < datos.size(); i++) {
            AdherenciaIntervalo intervalo = datos.get(i);
            entries.add(new BarEntry(i, intervalo.getPorcentaje()));
            labels.add(intervalo.getEtiqueta());
        }

        BarDataSet dataSet = new BarDataSet(entries, "% cumplimiento");
        dataSet.setColor(getResources().getColor(R.color.primary));
        dataSet.setValueTextSize(10f);
        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);

        chart.setData(data);
        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chart.invalidate();
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