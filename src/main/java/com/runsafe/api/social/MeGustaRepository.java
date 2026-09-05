package com.runsafe.api.social;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MeGustaRepository extends JpaRepository<MeGusta, Long> {
    long countByPublicacionId(Long publicacionId);
    Optional<MeGusta> findByPublicacionIdAndUsuarioId(Long publicacionId, Long usuarioId);
}
