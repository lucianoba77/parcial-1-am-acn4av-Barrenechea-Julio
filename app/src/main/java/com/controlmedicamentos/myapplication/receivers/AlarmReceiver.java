package com.controlmedicamentos.myapplication.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.controlmedicamentos.myapplication.services.NotificationService;
import com.controlmedicamentos.myapplication.services.FirebaseService;
import com.controlmedicamentos.myapplication.models.Medicamento;

/**
 * Receiver para manejar las alarmas programadas de medicamentos
 */
public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";
    private static final String EXTRA_MEDICAMENTO_ID = "medicamento_id";
    private static final String EXTRA_HORARIO = "horario";
    private static final String EXTRA_TIPO_ALERTA = "tipo_alerta";
    
    public static final int TIPO_ALERTA_AMARILLA = 1; // 10 minutos antes
    public static final int TIPO_ALERTA_ROJA = 2; // Horario exacto
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarma recibida");
        
        String medicamentoId = intent.getStringExtra(EXTRA_MEDICAMENTO_ID);
        String horario = intent.getStringExtra(EXTRA_HORARIO);
        int tipoAlerta = intent.getIntExtra(EXTRA_TIPO_ALERTA, TIPO_ALERTA_ROJA);
        
        if (medicamentoId == null || horario == null) {
            Log.e(TAG, "Datos de medicamento incompletos en la alarma");
            return;
        }
        
        // Obtener el medicamento desde Firebase
        FirebaseService firebaseService = new FirebaseService();
        firebaseService.obtenerMedicamento(medicamentoId, new FirebaseService.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                if (result instanceof Medicamento) {
                    Medicamento medicamento = (Medicamento) result;
                    
                    // Verificar que el medicamento esté activo
                    if (medicamento.isActivo() && !medicamento.isPausado()) {
                        // Actualizar estado de la toma programada
                        com.controlmedicamentos.myapplication.services.TomaTrackingService trackingService = 
                            new com.controlmedicamentos.myapplication.services.TomaTrackingService(context);
                        trackingService.inicializarTomasDia(medicamento);
                        
                        // Enviar notificación según el tipo de alarma
                        NotificationService notificationService = new NotificationService(context);
                        if (tipoAlerta == TIPO_ALERTA_AMARILLA) {
                            notificationService.enviarNotificacionAlertaAmarilla(medicamento, horario);
                            Log.d(TAG, "Alerta amarilla enviada para: " + medicamento.getNombre() + " a las " + horario);
                        } else {
                            notificationService.enviarNotificacionAlertaRoja(medicamento, horario);
                            Log.d(TAG, "Alerta roja enviada para: " + medicamento.getNombre() + " a las " + horario);
                        }
                    } else {
                        Log.d(TAG, "Medicamento no activo, no se envía notificación: " + medicamento.getNombre());
                    }
                } else {
                    Log.e(TAG, "No se pudo obtener el medicamento desde Firebase");
                }
            }
            
            @Override
            public void onError(Exception exception) {
                Log.e(TAG, "Error al obtener medicamento desde Firebase: " + 
                      (exception != null ? exception.getMessage() : "Error desconocido"));
            }
        });
    }
    
    /**
     * Crea un Intent para la alarma de un medicamento
     */
    public static Intent createIntent(Context context, String medicamentoId, String horario) {
        return createIntent(context, medicamentoId, horario, TIPO_ALERTA_ROJA);
    }
    
    /**
     * Crea un Intent para la alarma de un medicamento con tipo específico
     */
    public static Intent createIntent(Context context, String medicamentoId, String horario, int tipoAlerta) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(EXTRA_MEDICAMENTO_ID, medicamentoId);
        intent.putExtra(EXTRA_HORARIO, horario);
        intent.putExtra(EXTRA_TIPO_ALERTA, tipoAlerta);
        return intent;
    }
}

