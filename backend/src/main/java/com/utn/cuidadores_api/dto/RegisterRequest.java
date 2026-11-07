package com.utn.cuidadores_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO de entrada para el registro de nuevos usuarios (POST /api/auth/register).
 */
public record RegisterRequest(

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El formato del email no es válido")
        String email,

        // =========================================================================
        // VULNERABILIDAD INTENCIONAL 5.3 (b): Política débil de contraseñas (CWE-521)
        // Solo se valida que no esté vacía (@NotBlank), sin exigir longitud mínima,
        // mayúsculas, números ni caracteres especiales. Permite registrar "1234".
        // =========================================================================
        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {
}

