package com.controlmedicamentos.myapplication.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.controlmedicamentos.myapplication.models.Medicamento;
import com.controlmedicamentos.myapplication.models.TomaProgramada;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio para rastrear y gestionar el estado de las tomas programadas
 */
public class TomaTrackingService {
    private static final String TAG = "TomaTrackingService";
    private static final String PREF_TOMAS_PROGRAMADAS = "tomas_programadas";
    private static final String PREF_POSPOSICIONES = "posposiciones";
    
    private Context context;
    private SharedPreferences preferences;
    private Map<String, List<TomaProgramada>> tomasPorMedicamento;
    
    public TomaTrackingService(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences("ControlMedicamentos", Context.MODE_PRIVATE);
        this.tomasPorMedicamento = new HashMap<>();
        cargarTomasProgramadas();
    }
    
    /**
     * Inicializa las tomas programadas para un medicamento en el día actual
     */
    public void inicializarTomasDia(Medicamento medicamento) {
        if (medicamento == null || medicamento.getId() == null) {
            return;
        }
        
        List<String> horarios = medicamento.getHorariosTomas();
        if (horarios == null || horarios.isEmpty()) {
            return;
        }
        
        Calendar hoy = Calendar.getInstance();
        hoy.set(Calendar.HOUR_OF_DAY, 0);
        hoy.set(Calendar.MINUTE, 0);
        hoy.set(Calendar.SECOND, 0);
        hoy.set(Calendar.MILLISECOND, 0);
        
        List<TomaProgramada> tomas = new ArrayList<>();
        
        for (String horario : horarios) {
            try {
                String[] partes = horario.split(":");
                if (partes.length != 2) {
                    continue;
                }
                
                int hora = Integer.parseInt(partes[0]);
                int minuto = Integer.parseInt(partes[1]);
                
                Calendar fechaToma = (Calendar) hoy.clone();
                fechaToma.set(Calendar.HOUR_OF_DAY, hora);
                fechaToma.set(Calendar.MINUTE, minuto);
                
                // Si la hora ya pasó hoy, no incluirla (ya se procesó o se omitió)
                Calendar ahora = Calendar.getInstance();
                if (fechaToma.before(ahora) && !esTomaDelDia(fechaToma, ahora)) {
                    continue;
                }
                
                TomaProgramada toma = new TomaProgramada(
                    medicamento.getId(),
                    horario,
                    fechaToma.getTime()
                );
                
                tomas.add(toma);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error al parsear horario: " + horario, e);
            }
        }
        
        tomasPorMedicamento.put(medicamento.getId(), tomas);
        guardarTomasProgramadas();
    }
    
    /**
     * Verifica si una fecha es del día actual
     */
    private boolean esTomaDelDia(Calendar fechaToma, Calendar ahora) {
        return fechaToma.get(Calendar.YEAR) == ahora.get(Calendar.YEAR) &&
               fechaToma.get(Calendar.DAY_OF_YEAR) == ahora.get(Calendar.DAY_OF_YEAR);
    }
    
    /**
     * Obtiene el estado actual de una toma específica
     */
    public TomaProgramada.EstadoTomaProgramada obtenerEstadoToma(
            String medicamentoId, String horario) {
        List<TomaProgramada> tomas = tomasPorMedicamento.get(medicamentoId);
        if (tomas == null) {
            return TomaProgramada.EstadoTomaProgramada.PENDIENTE;
        }
        
        for (TomaProgramada toma : tomas) {
            if (toma.getHorario().equals(horario)) {
                actualizarEstadoToma(toma);
                return toma.getEstado();
            }
        }
        
        return TomaProgramada.EstadoTomaProgramada.PENDIENTE;
    }
    
    /**
     * Obtiene todas las tomas programadas de un medicamento
     */
    public List<TomaProgramada> obtenerTomasMedicamento(String medicamentoId) {
        List<TomaProgramada> tomas = tomasPorMedicamento.get(medicamentoId);
        if (tomas == null) {
            return new ArrayList<>();
        }
        
        // Actualizar estados antes de retornar
        for (TomaProgramada toma : tomas) {
            actualizarEstadoToma(toma);
        }
        
        return tomas;
    }
    
    /**
     * Actualiza el estado de una toma según el tiempo actual
     */
    private void actualizarEstadoToma(TomaProgramada toma) {
        if (toma == null || toma.isTomada()) {
            return; // Si ya fue tomada, no actualizar
        }
        
        Date ahora = new Date();
        Date fechaProgramada = toma.getFechaHoraProgramada();
        
        if (fechaProgramada == null) {
            return;
        }
        
        // Calcular fechas de transición
        Date fechaAlertaAmarilla = toma.calcularFechaAlertaAmarilla();
        Date fechaRetraso = toma.calcularFechaRetraso();
        Date fechaOmitida = toma.calcularFechaOmitida();
        
        // Actualizar estado según el tiempo
        if (ahora.after(fechaOmitida)) {
            if (toma.getEstado() != TomaProgramada.EstadoTomaProgramada.OMITIDA) {
                toma.setEstado(TomaProgramada.EstadoTomaProgramada.OMITIDA);
                toma.setFechaHoraOmitida(ahora);
                guardarTomasProgramadas();
            }
        } else if (ahora.after(fechaRetraso)) {
            if (toma.getEstado() != TomaProgramada.EstadoTomaProgramada.RETRASO &&
                toma.getEstado() != TomaProgramada.EstadoTomaProgramada.OMITIDA) {
                toma.setEstado(TomaProgramada.EstadoTomaProgramada.RETRASO);
                if (toma.getFechaHoraRetraso() == null) {
                    toma.setFechaHoraRetraso(ahora);
                }
                guardarTomasProgramadas();
            }
        } else if (ahora.after(fechaProgramada)) {
            if (toma.getEstado() != TomaProgramada.EstadoTomaProgramada.ALERTA_ROJA &&
                toma.getEstado() != TomaProgramada.EstadoTomaProgramada.RETRASO &&
                toma.getEstado() != TomaProgramada.EstadoTomaProgramada.OMITIDA) {
                toma.setEstado(TomaProgramada.EstadoTomaProgramada.ALERTA_ROJA);
                if (toma.getFechaHoraAlertaRoja() == null) {
                    toma.setFechaHoraAlertaRoja(ahora);
                }
                guardarTomasProgramadas();
            }
        } else if (fechaAlertaAmarilla != null && ahora.after(fechaAlertaAmarilla)) {
            if (toma.getEstado() == TomaProgramada.EstadoTomaProgramada.PENDIENTE) {
                toma.setEstado(TomaProgramada.EstadoTomaProgramada.ALERTA_AMARILLA);
                if (toma.getFechaHoraAlertaAmarilla() == null) {
                    toma.setFechaHoraAlertaAmarilla(ahora);
                }
                guardarTomasProgramadas();
            }
        }
    }
    
    /**
     * Marca una toma como tomada
     */
    public void marcarTomaComoTomada(String medicamentoId, String horario) {
        List<TomaProgramada> tomas = tomasPorMedicamento.get(medicamentoId);
        if (tomas == null) {
            return;
        }
        
        for (TomaProgramada toma : tomas) {
            if (toma.getHorario().equals(horario) && !toma.isTomada()) {
                toma.setTomada(true);
                toma.setEstado(TomaProgramada.EstadoTomaProgramada.PENDIENTE);
                guardarTomasProgramadas();
                break;
            }
        }
    }
    
    /**
     * Pospone una toma (máximo 3 veces)
     */
    public boolean posponerToma(String medicamentoId, String horario) {
        List<TomaProgramada> tomas = tomasPorMedicamento.get(medicamentoId);
        if (tomas == null) {
            return false;
        }
        
        for (TomaProgramada toma : tomas) {
            if (toma.getHorario().equals(horario) && !toma.isTomada()) {
                boolean pospuesta = toma.posponer();
                if (pospuesta) {
                    // Reprogramar la toma 10 minutos después
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(toma.getFechaHoraProgramada());
                    cal.add(Calendar.MINUTE, 10);
                    toma.setFechaHoraProgramada(cal.getTime());
                    toma.setEstado(TomaProgramada.EstadoTomaProgramada.PENDIENTE);
                    guardarTomasProgramadas();
                    return true;
                } else {
                    // Ya se pospuso 3 veces, marcar como omitida
                    toma.setEstado(TomaProgramada.EstadoTomaProgramada.OMITIDA);
                    toma.setFechaHoraOmitida(new Date());
                    guardarTomasProgramadas();
                    return false;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Verifica si un medicamento tiene tomas omitidas
     */
    public boolean tieneTomasOmitidas(String medicamentoId) {
        List<TomaProgramada> tomas = tomasPorMedicamento.get(medicamentoId);
        if (tomas == null) {
            return false;
        }
        
        for (TomaProgramada toma : tomas) {
            if (toma.getEstado() == TomaProgramada.EstadoTomaProgramada.OMITIDA) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Guarda las tomas programadas en SharedPreferences
     */
    private void guardarTomasProgramadas() {
        // Por simplicidad, guardamos solo los IDs y horarios
        // En producción, se podría usar JSON o una base de datos
        // Por ahora, las tomas se recalculan cada vez que se inicia la app
    }
    
    /**
     * Carga las tomas programadas desde SharedPreferences
     */
    private void cargarTomasProgramadas() {
        // Por simplicidad, las tomas se inicializan cuando se cargan los medicamentos
        // En producción, se podría cargar desde SharedPreferences o base de datos
    }
    
    /**
     * Limpia las tomas del día anterior
     */
    public void limpiarTomasAnteriores() {
        Calendar hoy = Calendar.getInstance();
        hoy.set(Calendar.HOUR_OF_DAY, 0);
        hoy.set(Calendar.MINUTE, 0);
        hoy.set(Calendar.SECOND, 0);
        hoy.set(Calendar.MILLISECOND, 0);
        
        for (List<TomaProgramada> tomas : tomasPorMedicamento.values()) {
            tomas.removeIf(toma -> {
                if (toma.getFechaHoraProgramada() == null) {
                    return true;
                }
                Calendar fechaToma = Calendar.getInstance();
                fechaToma.setTime(toma.getFechaHoraProgramada());
                return fechaToma.before(hoy);
            });
        }
        
        guardarTomasProgramadas();
    }
}

