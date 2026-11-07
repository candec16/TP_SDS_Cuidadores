package com.utn.cuidadores_api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * Servicio encargado de generar, firmar y verificar tokens JWT (HS256).
 */
@Service
public class JwtService {

    // =========================================================================
    // VULNERABILIDAD INTENCIONAL 5.3 (a): Clave secreta débil y hardcodeada
    // (CWE-798: Use of Hard-coded Credentials / CWE-321: Hard-coded Cryptographic Key)
    // El secreto proviene en texto claro desde application.properties en el repositorio,
    // y el tiempo de expiración es excesivo (24 horas = 86400000 ms).
    // Cualquier atacante que conozca ese secreto puede forjar un JWT válido con el
    // email de cualquier usuario ("sub") y suplantar su identidad sin su contraseña.
    // =========================================================================
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

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

    /**
     * Extrae el email ("sub") del token verificando la firma criptográfica.
     * Lanza JwtException si la firma es inválida o el token expiró.
     */
    public String extractEmail(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            String email = extractEmail(token);
            return email != null && !email.isBlank();
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }
}

