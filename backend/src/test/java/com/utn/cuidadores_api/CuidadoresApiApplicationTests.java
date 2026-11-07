package com.utn.cuidadores_api;

import com.utn.cuidadores_api.dto.ProfileResponse;
import com.utn.cuidadores_api.service.ProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class CuidadoresApiApplicationTests {

    @Autowired
    private ProfileService profileService;

    @Test
    void contextLoadsAndSqlInjectionDemonstrated() {
        // 1. Una búsqueda normal de una zona inexistente devuelve 0 resultados
        List<ProfileResponse> sinResultados = profileService.buscarPerfiles("zzz_inexistente");
        assertEquals(0, sinResultados.size(), "Una zona inexistente no debe arrojar perfiles");

        // 2. En la rama vulnerable (main), el payload de SQL Injection altera el WHERE y devuelve todos los perfiles
        List<ProfileResponse> ataqueSqli = profileService.buscarPerfiles("zzz' OR '1'='1");
        assertTrue(ataqueSqli.size() >= 4, "El payload SQLi debe devolver todos los perfiles cargados");
    }
}
