package pe.edu.vallegrande.ms_pagos.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pe.edu.vallegrande.ms_pagos.dto.request.PagoRequest;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PagoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void flujoCompletoPago_CrearConsultarActualizar_DeberiaFuncionar() throws Exception {
        // 1. Crear un pago
        PagoRequest pagoRequest = new PagoRequest();
        pagoRequest.setOrderId("ORD-INTEGRATION-001");
        pagoRequest.setMonto(new BigDecimal("2500.75"));
        pagoRequest.setMetodoPago("Tarjeta de Crédito");
        pagoRequest.setMoneda("PEN");
        pagoRequest.setClienteId("CLI-INTEGRATION-001");
        pagoRequest.setDescripcion("Pago de integración");
        pagoRequest.setPaymentGateway("Visa");

        MvcResult createResult = mockMvc.perform(post("/pagos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pagoRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value("ORD-INTEGRATION-001"))
                .andExpect(jsonPath("$.data.monto").value(2500.75))
                .andExpect(jsonPath("$.data.estado").value("COMPLETED"))
                .andReturn();

        // Extraer el ID del pago creado
        String responseBody = createResult.getResponse().getContentAsString();
        String pagoId = objectMapper.readTree(responseBody)
                .path("data").path("id").asText();

        // 2. Consultar el pago creado
        mockMvc.perform(get("/pagos/{pagoId}", pagoId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(pagoId))
                .andExpect(jsonPath("$.data.orderId").value("ORD-INTEGRATION-001"))
                .andExpect(jsonPath("$.data.estado").value("COMPLETED"));

        // 3. Consultar pagos por orden
        mockMvc.perform(get("/pagos/orden/{orderId}", "ORD-INTEGRATION-001"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].orderId").value("ORD-INTEGRATION-001"));

        // 4. Consultar pagos por cliente
        mockMvc.perform(get("/pagos/cliente/{clienteId}", "CLI-INTEGRATION-001"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].clienteId").value("CLI-INTEGRATION-001"));

        // 5. Procesar un refund
        mockMvc.perform(post("/pagos/{pagoId}/refund", pagoId)
                .param("monto", "1000.00"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.estado").value("REFUNDED"));
    }

    @Test
    void crearMultiplesPagos_YConsultarEstadisticas_DeberiaFuncionar() throws Exception {
        // Crear varios pagos con diferentes estados
        
        // Pago 1 - Monto normal (se completa automáticamente)
        PagoRequest pago1 = new PagoRequest();
        pago1.setOrderId("ORD-STATS-001");
        pago1.setMonto(new BigDecimal("1500.00"));
        pago1.setMetodoPago("Tarjeta");
        pago1.setMoneda("PEN");
        pago1.setClienteId("CLI-STATS-001");

        mockMvc.perform(post("/pagos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pago1)))
                .andExpect(status().isCreated());

        // Pago 2 - Monto alto (queda pendiente)
        PagoRequest pago2 = new PagoRequest();
        pago2.setOrderId("ORD-STATS-002");
        pago2.setMonto(new BigDecimal("15000.00"));
        pago2.setMetodoPago("Transferencia");
        pago2.setMoneda("USD");
        pago2.setClienteId("CLI-STATS-002");

        mockMvc.perform(post("/pagos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pago2)))
                .andExpect(status().isCreated());

        // Pago 3 - Monto muy bajo (falla automáticamente)
        PagoRequest pago3 = new PagoRequest();
        pago3.setOrderId("ORD-STATS-003");
        pago3.setMonto(new BigDecimal("0.50"));
        pago3.setMetodoPago("PayPal");
        pago3.setMoneda("PEN");
        pago3.setClienteId("CLI-STATS-003");

        mockMvc.perform(post("/pagos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pago3)))
                .andExpect(status().isCreated());

        // Consultar estadísticas
        mockMvc.perform(get("/pagos/stats"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalPagos").value(3))
                .andExpect(jsonPath("$.data.pagosCompletados").value(1))
                .andExpect(jsonPath("$.data.pagosPendientes").value(1))
                .andExpect(jsonPath("$.data.pagosFallidos").value(1))
                .andExpect(jsonPath("$.data.montoTotalCompletado").value(1500.00))
                .andExpect(jsonPath("$.data.tasaExito").value(33.33333333333333)); // 1/3 * 100
    }

    @Test
    void validacionesIntegrales_DeberianFuncionar() throws Exception {
        // Test de validación: Orden ID vacío
        PagoRequest pagoInvalido1 = new PagoRequest();
        pagoInvalido1.setOrderId("");
        pagoInvalido1.setMonto(new BigDecimal("1000.00"));
        pagoInvalido1.setMetodoPago("Tarjeta");
        pagoInvalido1.setMoneda("PEN");
        pagoInvalido1.setClienteId("CLI-001");

        mockMvc.perform(post("/pagos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pagoInvalido1)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        // Test de validación: Monto nulo
        PagoRequest pagoInvalido2 = new PagoRequest();
        pagoInvalido2.setOrderId("ORD-001");
        pagoInvalido2.setMonto(null);
        pagoInvalido2.setMetodoPago("Tarjeta");
        pagoInvalido2.setMoneda("PEN");
        pagoInvalido2.setClienteId("CLI-001");

        mockMvc.perform(post("/pagos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pagoInvalido2)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        // Test de validación: Monto excesivo
        PagoRequest pagoInvalido3 = new PagoRequest();
        pagoInvalido3.setOrderId("ORD-001");
        pagoInvalido3.setMonto(new BigDecimal("600000.00")); // Excede límite
        pagoInvalido3.setMetodoPago("Tarjeta");
        pagoInvalido3.setMoneda("PEN");
        pagoInvalido3.setClienteId("CLI-001");

        mockMvc.perform(post("/pagos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pagoInvalido3)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", containsString("excede el límite")));
    }

    @Test
    void healthCheck_DeberiaEstarDisponible() throws Exception {
        mockMvc.perform(get("/pagos/health"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Microservicio de pagos funcionando correctamente"));
    }

    @Test
    void consultarPagoInexistente_DeberiaRetornar404() throws Exception {
        mockMvc.perform(get("/pagos/{pagoId}", "pago-inexistente-123"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("PAGO_NOT_FOUND"));
    }

    @Test
    void actualizarEstadoInvalido_DeberiaRetornar400() throws Exception {
        // Primero crear un pago que falle (para tener un estado final)
        PagoRequest pagoRequest = new PagoRequest();
        pagoRequest.setOrderId("ORD-FAILED-001");
        pagoRequest.setMonto(new BigDecimal("0.25")); // Monto que falla
        pagoRequest.setMetodoPago("Tarjeta");
        pagoRequest.setMoneda("PEN");
        pagoRequest.setClienteId("CLI-001");

        MvcResult createResult = mockMvc.perform(post("/pagos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pagoRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String pagoId = objectMapper.readTree(responseBody)
                .path("data").path("id").asText();

        // Intentar cambiar estado de un pago fallido (estado final) a completado
        mockMvc.perform(patch("/pagos/{pagoId}/estado", pagoId)
                .param("estado", "COMPLETED"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void refundPagoInvalido_DeberiaRetornar400() throws Exception {
        // Crear un pago que quede pendiente
        PagoRequest pagoRequest = new PagoRequest();
        pagoRequest.setOrderId("ORD-PENDING-001");
        pagoRequest.setMonto(new BigDecimal("12000.00")); // Monto alto, queda pendiente
        pagoRequest.setMetodoPago("Transferencia");
        pagoRequest.setMoneda("PEN");
        pagoRequest.setClienteId("CLI-001");

        MvcResult createResult = mockMvc.perform(post("/pagos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pagoRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String pagoId = objectMapper.readTree(responseBody)
                .path("data").path("id").asText();

        // Intentar hacer refund de un pago no completado
        mockMvc.perform(post("/pagos/{pagoId}/refund", pagoId)
                .param("monto", "1000.00"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", containsString("Solo se pueden hacer refunds a pagos completados")));
    }

    @Test
    void consultarTodosSinPagos_DeberiaRetornarListaVacia() throws Exception {
        mockMvc.perform(get("/pagos"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.message").value("Se encontraron 0 pagos"));
    }
}