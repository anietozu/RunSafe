package com.runsafe.api.actividad;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ActividadRepository extends JpaRepository<Actividad, Long> {
    List<Actividad> findByUsuarioIdOrderByFechaInicioDesc(Long usuarioId);
    List<Actividad> findByUsuarioIdAndFechaInicioAfter(Long usuarioId, LocalDateTime after);
    List<Actividad> findByUsuarioIdAndFechaInicioGreaterThanEqual(Long usuarioId, LocalDateTime from);

    @Query("select distinct a from Actividad a left join fetch a.puntos where a.id = :id")
    Optional<Actividad> findWithPuntosById(@Param("id") Long id);

    @Query("select a from Actividad a join fetch a.usuario where a.publica = true order by a.fechaInicio desc")
    List<Actividad> findByPublicaTrueOrderByFechaInicioDesc();
}
