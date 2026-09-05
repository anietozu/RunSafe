package com.runsafe.api.usuario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmailIgnoreCase(String email);
    Optional<Usuario> findByLoginIgnoreCase(String login);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByLoginIgnoreCase(String login);
}
