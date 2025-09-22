package pe.edu.vallegrande.ms_pagos.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * DTO para solicitudes de reembolso restringido
 * Valida que el monto solicitado no exceda el límite máximo permitido
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {
    
    /**
     * ID de la orden para la cual se solicita el reembolso
     */
    @NotBlank(message = "El orderId no puede estar vacío")
    @Size(max = 50, message = "El orderId no puede exceder 50 caracteres")
    private String orderId;
    
    /**
     * Monto del reembolso solicitado
     */
    @NotNull(message = "El monto no puede ser null")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal amount;
    
    /**
     * Monto máximo permitido para reembolso
     */
    @NotNull(message = "El maxRefundable no puede ser null")
    @DecimalMin(value = "0.01", message = "El maxRefundable debe ser mayor a 0")
    private BigDecimal maxRefundable;
    
    /**
     * Booleano que indica si el reembolso es válido según las reglas de negocio
     */
    private Boolean valid;
    
    /**
     * Constructor para crear un RefundRequest válido
     */
    public RefundRequest(String orderId, BigDecimal amount, BigDecimal maxRefundable) {
        this.orderId = orderId;
        this.amount = amount;
        this.maxRefundable = maxRefundable;
        this.valid = amount.compareTo(maxRefundable) <= 0;
    }
    
    /**
     * Valida si el monto solicitado está dentro del límite
     */
    public boolean isValidRefund() {
        if (amount == null || maxRefundable == null) {
            return false;
        }
        return amount.compareTo(maxRefundable) <= 0;
    }
    
    /**
     * Obtiene el monto restante disponible para reembolso
     */
    public BigDecimal getRemainingRefundable() {
        if (maxRefundable == null || amount == null) {
            return BigDecimal.ZERO;
        }
        return maxRefundable.subtract(amount);
    }
}