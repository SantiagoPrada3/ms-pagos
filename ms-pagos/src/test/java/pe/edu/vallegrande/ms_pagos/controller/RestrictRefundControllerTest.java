package pe.edu.vallegrande.ms_pagos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.vallegrande.ms_pagos.dto.request.RefundRequest;
import pe.edu.vallegrande.ms_pagos.dto.response.PagoResponse;
import pe.edu.vallegrande.ms_pagos.exception.PagoNotFoundException;
import pe.edu.vallegrande.ms_pagos.exception.PagoValidationException;
import pe.edu.vallegrande.ms_pagos.model.Pago;
import pe.edu.vallegrande.ms_pagos.service.PagoService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PagoController.class)
class RestrictRefundControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PagoService pagoService;

    @Autowired
    private ObjectMapper objectMapper;

    private RefundRequest refundRequestValido;
    private PagoResponse pagoResponseMock;

    @BeforeEach
    void setUp() {
        // RefundRequest válido
        refundRequestValido = new RefundRequest();
        refundRequestValido.setOrderId("ORD-REST-001");
        refundRequestValido.setAmount(new BigDecimal("150.00"));
        refundRequestValido.setMaxRefundable(new BigDecimal("500.00"));

        // PagoResponse mock
        pagoResponseMock = new PagoResponse(
                "pago-refunded-123",
                "ORD-REST-001",
                new BigDecimal("1000.00"),
                Pago.EstadoPago.REFUNDED,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now(),
                "Tarjeta de Crédito",
                "PEN",
                "Pago de prueba",
                "CLI-001",
                "TXN_12345678",
                "Visa",
                "RESTRICTED_REFUND_SUCCESS",
                "Refund restringido procesado exitosamente. Monto: 150.00, Límite: 500.00, Restante: 350.00"
        );
    }

    @Test
    void restrictRefund_ConDatosValidos_DeberiaRetornar200() throws Exception {
        // Given
        when(pagoService.restrictRefund(any(RefundRequest.class))).thenReturn(pagoResponseMock);

        // When & Then
        mockMvc.perform(post("/pagos/restrict-refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refundRequestValido)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(containsString("Refund restringido procesado exitosamente")))
                .andExpect(jsonPath("$.data.id").value("pago-refunded-123"))
                .andExpect(jsonPath("$.data.estado").value("REFUNDED"));

        verify(pagoService).restrictRefund(any(RefundRequest.class));
    }

    @Test
    void restrictRefund_ConMontoExcediendoLimite_DeberiaRetornar400() throws Exception {
        // Given
        RefundRequest requestExcedido = new RefundRequest();
        requestExcedido.setOrderId("ORD-REST-001");
        requestExcedido.setAmount(new BigDecimal("600.00")); // Excede límite de 500
        requestExcedido.setMaxRefundable(new BigDecimal("500.00"));

        when(pagoService.restrictRefund(any(RefundRequest.class)))
                .thenThrow(new PagoValidationException("amount", "600.00", 
                    "El monto del refund (600.00) excede el límite máximo permitido (500.00)"));

        // When & Then
        mockMvc.perform(post("/pagos/restrict-refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestExcedido)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verify(pagoService).restrictRefund(any(RefundRequest.class));
    }

    @Test
    void restrictRefund_ConOrdenInexistente_DeberiaRetornar404() throws Exception {
        // Given
        RefundRequest requestInexistente = new RefundRequest();
        requestInexistente.setOrderId("ORD-INEXISTENTE");
        requestInexistente.setAmount(new BigDecimal("100.00"));
        requestInexistente.setMaxRefundable(new BigDecimal("500.00"));

        when(pagoService.restrictRefund(any(RefundRequest.class)))
                .thenThrow(new PagoNotFoundException("No se encontraron pagos para la orden: ORD-INEXISTENTE"));

        // When & Then
        mockMvc.perform(post("/pagos/restrict-refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestInexistente)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("PAGO_NOT_FOUND"));

        verify(pagoService).restrictRefund(any(RefundRequest.class));
    }

    @Test
    void restrictRefund_ConCamposObligatoriosVacios_DeberiaRetornar400() throws Exception {
        // Given
        RefundRequest requestInvalido = new RefundRequest();
        requestInvalido.setOrderId(""); // Vacío
        requestInvalido.setAmount(null); // Null
        requestInvalido.setMaxRefundable(new BigDecimal("500.00"));

        when(pagoService.restrictRefund(any(RefundRequest.class)))
                .thenThrow(new PagoValidationException("orderId", "", "El orderId es obligatorio"));

        // When & Then
        mockMvc.perform(post("/pagos/restrict-refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestInvalido)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verify(pagoService).restrictRefund(any(RefundRequest.class));
    }

    @Test
    void restrictRefund_ConMontoNegativo_DeberiaRetornar400() throws Exception {
        // Given
        RefundRequest requestMontoNegativo = new RefundRequest();
        requestMontoNegativo.setOrderId("ORD-REST-001");
        requestMontoNegativo.setAmount(new BigDecimal("-50.00")); // Negativo
        requestMontoNegativo.setMaxRefundable(new BigDecimal("500.00"));

        when(pagoService.restrictRefund(any(RefundRequest.class)))
                .thenThrow(new PagoValidationException("amount", "-50.00", "El amount debe ser mayor a 0"));

        // When & Then
        mockMvc.perform(post("/pagos/restrict-refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestMontoNegativo)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(pagoService).restrictRefund(any(RefundRequest.class));
    }
}