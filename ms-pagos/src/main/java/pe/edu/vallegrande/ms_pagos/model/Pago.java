package pe.edu.vallegrande.ms_pagos.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pago {
    
    private String id;                    // Identificador único del pago
    private String orderId;               // Identificador del pedido (orden)
    private BigDecimal monto;             // Monto del pago (ejemplo: 1999.99)
    private EstadoPago estado;            // Estado: PENDING, COMPLETED, FAILED
    private LocalDateTime fechaCreacion;  // Fecha y hora de creación
    private LocalDateTime fechaActualizacion; // Fecha y hora de última actualización
    private String metodoPago;            // Método de pago (Tarjeta, PayPal, etc.)
    private String moneda;                // Moneda (PEN, USD, etc.)
    private String descripcion;           // Descripción del pago
    private String clienteId;             // ID del cliente que realiza el pago
    
    // Información adicional del pago
    private String transactionId;         // ID de transacción externa
    private String paymentGateway;        // Pasarela de pago utilizada
    private String codigoRespuesta;       // Código de respuesta del procesador
    private String mensajeRespuesta;      // Mensaje de respuesta del procesador
    
    public enum EstadoPago {
        PENDING,
        COMPLETED,
        FAILED,
        CANCELLED,
        REFUNDED
    }
}