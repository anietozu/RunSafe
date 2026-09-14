package com.runsafe.api.usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RecuperacionPasswordRepository extends JpaRepository<RecuperacionPassword, Long> {
    Optional<RecuperacionPassword> findFirstByUsuarioIdAndUsadaFalseOrderByFechaAltaDesc(Long usuarioId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RecuperacionPassword r set r.usada = true where r.usuario.id = :uid and r.usada = false")
    void invalidarPendientes(@Param("uid") Long usuarioId);
}
