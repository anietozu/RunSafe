package com.runsafe.api.social;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComentarioRepository extends JpaRepository<Comentario, Long> {
    List<Comentario> findByPublicacionIdOrderByFechaAsc(Long publicacionId);
    long countByPublicacionId(Long publicacionId);
}
