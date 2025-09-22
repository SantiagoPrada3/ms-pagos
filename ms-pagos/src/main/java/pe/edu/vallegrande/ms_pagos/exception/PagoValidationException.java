package pe.edu.vallegrande.ms_pagos.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PagoValidationException extends RuntimeException {
    
    private final String field;
    private final String value;
    
    public PagoValidationException(String field, String value, String message) {
        super(message);
        this.field = field;
        this.value = value;
    }
    
    public PagoValidationException(String message) {
        super(message);
        this.field = null;
        this.value = null;
    }
}