package com.controlmedicamentos.myapplication.models;

import java.util.Calendar;
import java.util.Date;

/**
 * Modelo para rastrear el estado de una toma programada de un medicamento
 */
public class TomaProgramada {
    private String medicamentoId;
    private String horario; // formato "HH:mm"
    private Date fechaHoraProgramada; // fecha y hora exacta de la toma
    private EstadoTomaProgramada estado;
    private int posposiciones; // número de veces que se ha pospuesto (máximo 3)
    private Date fechaHoraAlertaAmarilla; // cuando se activó la alerta amarilla (10 min antes)
    private Date fechaHoraAlertaRoja; // cuando se activó la alerta roja (horario exacto)
    private Date fechaHoraRetraso; // cuando pasó a estado retraso (+10 min)
    private Date fechaHoraOmitida; // cuando se marcó como omitida (+1 hora)
    private boolean tomada; // si fue marcada como tomada

    public enum EstadoTomaProgramada {
        PENDIENTE,      // Blanca - esperando horario
        ALERTA_AMARILLA, // Amarilla - 10 minutos antes
        ALERTA_ROJA,    // Roja parpadeante - horario exacto
        RETRASO,        // Roja fija - +10 minutos sin tomar
        OMITIDA         // Roja - +1 hora sin tomar, medicamento al final
    }

    public TomaProgramada() {
        this.estado = EstadoTomaProgramada.PENDIENTE;
        this.posposiciones = 0;
        this.tomada = false;
    }

    public TomaProgramada(String medicamentoId, String horario, Date fechaHoraProgramada) {
        this.medicamentoId = medicamentoId;
        this.horario = horario;
        this.fechaHoraProgramada = fechaHoraProgramada;
        this.estado = EstadoTomaProgramada.PENDIENTE;
        this.posposiciones = 0;
        this.tomada = false;
    }

    // Getters y Setters
    public String getMedicamentoId() {
        return medicamentoId;
    }

    public void setMedicamentoId(String medicamentoId) {
        this.medicamentoId = medicamentoId;
    }

    public String getHorario() {
        return horario;
    }

    public void setHorario(String horario) {
        this.horario = horario;
    }

    public Date getFechaHoraProgramada() {
        return fechaHoraProgramada;
    }

    public void setFechaHoraProgramada(Date fechaHoraProgramada) {
        this.fechaHoraProgramada = fechaHoraProgramada;
    }

    public EstadoTomaProgramada getEstado() {
        return estado;
    }

    public void setEstado(EstadoTomaProgramada estado) {
        this.estado = estado;
    }

    public int getPosposiciones() {
        return posposiciones;
    }

    public void setPosposiciones(int posposiciones) {
        this.posposiciones = posposiciones;
    }

    public Date getFechaHoraAlertaAmarilla() {
        return fechaHoraAlertaAmarilla;
    }

    public void setFechaHoraAlertaAmarilla(Date fechaHoraAlertaAmarilla) {
        this.fechaHoraAlertaAmarilla = fechaHoraAlertaAmarilla;
    }

    public Date getFechaHoraAlertaRoja() {
        return fechaHoraAlertaRoja;
    }

    public void setFechaHoraAlertaRoja(Date fechaHoraAlertaRoja) {
        this.fechaHoraAlertaRoja = fechaHoraAlertaRoja;
    }

    public Date getFechaHoraRetraso() {
        return fechaHoraRetraso;
    }

    public void setFechaHoraRetraso(Date fechaHoraRetraso) {
        this.fechaHoraRetraso = fechaHoraRetraso;
    }

    public Date getFechaHoraOmitida() {
        return fechaHoraOmitida;
    }

    public void setFechaHoraOmitida(Date fechaHoraOmitida) {
        this.fechaHoraOmitida = fechaHoraOmitida;
    }

    public boolean isTomada() {
        return tomada;
    }

    public void setTomada(boolean tomada) {
        this.tomada = tomada;
    }

    /**
     * Intenta posponer la toma. Retorna true si se pudo posponer (máximo 3 veces)
     */
    public boolean posponer() {
        if (posposiciones < 3) {
            posposiciones++;
            return true;
        }
        return false; // Ya se pospuso 3 veces, se considera omitida
    }

    /**
     * Verifica si se puede posponer
     */
    public boolean sePuedePosponer() {
        return posposiciones < 3;
    }

    /**
     * Calcula la fecha/hora de la alerta amarilla (10 minutos antes)
     */
    public Date calcularFechaAlertaAmarilla() {
        if (fechaHoraProgramada == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(fechaHoraProgramada);
        cal.add(Calendar.MINUTE, -10);
        return cal.getTime();
    }

    /**
     * Calcula la fecha/hora de retraso (10 minutos después del horario)
     */
    public Date calcularFechaRetraso() {
        if (fechaHoraProgramada == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(fechaHoraProgramada);
        cal.add(Calendar.MINUTE, 10);
        return cal.getTime();
    }

    /**
     * Calcula la fecha/hora de omitida (1 hora después del horario)
     */
    public Date calcularFechaOmitida() {
        if (fechaHoraProgramada == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(fechaHoraProgramada);
        cal.add(Calendar.HOUR_OF_DAY, 1);
        return cal.getTime();
    }
}

