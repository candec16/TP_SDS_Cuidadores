package com.utn.cuidadores_api.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejador global de excepciones para devolver respuestas JSON claras y consistentes
 * tanto al frontend en React como a las herramientas de prueba (Postman / scripts).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatusException(ResponseStatusException ex) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", ex.getReason() != null ? ex.getReason() : "Error en la solicitud");
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String firstError = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getDefaultMessage())
                .orElse("Datos de entrada inválidos");

        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", firstError);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}

