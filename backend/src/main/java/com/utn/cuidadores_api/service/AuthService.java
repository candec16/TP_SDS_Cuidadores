package com.utn.cuidadores_api.service;

import com.utn.cuidadores_api.dto.AuthResponse;
import com.utn.cuidadores_api.dto.LoginRequest;
import com.utn.cuidadores_api.dto.RegisterRequest;
import com.utn.cuidadores_api.model.User;
import com.utn.cuidadores_api.repository.UserRepository;
import com.utn.cuidadores_api.security.JwtService;
import com.utn.cuidadores_api.security.LoginAttemptService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

/**
 * Servicio de registro e inicio de sesión de usuarios con controles contra
 * fuerza bruta (CWE-307) y enumeración de cuentas (CWE-204).
 */
@Service
public class AuthService {

    private static final String GENERIC_AUTH_ERROR = "Credenciales inválidas";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;
    private final String dummyBcryptHash;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginAttemptService = loginAttemptService;
        // Hash ficticio calculado al inicio para equilibrar tiempos de respuesta cuando el email no existe
        this.dummyBcryptHash = passwordEncoder.encode("DummyPasswordParaEvitarTimingSideChannel2026!");
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se pudo completar el registro con los datos ingresados"
            );
        }

        String hashedPassword = passwordEncoder.encode(request.password());
        User savedUser = userRepository.save(new User(normalizedEmail, hashedPassword));

        String token = jwtService.generateToken(savedUser.getEmail());
        return new AuthResponse(token, savedUser.getId(), savedUser.getEmail());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        // =========================================================================
        // MITIGACIÓN 5.3 (b): Bloqueo temporal tras múltiples intentos fallidos (CWE-307)
        // =========================================================================
        if (loginAttemptService.isBlocked(normalizedEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos fallidos. La cuenta está bloqueada temporalmente por 15 minutos."
            );
        }

        // =========================================================================
        // MITIGACIÓN 5.3 (c): Prevención de enumeración de usuarios (CWE-204)
        // Se devuelve exactamente el mismo código 401 y mensaje "Credenciales inválidas"
        // tanto si el correo no existe como si la contraseña no coincide, ejecutando
        // además una comparación BCrypt ficticia para equiparar el tiempo de respuesta.
        // =========================================================================
        Optional<User> optionalUser = userRepository.findByEmail(normalizedEmail);

        if (optionalUser.isEmpty()) {
            passwordEncoder.matches(request.password(), dummyBcryptHash);
            loginAttemptService.recordFailedAttempt(normalizedEmail);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, GENERIC_AUTH_ERROR);
        }

        User user = optionalUser.get();
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            loginAttemptService.recordFailedAttempt(normalizedEmail);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, GENERIC_AUTH_ERROR);
        }

        loginAttemptService.resetAttempts(normalizedEmail);
        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token, user.getId(), user.getEmail());
    }
}
