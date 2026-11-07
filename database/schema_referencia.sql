-- ============================================================================
-- Referencia SQL - Plataforma de Perfiles de Cuidadores (UTN FRC - SDS 4K2A)
-- Nota: No es obligatorio correr este script manualmente porque al iniciar
-- Spring Boot (ddl-auto=update + DataInitializer) las tablas y los 4 perfiles
-- de prueba se crean automáticamente en la base de datos "cuidadores".
-- ============================================================================

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL -- Hash BCrypt
);

CREATE TABLE IF NOT EXISTS profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id),
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    zona VARCHAR(120) NOT NULL,
    descripcion VARCHAR(1000) NOT NULL,
    intereses VARCHAR(500),
    estudios VARCHAR(500)
);

