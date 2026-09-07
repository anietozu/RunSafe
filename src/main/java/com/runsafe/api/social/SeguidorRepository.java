package com.runsafe.api.social;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface SeguidorRepository extends JpaRepository<Seguidor, Long> {
    boolean existsBySeguidorIdAndSeguidoId(Long seguidorId, Long seguidoId);
    List<Seguidor> findBySeguidorId(Long seguidorId);

    @Transactional
    void deleteBySeguidorIdAndSeguidoId(Long seguidorId, Long seguidoId);
}
