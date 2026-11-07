package com.utn.cuidadores_api.dto;

import com.utn.cuidadores_api.model.Profile;

/**
 * DTO de salida para exponer perfiles públicamente sin filtrar el hash de contraseña
 * de la entidad User asociada.
 */
public record ProfileResponse(
        Long id,
        Long userId,
        String userEmail,
        String nombre,
        String apellido,
        String zona,
        String descripcion,
        String intereses,
        String estudios
) {
    public static ProfileResponse fromEntity(Profile profile) {
        return new ProfileResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getUser().getEmail(),
                profile.getNombre(),
                profile.getApellido(),
                profile.getZona(),
                profile.getDescripcion(),
                profile.getIntereses(),
                profile.getEstudios()
        );
    }
}

