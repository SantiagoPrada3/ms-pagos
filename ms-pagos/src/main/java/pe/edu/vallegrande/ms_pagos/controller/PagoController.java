package pe.edu.vallegrande.ms_pagos.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.ms_pagos.dto.request.PagoRequest;
import pe.edu.vallegrande.ms_pagos.dto.request.RefundRequest;
import pe.edu.vallegrande.ms_pagos.dto.response.ApiResponse;
import pe.edu.vallegrande.ms_pagos.dto.response.PagoResponse;
import pe.edu.vallegrande.ms_pagos.dto.response.EstadisticasResponse;
import pe.edu.vallegrande.ms_pagos.model.Pago;
import pe.edu.vallegrande.ms_pagos.service.PagoService;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/pagos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PagoController {
    
    private final PagoService pagoService;
    
    /**
     * Crear un nuevo pago
     * POST /api/pagos
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PagoResponse>> crearPago(@RequestBody PagoRequest request) {
        log.info("Solicitud para crear pago para orden: {}", request.getOrderId());
        
        PagoResponse pago = pagoService.crearPago(request);
        ApiResponse<PagoResponse> response = ApiResponse.success("Pago creado exitosamente", pago);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Obtener un pago por ID
     * GET /api/pagos/{pagoId}
     */
    @GetMapping("/{pagoId}")
    public ResponseEntity<ApiResponse<PagoResponse>> obtenerPago(@PathVariable String pagoId) {
        log.info("Solicitud para obtener pago con ID: {}", pagoId);
        
        PagoResponse pago = pagoService.obtenerPagoPorId(pagoId);
        ApiResponse<PagoResponse> response = ApiResponse.success(pago);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Obtener todos los pagos
     * GET /api/pagos
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<PagoResponse>>> obtenerTodosLosPagos() {
        log.info("Solicitud para obtener todos los pagos");
        
        List<PagoResponse> pagos = pagoService.obtenerTodosLosPagos();
        ApiResponse<List<PagoResponse>> response = ApiResponse.success(
            "Se encontraron " + pagos.size() + " pagos", pagos);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Obtener pagos por orden
     * GET /api/pagos/orden/{orderId}
     */
    @GetMapping("/orden/{orderId}")
    public ResponseEntity<ApiResponse<List<PagoResponse>>> obtenerPagosPorOrden(@PathVariable String orderId) {
        log.info("Solicitud para obtener pagos de la orden: {}", orderId);
        
        List<PagoResponse> pagos = pagoService.obtenerPagosPorOrden(orderId);
        ApiResponse<List<PagoResponse>> response = ApiResponse.success(
            "Se encontraron " + pagos.size() + " pagos para la orden " + orderId, pagos);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Obtener pagos por cliente
     * GET /api/pagos/cliente/{clienteId}
     */
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<ApiResponse<List<PagoResponse>>> obtenerPagosPorCliente(@PathVariable String clienteId) {
        log.info("Solicitud para obtener pagos del cliente: {}", clienteId);
        
        List<PagoResponse> pagos = pagoService.obtenerPagosPorCliente(clienteId);
        ApiResponse<List<PagoResponse>> response = ApiResponse.success(
            "Se encontraron " + pagos.size() + " pagos para el cliente " + clienteId, pagos);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Actualizar estado de un pago
     * PATCH /api/pagos/{pagoId}/estado
     */
    @PatchMapping("/{pagoId}/estado")
    public ResponseEntity<ApiResponse<PagoResponse>> actualizarEstadoPago(
            @PathVariable String pagoId, 
            @RequestParam Pago.EstadoPago estado) {
        log.info("Solicitud para actualizar estado del pago {} a {}", pagoId, estado);
        
        PagoResponse pago = pagoService.actualizarEstadoPago(pagoId, estado);
        ApiResponse<PagoResponse> response = ApiResponse.success(
            "Estado del pago actualizado a " + estado, pago);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Procesar refund de un pago
     * POST /api/pagos/{pagoId}/refund
     */
    @PostMapping("/{pagoId}/refund")
    public ResponseEntity<ApiResponse<PagoResponse>> refundPago(
            @PathVariable String pagoId,
            @RequestParam BigDecimal monto) {
        log.info("Solicitud para refund del pago {} por monto {}", pagoId, monto);
        
        PagoResponse pago = pagoService.refundPago(pagoId, monto);
        ApiResponse<PagoResponse> response = ApiResponse.success(
            "Refund procesado exitosamente", pago);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Procesar refund restringido con validación de límite
     * POST /api/pagos/restrict-refund
     */
    @PostMapping("/restrict-refund")
    public ResponseEntity<ApiResponse<PagoResponse>> restrictRefund(@RequestBody RefundRequest request) {
        log.info("Solicitud para refund restringido - Orden: {}, Monto: {}, Límite: {}", 
                request.getOrderId(), request.getAmount(), request.getMaxRefundable());
        
        PagoResponse pago = pagoService.restrictRefund(request);
        ApiResponse<PagoResponse> response = ApiResponse.success(
            String.format("Refund restringido procesado exitosamente. Monto: %.2f, Límite: %.2f", 
                request.getAmount(), request.getMaxRefundable()), 
            pago);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Health check del microservicio
     * GET /api/pagos/health
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("Microservicio de pagos funcionando correctamente"));
    }
    
    /**
     * Obtener estadísticas básicas
     * GET /api/pagos/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<EstadisticasResponse>> obtenerEstadisticas() {
        log.info("Solicitud para obtener estadísticas de pagos");
        
        List<PagoResponse> todosPagos = pagoService.obtenerTodosLosPagos();
        
        // Calcular estadísticas básicas
        long totalPagosCount = todosPagos.size();
        long pagosCompletadosCount = todosPagos.stream()
                .filter(p -> p.getEstado() == Pago.EstadoPago.COMPLETED)
                .count();
        long pagosPendientesCount = todosPagos.stream()
                .filter(p -> p.getEstado() == Pago.EstadoPago.PENDING)
                .count();
        long pagosFallidosCount = todosPagos.stream()
                .filter(p -> p.getEstado() == Pago.EstadoPago.FAILED)
                .count();
        
        BigDecimal montoTotal = todosPagos.stream()
                .filter(p -> p.getEstado() == Pago.EstadoPago.COMPLETED)
                .map(PagoResponse::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        double tasaExito = totalPagosCount > 0 ? (double) pagosCompletadosCount / totalPagosCount * 100 : 0;
        
        EstadisticasResponse stats = new EstadisticasResponse(
                totalPagosCount,
                pagosCompletadosCount,
                pagosPendientesCount,
                pagosFallidosCount,
                montoTotal,
                tasaExito
        );
        
        ApiResponse<EstadisticasResponse> response = ApiResponse.success("Estadísticas obtenidas exitosamente", stats);
        return ResponseEntity.ok(response);
    }
}