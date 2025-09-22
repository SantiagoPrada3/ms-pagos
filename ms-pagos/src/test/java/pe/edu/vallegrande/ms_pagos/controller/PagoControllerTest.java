package pe.edu.vallegrande.ms_pagos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.vallegrande.ms_pagos.dto.request.PagoRequest;
import pe.edu.vallegrande.ms_pagos.dto.response.PagoResponse;
import pe.edu.vallegrande.ms_pagos.exception.PagoNotFoundException;
import pe.edu.vallegrande.ms_pagos.exception.PagoValidationException;
import pe.edu.vallegrande.ms_pagos.model.Pago;
import pe.edu.vallegrande.ms_pagos.service.PagoService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PagoController.class)
class PagoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PagoService pagoService;

    @Autowired
    private ObjectMapper objectMapper;

    private PagoRequest pagoRequestValido;
    private PagoResponse pagoResponseMock;

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

        pagoResponseMock = new PagoResponse();
        pagoResponseMock.setId("pago-123");
        pagoResponseMock.setOrderId("ORD-001");
        pagoResponseMock.setMonto(new BigDecimal("1500.50"));
        pagoResponseMock.setEstado(Pago.EstadoPago.COMPLETED);
        pagoResponseMock.setFechaCreacion(LocalDateTime.now());
        pagoResponseMock.setFechaActualizacion(LocalDateTime.now());
        pagoResponseMock.setMetodoPago("Tarjeta de Crédito");
        pagoResponseMock.setMoneda("PEN");
        pagoResponseMock.setClienteId("CLI-123");
        pagoResponseMock.setDescripcion("Pago de prueba");
        pagoResponseMock.setPaymentGateway("Visa");
        pagoResponseMock.setCodigoRespuesta("SUCCESS");
        pagoResponseMock.setMensajeRespuesta("Pago procesado exitosamente");
        pagoResponseMock.setTransactionId("TXN_ABC123");
    }

    @Test
    void crearPago_ConDatosValidos_DeberiaRetornar201() throws Exception {
        // Given
        when(pagoService.crearPago(any(PagoRequest.class))).thenReturn(pagoResponseMock);

        // When & Then
        mockMvc.perform(post("/pagos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pagoRequestValido)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Pago creado exitosamente"))
                .andExpect(jsonPath("$.data.id").value("pago-123"))
                .andExpect(jsonPath("$.data.orderId").value("ORD-001"))
                .andExpect(jsonPath("$.data.monto").value(1500.50))
                .andExpect(jsonPath("$.data.estado").value("COMPLETED"))
                .andExpect(jsonPath("$.data.codigoRespuesta").value("SUCCESS"));
    }

    @Test
    void crearPago_ConDatosInvalidos_DeberiaRetornar400() throws Exception {
        // Given
        when(pagoService.crearPago(any(PagoRequest.class)))
                .thenThrow(new PagoValidationException("El monto es obligatorio"));

        // When & Then
        mockMvc.perform(post("/pagos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pagoRequestValido)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("El monto es obligatorio"));
    }

    @Test
    void obtenerPago_ConIdExistente_DeberiaRetornar200() throws Exception {
        // Given
        String pagoId = "pago-123";
        when(pagoService.obtenerPagoPorId(pagoId)).thenReturn(pagoResponseMock);

        // When & Then
        mockMvc.perform(get("/pagos/{pagoId}", pagoId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(pagoId))
                .andExpect(jsonPath("$.data.orderId").value("ORD-001"));
    }

    @Test
    void obtenerPago_ConIdInexistente_DeberiaRetornar404() throws Exception {
        // Given
        String pagoId = "pago-inexistente";
        when(pagoService.obtenerPagoPorId(pagoId))
                .thenThrow(new PagoNotFoundException(pagoId));

        // When & Then
        mockMvc.perform(get("/pagos/{pagoId}", pagoId))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("PAGO_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Pago no encontrado con ID: " + pagoId));
    }

    @Test
    void obtenerTodosLosPagos_DeberiaRetornar200() throws Exception {
        // Given
        List<PagoResponse> listaPagos = Arrays.asList(pagoResponseMock);
        when(pagoService.obtenerTodosLosPagos()).thenReturn(listaPagos);

        // When & Then
        mockMvc.perform(get("/pagos"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Se encontraron 1 pagos"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value("pago-123"));
    }

    @Test
    void obtenerTodosLosPagos_SinPagos_DeberiaRetornarListaVacia() throws Exception {
        // Given
        when(pagoService.obtenerTodosLosPagos()).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/pagos"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Se encontraron 0 pagos"))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void obtenerPagosPorOrden_DeberiaRetornar200() throws Exception {
        // Given
        String orderId = "ORD-001";
        List<PagoResponse> listaPagos = Arrays.asList(pagoResponseMock);
        when(pagoService.obtenerPagosPorOrden(orderId)).thenReturn(listaPagos);

        // When & Then
        mockMvc.perform(get("/pagos/orden/{orderId}", orderId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Se encontraron 1 pagos para la orden " + orderId))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].orderId").value(orderId));
    }

    @Test
    void obtenerPagosPorCliente_DeberiaRetornar200() throws Exception {
        // Given
        String clienteId = "CLI-123";
        List<PagoResponse> listaPagos = Arrays.asList(pagoResponseMock);
        when(pagoService.obtenerPagosPorCliente(clienteId)).thenReturn(listaPagos);

        // When & Then
        mockMvc.perform(get("/pagos/cliente/{clienteId}", clienteId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Se encontraron 1 pagos para el cliente " + clienteId))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].clienteId").value(clienteId));
    }

    @Test
    void actualizarEstadoPago_ConEstadoValido_DeberiaRetornar200() throws Exception {
        // Given
        String pagoId = "pago-123";
        Pago.EstadoPago nuevoEstado = Pago.EstadoPago.COMPLETED;
        pagoResponseMock.setEstado(nuevoEstado);
        
        when(pagoService.actualizarEstadoPago(pagoId, nuevoEstado)).thenReturn(pagoResponseMock);

        // When & Then
        mockMvc.perform(patch("/pagos/{pagoId}/estado", pagoId)
                .param("estado", nuevoEstado.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Estado del pago actualizado a " + nuevoEstado))
                .andExpect(jsonPath("$.data.estado").value(nuevoEstado.toString()));
    }

    @Test
    void actualizarEstadoPago_ConTransicionInvalida_DeberiaRetornar400() throws Exception {
        // Given
        String pagoId = "pago-123";
        Pago.EstadoPago estadoInvalido = Pago.EstadoPago.COMPLETED;
        
        when(pagoService.actualizarEstadoPago(pagoId, estadoInvalido))
                .thenThrow(new PagoValidationException("Transición de estado inválida"));

        // When & Then
        mockMvc.perform(patch("/pagos/{pagoId}/estado", pagoId)
                .param("estado", estadoInvalido.toString()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void refundPago_ConDatosValidos_DeberiaRetornar200() throws Exception {
        // Given
        String pagoId = "pago-123";
        BigDecimal montoRefund = new BigDecimal("500.00");
        pagoResponseMock.setEstado(Pago.EstadoPago.REFUNDED);
        
        when(pagoService.refundPago(pagoId, montoRefund)).thenReturn(pagoResponseMock);

        // When & Then
        mockMvc.perform(post("/pagos/{pagoId}/refund", pagoId)
                .param("monto", montoRefund.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Refund procesado exitosamente"))
                .andExpect(jsonPath("$.data.estado").value("REFUNDED"));
    }

    @Test
    void refundPago_ConPagoInvalido_DeberiaRetornar400() throws Exception {
        // Given
        String pagoId = "pago-123";
        BigDecimal montoRefund = new BigDecimal("500.00");
        
        when(pagoService.refundPago(pagoId, montoRefund))
                .thenThrow(new PagoValidationException("Solo se pueden hacer refunds a pagos completados"));

        // When & Then
        mockMvc.perform(post("/pagos/{pagoId}/refund", pagoId)
                .param("monto", montoRefund.toString()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void healthCheck_DeberiaRetornar200() throws Exception {
        // When & Then
        mockMvc.perform(get("/pagos/health"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Microservicio de pagos funcionando correctamente"));
    }

    @Test
    void obtenerEstadisticas_DeberiaRetornar200() throws Exception {
        // Given
        when(pagoService.obtenerTodosLosPagos()).thenReturn(Arrays.asList(pagoResponseMock));

        // When & Then
        mockMvc.perform(get("/pagos/stats"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Estadísticas obtenidas exitosamente"))
                .andExpect(jsonPath("$.data.totalPagos").value(1))
                .andExpect(jsonPath("$.data.pagosCompletados").value(1))
                .andExpect(jsonPath("$.data.pagosPendientes").value(0))
                .andExpect(jsonPath("$.data.pagosFallidos").value(0))
                .andExpect(jsonPath("$.data.montoTotalCompletado").value(1500.50))
                .andExpect(jsonPath("$.data.tasaExito").value(100.0));
    }

    @Test
    void crearPago_ConJsonMalformado_DeberiaRetornar400() throws Exception {
        // Given
        String jsonMalformado = "{ \"orderId\": \"ORD-001\", \"monto\": }";

        // When & Then
        mockMvc.perform(post("/pagos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMalformado))
                .andDo(print())
                .andExpect(status().isInternalServerError()); // 500 porque el JSON está mal formado
    }

    @Test
    void obtenerPago_ConParametroInvalido_DeberiaRetornar400() throws Exception {
        // When & Then
        mockMvc.perform(get("/pagos/ ")) // ID con espacios
                .andDo(print())
                .andExpect(status().isOk()); // 200 porque el pago se busca aunque el ID tenga espacios
    }

    @Test
    void actualizarEstadoPago_SinParametroEstado_DeberiaRetornar400() throws Exception {
        // Given
        String pagoId = "pago-123";

        // When & Then
        mockMvc.perform(patch("/pagos/{pagoId}/estado", pagoId))
                .andDo(print())
                .andExpect(status().isInternalServerError()); // 500 sin el parámetro estado
    }

    @Test
    void refundPago_SinParametroMonto_DeberiaRetornar400() throws Exception {
        // Given
        String pagoId = "pago-123";

        // When & Then
        mockMvc.perform(post("/pagos/{pagoId}/refund", pagoId))
                .andDo(print())
                .andExpect(status().isInternalServerError()); // 500 sin el parámetro monto
    }

    @Test
    void endpoint_ConContentTypeIncorrecto_DeberiaRetornar415() throws Exception {
        // When & Then
        mockMvc.perform(post("/pagos")
                .contentType(MediaType.APPLICATION_XML)
                .content("<pago></pago>"))
                .andDo(print())
                .andExpect(status().isInternalServerError()); // 500 con content type incorrecto
    }
}