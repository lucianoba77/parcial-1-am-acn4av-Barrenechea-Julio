package com.controlmedicamentos.myapplication.models;

import java.util.Date;

public class Toma {
    private String id;
    private String medicamentoId;
    private String medicamentoNombre;
    private String userId;
    private Date fechaHoraProgramada;
    private Date fechaHoraTomada;
    private EstadoToma estado;
    private String observaciones;

    // Enum para los estados de la toma
    public enum EstadoToma {
        PENDIENTE,    // Blanca - esperando horario
        PROXIMA,      // Amarilla - llegó el horario
        TOMADA,       // Verde - usuario confirmó
        PERDIDA       // Roja - no se tomó a tiempo
    }

    // Constructor por defecto
    public Toma() {
        this.estado = EstadoToma.PENDIENTE;
    }

    // Constructor con parámetros
    public Toma(String id, String medicamentoId, Date fechaHoraProgramada) {
        this.id = id;
        this.medicamentoId = medicamentoId;
        this.fechaHoraProgramada = fechaHoraProgramada;
        this.estado = EstadoToma.PENDIENTE;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMedicamentoId() {
        return medicamentoId;
    }

    public void setMedicamentoId(String medicamentoId) {
        this.medicamentoId = medicamentoId;
    }

    public String getMedicamentoNombre() {
        return medicamentoNombre;
    }

    public void setMedicamentoNombre(String medicamentoNombre) {
        this.medicamentoNombre = medicamentoNombre;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Date getFechaHoraProgramada() {
        return fechaHoraProgramada;
    }

    public void setFechaHoraProgramada(Date fechaHoraProgramada) {
        this.fechaHoraProgramada = fechaHoraProgramada;
    }

    public Date getFechaHoraTomada() {
        return fechaHoraTomada;
    }

    public void setFechaHoraTomada(Date fechaHoraTomada) {
        this.fechaHoraTomada = fechaHoraTomada;
    }

    public EstadoToma getEstado() {
        return estado;
    }

    public void setEstado(EstadoToma estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    // Métodos útiles
    public void marcarComoTomada() {
        this.estado = EstadoToma.TOMADA;
        this.fechaHoraTomada = new Date();
    }

    public void marcarComoPerdida() {
        this.estado = EstadoToma.PERDIDA;
    }

    public boolean estaTomada() {
        return estado == EstadoToma.TOMADA;
    }

    public boolean estaPerdida() {
        return estado == EstadoToma.PERDIDA;
    }

    public boolean estaPendiente() {
        return estado == EstadoToma.PENDIENTE;
    }

    public boolean estaProxima() {
        return estado == EstadoToma.PROXIMA;
    }

    public long getTiempoRestante() {
        if (fechaHoraProgramada == null) {
            return 0;
        }
        return fechaHoraProgramada.getTime() - System.currentTimeMillis();
    }

    public boolean esHoraDeTomar() {
        return getTiempoRestante() <= 0 && !estaTomada() && !estaPerdida();
    }
}
