package com.utn.cuidadores_api.service;

import com.utn.cuidadores_api.dto.AuthResponse;
import com.utn.cuidadores_api.dto.LoginRequest;
import com.utn.cuidadores_api.dto.RegisterRequest;
import com.utn.cuidadores_api.model.User;
import com.utn.cuidadores_api.repository.UserRepository;
import com.utn.cuidadores_api.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Servicio de registro e inicio de sesión de usuarios.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe una cuenta registrada con ese email"
            );
        }

        // Control presente: la contraseña nunca se guarda en texto plano, se aplica hash BCrypt
        String hashedPassword = passwordEncoder.encode(request.password());
        User savedUser = userRepository.save(new User(normalizedEmail, hashedPassword));

        String token = jwtService.generateToken(savedUser.getEmail());
        return new AuthResponse(token, savedUser.getId(), savedUser.getEmail());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        // =========================================================================
        // VULNERABILIDAD INTENCIONAL 5.3 (b): Sin límite de intentos fallidos
        // (CWE-307: Improper Restriction of Excessive Authentication Attempts)
        // No se contabilizan ni bloquean los intentos fallidos por cuenta o IP,
        // permitiendo ataques automatizados de fuerza bruta o diccionario.
        // =========================================================================

        // =========================================================================
        // VULNERABILIDAD INTENCIONAL 5.3 (c): Enumeración de usuarios
        // (CWE-204 / OWASP A07:2021 Identification and Authentication Failures)
        // La API devuelve mensajes distintos cuando el email no existe vs. cuando
        // la contraseña es incorrecta, permitiendo descubrir qué correos están registrados.
        // =========================================================================
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "No existe ningún usuario registrado con el email: " + normalizedEmail
                ));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Contraseña incorrecta para el usuario ingresado"
            );
        }

        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token, user.getId(), user.getEmail());
    }
}

