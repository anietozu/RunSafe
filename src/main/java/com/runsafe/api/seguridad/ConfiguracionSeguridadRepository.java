package com.runsafe.api.seguridad;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfiguracionSeguridadRepository extends JpaRepository<ConfiguracionSeguridad, Long> {
    Optional<ConfiguracionSeguridad> findByUsuarioId(Long usuarioId);
}
