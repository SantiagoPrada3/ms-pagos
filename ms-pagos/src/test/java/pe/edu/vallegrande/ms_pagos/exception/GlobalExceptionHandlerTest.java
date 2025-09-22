package pe.edu.vallegrande.ms_pagos.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.vallegrande.ms_pagos.controller.PagoController;
import pe.edu.vallegrande.ms_pagos.dto.request.PagoRequest;
import pe.edu.vallegrande.ms_pagos.service.PagoService;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PagoController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PagoService pagoService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void handlePagoNotFound_DeberiaRetornar404() throws Exception {
        // Given
        String pagoId = "pago-inexistente";
        when(pagoService.obtenerPagoPorId(pagoId))
                .thenThrow(new PagoNotFoundException(pagoId));

        // When & Then
        mockMvc.perform(get("/pagos/{pagoId}", pagoId))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("PAGO_NOT_FOUND"));
    }

    @Test
    void handlePagoValidation_DeberiaRetornar400() throws Exception {
        // Given
        PagoRequest pagoRequest = new PagoRequest();
        pagoRequest.setOrderId("ORD-001");
        pagoRequest.setMonto(new BigDecimal("1000"));
        pagoRequest.setMetodoPago("Tarjeta");
        pagoRequest.setMoneda("PEN");
        pagoRequest.setClienteId("CLI-001");

        when(pagoService.crearPago(any(PagoRequest.class)))
                .thenThrow(new PagoValidationException("El monto debe ser mayor a 0"));

        // When & Then
        mockMvc.perform(post("/pagos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pagoRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void handleGenericException_DeberiaRetornar500() throws Exception {
        // Given
        when(pagoService.obtenerTodosLosPagos())
                .thenThrow(new RuntimeException("Error inesperado"));

        // When & Then
        mockMvc.perform(get("/pagos"))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_SERVER_ERROR"));
    }
}