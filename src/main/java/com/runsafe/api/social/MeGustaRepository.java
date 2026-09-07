package com.runsafe.api.social;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MeGustaRepository extends JpaRepository<MeGusta, Long> {
    long countByPublicacionId(Long publicacionId);
    Optional<MeGusta> findByPublicacionIdAndUsuarioId(Long publicacionId, Long usuarioId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from MeGusta m where m.publicacion.id = :id")
    void deleteByPublicacionId(@Param("id") Long id);
}
