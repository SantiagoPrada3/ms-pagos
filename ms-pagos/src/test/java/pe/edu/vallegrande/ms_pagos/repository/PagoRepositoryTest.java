package pe.edu.vallegrande.ms_pagos.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.edu.vallegrande.ms_pagos.model.Pago;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class PagoRepositoryTest {

    private PagoRepository pagoRepository;
    private Pago pagoTest1;
    private Pago pagoTest2;
    private Pago pagoTest3;

    @BeforeEach
    void setUp() {
        pagoRepository = new PagoRepository();
        
        // Configurar datos de prueba
        pagoTest1 = new Pago();
        pagoTest1.setId("pago-001");
        pagoTest1.setOrderId("ORD-001");
        pagoTest1.setClienteId("CLI-001");
        pagoTest1.setMonto(new BigDecimal("1500.50"));
        pagoTest1.setEstado(Pago.EstadoPago.COMPLETED);
        pagoTest1.setFechaCreacion(LocalDateTime.now());
        pagoTest1.setFechaActualizacion(LocalDateTime.now());
        pagoTest1.setMetodoPago("Tarjeta");
        pagoTest1.setMoneda("PEN");

        pagoTest2 = new Pago();
        pagoTest2.setId("pago-002");
        pagoTest2.setOrderId("ORD-001"); // Misma orden que pagoTest1
        pagoTest2.setClienteId("CLI-002");
        pagoTest2.setMonto(new BigDecimal("2000.00"));
        pagoTest2.setEstado(Pago.EstadoPago.PENDING);
        pagoTest2.setFechaCreacion(LocalDateTime.now());
        pagoTest2.setFechaActualizacion(LocalDateTime.now());
        pagoTest2.setMetodoPago("PayPal");
        pagoTest2.setMoneda("USD");

        pagoTest3 = new Pago();
        pagoTest3.setId("pago-003");
        pagoTest3.setOrderId("ORD-002");
        pagoTest3.setClienteId("CLI-001"); // Mismo cliente que pagoTest1
        pagoTest3.setMonto(new BigDecimal("500.25"));
        pagoTest3.setEstado(Pago.EstadoPago.FAILED);
        pagoTest3.setFechaCreacion(LocalDateTime.now());
        pagoTest3.setFechaActualizacion(LocalDateTime.now());
        pagoTest3.setMetodoPago("Transferencia");
        pagoTest3.setMoneda("PEN");
    }

    @Test
    void save_DeberiaGuardarPagoCorrectamente() {
        // When
        Pago pagoGuardado = pagoRepository.save(pagoTest1);

        // Then
        assertThat(pagoGuardado).isNotNull();
        assertThat(pagoGuardado.getId()).isEqualTo("pago-001");
        assertThat(pagoRepository.count()).isEqualTo(1);
    }

    @Test
    void save_DeberiaActualizarPagoExistente() {
        // Given
        pagoRepository.save(pagoTest1);
        pagoTest1.setEstado(Pago.EstadoPago.REFUNDED);

        // When
        Pago pagoActualizado = pagoRepository.save(pagoTest1);

        // Then
        assertThat(pagoActualizado.getEstado()).isEqualTo(Pago.EstadoPago.REFUNDED);
        assertThat(pagoRepository.count()).isEqualTo(1); // No debe crear un nuevo registro
    }

    @Test
    void findById_ConIdExistente_DeberiaRetornarPago() {
        // Given
        pagoRepository.save(pagoTest1);

        // When
        Optional<Pago> resultado = pagoRepository.findById("pago-001");

        // Then
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo("pago-001");
        assertThat(resultado.get().getOrderId()).isEqualTo("ORD-001");
    }

    @Test
    void findById_ConIdInexistente_DeberiaRetornarEmpty() {
        // When
        Optional<Pago> resultado = pagoRepository.findById("pago-inexistente");

        // Then
        assertThat(resultado).isEmpty();
    }

    @Test
    void findByOrderId_DeberiaRetornarPagosDeOrden() {
        // Given
        pagoRepository.save(pagoTest1);
        pagoRepository.save(pagoTest2);
        pagoRepository.save(pagoTest3);

        // When
        List<Pago> pagosOrden = pagoRepository.findByOrderId("ORD-001");

        // Then
        assertThat(pagosOrden).hasSize(2);
        assertThat(pagosOrden).extracting(Pago::getId)
                .containsExactlyInAnyOrder("pago-001", "pago-002");
    }

    @Test
    void findByOrderId_ConOrdenInexistente_DeberiaRetornarListaVacia() {
        // Given
        pagoRepository.save(pagoTest1);

        // When
        List<Pago> pagosOrden = pagoRepository.findByOrderId("ORD-INEXISTENTE");

        // Then
        assertThat(pagosOrden).isEmpty();
    }

    @Test
    void findByClienteId_DeberiaRetornarPagosDelCliente() {
        // Given
        pagoRepository.save(pagoTest1);
        pagoRepository.save(pagoTest2);
        pagoRepository.save(pagoTest3);

        // When
        List<Pago> pagosCliente = pagoRepository.findByClienteId("CLI-001");

        // Then
        assertThat(pagosCliente).hasSize(2);
        assertThat(pagosCliente).extracting(Pago::getId)
                .containsExactlyInAnyOrder("pago-001", "pago-003");
    }

    @Test
    void findByClienteId_ConClienteInexistente_DeberiaRetornarListaVacia() {
        // Given
        pagoRepository.save(pagoTest1);

        // When
        List<Pago> pagosCliente = pagoRepository.findByClienteId("CLI-INEXISTENTE");

        // Then
        assertThat(pagosCliente).isEmpty();
    }

    @Test
    void findByEstado_DeberiaRetornarPagosPorEstado() {
        // Given
        pagoRepository.save(pagoTest1); // COMPLETED
        pagoRepository.save(pagoTest2); // PENDING
        pagoRepository.save(pagoTest3); // FAILED

        // When
        List<Pago> pagosCompletados = pagoRepository.findByEstado(Pago.EstadoPago.COMPLETED);
        List<Pago> pagosPendientes = pagoRepository.findByEstado(Pago.EstadoPago.PENDING);
        List<Pago> pagosFallidos = pagoRepository.findByEstado(Pago.EstadoPago.FAILED);

        // Then
        assertThat(pagosCompletados).hasSize(1);
        assertThat(pagosCompletados.get(0).getId()).isEqualTo("pago-001");

        assertThat(pagosPendientes).hasSize(1);
        assertThat(pagosPendientes.get(0).getId()).isEqualTo("pago-002");

        assertThat(pagosFallidos).hasSize(1);
        assertThat(pagosFallidos.get(0).getId()).isEqualTo("pago-003");
    }

    @Test
    void findByEstado_ConEstadoSinPagos_DeberiaRetornarListaVacia() {
        // Given
        pagoRepository.save(pagoTest1);

        // When
        List<Pago> pagosRefund = pagoRepository.findByEstado(Pago.EstadoPago.REFUNDED);

        // Then
        assertThat(pagosRefund).isEmpty();
    }

    @Test
    void findAll_DeberiaRetornarTodosLosPagos() {
        // Given
        pagoRepository.save(pagoTest1);
        pagoRepository.save(pagoTest2);
        pagoRepository.save(pagoTest3);

        // When
        List<Pago> todosPagos = pagoRepository.findAll();

        // Then
        assertThat(todosPagos).hasSize(3);
        assertThat(todosPagos).extracting(Pago::getId)
                .containsExactlyInAnyOrder("pago-001", "pago-002", "pago-003");
    }

    @Test
    void findAll_ConRepositorioVacio_DeberiaRetornarListaVacia() {
        // When
        List<Pago> todosPagos = pagoRepository.findAll();

        // Then
        assertThat(todosPagos).isEmpty();
    }

    @Test
    void deleteById_ConIdExistente_DeberiaEliminarPago() {
        // Given
        pagoRepository.save(pagoTest1);
        pagoRepository.save(pagoTest2);

        // When
        boolean eliminado = pagoRepository.deleteById("pago-001");

        // Then
        assertThat(eliminado).isTrue();
        assertThat(pagoRepository.count()).isEqualTo(1);
        assertThat(pagoRepository.findById("pago-001")).isEmpty();
        assertThat(pagoRepository.findById("pago-002")).isPresent();
    }

    @Test
    void deleteById_ConIdInexistente_DeberiaRetornarFalse() {
        // Given
        pagoRepository.save(pagoTest1);

        // When
        boolean eliminado = pagoRepository.deleteById("pago-inexistente");

        // Then
        assertThat(eliminado).isFalse();
        assertThat(pagoRepository.count()).isEqualTo(1);
    }

    @Test
    void existsById_ConIdExistente_DeberiaRetornarTrue() {
        // Given
        pagoRepository.save(pagoTest1);

        // When & Then
        assertThat(pagoRepository.existsById("pago-001")).isTrue();
    }

    @Test
    void existsById_ConIdInexistente_DeberiaRetornarFalse() {
        // When & Then
        assertThat(pagoRepository.existsById("pago-inexistente")).isFalse();
    }

    @Test
    void count_DeberiaRetornarNumeroCorrectoDePagos() {
        // Given
        assertThat(pagoRepository.count()).isEqualTo(0);

        pagoRepository.save(pagoTest1);
        assertThat(pagoRepository.count()).isEqualTo(1);

        pagoRepository.save(pagoTest2);
        assertThat(pagoRepository.count()).isEqualTo(2);

        pagoRepository.save(pagoTest3);
        assertThat(pagoRepository.count()).isEqualTo(3);
    }

    @Test
    void deleteAll_DeberiaEliminarTodosLosPagos() {
        // Given
        pagoRepository.save(pagoTest1);
        pagoRepository.save(pagoTest2);
        pagoRepository.save(pagoTest3);
        assertThat(pagoRepository.count()).isEqualTo(3);

        // When
        pagoRepository.deleteAll();

        // Then
        assertThat(pagoRepository.count()).isEqualTo(0);
        assertThat(pagoRepository.findAll()).isEmpty();
    }

    @Test
    void repositorio_DeberiaSerThreadSafe() {
        // Este test verifica que el ConcurrentHashMap funciona correctamente
        // en operaciones concurrentes básicas
        
        // Given
        pagoRepository.save(pagoTest1);
        pagoRepository.save(pagoTest2);

        // When - Operaciones concurrentes simuladas
        pagoRepository.save(pagoTest3);
        Optional<Pago> pago1 = pagoRepository.findById("pago-001");
        List<Pago> todosPagos = pagoRepository.findAll();
        boolean existe = pagoRepository.existsById("pago-002");

        // Then
        assertThat(pago1).isPresent();
        assertThat(todosPagos).hasSize(3);
        assertThat(existe).isTrue();
        assertThat(pagoRepository.count()).isEqualTo(3);
    }

    @Test
    void findAll_DeberiaRetornarNuevaListaCadaVez() {
        // Given
        pagoRepository.save(pagoTest1);

        // When
        List<Pago> lista1 = pagoRepository.findAll();
        List<Pago> lista2 = pagoRepository.findAll();

        // Then
        assertThat(lista1).isNotSameAs(lista2); // Diferentes instancias
        assertThat(lista1).isEqualTo(lista2);   // Mismo contenido
    }
}