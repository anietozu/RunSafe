package com.runsafe.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SchemaPatch implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaPatch.class);
    private final JdbcTemplate jdbc;

    public SchemaPatch(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbc.execute("ALTER TABLE usuarios DROP COLUMN IF EXISTS activo_usuario");
        } catch (Exception e) {
            log.warn("No se pudo eliminar usuarios.activo_usuario: {}", e.getMessage());
        }
    }
}
