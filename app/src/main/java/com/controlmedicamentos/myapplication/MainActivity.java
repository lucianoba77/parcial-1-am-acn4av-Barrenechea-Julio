package com.controlmedicamentos.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.controlmedicamentos.myapplication.adapters.MedicamentoAdapter;
import com.controlmedicamentos.myapplication.models.Medicamento;
import com.controlmedicamentos.myapplication.services.AuthService;
import com.controlmedicamentos.myapplication.services.FirebaseService;
import com.controlmedicamentos.myapplication.utils.NetworkUtils;
import com.controlmedicamentos.myapplication.utils.StockAlertUtils;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MedicamentoAdapter.OnMedicamentoClickListener {

    private static final String TAG = "MainActivity";
    private RecyclerView rvMedicamentos;
    // Botones de navegación
    private MaterialButton btnNavHome, btnNavNuevaMedicina, btnNavBotiquin, btnNavAjustes;
    private ProgressBar progressBar;
    private MedicamentoAdapter adapter;
    private List<Medicamento> medicamentos;
    private AuthService authService;
    private FirebaseService firebaseService;
    private ListenerRegistration medicamentosListener;
    private boolean listenerYaActualizo = false; // Flag para evitar que la carga inicial sobrescriba los datos del listener

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Ocultar ActionBar/Toolbar para que no muestre el título duplicado
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        try {
            Log.d(TAG, "=== Iniciando MainActivity ===");
            setContentView(R.layout.activity_main);

            // Inicializar servicios
            authService = new AuthService();
            firebaseService = new FirebaseService();

            // Verificar autenticación
            if (!authService.isUserLoggedIn()) {
                Log.w(TAG, "Usuario no autenticado, redirigiendo a login");
                irALogin();
                return;
            }

            Log.d(TAG, "Usuario autenticado, continuando con inicialización");

            // Inicializar vistas
            inicializarVistas();

            // Configurar RecyclerView
            configurarRecyclerView();

            // Cargar datos desde Firebase
            cargarDatosDesdeFirebase();

            // Configurar navegación
            configurarNavegacion();
            
            Log.d(TAG, "MainActivity inicializada correctamente");
        } catch (Exception e) {
            Log.e(TAG, "Error crítico en onCreate", e);
            Toast.makeText(this, "Error al iniciar la aplicación", Toast.LENGTH_LONG).show();
            // Intentar ir a login como fallback
            try {
                irALogin();
            } catch (Exception ex) {
                Log.e(TAG, "Error al redirigir a login", ex);
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remover listener cuando se destruye la actividad
        if (medicamentosListener != null) {
            medicamentosListener.remove();
        }
    }

    private void irALogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void inicializarVistas() {
        try {
            rvMedicamentos = findViewById(R.id.rvMedicamentos);
            // Botones de navegación
            btnNavHome = findViewById(R.id.btnNavHome);
            btnNavNuevaMedicina = findViewById(R.id.btnNavNuevaMedicina);
            btnNavBotiquin = findViewById(R.id.btnNavBotiquin);
            btnNavAjustes = findViewById(R.id.btnNavAjustes);
            
            // Verificar que las vistas críticas existan
            if (rvMedicamentos == null) {
                Log.e(TAG, "rvMedicamentos es null!");
                throw new RuntimeException("RecyclerView no encontrado en el layout");
            }
            
            // Intentar encontrar ProgressBar si existe en el layout
            progressBar = findViewById(R.id.progressBar);
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }
            
            Log.d(TAG, "Vistas inicializadas correctamente");
        } catch (Exception e) {
            Log.e(TAG, "Error al inicializar vistas", e);
            throw e;
        }
    }

    private void configurarRecyclerView() {
        medicamentos = new ArrayList<>();
        adapter = new MedicamentoAdapter(this, medicamentos);
        adapter.setOnMedicamentoClickListener(this);

        rvMedicamentos.setLayoutManager(new LinearLayoutManager(this));
        rvMedicamentos.setAdapter(adapter);
    }

    private void cargarDatosDesdeFirebase() {
        // Verificar conexión a internet
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No hay conexión a internet", Toast.LENGTH_LONG).show();
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            return;
        }

        // Cargar medicamentos activos desde Firebase
        Log.d(TAG, "Iniciando carga de medicamentos desde Firebase");
        firebaseService.obtenerMedicamentosActivos(new FirebaseService.FirestoreListCallback() {
            @Override
            public void onSuccess(List<?> result) {
                try {
                    Log.d(TAG, "Medicamentos cargados exitosamente: " + (result != null ? result.size() : 0));
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    
                    if (result != null) {
                        medicamentos = (List<Medicamento>) result;
                    } else {
                        medicamentos = new ArrayList<>();
                    }
                    
                    // Filtrar medicamentos: solo mostrar en dashboard los que tienen
                    // tomas diarias > 0 y horario configurado (medicamentos regulares)
                    // Lógica consistente con React: DashboardScreen.jsx líneas 19-23
                    List<Medicamento> medicamentosParaDashboard = new ArrayList<>();
                    for (Medicamento med : medicamentos) {
                        // Log detallado para debugging
                        Log.d(TAG, "Medicamento: " + med.getNombre() + 
                            ", Activo: " + med.isActivo() + 
                            ", TomasDiarias: " + med.getTomasDiarias() + 
                            ", PrimeraToma: '" + med.getHorarioPrimeraToma() + "'");
                        
                        // Mostrar solo si:
                        // 1. activo !== false (activo es true o no está definido)
                        // 2. tomasDiarias > 0
                        // 3. primeraToma está definida (no null, no vacío, no "00:00")
                        if ((med.isActivo()) && 
                            med.getTomasDiarias() > 0 && 
                            med.getHorarioPrimeraToma() != null && 
                            !med.getHorarioPrimeraToma().isEmpty() &&
                            !med.getHorarioPrimeraToma().equals("00:00")) {
                            medicamentosParaDashboard.add(med);
                            Log.d(TAG, "  -> INCLUIDO en dashboard");
                        } else {
                            Log.d(TAG, "  -> EXCLUIDO del dashboard");
                        }
                        // Los medicamentos ocasionales (tomasDiarias = 0) no aparecen aquí,
                        // solo en el botiquín
                    }
                    Log.d(TAG, "Total medicamentos para dashboard: " + medicamentosParaDashboard.size());
                    
                    // Verificar que el adapter esté inicializado
                    // Solo actualizar si el listener no ha actualizado ya (evitar sobrescribir datos más recientes)
                    if (adapter != null && !listenerYaActualizo) {
                        // Ordenar por horario más próximo
                        medicamentos = medicamentosParaDashboard;
                        ordenarMedicamentosPorHorario();
                        adapter.actualizarMedicamentos(medicamentos);
                        Log.d(TAG, "Carga inicial: dashboard actualizado con " + medicamentos.size() + " medicamentos");
                    } else if (listenerYaActualizo) {
                        Log.d(TAG, "Carga inicial: omitida porque el listener ya actualizó los datos");
                    } else {
                        Log.e(TAG, "Adapter es null, no se puede actualizar");
                    }
                    
                    // Verificar alertas de stock con TODOS los medicamentos (no solo los del dashboard)
                    // Consistente con React: useStockAlerts
                    verificarAlertasStock((List<Medicamento>) result);
                    
                    // NO mostrar el mensaje de "no hay medicamentos" en la carga inicial porque:
                    // 1. El listener en tiempo real se ejecutará inmediatamente después y puede actualizar los datos
                    // 2. Si realmente no hay medicamentos, el usuario puede verlo en la lista vacía
                    // 3. El mensaje puede aparecer brevemente y confundir al usuario
                    // El mensaje se mostrará solo en el listener si después de un delay sigue vacío
                } catch (Exception e) {
                    Log.e(TAG, "Error al procesar medicamentos cargados", e);
                    Toast.makeText(MainActivity.this, "Error al procesar medicamentos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Exception exception) {
                Log.e(TAG, "Error al cargar medicamentos desde Firebase", exception);
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                
                String mensaje = "Error al cargar medicamentos";
                if (exception != null && exception.getMessage() != null) {
                    mensaje += ": " + exception.getMessage();
                }
                Toast.makeText(MainActivity.this, mensaje, Toast.LENGTH_LONG).show();
            }
        });

        // Configurar listener para actualizaciones en tiempo real
        try {
            configurarListenerTiempoReal();
        } catch (Exception e) {
            Log.e(TAG, "Error al configurar listener de tiempo real", e);
            // No es crítico, continuar sin el listener
        }
    }

    private void configurarListenerTiempoReal() {
        try {
            Log.d(TAG, "Configurando listener de tiempo real");
            medicamentosListener = firebaseService.agregarListenerMedicamentos(
                new FirebaseService.FirestoreListCallback() {
                    @Override
                    public void onSuccess(List<?> result) {
                        try {
                            Log.d(TAG, "Listener: medicamentos recibidos: " + (result != null ? result.size() : 0));
                            List<Medicamento> todosLosMedicamentos = new ArrayList<>();
                            if (result != null) {
                                todosLosMedicamentos = (List<Medicamento>) result;
                            }
                            
                            // Filtrar medicamentos activos: solo activos y no pausados
                            List<Medicamento> medicamentosActivos = new ArrayList<>();
                            for (Medicamento med : todosLosMedicamentos) {
                                if (med.isActivo() && !med.isPausado()) {
                                    medicamentosActivos.add(med);
                                }
                            }
                            
                            // Filtrar medicamentos: solo mostrar en dashboard los que tienen
                            // tomas diarias > 0 y horario configurado (medicamentos regulares)
                            // Lógica consistente con React: DashboardScreen.jsx líneas 19-23
                            List<Medicamento> medicamentosParaDashboard = new ArrayList<>();
                            for (Medicamento med : medicamentosActivos) {
                                // Mostrar solo si:
                                // 1. activo !== false (ya filtrado arriba)
                                // 2. tomasDiarias > 0
                                // 3. primeraToma está definida (no null, no vacío, no "00:00")
                                if (med.getTomasDiarias() > 0 && 
                                    med.getHorarioPrimeraToma() != null && 
                                    !med.getHorarioPrimeraToma().isEmpty() &&
                                    !med.getHorarioPrimeraToma().equals("00:00")) {
                                    medicamentosParaDashboard.add(med);
                                }
                                // Los medicamentos ocasionales (tomasDiarias = 0) no aparecen aquí,
                                // solo en el botiquín
                            }
                            
                            medicamentos = medicamentosParaDashboard;
                            
                            if (adapter != null) {
                                ordenarMedicamentosPorHorario();
                                adapter.actualizarMedicamentos(medicamentos);
                                listenerYaActualizo = true; // Marcar DESPUÉS de actualizar el adapter
                                Log.d(TAG, "Listener: dashboard actualizado con " + medicamentos.size() + " medicamentos");
                                
                                // Solo mostrar mensaje si realmente no hay medicamentos después de que el listener se haya ejecutado
                                // y solo si la lista sigue vacía después de un pequeño delay para evitar mensajes intermitentes
                                if (medicamentos.isEmpty()) {
                                    // Usar un handler para mostrar el mensaje después de un pequeño delay
                                    // Esto evita que el mensaje aparezca brevemente antes de que los datos se carguen
                                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            // Verificar nuevamente si sigue vacío (por si acaso se actualizó en el delay)
                                            if (adapter != null && adapter.getItemCount() == 0) {
                                                // No mostrar Toast, solo log para debugging
                                                Log.d(TAG, "No hay medicamentos programados para mostrar en el dashboard");
                                            }
                                        }
                                    }, 500); // Esperar 500ms antes de verificar
                                }
                            }
                            
                            // Verificar alertas de stock cuando se actualizan los medicamentos
                            verificarAlertasStock(todosLosMedicamentos);
                        } catch (Exception e) {
                            Log.e(TAG, "Error en callback del listener", e);
                        }
                    }

                    @Override
                    public void onError(Exception exception) {
                        Log.w(TAG, "Error en listener de tiempo real", exception);
                        // Silencioso para el usuario, solo log
                    }
                }
            );
            
            if (medicamentosListener == null) {
                Log.w(TAG, "Listener de medicamentos retornó null (usuario no autenticado?)");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al configurar listener de tiempo real", e);
            // No es crítico, continuar sin el listener
        }
    }

    private void ordenarMedicamentosPorHorario() {
        // Ordenar medicamentos por el horario de la toma más próxima
        // Por ahora ordenamiento simple, se puede mejorar más adelante
        // TODO: Implementar ordenamiento real por horario más próximo
    }

    private void configurarNavegacion() {
        // Botón Home - ya estamos en home, no hacer nada
        if (btnNavHome != null) {
            btnNavHome.setOnClickListener(v -> {
                // Ya estamos en home
            });
        }
        
        // Botón Nueva Medicina
        if (btnNavNuevaMedicina != null) {
            btnNavNuevaMedicina.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, NuevaMedicinaActivity.class);
                startActivity(intent);
            });
        }

        // Botón Botiquín
        if (btnNavBotiquin != null) {
            btnNavBotiquin.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, BotiquinActivity.class);
                startActivity(intent);
                finish();
            });
        }

        // Botón Ajustes
        if (btnNavAjustes != null) {
            btnNavAjustes.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, AjustesActivity.class);
                startActivity(intent);
                finish();
            });
        }
    }

    // Implementar métodos de la interfaz
    @Override
    public void onTomadoClick(Medicamento medicamento) {
        // Consumir dosis
        medicamento.consumirDosis();

        // Mostrar confirmación
        Toast.makeText(this, "✓ " + medicamento.getNombre() + " marcado como tomado", Toast.LENGTH_SHORT).show();

        // Verificar si se completó el tratamiento
        if (medicamento.estaAgotado()) {
            Toast.makeText(this, "¡Tratamiento de " + medicamento.getNombre() + " completado!", Toast.LENGTH_LONG).show();
            // Pausar el medicamento
            medicamento.pausarMedicamento();
        }

        // Actualizar en Firebase
        firebaseService.actualizarMedicamento(medicamento, new FirebaseService.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                // Actualización exitosa - el listener de tiempo real actualizará la UI
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Exception exception) {
                Toast.makeText(MainActivity.this, "Error al actualizar medicamento", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMedicamentoClick(Medicamento medicamento) {
        // Mostrar detalles del medicamento
        Toast.makeText(this, "Detalles de " + medicamento.getNombre(), Toast.LENGTH_SHORT).show();
        // TODO: Implementar pantalla de detalles
    }

    /**
     * Verifica alertas de stock para todos los medicamentos
     * Consistente con React: useStockAlerts.js
     */
    private void verificarAlertasStock(List<Medicamento> todosLosMedicamentos) {
        // Obtener días de antelación desde SharedPreferences (por defecto 7)
        android.content.SharedPreferences prefs = getSharedPreferences("ControlMedicamentos", MODE_PRIVATE);
        int diasAntesAlerta = prefs.getInt("dias_antelacion_stock", 7);
        
        StockAlertUtils.verificarStock(todosLosMedicamentos, new StockAlertUtils.StockAlertListener() {
            @Override
            public void onStockAgotado(Medicamento medicamento) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, 
                        "⚠️ " + medicamento.getNombre() + " se ha agotado. Por favor, recarga tu stock.", 
                        Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onStockBajo(Medicamento medicamento, int diasRestantes, String mensaje) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "⚠️ " + mensaje, Toast.LENGTH_LONG).show();
                });
            }
        }, diasAntesAlerta);
    }
}