package com.utn.cuidadores_api.dto;

/**
 * DTO de respuesta al registrarse o iniciar sesión exitosamente.
 * Devuelve el token JWT firmado junto con los datos básicos de la cuenta.
 */
public record AuthResponse(
        String token,
        Long userId,
        String email
) {
}

