package pe.edu.vallegrande.ms_pagos.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagoRequest {
    
    private String orderId;      // ID de la orden es obligatorio
    private BigDecimal monto;    // El monto es obligatorio y debe ser mayor a 0
    private String metodoPago;   // El método de pago es obligatorio
    private String moneda;       // La moneda es obligatoria
    private String descripcion;
    private String clienteId;    // El ID del cliente es obligatorio
    private String paymentGateway;
}