package com.utn.cuidadores_api.service;

import com.utn.cuidadores_api.dto.ProfileRequest;
import com.utn.cuidadores_api.dto.ProfileResponse;
import com.utn.cuidadores_api.model.Profile;
import com.utn.cuidadores_api.model.User;
import com.utn.cuidadores_api.repository.ProfileRepository;
import com.utn.cuidadores_api.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Servicio de negocio para la consulta pública, creación y edición de perfiles de cuidadores.
 */
@Service
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    public ProfileService(ProfileRepository profileRepository, UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    /**
     * Consulta pública de perfiles con filtro opcional por zona.
     */
    @Transactional(readOnly = true)
    public List<ProfileResponse> buscarPerfiles(String zona) {
        if (zona == null || zona.isBlank()) {
            return profileRepository.findAll().stream()
                    .map(ProfileResponse::fromEntity)
                    .toList();
        }

        String zonaLimpia = zona.trim();
        if (zonaLimpia.length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El texto de búsqueda por zona no puede superar los 100 caracteres"
            );
        }

        // =========================================================================
        // MITIGACIÓN 5.1: Prevención de Inyección SQL (CWE-89 / OWASP A03:2021)
        // Se reemplaza la consulta SQL nativa concatenada por un método derivado de
        // Spring Data JPA, que utiliza consultas parametrizadas (PreparedStatement)
        // separando estrictamente la lógica SQL de los datos del usuario.
        // =========================================================================
        List<Profile> resultados = profileRepository.findByZonaContainingIgnoreCase(zonaLimpia);

        return resultados.stream()
                .map(ProfileResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProfileResponse obtenerPorId(Long id) {
        Profile profile = profileRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró el perfil con ID " + id
                ));
        return ProfileResponse.fromEntity(profile);
    }

    /**
     * Alta del perfil propio: asocia el nuevo perfil al usuario identificado en el JWT.
     */
    @Transactional
    public ProfileResponse crearPerfil(ProfileRequest request, String authenticatedEmail) {
        User owner = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Usuario autenticado no encontrado"
                ));

        if (profileRepository.existsByUserId(owner.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ya tenés un perfil creado. Podés editar tu perfil actual."
            );
        }

        Profile profile = new Profile(
                owner,
                request.nombre().trim(),
                request.apellido().trim(),
                request.zona().trim(),
                request.descripcion().trim(),
                request.intereses() != null ? request.intereses().trim() : "",
                request.estudios() != null ? request.estudios().trim() : ""
        );

        Profile saved = profileRepository.save(profile);
        return ProfileResponse.fromEntity(saved);
    }

    /**
     * Edición de un perfil existente por su ID.
     */
    @Transactional
    public ProfileResponse actualizarPerfil(Long id, ProfileRequest request, String authenticatedEmail) {
        Profile profile = profileRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró el perfil con ID " + id
                ));

        // =========================================================================
        // MITIGACIÓN 5.2: Control de Autorización a Nivel de Objeto (CWE-639)
        // Se verifica estrictamente en el servidor que el propietario real del perfil
        // coincida con el usuario autenticado mediante el token JWT. Si no coinciden,
        // se rechaza la operación con 403 Forbidden.
        // =========================================================================
        if (!profile.getUser().getEmail().equalsIgnoreCase(authenticatedEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Acceso denegado: no tenés permiso para modificar un perfil que pertenece a otro usuario"
            );
        }

        profile.setNombre(request.nombre().trim());
        profile.setApellido(request.apellido().trim());
        profile.setZona(request.zona().trim());
        profile.setDescripcion(request.descripcion().trim());
        profile.setIntereses(request.intereses() != null ? request.intereses().trim() : "");
        profile.setEstudios(request.estudios() != null ? request.estudios().trim() : "");

        Profile updated = profileRepository.save(profile);
        return ProfileResponse.fromEntity(updated);
    }
}

