package com.utn.cuidadores_api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * Servicio encargado de generar, firmar y verificar tokens JWT (HS256)
 * con validaciones estrictas de entropía/longitud de clave y expiración obligatoria.
 */
@Service
public class JwtService {

    private static final int MIN_SECRET_BYTES = 32;
    private static final String COMPROMISED_DEMO_SECRET = "clave_secreta_super_debil_1234567890";

    // =========================================================================
    // MITIGACIÓN 5.3 (a): Gestión segura del secreto criptográfico (CWE-798 / CWE-321)
    // El secreto proviene obligatoriamente de la variable de entorno JWT_SECRET y se
    // valida al iniciar la aplicación que cumpla al menos 256 bits (32 bytes) y no
    // reutilice claves comprometidas conocidas.
    // =========================================================================
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    @PostConstruct
    public void validateSecretConfiguration() {
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "Configuración insegura: la variable de entorno JWT_SECRET debe tener al menos 32 caracteres."
            );
        }
        if (COMPROMISED_DEMO_SECRET.equals(jwtSecret)) {
            throw new IllegalStateException(
                    "Configuración insegura: no se permite utilizar el secreto de demostración comprometido."
            );
        }
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Emite un JWT firmado con HS256 cuyo subject ("sub") es el email del usuario.
     */
    public String generateToken(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            String email = claims.getSubject();
            Date expiration = claims.getExpiration();
            return email != null
                    && !email.isBlank()
                    && expiration != null
                    && expiration.after(new Date());
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }
}
