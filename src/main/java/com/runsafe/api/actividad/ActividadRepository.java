package com.runsafe.api.actividad;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ActividadRepository extends JpaRepository<Actividad, Long> {
    List<Actividad> findByUsuarioIdOrderByFechaInicioDesc(Long usuarioId);
    List<Actividad> findByUsuarioIdAndFechaInicioAfter(Long usuarioId, LocalDateTime after);
}
