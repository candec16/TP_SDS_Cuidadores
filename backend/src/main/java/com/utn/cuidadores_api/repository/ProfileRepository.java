package com.utn.cuidadores_api.repository;

import com.utn.cuidadores_api.model.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

    Optional<Profile> findByUserEmail(String email);

    boolean existsByUserId(Long userId);

    /**
     * Mitigación CWE-89 (SQL Injection): método derivado de Spring Data JPA
     * que genera internamente un PreparedStatement parametrizado seguro.
     */
    List<Profile> findByZonaContainingIgnoreCase(String zona);
}
