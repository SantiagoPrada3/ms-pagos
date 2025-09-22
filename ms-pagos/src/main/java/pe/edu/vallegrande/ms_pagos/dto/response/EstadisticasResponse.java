package pe.edu.vallegrande.ms_pagos.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstadisticasResponse {
    
    private long totalPagos;
    private long pagosCompletados;
    private long pagosPendientes;
    private long pagosFallidos;
    private BigDecimal montoTotalCompletado;
    private double tasaExito;
}