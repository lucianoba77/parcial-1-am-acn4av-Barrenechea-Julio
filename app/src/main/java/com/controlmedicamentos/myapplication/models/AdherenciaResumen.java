package com.controlmedicamentos.myapplication.models;

/**
 * Resumen de adherencia para un medicamento en un intervalo determinado.
 */
public class AdherenciaResumen {
    private final String medicamentoId;
    private final String medicamentoNombre;
    private final int tomasEsperadas;
    private final int tomasRealizadas;
    private final float porcentaje;
    private final boolean esCronico;

    public AdherenciaResumen(String medicamentoId,
                             String medicamentoNombre,
                             int tomasEsperadas,
                             int tomasRealizadas,
                             float porcentaje,
                             boolean esCronico) {
        this.medicamentoId = medicamentoId;
        this.medicamentoNombre = medicamentoNombre;
        this.tomasEsperadas = tomasEsperadas;
        this.tomasRealizadas = tomasRealizadas;
        this.porcentaje = porcentaje;
        this.esCronico = esCronico;
    }

    public String getMedicamentoId() {
        return medicamentoId;
    }

    public String getMedicamentoNombre() {
        return medicamentoNombre;
    }

    public int getTomasEsperadas() {
        return tomasEsperadas;
    }

    public int getTomasRealizadas() {
        return tomasRealizadas;
    }

    public float getPorcentaje() {
        return porcentaje;
    }

    public boolean esCronico() {
        return esCronico;
    }
}

