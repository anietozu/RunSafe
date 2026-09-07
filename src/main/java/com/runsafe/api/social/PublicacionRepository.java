package com.runsafe.api.social;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PublicacionRepository extends JpaRepository<Publicacion, Long> {
    List<Publicacion> findAllByOrderByFechaDesc();
    List<Publicacion> findByActividadId(Long actividadId);
    boolean existsByUsuarioIdAndActividadId(Long usuarioId, Long actividadId);

    @Query("select p from Publicacion p join fetch p.usuario left join fetch p.actividad order by p.fecha desc")
    List<Publicacion> findFeed();

    @Query("select p from Publicacion p join fetch p.usuario left join fetch p.actividad where p.id = :id")
    Optional<Publicacion> findDetailedById(@Param("id") Long id);
}
