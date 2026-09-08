package com.runsafe.api.usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmailIgnoreCase(String email);
    Optional<Usuario> findByLoginIgnoreCase(String login);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByLoginIgnoreCase(String login);

    @Query("""
            select u from Usuario u
            where replace(replace(replace(replace(coalesce(u.telefono, ''), ' ', ''), '-', ''), '(', ''), ')', '') = :tel
            """)
    List<Usuario> findAllByTelefonoNorm(@Param("tel") String tel);

    @Query("""
            select u from Usuario u
            where u.id <> :meId
              and (lower(coalesce(u.login, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(u.nombre, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(u.apellidos, '')) like lower(concat('%', :q, '%')))
            order by u.login
            """)
    List<Usuario> buscarOtros(@Param("meId") Long meId, @Param("q") String q);

    @Query("select u from Usuario u where u.id <> :meId order by u.nombre")
    List<Usuario> findOtros(@Param("meId") Long meId);
}
