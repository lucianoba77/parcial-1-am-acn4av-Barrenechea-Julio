package com.controlmedicamentos.myapplication.services;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.controlmedicamentos.myapplication.models.Medicamento;
import com.controlmedicamentos.myapplication.models.Toma;
import com.controlmedicamentos.myapplication.models.TomaProgramada;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Servicio en segundo plano que verifica periódicamente el estado de las tomas programadas
 * y actualiza automáticamente los estados (retraso, omitida)
 */
public class TomaStateCheckerService extends Service {
    private static final String TAG = "TomaStateChecker";
    private static final long INTERVALO_VERIFICACION = 60 * 1000; // Verificar cada minuto
    
    private Handler handler;
    private Runnable verificacionRunnable;
    private FirebaseService firebaseService;
    private TomaTrackingService trackingService;
    private boolean ejecutando = false;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Servicio creado");
        handler = new Handler(Looper.getMainLooper());
        firebaseService = new FirebaseService();
        trackingService = new TomaTrackingService(this);
        
        verificacionRunnable = new Runnable() {
            @Override
            public void run() {
                if (ejecutando) {
                    verificarEstadosTomas();
                    handler.postDelayed(this, INTERVALO_VERIFICACION);
                }
            }
        };
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Servicio iniciado");
        ejecutando = true;
        verificarEstadosTomas();
        handler.postDelayed(verificacionRunnable, INTERVALO_VERIFICACION);
        
        // Retornar START_STICKY para que el servicio se reinicie si se mata
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Servicio destruido");
        ejecutando = false;
        if (handler != null && verificacionRunnable != null) {
            handler.removeCallbacks(verificacionRunnable);
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Servicio no vinculado
    }
    
    /**
     * Verifica y actualiza el estado de todas las tomas programadas
     */
    private void verificarEstadosTomas() {
        Log.d(TAG, "Verificando estados de tomas...");
        
        // Obtener todos los medicamentos activos
        firebaseService.obtenerMedicamentosActivos(new FirebaseService.FirestoreListCallback() {
            @Override
            public void onSuccess(List<?> result) {
                if (result == null) {
                    return;
                }
                
                List<Medicamento> medicamentos = (List<Medicamento>) result;
                Date ahora = new Date();
                
                for (Medicamento medicamento : medicamentos) {
                    if (!medicamento.isActivo() || medicamento.isPausado()) {
                        continue;
                    }
                    
                    // Inicializar tomas del día si no están inicializadas
                    trackingService.inicializarTomasDia(medicamento);
                    
                    // Obtener todas las tomas programadas del medicamento
                    List<TomaProgramada> tomas = trackingService.obtenerTomasMedicamento(medicamento.getId());
                    
                    for (TomaProgramada toma : tomas) {
                        if (toma.isTomada()) {
                            continue; // Ya fue tomada, no verificar
                        }
                        
                        Date fechaProgramada = toma.getFechaHoraProgramada();
                        if (fechaProgramada == null) {
                            continue;
                        }
                        
                        // Verificar si debe pasar a estado RETRASO (+10 minutos)
                        Date fechaRetraso = toma.calcularFechaRetraso();
                        if (fechaRetraso != null && ahora.after(fechaRetraso) && 
                            toma.getEstado() == TomaProgramada.EstadoTomaProgramada.ALERTA_ROJA) {
                            toma.setEstado(TomaProgramada.EstadoTomaProgramada.RETRASO);
                            if (toma.getFechaHoraRetraso() == null) {
                                toma.setFechaHoraRetraso(ahora);
                            }
                            Log.d(TAG, "Toma marcada como RETRASO: " + medicamento.getNombre() + " - " + toma.getHorario());
                        }
                        
                        // Verificar si debe pasar a estado OMITIDA (+1 hora)
                        Date fechaOmitida = toma.calcularFechaOmitida();
                        if (fechaOmitida != null && ahora.after(fechaOmitida) && 
                            toma.getEstado() != TomaProgramada.EstadoTomaProgramada.OMITIDA) {
                            toma.setEstado(TomaProgramada.EstadoTomaProgramada.OMITIDA);
                            if (toma.getFechaHoraOmitida() == null) {
                                toma.setFechaHoraOmitida(ahora);
                            }
                            
                            // Registrar la toma omitida en Firestore para afectar adherencia
                            registrarTomaOmitida(medicamento, toma);
                            
                            Log.d(TAG, "Toma marcada como OMITIDA: " + medicamento.getNombre() + " - " + toma.getHorario());
                        }
                    }
                }
            }
            
            @Override
            public void onError(Exception exception) {
                Log.e(TAG, "Error al verificar estados de tomas", exception);
            }
        });
    }
    
    /**
     * Registra una toma omitida en Firestore para que afecte el cálculo de adherencia
     */
    private void registrarTomaOmitida(Medicamento medicamento, TomaProgramada toma) {
        Toma tomaOmitida = new Toma();
        tomaOmitida.setMedicamentoId(medicamento.getId());
        tomaOmitida.setMedicamentoNombre(medicamento.getNombre());
        tomaOmitida.setFechaHoraProgramada(toma.getFechaHoraProgramada());
        tomaOmitida.setFechaHoraTomada(null); // No fue tomada
        tomaOmitida.setEstado(Toma.EstadoToma.PERDIDA);
        tomaOmitida.setObservaciones("Toma omitida automáticamente después de 1 hora sin tomar");
        
        firebaseService.guardarToma(tomaOmitida, new FirebaseService.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                Log.d(TAG, "Toma omitida registrada en Firestore: " + medicamento.getNombre());
            }
            
            @Override
            public void onError(Exception exception) {
                Log.e(TAG, "Error al registrar toma omitida en Firestore", exception);
            }
        });
    }
}

