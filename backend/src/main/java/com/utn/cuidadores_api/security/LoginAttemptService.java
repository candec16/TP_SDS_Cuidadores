package com.utn.cuidadores_api.security;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mitigación 5.3 (b) - CWE-307 (Improper Restriction of Excessive Authentication Attempts):
 * Servicio en memoria thread-safe que contabiliza intentos de login fallidos consecutivos
 * y aplica un bloqueo temporal tras alcanzar el umbral configurado.
 */
@Service
public class LoginAttemptService {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private record AttemptRecord(int failedCount, Instant lockedUntil) {
    }

    private final ConcurrentHashMap<String, AttemptRecord> attemptsByEmail = new ConcurrentHashMap<>();

    public boolean isBlocked(String emailKey) {
        AttemptRecord record = attemptsByEmail.get(emailKey);
        if (record == null || record.lockedUntil() == null) {
            return false;
        }
        if (Instant.now().isBefore(record.lockedUntil())) {
            return true;
        }
        // Si ya venció la ventana de bloqueo, se limpia el registro
        attemptsByEmail.remove(emailKey);
        return false;
    }

    public void recordFailedAttempt(String emailKey) {
        attemptsByEmail.compute(emailKey, (key, current) -> {
            int count = (current == null || (current.lockedUntil() != null && Instant.now().isAfter(current.lockedUntil())))
                    ? 1
                    : current.failedCount() + 1;

            Instant lockUntil = count >= MAX_FAILED_ATTEMPTS
                    ? Instant.now().plus(LOCK_DURATION)
                    : null;

            return new AttemptRecord(count, lockUntil);
        });
    }

    public void resetAttempts(String emailKey) {
        attemptsByEmail.remove(emailKey);
    }
}

