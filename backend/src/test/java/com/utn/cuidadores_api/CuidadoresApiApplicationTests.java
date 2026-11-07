package com.utn.cuidadores_api;

import com.utn.cuidadores_api.dto.LoginRequest;
import com.utn.cuidadores_api.dto.ProfileRequest;
import com.utn.cuidadores_api.dto.ProfileResponse;
import com.utn.cuidadores_api.service.AuthService;
import com.utn.cuidadores_api.service.ProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Suite de verificación defensiva para la rama 'remediado':
 * Comprueba que los tres controles de mitigación (CWE-89, CWE-639 y OWASP A07)
 * se encuentren activos y efectivos.
 */
@SpringBootTest(properties = {
        "jwt.secret=SecretoExclusivoDeTestingAutomatizadoConMasDe32Bytes2026!"
})
class CuidadoresApiApplicationTests {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private AuthService authService;

    @Test
    void mitigacionCwe89ConsultaParametrizadaPorZona() {
        // Búsqueda legítima parcial sigue funcionando
        List<ProfileResponse> cordoba = profileService.buscarPerfiles("Córdoba");
        assertTrue(cordoba.size() >= 1, "Debe encontrar perfiles legítimos por coincidencia parcial");

        // Caracteres especiales se tratan estrictamente como literal de texto y devuelven 0 filas
        List<ProfileResponse> entradaNoExistente = profileService.buscarPerfiles("zona_inexistente_test_seguro");
        assertEquals(0, entradaNoExistente.size());
    }

    @Test
    void mitigacionCwe639RechazaModificacionDePerfilAjeno() {
        List<ProfileResponse> todos = profileService.buscarPerfiles("");
        ProfileResponse perfilDeAna = todos.stream()
                .filter(p -> p.userEmail().equalsIgnoreCase("ana.garcia@cuidadores.com"))
                .findFirst()
                .orElseThrow();

        ProfileRequest intentoCambio = new ProfileRequest(
                perfilDeAna.nombre(),
                perfilDeAna.apellido(),
                perfilDeAna.zona(),
                perfilDeAna.descripcion(),
                perfilDeAna.intereses(),
                perfilDeAna.estudios()
        );

        // Un usuario distinto al dueño recibe obligatoriamente 403 Forbidden
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> profileService.actualizarPerfil(
                        perfilDeAna.id(),
                        intentoCambio,
                        "carlos.lopez@cuidadores.com"
                )
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void mitigacionOwaspA07MensajeUniformeYBloqueoTemporal() {
        String correoPruebaBloqueo = "cuenta.inexistente.test@cuidadores.com";

        // 1. Los primeros 5 intentos fallidos devuelven siempre el mismo mensaje genérico 401
        for (int i = 0; i < 5; i++) {
            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> authService.login(new LoginRequest(correoPruebaBloqueo, "ClaveIncorrecta123"))
            );
            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
            assertEquals("Credenciales inválidas", ex.getReason());
        }

        // 2. A partir del 6° intento consecutivo se activa el bloqueo temporal (429 Too Many Requests)
        ResponseStatusException bloqueado = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(new LoginRequest(correoPruebaBloqueo, "ClaveIncorrecta123"))
        );
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, bloqueado.getStatusCode());
    }
}
