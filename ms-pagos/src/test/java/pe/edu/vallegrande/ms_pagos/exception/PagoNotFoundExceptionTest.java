package pe.edu.vallegrande.ms_pagos.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class PagoNotFoundExceptionTest {

    @Test
    void constructor_ConPagoId_DeberiaCrearExcepcionCorrectamente() {
        // Given
        String pagoId = "pago-test-123";

        // When
        PagoNotFoundException excepcion = new PagoNotFoundException(pagoId);

        // Then
        assertThat(excepcion.getMessage()).isEqualTo("Pago no encontrado con ID: " + pagoId);
        assertThat(excepcion.getPagoId()).isEqualTo(pagoId);
        assertThat(excepcion).isInstanceOf(RuntimeException.class);
    }

    @Test
    void constructor_ConPagoIdNulo_DeberiaCrearExcepcionConNull() {
        // Given
        String pagoId = null;

        // When
        PagoNotFoundException excepcion = new PagoNotFoundException(pagoId);

        // Then
        assertThat(excepcion.getMessage()).isEqualTo("Pago no encontrado con ID: null");
        assertThat(excepcion.getPagoId()).isNull();
    }

    @Test
    void constructor_ConPagoIdVacio_DeberiaCrearExcepcionConStringVacio() {
        // Given
        String pagoId = "";

        // When
        PagoNotFoundException excepcion = new PagoNotFoundException(pagoId);

        // Then
        assertThat(excepcion.getMessage()).isEqualTo("Pago no encontrado con ID: ");
        assertThat(excepcion.getPagoId()).isEmpty();
    }

    @Test
    void excepcion_DeberiaSerLanzableCatcheable() {
        // Given
        String pagoId = "pago-inexistente";

        // When & Then
        assertThatThrownBy(() -> {
            throw new PagoNotFoundException(pagoId);
        })
        .isInstanceOf(PagoNotFoundException.class)
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Pago no encontrado con ID: " + pagoId)
        .hasFieldOrPropertyWithValue("pagoId", pagoId);
    }
}