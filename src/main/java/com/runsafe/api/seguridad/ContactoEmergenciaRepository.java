package com.runsafe.api.seguridad;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContactoEmergenciaRepository extends JpaRepository<ContactoEmergencia, Long> {
    List<ContactoEmergencia> findByUsuarioId(Long usuarioId);
}
