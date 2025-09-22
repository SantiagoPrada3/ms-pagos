package pe.edu.vallegrande.ms_pagos.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.vallegrande.ms_pagos.model.Pago.EstadoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagoResponse {
    
    private String id;
    private String orderId;
    private BigDecimal monto;
    private EstadoPago estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private String metodoPago;
    private String moneda;
    private String descripcion;
    private String clienteId;
    private String transactionId;
    private String paymentGateway;
    private String codigoRespuesta;
    private String mensajeRespuesta;
}