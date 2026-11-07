package com.utn.cuidadores_api.controller;

import com.utn.cuidadores_api.dto.ProfileRequest;
import com.utn.cuidadores_api.dto.ProfileResponse;
import com.utn.cuidadores_api.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST de perfiles de cuidadores (/api/profiles).
 * - GET: acceso público sin autenticación.
 * - POST y PUT: requieren token JWT válido en la cabecera Authorization.
 */
@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ResponseEntity<List<ProfileResponse>> listarPerfiles(
            @RequestParam(name = "zona", required = false) String zona) {
        return ResponseEntity.ok(profileService.buscarPerfiles(zona));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProfileResponse> obtenerPerfil(@PathVariable Long id) {
        return ResponseEntity.ok(profileService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<ProfileResponse> crearPerfil(
            @Valid @RequestBody ProfileRequest request,
            Authentication authentication) {
        String emailAutenticado = authentication.getName();
        ProfileResponse creado = profileService.crearPerfil(request, emailAutenticado);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProfileResponse> actualizarPerfil(
            @PathVariable Long id,
            @Valid @RequestBody ProfileRequest request,
            Authentication authentication) {
        String emailAutenticado = authentication.getName();
        ProfileResponse actualizado = profileService.actualizarPerfil(id, request, emailAutenticado);
        return ResponseEntity.ok(actualizado);
    }
}

