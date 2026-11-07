package com.utn.cuidadores_api.service;

import com.utn.cuidadores_api.dto.ProfileRequest;
import com.utn.cuidadores_api.dto.ProfileResponse;
import com.utn.cuidadores_api.model.Profile;
import com.utn.cuidadores_api.model.User;
import com.utn.cuidadores_api.repository.ProfileRepository;
import com.utn.cuidadores_api.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

    @PersistenceContext
    private EntityManager entityManager;

    public ProfileService(ProfileRepository profileRepository, UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    /**
     * Consulta pública de perfiles con filtro opcional por zona.
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<ProfileResponse> buscarPerfiles(String zona) {
        if (zona == null || zona.isBlank()) {
            return profileRepository.findAll().stream()
                    .map(ProfileResponse::fromEntity)
                    .toList();
        }

        // =========================================================================
        // VULNERABILIDAD INTENCIONAL 5.1: Inyección SQL (CWE-89 / OWASP A03:2021)
        // El parámetro 'zona' enviado por el usuario se concatena directamente dentro
        // de una sentencia SQL nativa en vez de usar parámetros enlazados o Spring Data.
        // Demostración: buscar  zzz' OR '1'='1  altera la condición lógica WHERE y
        // devuelve todos los perfiles registrados aunque la zona no exista.
        // =========================================================================
        String sql = "SELECT * FROM profiles WHERE zona ILIKE '%" + zona + "%' OR zona = '" + zona + "'";

        List<Profile> resultados = entityManager
                .createNativeQuery(sql, Profile.class)
                .getResultList();

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
        // VULNERABILIDAD INTENCIONAL 5.2: IDOR - Referencia Directa Insegura a Objetos
        // (CWE-639: Authorization Bypass Through User-Controlled Key / OWASP A01:2021)
        // Spring Security exige que exista un token JWT válido (authenticatedEmail),
        // pero este método NO verifica que profile.getUser().getEmail() coincida con
        // authenticatedEmail. Cualquier usuario autenticado B puede enviar un PUT a
        // /api/profiles/{id_de_A} y sobrescribir el perfil ajeno de A.
        // =========================================================================

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

