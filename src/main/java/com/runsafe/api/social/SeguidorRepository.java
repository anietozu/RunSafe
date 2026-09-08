package com.runsafe.api.social;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface SeguidorRepository extends JpaRepository<Seguidor, Long> {
    boolean existsBySeguidorIdAndSeguidoId(Long seguidorId, Long seguidoId);

    @Query("select s from Seguidor s join fetch s.seguido where s.seguidor.id = :id")
    List<Seguidor> findBySeguidorId(@Param("id") Long id);

    @Transactional
    void deleteBySeguidorIdAndSeguidoId(Long seguidorId, Long seguidoId);
}
