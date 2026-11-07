package com.utn.cuidadores_api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de entrada para el inicio de sesión (POST /api/auth/login).
 */
public record LoginRequest(

        @NotBlank(message = "El email es obligatorio")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {
}

