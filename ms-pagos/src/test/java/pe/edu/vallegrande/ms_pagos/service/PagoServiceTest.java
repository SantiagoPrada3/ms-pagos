package pe.edu.vallegrande.ms_pagos.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.vallegrande.ms_pagos.dto.request.PagoRequest;
import pe.edu.vallegrande.ms_pagos.dto.response.PagoResponse;
import pe.edu.vallegrande.ms_pagos.exception.PagoNotFoundException;
import pe.edu.vallegrande.ms_pagos.exception.PagoValidationException;
import pe.edu.vallegrande.ms_pagos.model.Pago;
import pe.edu.vallegrande.ms_pagos.repository.PagoRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    @Mock
    private PagoRepository pagoRepository;

    @InjectMocks
    private PagoService pagoService;

    private PagoRequest pagoRequestValido;
    private Pago pagoMock;

    @BeforeEach
    void setUp() {
        pagoRequestValido = new PagoRequest();
        pagoRequestValido.setOrderId("ORD-001");
        pagoRequestValido.setMonto(new BigDecimal("1500.50"));
        pagoRequestValido.setMetodoPago("Tarjeta de Crédito");
        pagoRequestValido.setMoneda("PEN");
        pagoRequestValido.setClienteId("CLI-123");
        pagoRequestValido.setDescripcion("Pago de prueba");
        pagoRequestValido.setPaymentGateway("Visa");

        pagoMock = new Pago();
        pagoMock.setId("test-id-123");
        pagoMock.setOrderId("ORD-001");
        pagoMock.setMonto(new BigDecimal("1500.50"));
        pagoMock.setEstado(Pago.EstadoPago.COMPLETED);
        pagoMock.setFechaCreacion(LocalDateTime.now());
        pagoMock.setFechaActualizacion(LocalDateTime.now());
        pagoMock.setMetodoPago("Tarjeta de Crédito");
        pagoMock.setMoneda("PEN");
        pagoMock.setClienteId("CLI-123");
        pagoMock.setDescripcion("Pago de prueba");
        pagoMock.setPaymentGateway("Visa");
        pagoMock.setCodigoRespuesta("SUCCESS");
        pagoMock.setMensajeRespuesta("Pago procesado exitosamente");
        pagoMock.setTransactionId("TXN_ABC123");
    }

    @Test
    void crearPago_ConDatosValidos_DeberiaRetornarPagoResponse() {
        // Given
        when(pagoRepository.save(any(Pago.class))).thenReturn(pagoMock);

        // When
        PagoResponse resultado = pagoService.crearPago(pagoRequestValido);

        // Then
        assertThat(resultado).isNotNull();
        assertThat(resultado.getOrderId()).isEqualTo("ORD-001");
        assertThat(resultado.getMonto()).isEqualTo(new BigDecimal("1500.50"));
        assertThat(resultado.getEstado()).isEqualTo(Pago.EstadoPago.COMPLETED);
        assertThat(resultado.getCodigoRespuesta()).isEqualTo("SUCCESS");

        verify(pagoRepository, times(1)).save(any(Pago.class));
    }

    @Test
    void crearPago_ConMontoAlto_DeberiaQuedarPendiente() {
        // Given
        pagoRequestValido.setMonto(new BigDecimal("15000.00")); // Monto alto
        Pago pagoAltoMonto = new Pago();
        pagoAltoMonto.setMonto(new BigDecimal("15000.00"));
        pagoAltoMonto.setEstado(Pago.EstadoPago.PENDING);
        pagoAltoMonto.setMensajeRespuesta("Pago en validación por monto alto");

        when(pagoRepository.save(any(Pago.class))).thenReturn(pagoAltoMonto);

        // When
        PagoResponse resultado = pagoService.crearPago(pagoRequestValido);

        // Then
        assertThat(resultado.getEstado()).isEqualTo(Pago.EstadoPago.PENDING);
        assertThat(resultado.getMensajeRespuesta()).contains("validación por monto alto");
    }

    @Test
    void crearPago_ConMontoMuyBajo_DeberiaFallar() {
        // Given
        pagoRequestValido.setMonto(new BigDecimal("0.50")); // Monto muy bajo
        Pago pagoBajoMonto = new Pago();
        pagoBajoMonto.setMonto(new BigDecimal("0.50"));
        pagoBajoMonto.setEstado(Pago.EstadoPago.FAILED);
        pagoBajoMonto.setCodigoRespuesta("AMOUNT_TOO_LOW");

        when(pagoRepository.save(any(Pago.class))).thenReturn(pagoBajoMonto);

        // When
        PagoResponse resultado = pagoService.crearPago(pagoRequestValido);

        // Then
        assertThat(resultado.getEstado()).isEqualTo(Pago.EstadoPago.FAILED);
        assertThat(resultado.getCodigoRespuesta()).isEqualTo("AMOUNT_TOO_LOW");
    }

    @Test
    void obtenerPagoPorId_ConIdExistente_DeberiaRetornarPago() {
        // Given
        String pagoId = "test-id-123";
        when(pagoRepository.findById(pagoId)).thenReturn(Optional.of(pagoMock));

        // When
        PagoResponse resultado = pagoService.obtenerPagoPorId(pagoId);

        // Then
        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(pagoId);
        assertThat(resultado.getOrderId()).isEqualTo("ORD-001");

        verify(pagoRepository, times(1)).findById(pagoId);
    }

    @Test
    void obtenerPagoPorId_ConIdInexistente_DeberiaLanzarExcepcion() {
        // Given
        String pagoId = "id-inexistente";
        when(pagoRepository.findById(pagoId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> pagoService.obtenerPagoPorId(pagoId))
                .isInstanceOf(PagoNotFoundException.class)
                .hasMessageContaining("Pago no encontrado con ID: " + pagoId);

        verify(pagoRepository, times(1)).findById(pagoId);
    }

    @Test
    void obtenerPagosPorOrden_DeberiaRetornarListaPagos() {
        // Given
        String orderId = "ORD-001";
        List<Pago> pagosMock = Arrays.asList(pagoMock);
        when(pagoRepository.findByOrderId(orderId)).thenReturn(pagosMock);

        // When
        List<PagoResponse> resultado = pagoService.obtenerPagosPorOrden(orderId);

        // Then
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getOrderId()).isEqualTo(orderId);

        verify(pagoRepository, times(1)).findByOrderId(orderId);
    }

    @Test
    void obtenerPagosPorCliente_DeberiaRetornarListaPagos() {
        // Given
        String clienteId = "CLI-123";
        List<Pago> pagosMock = Arrays.asList(pagoMock);
        when(pagoRepository.findByClienteId(clienteId)).thenReturn(pagosMock);

        // When
        List<PagoResponse> resultado = pagoService.obtenerPagosPorCliente(clienteId);

        // Then
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getClienteId()).isEqualTo(clienteId);

        verify(pagoRepository, times(1)).findByClienteId(clienteId);
    }

    @Test
    void actualizarEstadoPago_ConTransicionValida_DeberiaActualizarEstado() {
        // Given
        String pagoId = "test-id-123";
        pagoMock.setEstado(Pago.EstadoPago.PENDING);
        Pago pagoActualizado = new Pago();
        pagoActualizado.setId(pagoId);
        pagoActualizado.setEstado(Pago.EstadoPago.COMPLETED);

        when(pagoRepository.findById(pagoId)).thenReturn(Optional.of(pagoMock));
        when(pagoRepository.save(any(Pago.class))).thenReturn(pagoActualizado);

        // When
        PagoResponse resultado = pagoService.actualizarEstadoPago(pagoId, Pago.EstadoPago.COMPLETED);

        // Then
        assertThat(resultado.getEstado()).isEqualTo(Pago.EstadoPago.COMPLETED);

        verify(pagoRepository, times(1)).findById(pagoId);
        verify(pagoRepository, times(1)).save(any(Pago.class));
    }

    @Test
    void actualizarEstadoPago_ConTransicionInvalida_DeberiaLanzarExcepcion() {
        // Given
        String pagoId = "test-id-123";
        pagoMock.setEstado(Pago.EstadoPago.FAILED); // Estado final
        when(pagoRepository.findById(pagoId)).thenReturn(Optional.of(pagoMock));

        // When & Then
        assertThatThrownBy(() -> pagoService.actualizarEstadoPago(pagoId, Pago.EstadoPago.COMPLETED))
                .isInstanceOf(PagoValidationException.class)
                .hasMessageContaining("No se puede cambiar el estado");

        verify(pagoRepository, times(1)).findById(pagoId);
        verify(pagoRepository, never()).save(any(Pago.class));
    }

    @Test
    void refundPago_ConPagoCompletado_DeberiaProceserRefund() {
        // Given
        String pagoId = "test-id-123";
        BigDecimal montoRefund = new BigDecimal("500.00");
        pagoMock.setEstado(Pago.EstadoPago.COMPLETED);
        
        Pago pagoRefund = new Pago();
        pagoRefund.setId(pagoId);
        pagoRefund.setEstado(Pago.EstadoPago.REFUNDED);
        
        when(pagoRepository.findById(pagoId)).thenReturn(Optional.of(pagoMock));
        when(pagoRepository.save(any(Pago.class))).thenReturn(pagoRefund);

        // When
        PagoResponse resultado = pagoService.refundPago(pagoId, montoRefund);

        // Then
        assertThat(resultado.getEstado()).isEqualTo(Pago.EstadoPago.REFUNDED);

        verify(pagoRepository, times(1)).findById(pagoId);
        verify(pagoRepository, times(1)).save(any(Pago.class));
    }

    @Test
    void refundPago_ConPagoNoProcesado_DeberiaLanzarExcepcion() {
        // Given
        String pagoId = "test-id-123";
        BigDecimal montoRefund = new BigDecimal("500.00");
        pagoMock.setEstado(Pago.EstadoPago.PENDING);
        
        when(pagoRepository.findById(pagoId)).thenReturn(Optional.of(pagoMock));

        // When & Then
        assertThatThrownBy(() -> pagoService.refundPago(pagoId, montoRefund))
                .isInstanceOf(PagoValidationException.class)
                .hasMessageContaining("Solo se pueden hacer refunds a pagos completados");

        verify(pagoRepository, times(1)).findById(pagoId);
        verify(pagoRepository, never()).save(any(Pago.class));
    }

    @Test
    void refundPago_ConMontoMayorAlPago_DeberiaLanzarExcepcion() {
        // Given
        String pagoId = "test-id-123";
        BigDecimal montoRefund = new BigDecimal("2000.00"); // Mayor al monto del pago
        pagoMock.setEstado(Pago.EstadoPago.COMPLETED);
        
        when(pagoRepository.findById(pagoId)).thenReturn(Optional.of(pagoMock));

        // When & Then
        assertThatThrownBy(() -> pagoService.refundPago(pagoId, montoRefund))
                .isInstanceOf(PagoValidationException.class)
                .hasMessageContaining("El monto del refund no puede ser mayor al monto del pago");

        verify(pagoRepository, times(1)).findById(pagoId);
        verify(pagoRepository, never()).save(any(Pago.class));
    }
}