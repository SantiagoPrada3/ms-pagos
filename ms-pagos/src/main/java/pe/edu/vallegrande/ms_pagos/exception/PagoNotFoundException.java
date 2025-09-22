package pe.edu.vallegrande.ms_pagos.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PagoNotFoundException extends RuntimeException {
    
    private final String pagoId;
    
    public PagoNotFoundException(String pagoId) {
        super("Pago no encontrado con ID: " + pagoId);
        this.pagoId = pagoId;
    }
}