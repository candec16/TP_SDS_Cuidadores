package com.utn.cuidadores_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para el registro de nuevos usuarios (POST /api/auth/register).
 */
public record RegisterRequest(

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El formato del email no es válido")
        String email,

        // =========================================================================
        // MITIGACIÓN 5.3 (b): Política mínima de contraseñas (CWE-521)
        // Se exige un mínimo de 8 caracteres (máximo 72 por límite interno de BCrypt)
        // y combinación obligatoria de al menos una mayúscula, una minúscula y un número.
        // =========================================================================
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "La contraseña debe incluir al menos una mayúscula, una minúscula y un número"
        )
        String password
) {
}
