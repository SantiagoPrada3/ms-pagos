package pe.edu.vallegrande.ms_pagos.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.edu.vallegrande.ms_pagos.dto.response.ApiResponse;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(PagoNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handlePagoNotFound(PagoNotFoundException ex) {
        log.error("Pago no encontrado: {}", ex.getMessage());
        
        ApiResponse<Object> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage(ex.getMessage());
        response.setErrorCode("PAGO_NOT_FOUND");
        response.setTimestamp(LocalDateTime.now());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    @ExceptionHandler(PagoValidationException.class)
    public ResponseEntity<ApiResponse<Object>> handlePagoValidation(PagoValidationException ex) {
        log.error("Error de validación en pago: {}", ex.getMessage());
        
        ApiResponse<Object> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage(ex.getMessage());
        response.setErrorCode("VALIDATION_ERROR");
        response.setTimestamp(LocalDateTime.now());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex) {
        log.error("Error interno del servidor: {}", ex.getMessage(), ex);
        
        ApiResponse<Object> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage("Error interno del servidor");
        response.setErrorCode("INTERNAL_SERVER_ERROR");
        response.setTimestamp(LocalDateTime.now());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}