package com.utn.cuidadores_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para crear o actualizar un perfil de cuidador/a.
 */
public record ProfileRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "El apellido no puede superar los 100 caracteres")
        String apellido,

        @NotBlank(message = "La zona de residencia es obligatoria")
        @Size(max = 120, message = "La zona no puede superar los 120 caracteres")
        String zona,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
        String descripcion,

        @Size(max = 500, message = "Los intereses no pueden superar los 500 caracteres")
        String intereses,

        @Size(max = 500, message = "Los estudios no pueden superar los 500 caracteres")
        String estudios
) {
}

