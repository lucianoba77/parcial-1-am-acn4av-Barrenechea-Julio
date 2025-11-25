package com.controlmedicamentos.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.content.ComponentName;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.controlmedicamentos.myapplication.adapters.MedicamentoAdapter;
import com.controlmedicamentos.myapplication.models.Medicamento;
import com.controlmedicamentos.myapplication.models.Toma;
import com.controlmedicamentos.myapplication.models.TomaProgramada;
import com.controlmedicamentos.myapplication.services.AuthService;
import com.controlmedicamentos.myapplication.services.FirebaseService;
import com.controlmedicamentos.myapplication.services.TomaStateCheckerService;
import com.controlmedicamentos.myapplication.services.TomaTrackingService;
import com.controlmedicamentos.myapplication.utils.NetworkUtils;
import com.controlmedicamentos.myapplication.utils.StockAlertUtils;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
    private TomaTrackingService tomaTrackingService;
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
            setContentView(R.layout.activity_main);

            // Inicializar servicios
            authService = new AuthService();
            firebaseService = new FirebaseService();
            tomaTrackingService = new TomaTrackingService(this);

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
        
        // Iniciar servicio de verificación de estados de tomas
        iniciarServicioVerificacionTomas();
            
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
                    
                    // Inicializar tomas del día para cada medicamento
                    for (Medicamento med : medicamentos) {
                        tomaTrackingService.inicializarTomasDia(med);
                    }
                    
                    // Filtrar medicamentos: solo mostrar en dashboard los que tienen
                    // tomas diarias > 0 y horario configurado (medicamentos regulares)
                    // Lógica consistente con React: DashboardScreen.jsx líneas 19-23
                    List<Medicamento> medicamentosParaDashboard = new ArrayList<>();
                    for (Medicamento med : medicamentos) {
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
                        }
                        // Los medicamentos ocasionales (tomasDiarias = 0) no aparecen aquí,
                        // solo en el botiquín
                    }
                    
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
                            
                            // Inicializar tomas del día para cada medicamento activo
                            for (Medicamento med : medicamentosActivos) {
                                tomaTrackingService.inicializarTomasDia(med);
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
                                                // Lista vacía, no hay medicamentos programados
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

    /**
     * Ordena los medicamentos por la próxima toma programada.
     * Los medicamentos con la toma más próxima aparecen primero.
     * Los medicamentos con tomas omitidas van al final.
     */
    private void ordenarMedicamentosPorHorario() {
        if (medicamentos == null || medicamentos.isEmpty()) {
            return;
        }

        // Obtener hora actual
        Calendar cal = Calendar.getInstance();
        int horaActual = cal.get(Calendar.HOUR_OF_DAY);
        int minutoActual = cal.get(Calendar.MINUTE);
        int minutosActuales = horaActual * 60 + minutoActual;

        // Ordenar usando un comparador personalizado
        medicamentos.sort((med1, med2) -> {
            // Verificar si tienen tomas omitidas
            boolean tieneOmitidas1 = tomaTrackingService.tieneTomasOmitidas(med1.getId());
            boolean tieneOmitidas2 = tomaTrackingService.tieneTomasOmitidas(med2.getId());
            
            // Si uno tiene omitidas y el otro no, el que tiene omitidas va al final
            if (tieneOmitidas1 && !tieneOmitidas2) {
                return 1; // med1 va después
            }
            if (!tieneOmitidas1 && tieneOmitidas2) {
                return -1; // med2 va después
            }
            
            // Si ambos tienen o no tienen omitidas, ordenar por próxima toma
            long proximaToma1 = calcularMinutosHastaProximaToma(med1, minutosActuales);
            long proximaToma2 = calcularMinutosHastaProximaToma(med2, minutosActuales);
            return Long.compare(proximaToma1, proximaToma2);
        });
    }

    /**
     * Calcula los minutos hasta la próxima toma de un medicamento.
     * @param medicamento El medicamento a evaluar
     * @param minutosActuales Minutos transcurridos desde medianoche (0-1439)
     * @return Minutos hasta la próxima toma (0 si es la próxima hora, o minutos hasta mañana)
     */
    private long calcularMinutosHastaProximaToma(Medicamento medicamento, int minutosActuales) {
        if (medicamento == null || medicamento.getHorariosTomas() == null || 
            medicamento.getHorariosTomas().isEmpty()) {
            // Si no tiene horarios, ponerlo al final
            return Long.MAX_VALUE;
        }

        List<String> horarios = medicamento.getHorariosTomas();
        long minutosMinimos = Long.MAX_VALUE;

        // Buscar el próximo horario de hoy
        for (String horario : horarios) {
            if (horario == null || horario.isEmpty()) {
                continue;
            }

            try {
                String[] partes = horario.split(":");
                if (partes.length < 2) {
                    continue;
                }

                int hora = Integer.parseInt(partes[0]);
                int minuto = Integer.parseInt(partes[1]);
                int minutosHorario = hora * 60 + minuto;

                if (minutosHorario >= minutosActuales) {
                    // Este horario es hoy
                    long diferencia = minutosHorario - minutosActuales;
                    if (diferencia < minutosMinimos) {
                        minutosMinimos = diferencia;
                    }
                } else {
                    // Este horario ya pasó hoy, será mañana
                    long minutosHastaMedianoche = (24 * 60) - minutosActuales;
                    long minutosDesdeMedianoche = minutosHorario;
                    long diferencia = minutosHastaMedianoche + minutosDesdeMedianoche;
                    if (diferencia < minutosMinimos) {
                        minutosMinimos = diferencia;
                    }
                }
            } catch (NumberFormatException e) {
                // Ignorar horarios con formato inválido
                continue;
            }
        }

        // Si no se encontró ningún horario válido, ponerlo al final
        return minutosMinimos == Long.MAX_VALUE ? Long.MAX_VALUE : minutosMinimos;
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
        if (medicamento == null) {
            return;
        }

        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No hay conexión a internet", Toast.LENGTH_LONG).show();
            return;
        }

        if (medicamento.getId() == null || medicamento.getId().isEmpty()) {
            Toast.makeText(this, "Medicamento sin identificador válido", Toast.LENGTH_SHORT).show();
            return;
        }

        final int stockAnterior = medicamento.getStockActual();
        final int diasRestantesAnteriores = medicamento.getDiasRestantesDuracion();
        final boolean estabaPausado = medicamento.isPausado();

        medicamento.consumirDosis();
        final boolean tratamientoCompletado = medicamento.estaAgotado();
        if (tratamientoCompletado) {
            medicamento.pausarMedicamento();
        }

        // Obtener el horario de la toma más próxima para marcarla como tomada
        String horarioToma = obtenerHorarioTomaProxima(medicamento);
        
        Toma toma = new Toma();
        toma.setMedicamentoId(medicamento.getId());
        toma.setMedicamentoNombre(medicamento.getNombre());
        Date ahora = new Date();
        toma.setFechaHoraProgramada(ahora);
        toma.setFechaHoraTomada(ahora);
        toma.setEstado(Toma.EstadoToma.TOMADA);
        toma.setObservaciones("Registrada desde el panel principal");

        firebaseService.guardarToma(toma, new FirebaseService.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                // Marcar la toma como tomada en el tracking service
                if (horarioToma != null) {
                    tomaTrackingService.marcarTomaComoTomada(medicamento.getId(), horarioToma);
                }
                
                firebaseService.actualizarMedicamento(medicamento, new FirebaseService.FirestoreCallback() {
                    @Override
                    public void onSuccess(Object updateResult) {
                        // Reordenar medicamentos después de marcar como tomada
                        ordenarMedicamentosPorHorario();
                        adapter.notifyDataSetChanged();
                        Toast.makeText(MainActivity.this,
                                "✓ " + medicamento.getNombre() + " marcado como tomado",
                                Toast.LENGTH_SHORT).show();
                        if (tratamientoCompletado) {
                            Toast.makeText(MainActivity.this,
                                    "¡Tratamiento de " + medicamento.getNombre() + " completado!",
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onError(Exception exception) {
                        revertirCambiosMedicamento(medicamento, stockAnterior, diasRestantesAnteriores, estabaPausado);
                        Toast.makeText(MainActivity.this,
                                "Error al actualizar medicamento",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(Exception exception) {
                revertirCambiosMedicamento(medicamento, stockAnterior, diasRestantesAnteriores, estabaPausado);
                Toast.makeText(MainActivity.this,
                        "Error al registrar la toma. Intenta nuevamente.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Obtiene el horario de la toma más próxima del medicamento
     */
    private String obtenerHorarioTomaProxima(Medicamento medicamento) {
        if (medicamento == null || medicamento.getHorariosTomas() == null || 
            medicamento.getHorariosTomas().isEmpty()) {
            return null;
        }
        
        Calendar ahora = Calendar.getInstance();
        int horaActual = ahora.get(Calendar.HOUR_OF_DAY);
        int minutoActual = ahora.get(Calendar.MINUTE);
        int minutosActuales = horaActual * 60 + minutoActual;
        
        String horarioProximo = null;
        long minutosMinimos = Long.MAX_VALUE;
        
        for (String horario : medicamento.getHorariosTomas()) {
            try {
                String[] partes = horario.split(":");
                if (partes.length != 2) {
                    continue;
                }
                
                int hora = Integer.parseInt(partes[0]);
                int minuto = Integer.parseInt(partes[1]);
                int minutosHorario = hora * 60 + minuto;
                
                long diferencia;
                if (minutosHorario >= minutosActuales) {
                    diferencia = minutosHorario - minutosActuales;
                } else {
                    diferencia = (24 * 60) - minutosActuales + minutosHorario;
                }
                
                if (diferencia < minutosMinimos) {
                    minutosMinimos = diferencia;
                    horarioProximo = horario;
                }
            } catch (NumberFormatException e) {
                continue;
            }
        }
        
        return horarioProximo;
    }
    
    private void revertirCambiosMedicamento(Medicamento medicamento,
                                            int stockAnterior,
                                            int diasRestantesAnteriores,
                                            boolean estabaPausado) {
        medicamento.setStockActual(stockAnterior);
        medicamento.setDiasRestantesDuracion(diasRestantesAnteriores);
        medicamento.setPausado(estabaPausado);
    }

    @Override
    public void onMedicamentoClick(Medicamento medicamento) {
        if (medicamento == null || medicamento.getId() == null) {
            Toast.makeText(this, "Error al abrir detalles del medicamento", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Abrir pantalla de detalles
        Intent intent = DetallesMedicamentoActivity.createIntent(this, medicamento.getId());
        startActivity(intent);
    }
    
    @Override
    public void onPosponerClick(Medicamento medicamento) {
        if (medicamento == null) {
            return;
        }
        
        // Obtener el horario de la toma más próxima en estado de alerta
        String horarioToma = obtenerHorarioTomaEnAlerta(medicamento);
        if (horarioToma == null) {
            Toast.makeText(this, "No hay tomas pendientes para posponer", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Intentar posponer la toma
        boolean pospuesta = tomaTrackingService.posponerToma(medicamento.getId(), horarioToma);
        
        if (pospuesta) {
            Toast.makeText(this, "Toma pospuesta 10 minutos. Quedan " + 
                    (3 - obtenerPosposicionesRestantes(medicamento, horarioToma)) + 
                    " posposiciones disponibles", Toast.LENGTH_LONG).show();
            
            // Reordenar y actualizar la lista
            ordenarMedicamentosPorHorario();
            adapter.notifyDataSetChanged();
        } else {
            Toast.makeText(this, "No se puede posponer más. Máximo 3 posposiciones alcanzado. La toma se considera omitida.", 
                    Toast.LENGTH_LONG).show();
            
            // Reordenar y actualizar la lista (el medicamento irá al final)
            ordenarMedicamentosPorHorario();
            adapter.notifyDataSetChanged();
        }
    }
    
    /**
     * Obtiene el horario de la toma más próxima en estado de alerta
     */
    private String obtenerHorarioTomaEnAlerta(Medicamento medicamento) {
        List<TomaProgramada> tomas = tomaTrackingService.obtenerTomasMedicamento(medicamento.getId());
        
        for (TomaProgramada toma : tomas) {
            if (!toma.isTomada() && 
                (toma.getEstado() == TomaProgramada.EstadoTomaProgramada.ALERTA_ROJA ||
                 toma.getEstado() == TomaProgramada.EstadoTomaProgramada.RETRASO)) {
                return toma.getHorario();
            }
        }
        
        return null;
    }
    
    /**
     * Obtiene las posposiciones restantes para una toma
     */
    private int obtenerPosposicionesRestantes(Medicamento medicamento, String horario) {
        List<TomaProgramada> tomas = tomaTrackingService.obtenerTomasMedicamento(medicamento.getId());
        
        for (TomaProgramada toma : tomas) {
            if (toma.getHorario().equals(horario)) {
                return 3 - toma.getPosposiciones();
            }
        }
        
        return 0;
    }

    /**
     * Inicia el servicio de verificación de estados de tomas
     */
    private void iniciarServicioVerificacionTomas() {
        Intent serviceIntent = new Intent(this, TomaStateCheckerService.class);
        startService(serviceIntent);
        Log.d(TAG, "Servicio de verificación de tomas iniciado");
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