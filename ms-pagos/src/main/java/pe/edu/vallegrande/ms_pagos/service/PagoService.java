package pe.edu.vallegrande.ms_pagos.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.ms_pagos.dto.request.PagoRequest;
import pe.edu.vallegrande.ms_pagos.dto.request.RefundRequest;
import pe.edu.vallegrande.ms_pagos.dto.response.PagoResponse;
import pe.edu.vallegrande.ms_pagos.exception.PagoNotFoundException;
import pe.edu.vallegrande.ms_pagos.exception.PagoValidationException;
import pe.edu.vallegrande.ms_pagos.model.Pago;
import pe.edu.vallegrande.ms_pagos.repository.PagoRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PagoService {
    
    private final PagoRepository pagoRepository;
    
    /**
     * Crea un nuevo pago
     */
    public PagoResponse crearPago(PagoRequest request) {
        log.info("Creando nuevo pago para orden: {}", request.getOrderId());
        
        // Validaciones
        validatePagoRequest(request);
        
        // Crear el pago
        Pago pago = new Pago();
        pago.setId(UUID.randomUUID().toString());
        pago.setOrderId(request.getOrderId());
        pago.setMonto(request.getMonto());
        pago.setEstado(Pago.EstadoPago.PENDING);
        pago.setFechaCreacion(LocalDateTime.now());
        pago.setFechaActualizacion(LocalDateTime.now());
        pago.setMetodoPago(request.getMetodoPago());
        pago.setMoneda(request.getMoneda());
        pago.setDescripcion(request.getDescripcion());
        pago.setClienteId(request.getClienteId());
        pago.setPaymentGateway(request.getPaymentGateway() != null ? request.getPaymentGateway() : "DEFAULT");
        
        // Simular procesamiento del pago
        procesarPago(pago);
        
        // Guardar el pago
        Pago pagoGuardado = pagoRepository.save(pago);
        
        log.info("Pago creado exitosamente con ID: {}", pagoGuardado.getId());
        return convertToResponse(pagoGuardado);
    }
    
    /**
     * Obtiene un pago por su ID
     */
    public PagoResponse obtenerPagoPorId(String pagoId) {
        log.info("Obteniendo pago con ID: {}", pagoId);
        
        Pago pago = pagoRepository.findById(pagoId)
                .orElseThrow(() -> new PagoNotFoundException(pagoId));
        
        return convertToResponse(pago);
    }
    
    /**
     * Obtiene todos los pagos de una orden
     */
    public List<PagoResponse> obtenerPagosPorOrden(String orderId) {
        log.info("Obteniendo pagos para orden: {}", orderId);
        
        List<Pago> pagos = pagoRepository.findByOrderId(orderId);
        return pagos.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtiene todos los pagos de un cliente
     */
    public List<PagoResponse> obtenerPagosPorCliente(String clienteId) {
        log.info("Obteniendo pagos para cliente: {}", clienteId);
        
        List<Pago> pagos = pagoRepository.findByClienteId(clienteId);
        return pagos.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtiene todos los pagos
     */
    public List<PagoResponse> obtenerTodosLosPagos() {
        log.info("Obteniendo todos los pagos");
        
        List<Pago> pagos = pagoRepository.findAll();
        return pagos.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Actualiza el estado de un pago
     */
    public PagoResponse actualizarEstadoPago(String pagoId, Pago.EstadoPago nuevoEstado) {
        log.info("Actualizando estado del pago {} a {}", pagoId, nuevoEstado);
        
        Pago pago = pagoRepository.findById(pagoId)
                .orElseThrow(() -> new PagoNotFoundException(pagoId));
        
        // Validar transición de estado
        validateEstadoTransition(pago.getEstado(), nuevoEstado);
        
        pago.setEstado(nuevoEstado);
        pago.setFechaActualizacion(LocalDateTime.now());
        
        // Si es completado, simular respuesta exitosa
        if (nuevoEstado == Pago.EstadoPago.COMPLETED) {
            pago.setCodigoRespuesta("SUCCESS");
            pago.setMensajeRespuesta("Pago procesado exitosamente");
            pago.setTransactionId("TXN_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        
        Pago pagoActualizado = pagoRepository.save(pago);
        
        log.info("Estado del pago {} actualizado exitosamente", pagoId);
        return convertToResponse(pagoActualizado);
    }
    
    /**
     * Refund de un pago
     */
    public PagoResponse refundPago(String pagoId, BigDecimal montoRefund) {
        log.info("Procesando refund para pago {} por monto {}", pagoId, montoRefund);
        
        Pago pago = pagoRepository.findById(pagoId)
                .orElseThrow(() -> new PagoNotFoundException(pagoId));
        
        // Validar que el pago esté completado
        if (pago.getEstado() != Pago.EstadoPago.COMPLETED) {
            throw new PagoValidationException("Solo se pueden hacer refunds a pagos completados");
        }
        
        // Validar monto de refund
        if (montoRefund.compareTo(pago.getMonto()) > 0) {
            throw new PagoValidationException("El monto del refund no puede ser mayor al monto del pago");
        }
        
        pago.setEstado(Pago.EstadoPago.REFUNDED);
        pago.setFechaActualizacion(LocalDateTime.now());
        pago.setCodigoRespuesta("REFUNDED");
        pago.setMensajeRespuesta("Refund procesado exitosamente por " + montoRefund);
        
        Pago pagoActualizado = pagoRepository.save(pago);
        
        log.info("Refund procesado exitosamente para pago {}", pagoId);
        return convertToResponse(pagoActualizado);
    }
    
    /**
     * Valida la request de pago
     */
    private void validatePagoRequest(PagoRequest request) {
        if (request.getOrderId() == null || request.getOrderId().trim().isEmpty()) {
            throw new PagoValidationException("orderId", request.getOrderId(), "El ID de la orden es obligatorio");
        }
        
        if (request.getMonto() == null) {
            throw new PagoValidationException("monto", null, "El monto es obligatorio");
        }
        
        if (request.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PagoValidationException("monto", request.getMonto().toString(), "El monto debe ser mayor a 0");
        }
        
        if (request.getMetodoPago() == null || request.getMetodoPago().trim().isEmpty()) {
            throw new PagoValidationException("metodoPago", request.getMetodoPago(), "El método de pago es obligatorio");
        }
        
        if (request.getMoneda() == null || request.getMoneda().trim().isEmpty()) {
            throw new PagoValidationException("moneda", request.getMoneda(), "La moneda es obligatoria");
        }
        
        if (request.getClienteId() == null || request.getClienteId().trim().isEmpty()) {
            throw new PagoValidationException("clienteId", request.getClienteId(), "El ID del cliente es obligatorio");
        }
        
        // Validar límite de monto (ejemplo: máximo 500,000)
        if (request.getMonto().compareTo(new BigDecimal("500000")) > 0) {
            throw new PagoValidationException("monto", request.getMonto().toString(), "El monto excede el límite permitido");
        }
    }
    
    /**
     * Simula el procesamiento del pago
     */
    private void procesarPago(Pago pago) {
        // Simular diferentes escenarios basados en el monto
        BigDecimal monto = pago.getMonto();
        
        if (monto.compareTo(new BigDecimal("10000")) > 0) {
            // Montos altos requieren validación adicional
            pago.setEstado(Pago.EstadoPago.PENDING);
            pago.setMensajeRespuesta("Pago en validación por monto alto");
        } else if (monto.compareTo(new BigDecimal("1")) < 0) {
            // Montos muy bajos fallan
            pago.setEstado(Pago.EstadoPago.FAILED);
            pago.setCodigoRespuesta("AMOUNT_TOO_LOW");
            pago.setMensajeRespuesta("Monto muy bajo para procesar");
        } else {
            // Pagos normales se completan automáticamente
            pago.setEstado(Pago.EstadoPago.COMPLETED);
            pago.setCodigoRespuesta("SUCCESS");
            pago.setMensajeRespuesta("Pago procesado exitosamente");
            pago.setTransactionId("TXN_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
    }
    
    /**
     * Valida las transiciones de estado
     */
    private void validateEstadoTransition(Pago.EstadoPago estadoActual, Pago.EstadoPago nuevoEstado) {
        // Definir transiciones válidas
        boolean transicionValida = false;
        
        switch (estadoActual) {
            case PENDING:
                transicionValida = nuevoEstado == Pago.EstadoPago.COMPLETED || 
                                 nuevoEstado == Pago.EstadoPago.FAILED || 
                                 nuevoEstado == Pago.EstadoPago.CANCELLED;
                break;
            case COMPLETED:
                transicionValida = nuevoEstado == Pago.EstadoPago.REFUNDED;
                break;
            case FAILED:
            case CANCELLED:
            case REFUNDED:
                // Estados finales, no permiten transiciones
                transicionValida = false;
                break;
        }
        
        if (!transicionValida) {
            throw new PagoValidationException("No se puede cambiar el estado de " + estadoActual + " a " + nuevoEstado);
        }
    }
    
    /**
     * Procesa un refund restringido validando el límite máximo
     * @param refundRequest Request con orderId, amount y maxRefundable
     * @return PagoResponse del pago procesado
     */
    public PagoResponse restrictRefund(RefundRequest refundRequest) {
        log.info("Procesando refund restringido para orden: {} con monto: {} y límite: {}", 
                refundRequest.getOrderId(), refundRequest.getAmount(), refundRequest.getMaxRefundable());
        
        // Validar el request
        validateRefundRequest(refundRequest);
        
        // Validar que el monto no exceda el límite
        if (refundRequest.getAmount().compareTo(refundRequest.getMaxRefundable()) > 0) {
            throw new PagoValidationException(
                "amount", 
                refundRequest.getAmount().toString(),
                String.format("El monto del refund (%.2f) excede el límite máximo permitido (%.2f)",
                    refundRequest.getAmount(), refundRequest.getMaxRefundable())
            );
        }
        
        // Buscar pagos de la orden
        List<Pago> pagosOrden = pagoRepository.findByOrderId(refundRequest.getOrderId());
        
        if (pagosOrden.isEmpty()) {
            throw new PagoNotFoundException("No se encontraron pagos para la orden: " + refundRequest.getOrderId());
        }
        
        // Buscar el primer pago completado para procesar el refund
        Pago pagoParaRefund = pagosOrden.stream()
                .filter(p -> p.getEstado() == Pago.EstadoPago.COMPLETED)
                .findFirst()
                .orElseThrow(() -> new PagoValidationException(
                    "estado", 
                    "N/A", 
                    "No hay pagos completados disponibles para refund en la orden: " + refundRequest.getOrderId()
                ));
        
        // Validar que el monto del refund no sea mayor al monto del pago original
        if (refundRequest.getAmount().compareTo(pagoParaRefund.getMonto()) > 0) {
            throw new PagoValidationException(
                "amount",
                refundRequest.getAmount().toString(),
                String.format("El monto del refund (%.2f) no puede ser mayor al monto del pago original (%.2f)",
                    refundRequest.getAmount(), pagoParaRefund.getMonto())
            );
        }
        
        // Procesar el refund
        pagoParaRefund.setEstado(Pago.EstadoPago.REFUNDED);
        pagoParaRefund.setFechaActualizacion(LocalDateTime.now());
        pagoParaRefund.setCodigoRespuesta("RESTRICTED_REFUND_SUCCESS");
        pagoParaRefund.setMensajeRespuesta(
            String.format("Refund restringido procesado exitosamente. Monto: %.2f, Límite: %.2f, Restante: %.2f",
                refundRequest.getAmount(), 
                refundRequest.getMaxRefundable(),
                refundRequest.getRemainingRefundable())
        );
        
        Pago pagoActualizado = pagoRepository.save(pagoParaRefund);
        
        log.info("Refund restringido procesado exitosamente para pago {} de la orden {}", 
                pagoActualizado.getId(), refundRequest.getOrderId());
        
        return convertToResponse(pagoActualizado);
    }
    
    /**
     * Valida el request de refund restringido
     */
    private void validateRefundRequest(RefundRequest request) {
        if (request.getOrderId() == null || request.getOrderId().trim().isEmpty()) {
            throw new PagoValidationException("orderId", request.getOrderId(), "El orderId es obligatorio");
        }
        
        if (request.getAmount() == null) {
            throw new PagoValidationException("amount", null, "El amount es obligatorio");
        }
        
        if (request.getMaxRefundable() == null) {
            throw new PagoValidationException("maxRefundable", null, "El maxRefundable es obligatorio");
        }
        
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PagoValidationException("amount", request.getAmount().toString(), "El amount debe ser mayor a 0");
        }
        
        if (request.getMaxRefundable().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PagoValidationException("maxRefundable", request.getMaxRefundable().toString(), "El maxRefundable debe ser mayor a 0");
        }
    }

    /**
     * Convierte un Pago a PagoResponse
     */
    private PagoResponse convertToResponse(Pago pago) {
        return new PagoResponse(
                pago.getId(),
                pago.getOrderId(),
                pago.getMonto(),
                pago.getEstado(),
                pago.getFechaCreacion(),
                pago.getFechaActualizacion(),
                pago.getMetodoPago(),
                pago.getMoneda(),
                pago.getDescripcion(),
                pago.getClienteId(),
                pago.getTransactionId(),
                pago.getPaymentGateway(),
                pago.getCodigoRespuesta(),
                pago.getMensajeRespuesta()
        );
    }
}