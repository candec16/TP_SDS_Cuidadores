package com.utn.cuidadores_api.config;

import com.utn.cuidadores_api.model.Profile;
import com.utn.cuidadores_api.model.User;
import com.utn.cuidadores_api.repository.ProfileRepository;
import com.utn.cuidadores_api.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Carga automática de datos de ejemplo (seed) al iniciar la aplicación por primera vez
 * si la base de datos "cuidadores" está vacía.
 * Facilita las pruebas funcionales y la demostración en vivo de las vulnerabilidades.
 */
@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initDemoData(UserRepository userRepository,
                                          ProfileRepository profileRepository,
                                          PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() > 0) {
                return;
            }

            // Usuario 1: Ana García (contraseña de diccionario a propósito para demo de fuerza bruta 5.3b)
            User ana = userRepository.save(new User(
                    "ana.garcia@cuidadores.com",
                    passwordEncoder.encode("cuidadores123")
            ));
            profileRepository.save(new Profile(
                    ana,
                    "Ana",
                    "García",
                    "Nueva Córdoba",
                    "Enfermera profesional con 8 años de experiencia en cuidado integral de personas mayores, control de signos vitales, administración de medicación y acompañamiento postoperatorio.",
                    "Lectura en voz alta, caminatas recreativas, música clásica, juegos de memoria",
                    "Licenciatura en Enfermería (UNC) - Curso de Primeros Auxilios y RCP Avanzado"
            ));

            // Usuario 2: Carlos López
            User carlos = userRepository.save(new User(
                    "carlos.lopez@cuidadores.com",
                    passwordEncoder.encode("Carlos2026!")
            ));
            profileRepository.save(new Profile(
                    carlos,
                    "Carlos",
                    "López",
                    "Alberdi",
                    "Acompañante terapéutico especializado en adultos mayores con movilidad reducida y deterioro cognitivo leve. Trato cálido, paciente y referencias comprobables.",
                    "Ajedrez, jardinería, conversación, acompañamiento a turnos médicos",
                    "Tecnicatura en Acompañamiento Terapéutico (UPC) - Diplomatura en Gerontología"
            ));

            // Usuario 3: Marta Rodríguez
            User marta = userRepository.save(new User(
                    "marta.rodriguez@cuidadores.com",
                    passwordEncoder.encode("MartaSegura99!")
            ));
            profileRepository.save(new Profile(
                    marta,
                    "Marta",
                    "Rodríguez",
                    "Cerro de las Rosas",
                    "Cuidadora domiciliaria dedicada a la asistencia diaria, preparación de comidas saludables adaptadas a dietas médicas y estimulación cognitiva.",
                    "Cocina saludable, tejido, crucigramas, radio y charlas",
                    "Auxiliar en Cuidados Gerontológicos (Ministerio de Desarrollo Social)"
            ));

            // Usuario 4: Lucas Fernández (ejemplo de cuenta registrada con clave débil "1234" - CWE-521)
            User lucas = userRepository.save(new User(
                    "lucas.fernandez@cuidadores.com",
                    passwordEncoder.encode("1234")
            ));
            profileRepository.save(new Profile(
                    lucas,
                    "Lucas",
                    "Fernández",
                    "Centro",
                    "Estudiante avanzado de Kinesiología con disponibilidad en turnos tarde y noche. Asistencia en ejercicios de rehabilitación suave y prevención de caídas.",
                    "Rehabilitación motora suave, deportes, lectura de diarios",
                    "4to año de Lic. en Kinesiología y Fisioterapia (UNC)"
            ));

            System.out.println(">>> [DataInitializer] 4 usuarios y perfiles de ejemplo cargados en PostgreSQL ('cuidadores').");
        };
    }
}

