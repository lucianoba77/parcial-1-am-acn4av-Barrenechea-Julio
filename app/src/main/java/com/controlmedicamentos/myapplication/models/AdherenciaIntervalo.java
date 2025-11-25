package com.controlmedicamentos.myapplication.models;

/**
 * Representa la adherencia en un intervalo (día, semana, etc.).
 */
public class AdherenciaIntervalo {
    private final String etiqueta;
    private final int tomasEsperadas;
    private final int tomasRealizadas;
    private final float porcentaje;

    public AdherenciaIntervalo(String etiqueta, int tomasEsperadas, int tomasRealizadas, float porcentaje) {
        this.etiqueta = etiqueta;
        this.tomasEsperadas = tomasEsperadas;
        this.tomasRealizadas = tomasRealizadas;
        this.porcentaje = porcentaje;
    }

    public String getEtiqueta() {
        return etiqueta;
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
}

