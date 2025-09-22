package pe.edu.vallegrande.ms_pagos.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.vallegrande.ms_pagos.dto.request.RefundRequest;
import pe.edu.vallegrande.ms_pagos.dto.response.PagoResponse;
import pe.edu.vallegrande.ms_pagos.exception.PagoNotFoundException;
import pe.edu.vallegrande.ms_pagos.exception.PagoValidationException;
import pe.edu.vallegrande.ms_pagos.model.Pago;
import pe.edu.vallegrande.ms_pagos.repository.PagoRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagoServiceRestrictRefundTest {

    @Mock
    private PagoRepository pagoRepository;

    @InjectMocks
    private PagoService pagoService;

    private RefundRequest refundRequestValido;
    private Pago pagoCompletado;
    private Pago pagoPendiente;

    @BeforeEach
    void setUp() {
        // RefundRequest válido
        refundRequestValido = new RefundRequest();
        refundRequestValido.setOrderId("ORD-RESTRICT-001");
        refundRequestValido.setAmount(new BigDecimal("100.00"));
        refundRequestValido.setMaxRefundable(new BigDecimal("500.00"));

        // Pago completado mock
        pagoCompletado = new Pago();
        pagoCompletado.setId("pago-completed-123");
        pagoCompletado.setOrderId("ORD-RESTRICT-001");
        pagoCompletado.setMonto(new BigDecimal("1000.00"));
        pagoCompletado.setEstado(Pago.EstadoPago.COMPLETED);
        pagoCompletado.setFechaCreacion(LocalDateTime.now().minusHours(1));
        pagoCompletado.setFechaActualizacion(LocalDateTime.now().minusHours(1));
        pagoCompletado.setMetodoPago("Tarjeta de Crédito");
        pagoCompletado.setMoneda("PEN");
        pagoCompletado.setClienteId("CLI-001");

        // Pago pendiente mock
        pagoPendiente = new Pago();
        pagoPendiente.setId("pago-pending-456");
        pagoPendiente.setOrderId("ORD-RESTRICT-001");
        pagoPendiente.setMonto(new BigDecimal("500.00"));
        pagoPendiente.setEstado(Pago.EstadoPago.PENDING);
        pagoPendiente.setFechaCreacion(LocalDateTime.now().minusMinutes(30));
        pagoPendiente.setFechaActualizacion(LocalDateTime.now().minusMinutes(30));
        pagoPendiente.setMetodoPago("PayPal");
        pagoPendiente.setMoneda("USD");
        pagoPendiente.setClienteId("CLI-002");
    }

    @Test
    void restrictRefund_ConDatosValidos_DeberiaRetornarPagoRefunded() {
        // Given
        List<Pago> pagosOrden = Arrays.asList(pagoCompletado, pagoPendiente);
        when(pagoRepository.findByOrderId("ORD-RESTRICT-001")).thenReturn(pagosOrden);
        when(pagoRepository.save(any(Pago.class))).thenReturn(pagoCompletado);

        // When
        PagoResponse resultado = pagoService.restrictRefund(refundRequestValido);

        // Then
        assertThat(resultado).isNotNull();
        assertThat(resultado.getEstado()).isEqualTo(Pago.EstadoPago.REFUNDED);
        assertThat(resultado.getCodigoRespuesta()).isEqualTo("RESTRICTED_REFUND_SUCCESS");
        assertThat(resultado.getMensajeRespuesta()).contains("Refund restringido procesado exitosamente");
        assertThat(resultado.getMensajeRespuesta()).contains("100.00");
        assertThat(resultado.getMensajeRespuesta()).contains("500.00");
        assertThat(resultado.getMensajeRespuesta()).contains("400.00"); // remaining

        verify(pagoRepository).findByOrderId("ORD-RESTRICT-001");
        verify(pagoRepository).save(any(Pago.class));
    }

    @Test
    void restrictRefund_ConMontoExcediendoLimite_DeberiaLanzarExcepcion() {
        // Given
        RefundRequest requestExcedido = new RefundRequest();
        requestExcedido.setOrderId("ORD-RESTRICT-001");
        requestExcedido.setAmount(new BigDecimal("600.00")); // Excede el límite de 500
        requestExcedido.setMaxRefundable(new BigDecimal("500.00"));

        // When & Then
        assertThatThrownBy(() -> pagoService.restrictRefund(requestExcedido))
                .isInstanceOf(PagoValidationException.class)
                .hasMessageContaining("El monto del refund (600.00) excede el límite máximo permitido (500.00)");

        verify(pagoRepository, never()).findByOrderId(any());
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void restrictRefund_ConOrdenInexistente_DeberiaLanzarExcepcion() {
        // Given
        when(pagoRepository.findByOrderId("ORD-INEXISTENTE")).thenReturn(Collections.emptyList());

        RefundRequest requestInexistente = new RefundRequest();
        requestInexistente.setOrderId("ORD-INEXISTENTE");
        requestInexistente.setAmount(new BigDecimal("100.00"));
        requestInexistente.setMaxRefundable(new BigDecimal("500.00"));

        // When & Then
        assertThatThrownBy(() -> pagoService.restrictRefund(requestInexistente))
                .isInstanceOf(PagoNotFoundException.class)
                .hasMessageContaining("No se encontraron pagos para la orden: ORD-INEXISTENTE");

        verify(pagoRepository).findByOrderId("ORD-INEXISTENTE");
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void restrictRefund_SinPagosCompletados_DeberiaLanzarExcepcion() {
        // Given
        List<Pago> solopagosPendientes = Arrays.asList(pagoPendiente);
        when(pagoRepository.findByOrderId("ORD-RESTRICT-001")).thenReturn(solopagosPendientes);

        // When & Then
        assertThatThrownBy(() -> pagoService.restrictRefund(refundRequestValido))
                .isInstanceOf(PagoValidationException.class)
                .hasMessageContaining("No hay pagos completados disponibles para refund en la orden: ORD-RESTRICT-001");

        verify(pagoRepository).findByOrderId("ORD-RESTRICT-001");
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void restrictRefund_ConMontoMayorAlPagoOriginal_DeberiaLanzarExcepcion() {
        // Given
        Pago pagoMenor = new Pago();
        pagoMenor.setId("pago-menor-789");
        pagoMenor.setOrderId("ORD-RESTRICT-001");
        pagoMenor.setMonto(new BigDecimal("50.00")); // Menor al refund solicitado de 100
        pagoMenor.setEstado(Pago.EstadoPago.COMPLETED);

        List<Pago> pagosOrden = Arrays.asList(pagoMenor);
        when(pagoRepository.findByOrderId("ORD-RESTRICT-001")).thenReturn(pagosOrden);

        // When & Then
        assertThatThrownBy(() -> pagoService.restrictRefund(refundRequestValido))
                .isInstanceOf(PagoValidationException.class)
                .hasMessageContaining("El monto del refund (100.00) no puede ser mayor al monto del pago original (50.00)");

        verify(pagoRepository).findByOrderId("ORD-RESTRICT-001");
        verify(pagoRepository, never()).save(any());
    }

    @ParameterizedTest(name = "OrderId inválido: ''{0}''")
    @CsvSource({
        "'', El orderId es obligatorio",
        "' ', El orderId es obligatorio",
        "null, El orderId es obligatorio"
    })
    void restrictRefund_ConOrderIdInvalido_DeberiaLanzarExcepcion(String orderIdInvalido, String mensajeEsperado) {
        // Given
        RefundRequest requestInvalido = new RefundRequest();
        requestInvalido.setOrderId("null".equals(orderIdInvalido) ? null : orderIdInvalido);
        requestInvalido.setAmount(new BigDecimal("100.00"));
        requestInvalido.setMaxRefundable(new BigDecimal("500.00"));

        // When & Then
        assertThatThrownBy(() -> pagoService.restrictRefund(requestInvalido))
                .isInstanceOf(PagoValidationException.class)
                .hasMessageContaining(mensajeEsperado);

        verify(pagoRepository, never()).findByOrderId(any());
        verify(pagoRepository, never()).save(any());
    }

    @ParameterizedTest(name = "Amount inválido: {0}")
    @CsvSource({
        "null, El amount es obligatorio",
        "0, El amount debe ser mayor a 0",
        "-50.00, El amount debe ser mayor a 0"
    })
    void restrictRefund_ConAmountInvalido_DeberiaLanzarExcepcion(String amountStr, String mensajeEsperado) {
        // Given
        RefundRequest requestInvalido = new RefundRequest();
        requestInvalido.setOrderId("ORD-RESTRICT-001");
        requestInvalido.setAmount("null".equals(amountStr) ? null : new BigDecimal(amountStr));
        requestInvalido.setMaxRefundable(new BigDecimal("500.00"));

        // When & Then
        assertThatThrownBy(() -> pagoService.restrictRefund(requestInvalido))
                .isInstanceOf(PagoValidationException.class)
                .hasMessageContaining(mensajeEsperado);

        verify(pagoRepository, never()).findByOrderId(any());
        verify(pagoRepository, never()).save(any());
    }

    @ParameterizedTest(name = "MaxRefundable inválido: {0}")
    @CsvSource({
        "null, El maxRefundable es obligatorio",
        "0, El maxRefundable debe ser mayor a 0",
        "-100.00, El maxRefundable debe ser mayor a 0"
    })
    void restrictRefund_ConMaxRefundableInvalido_DeberiaLanzarExcepcion(String maxRefundableStr, String mensajeEsperado) {
        // Given
        RefundRequest requestInvalido = new RefundRequest();
        requestInvalido.setOrderId("ORD-RESTRICT-001");
        requestInvalido.setAmount(new BigDecimal("100.00"));
        requestInvalido.setMaxRefundable("null".equals(maxRefundableStr) ? null : new BigDecimal(maxRefundableStr));

        // When & Then
        assertThatThrownBy(() -> pagoService.restrictRefund(requestInvalido))
                .isInstanceOf(PagoValidationException.class)
                .hasMessageContaining(mensajeEsperado);

        verify(pagoRepository, never()).findByOrderId(any());
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void restrictRefund_ConMultiplesPagosCompletados_DeberiaUsarPrimero() {
        // Given
        Pago segundoPagoCompletado = new Pago();
        segundoPagoCompletado.setId("pago-completed-second");
        segundoPagoCompletado.setOrderId("ORD-RESTRICT-001");
        segundoPagoCompletado.setMonto(new BigDecimal("2000.00"));
        segundoPagoCompletado.setEstado(Pago.EstadoPago.COMPLETED);

        List<Pago> pagosOrden = Arrays.asList(pagoCompletado, segundoPagoCompletado, pagoPendiente);
        when(pagoRepository.findByOrderId("ORD-RESTRICT-001")).thenReturn(pagosOrden);
        when(pagoRepository.save(any(Pago.class))).thenReturn(pagoCompletado);

        // When
        PagoResponse resultado = pagoService.restrictRefund(refundRequestValido);

        // Then
        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo("pago-completed-123"); // Primer pago completado

        verify(pagoRepository).findByOrderId("ORD-RESTRICT-001");
        verify(pagoRepository).save(any(Pago.class));
    }

    @Test
    void restrictRefund_ConMontoExactoAlLimite_DeberiaFuncionar() {
        // Given
        RefundRequest requestLimiteExacto = new RefundRequest();
        requestLimiteExacto.setOrderId("ORD-RESTRICT-001");
        requestLimiteExacto.setAmount(new BigDecimal("500.00")); // Exacto al límite
        requestLimiteExacto.setMaxRefundable(new BigDecimal("500.00"));

        List<Pago> pagosOrden = Arrays.asList(pagoCompletado);
        when(pagoRepository.findByOrderId("ORD-RESTRICT-001")).thenReturn(pagosOrden);
        when(pagoRepository.save(any(Pago.class))).thenReturn(pagoCompletado);

        // When
        PagoResponse resultado = pagoService.restrictRefund(requestLimiteExacto);

        // Then
        assertThat(resultado).isNotNull();
        assertThat(resultado.getEstado()).isEqualTo(Pago.EstadoPago.REFUNDED);
        assertThat(resultado.getMensajeRespuesta()).contains("500.00");
        assertThat(resultado.getMensajeRespuesta()).contains("0.00"); // remaining = 0

        verify(pagoRepository).findByOrderId("ORD-RESTRICT-001");
        verify(pagoRepository).save(any(Pago.class));
    }
}