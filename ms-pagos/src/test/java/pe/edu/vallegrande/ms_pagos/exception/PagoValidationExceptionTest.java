package pe.edu.vallegrande.ms_pagos.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class PagoValidationExceptionTest {

    @Test
    void constructorCompleto_ConTodosLosParametros_DeberiaCrearExcepcionCorrectamente() {
        // Given
        String field = "monto";
        String value = "0";
        String message = "El monto debe ser mayor a 0";

        // When
        PagoValidationException excepcion = new PagoValidationException(field, value, message);

        // Then
        assertThat(excepcion.getMessage()).isEqualTo(message);
        assertThat(excepcion.getField()).isEqualTo(field);
        assertThat(excepcion.getValue()).isEqualTo(value);
        assertThat(excepcion).isInstanceOf(RuntimeException.class);
    }

    @Test
    void constructorSoloMensaje_DeberiaCrearExcepcionConCamposNulos() {
        // Given
        String message = "Error de validación general";

        // When
        PagoValidationException excepcion = new PagoValidationException(message);

        // Then
        assertThat(excepcion.getMessage()).isEqualTo(message);
        assertThat(excepcion.getField()).isNull();
        assertThat(excepcion.getValue()).isNull();
    }

    @Test
    void constructorCompleto_ConParametrosNulos_DeberiaPermitirNulos() {
        // Given
        String field = null;
        String value = null;
        String message = "Mensaje de error";

        // When
        PagoValidationException excepcion = new PagoValidationException(field, value, message);

        // Then
        assertThat(excepcion.getMessage()).isEqualTo(message);
        assertThat(excepcion.getField()).isNull();
        assertThat(excepcion.getValue()).isNull();
    }

    @Test
    void constructorCompleto_ConCamposVacios_DeberiaPermitirStringsVacios() {
        // Given
        String field = "";
        String value = "";
        String message = "Campo requerido";

        // When
        PagoValidationException excepcion = new PagoValidationException(field, value, message);

        // Then
        assertThat(excepcion.getMessage()).isEqualTo(message);
        assertThat(excepcion.getField()).isEmpty();
        assertThat(excepcion.getValue()).isEmpty();
    }

    @Test
    void excepcion_DeberiaSerLanzableCatcheable() {
        // Given
        String field = "clienteId";
        String value = "null";
        String message = "El ID del cliente es obligatorio";

        // When & Then
        assertThatThrownBy(() -> {
            throw new PagoValidationException(field, value, message);
        })
        .isInstanceOf(PagoValidationException.class)
        .isInstanceOf(RuntimeException.class)
        .hasMessage(message)
        .satisfies(ex -> {
            PagoValidationException pve = (PagoValidationException) ex;
            assertThat(pve.getField()).isEqualTo(field);
            assertThat(pve.getValue()).isEqualTo(value);
        });
    }

    @Test
    void excepcionSoloMensaje_DeberiaSerLanzableCatcheable() {
        // Given
        String message = "Transición de estado inválida";

        // When & Then
        assertThatThrownBy(() -> {
            throw new PagoValidationException(message);
        })
        .isInstanceOf(PagoValidationException.class)
        .isInstanceOf(RuntimeException.class)
        .hasMessage(message)
        .satisfies(ex -> {
            PagoValidationException pve = (PagoValidationException) ex;
            assertThat(pve.getField()).isNull();
            assertThat(pve.getValue()).isNull();
        });
    }

    @Test
    void equals_DeberiaFuncionarCorrectamente() {
        // Given
        PagoValidationException excepcion1 = new PagoValidationException("field", "value", "message");
        PagoValidationException excepcion2 = new PagoValidationException("field", "value", "message");
        PagoValidationException excepcion3 = new PagoValidationException("other", "other", "other");

        // When & Then
        assertThat(excepcion1.getField()).isEqualTo(excepcion2.getField());
        assertThat(excepcion1.getValue()).isEqualTo(excepcion2.getValue());
        assertThat(excepcion1.getMessage()).isEqualTo(excepcion2.getMessage());
        assertThat(excepcion1.getField()).isNotEqualTo(excepcion3.getField());
    }
}