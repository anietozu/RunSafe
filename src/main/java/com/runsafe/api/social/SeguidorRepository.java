package com.runsafe.api.social;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeguidorRepository extends JpaRepository<Seguidor, Long> {
    boolean existsBySeguidorIdAndSeguidoId(Long seguidorId, Long seguidoId);
    List<Seguidor> findBySeguidorId(Long seguidorId);
}
