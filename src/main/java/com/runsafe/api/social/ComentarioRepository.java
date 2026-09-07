package com.runsafe.api.social;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ComentarioRepository extends JpaRepository<Comentario, Long> {
    @Query("select c from Comentario c join fetch c.usuario where c.publicacion.id = :id order by c.fecha asc")
    List<Comentario> findByPublicacionIdOrderByFechaAsc(@Param("id") Long id);

    long countByPublicacionId(Long publicacionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Comentario c where c.publicacion.id = :id")
    void deleteByPublicacionId(@Param("id") Long id);
}
