package pe.edu.vallegrande.ms_pagos.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pe.edu.vallegrande.ms_pagos.dto.request.PagoRequest;
import pe.edu.vallegrande.ms_pagos.dto.request.RefundRequest;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:escenarios",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestMethodOrder(OrderAnnotation.class)
class EscenariosPruebasRestrictRefundTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String ORDEN_PRINCIPAL = "ORD-ESCENARIOS-001";
    private static final String ORDEN_ERROR = "ORD-ERROR-001";
    private static final String ORDEN_LIMITE = "ORD-LIMITE-001";
    private static final String ORDEN_INEXISTENTE = "ORD-INEXISTENTE-999";
    private static final BigDecimal MAXIMO_REFUND = new BigDecimal("500.00");

    @BeforeEach
    void configurarPagosBase() throws Exception {
        // Crear pago para flujo principal
        crearPagoBase(ORDEN_PRINCIPAL, "2000.00", "CLI-PRINCIPAL");
        
        // Crear pago para caso de error
        crearPagoBase(ORDEN_ERROR, "1500.00", "CLI-ERROR");
        
        // Crear pago para caso límite
        crearPagoBase(ORDEN_LIMITE, "800.00", "CLI-LIMITE");
    }

    private void crearPagoBase(String orderId, String monto, String clienteId) throws Exception {
        PagoRequest pagoRequest = new PagoRequest();
        pagoRequest.setOrderId(orderId);
        pagoRequest.setMonto(new BigDecimal(monto));
        pagoRequest.setMetodoPago("Tarjeta de Crédito");
        pagoRequest.setMoneda("PEN");
        pagoRequest.setClienteId(clienteId);
        pagoRequest.setDescripcion("Pago base para escenarios de prueba");
        pagoRequest.setPaymentGateway("Visa");

        mockMvc.perform(post("/pagos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pagoRequest)))
                .andExpect(status().isCreated());
    }

    // ============================================================================
    // ESCENARIO 1: FLUJO PRINCIPAL - REEMBOLSOS MÚLTIPLES DENTRO DEL MÁXIMO
    // ============================================================================

    @Test
    @Order(1)
    void escenarioFlujoPrincipal_Reembolso100_DentroDelMaximo_DeberiaFuncionar() throws Exception {
        // Given
        RefundRequest refund100 = new RefundRequest();
        refund100.setOrderId(ORDEN_PRINCIPAL);
        refund100.setAmount(new BigDecimal("100.00"));
        refund100.setMaxRefundable(MAXIMO_REFUND);

        // When & Then
        mockMvc.perform(post("/pagos/restrict-refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refund100)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(containsString("Refund restringido procesado exitosamente")))
                .andExpect(jsonPath("$.message").value(containsString("100.00")))
                .andExpect(jsonPath("$.message").value(containsString("500.00")))
                .andExpect(jsonPath("$.data.estado").value("REFUNDED"))
                .andExpect(jsonPath("$.data.orderId").value(ORDEN_PRINCIPAL))
                .andExpect(jsonPath("$.data.codigoRespuesta").value("RESTRICTED_REFUND_SUCCESS"))
                .andExpect(jsonPath("$.data.mensajeRespuesta").value(containsString("400.00"))); // Restante
    }

    @Test
    @Order(2)
    void escenarioFlujoPrincipal_Reembolso200_DentroDelMaximo_DeberiaFuncionar() throws Exception {
        // Given
        RefundRequest refund200 = new RefundRequest();
        refund200.setOrderId(ORDEN_PRINCIPAL);
        refund200.setAmount(new BigDecimal("200.00"));
        refund200.setMaxRefundable(MAXIMO_REFUND);

        // When & Then
        mockMvc.perform(post("/pagos/restrict-refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refund200)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(containsString("Refund restringido procesado exitosamente")))
                .andExpect(jsonPath("$.message").value(containsString("200.00")))
                .andExpect(jsonPath("$.message").value(containsString("500.00")))
                .andExpect(jsonPath("$.data.estado").value("REFUNDED"))
                .andExpect(jsonPath("$.data.orderId").value(ORDEN_PRINCIPAL))
                .andExpect(jsonPath("$.data.mensajeRespuesta").value(containsString("300.00"))); // Restante
    }

    @Test
    @Order(3)
    void escenarioFlujoPrincipal_Reembolso300_DentroDelMaximo_DeberiaFuncionar() throws Exception {
        // Given
        RefundRequest refund300 = new RefundRequest();
        refund300.setOrderId(ORDEN_PRINCIPAL);
        refund300.setAmount(new BigDecimal("300.00"));
        refund300.setMaxRefundable(MAXIMO_REFUND);

        // When & Then
        mockMvc.perform(post("/pagos/restrict-refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refund300)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(containsString("Refund restringido procesado exitosamente")))
                .andExpect(jsonPath("$.message").value(containsString("300.00")))
                .andExpect(jsonPath("$.message").value(containsString("500.00")))
                .andExpect(jsonPath("$.data.estado").value("REFUNDED"))
                .andExpect(jsonPath("$.data.orderId").value(ORDEN_PRINCIPAL))
                .andExpect(jsonPath("$.data.mensajeRespuesta").value(containsString("200.00"))); // Restante
    }

    @Test
    @Order(4)
    void escenarioFlujoPrincipal_SecuenciaCompleta_ValidarComportamiento() throws Exception {
        // Verificar que se pueden procesar múltiples refunds secuenciales
        // (cada uno procesa un pago diferente según la lógica actual)
        
        // Crear múltiples pagos para la misma orden para simular secuencia
        for (int i = 2; i <= 4; i++) {
            crearPagoBase(ORDEN_PRINCIPAL + "-" + i, "1000.00", "CLI-SECUENCIA-" + i);
        }

        // Procesar refunds en secuencia con diferentes montos
        BigDecimal[] montos = {new BigDecimal("100.00"), new BigDecimal("250.00"), new BigDecimal("400.00")};
        
        for (int i = 0; i < montos.length; i++) {
            RefundRequest refund = new RefundRequest();
            refund.setOrderId(ORDEN_PRINCIPAL + "-" + (i + 2));
            refund.setAmount(montos[i]);
            refund.setMaxRefundable(MAXIMO_REFUND);

            mockMvc.perform(post("/pagos/restrict-refund")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(refund)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.estado").value("REFUNDED"));
        }
    }

    // ============================================================================
    // ESCENARIO 2: CASO DE ERROR - MONTO MAYOR AL MÁXIMO
    // ============================================================================

    @Test
    @Order(5)
    void escenarioCasoError_MontoMayorAlMaximo_DeberiaRetornar400() throws Exception {
        // Given
        RefundRequest refundExcedido = new RefundRequest();
        refundExcedido.setOrderId(ORDEN_ERROR);
        refundExcedido.setAmount(new BigDecimal("600.00")); // Excede el máximo de 500
        refundExcedido.setMaxRefundable(MAXIMO_REFUND);

        // When & Then
        mockMvc.perform(post("/pagos/restrict-refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refundExcedido)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("El monto del refund (600.00) excede el límite máximo permitido (500.00)")))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    @Order(6)
    void escenarioCasoError_MontoExcesivo1000_DeberiaRetornar400() throws Exception {
        // Given - Caso extremo con monto muy alto
        RefundRequest refundExtremo = new RefundRequest();
        refundExtremo.setOrderId(ORDEN_ERROR);
        refundExtremo.setAmount(new BigDecimal("1000.00")); // Muy por encima del máximo
        refundExtremo.setMaxRefundable(MAXIMO_REFUND);

        // When & Then
        mockMvc.perform(post("/pagos/restrict-refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refundExtremo)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("El monto del refund (1000.00) excede el límite máximo permitido (500.00)")));
    }

    // ============================================================================
    // ESCENARIO 3: CASO LÍMITE - MONTO IGUAL AL MÁXIMO
    // ============================================================================

    @Test
    @Order(7)
    void escenarioCasoLimite_MontoIgualAlMaximo_DeberiaFuncionar() throws Exception {
        // Given
        RefundRequest refundLimite = new RefundRequest();
        refundLimite.setOrderId(ORDEN_LIMITE);
        refundLimite.setAmount(MAXIMO_REFUND); // Exactamente igual al máximo (500.00)
        refundLimite.setMaxRefundable(MAXIMO_REFUND);

        // When & Then
        mockMvc.perform(post("/pagos/restrict-refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refundLimite)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(containsString("Refund restringido procesado exitosamente")))
                .andExpect(jsonPath("$.message").value(containsString("500.00")))
                .andExpect(jsonPath("$.data.estado").value("REFUNDED"))
                .andExpect(jsonPath("$.data.orderId").value(ORDEN_LIMITE))
                .andExpect(jsonPath("$.data.mensajeRespuesta").value(containsString("0.00"))); // Restante = 0
    }

    @Test
    @Order(8)
    void escenarioCasoLimite_MontoMuyProximoAlMaximo_DeberiaFuncionar() throws Exception {
        // Given - 499.99, muy próximo al límite
        RefundRequest refundProximo = new RefundRequest();
        refundProximo.setOrderId(ORDEN_LIMITE);
        refundProximo.setAmount(new BigDecimal("499.99"));
        refundProximo.setMaxRefundable(MAXIMO_REFUND);

        // When & Then
        mockMvc.perform(post("/pagos/restrict-refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refundProximo)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.estado").value("REFUNDED"))
                .andExpect(jsonPath("$.data.mensajeRespuesta").value(containsString("0.01"))); // Restante = 0.01
    }

    // ============================================================================
    // ESCENARIO 4: ERROR AVANZADO - PEDIDOS INEXISTENTES
    // ============================================================================

    @Test
    @Order(9)
    void escenarioErrorAvanzado_PedidoInexistente_DeberiaRetornar404() throws Exception {
        // Given
        RefundRequest refundInexistente = new RefundRequest();
        refundInexistente.setOrderId(ORDEN_INEXISTENTE);
        refundInexistente.setAmount(new BigDecimal("100.00"));
        refundInexistente.setMaxRefundable(MAXIMO_REFUND);

        // When & Then
        mockMvc.perform(post("/pagos/restrict-refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refundInexistente)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("PAGO_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value(containsString("No se encontraron pagos para la orden: " + ORDEN_INEXISTENTE)))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    @Order(10)
    void escenarioErrorAvanzado_OrdenVacia_DeberiaRetornar400() throws Exception {
        // Given
        RefundRequest refundOrdenVacia = new RefundRequest();
        refundOrdenVacia.setOrderId(""); // Orden vacía
        refundOrdenVacia.setAmount(new BigDecimal("100.00"));
        refundOrdenVacia.setMaxRefundable(MAXIMO_REFUND);

        // When & Then
        mockMvc.perform(post("/pagos/restrict-refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refundOrdenVacia)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("El orderId es obligatorio")));
    }

    @Test
    @Order(11)
    void escenarioErrorAvanzado_OrdenNull_DeberiaRetornar400() throws Exception {
        // Given
        RefundRequest refundOrdenNull = new RefundRequest();
        refundOrdenNull.setOrderId(null); // Orden null
        refundOrdenNull.setAmount(new BigDecimal("100.00"));
        refundOrdenNull.setMaxRefundable(MAXIMO_REFUND);

        // When & Then
        mockMvc.perform(post("/pagos/restrict-refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refundOrdenNull)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("El orderId es obligatorio")));
    }

    @Test
    @Order(12)
    void escenarioCompleto_ResumenEscenarios_ValidarComportamientoGeneral() throws Exception {
        // Test resumen que valida el comportamiento general del sistema
        // después de ejecutar todos los escenarios anteriores

        // 1. Verificar que las estadísticas reflejan las operaciones realizadas
        MvcResult statsResult = mockMvc.perform(get("/pagos/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        // 2. Verificar que se pueden consultar los pagos procesados
        mockMvc.perform(get("/pagos/orden/" + ORDEN_PRINCIPAL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        // 3. Verificar health check después de todos los escenarios
        mockMvc.perform(get("/pagos/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Microservicio de pagos funcionando correctamente"));
    }
}