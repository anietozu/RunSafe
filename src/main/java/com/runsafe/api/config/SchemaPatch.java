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
        String[] ddl = {
                "DROP TABLE IF EXISTS objetivos",
                "DROP TABLE IF EXISTS planes_entrenamiento",
                "DROP TABLE IF EXISTS dispositivos",
                "ALTER TABLE configuracion_seguridad ADD COLUMN IF NOT EXISTS sensibilidad_caida INTEGER NOT NULL DEFAULT 56",
                "ALTER TABLE configuracion_seguridad ADD COLUMN IF NOT EXISTS analisis_avanzado_caidas BOOLEAN NOT NULL DEFAULT TRUE",
                """
                CREATE TABLE IF NOT EXISTS grupos (
                    id          BIGSERIAL PRIMARY KEY,
                    creador_id  BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
                    nombre      VARCHAR(120) NOT NULL,
                    descripcion VARCHAR(1000),
                    fecha_alta  TIMESTAMP(6) NOT NULL DEFAULT NOW()
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS grupo_miembros (
                    grupo_id    BIGINT NOT NULL REFERENCES grupos(id) ON DELETE CASCADE,
                    usuario_id  BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
                    fecha       TIMESTAMP(6) NOT NULL DEFAULT NOW(),
                    PRIMARY KEY (grupo_id, usuario_id)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS retos (
                    id          BIGSERIAL PRIMARY KEY,
                    creador_id  BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
                    grupo_id    BIGINT REFERENCES grupos(id) ON DELETE SET NULL,
                    titulo      VARCHAR(120) NOT NULL,
                    metrica     VARCHAR(30) NOT NULL,
                    objetivo    DOUBLE PRECISION NOT NULL,
                    fecha_fin   TIMESTAMP(6) NOT NULL,
                    fecha_alta  TIMESTAMP(6) NOT NULL DEFAULT NOW()
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS reto_inscripciones (
                    reto_id     BIGINT NOT NULL REFERENCES retos(id) ON DELETE CASCADE,
                    usuario_id  BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
                    fecha       TIMESTAMP(6) NOT NULL DEFAULT NOW(),
                    PRIMARY KEY (reto_id, usuario_id)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS sincronizacion (
                    usuario_id     BIGINT PRIMARY KEY REFERENCES usuarios(id) ON DELETE CASCADE,
                    ultima_sync    TIMESTAMP(6) NOT NULL DEFAULT NOW(),
                    operaciones    INTEGER NOT NULL DEFAULT 0
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS eventos_grupo (
                    id              BIGSERIAL PRIMARY KEY,
                    organizador_id  BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
                    titulo          VARCHAR(120) NOT NULL,
                    descripcion     VARCHAR(1000),
                    tipo            VARCHAR(30) NOT NULL,
                    fecha_evento    TIMESTAMP(6) NOT NULL,
                    latitud         DOUBLE PRECISION,
                    longitud        DOUBLE PRECISION,
                    punto_encuentro VARCHAR(200)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS eventos_participantes (
                    evento_id   BIGINT NOT NULL REFERENCES eventos_grupo(id) ON DELETE CASCADE,
                    usuario_id  BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
                    PRIMARY KEY (evento_id, usuario_id)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS grupo_mensajes (
                    id          BIGSERIAL PRIMARY KEY,
                    grupo_id    BIGINT NOT NULL REFERENCES grupos(id) ON DELETE CASCADE,
                    usuario_id  BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
                    texto       VARCHAR(1000) NOT NULL,
                    fecha       TIMESTAMP(6) NOT NULL DEFAULT NOW()
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS evento_mensajes (
                    id          BIGSERIAL PRIMARY KEY,
                    evento_id   BIGINT NOT NULL REFERENCES eventos_grupo(id) ON DELETE CASCADE,
                    usuario_id  BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
                    texto       VARCHAR(1000) NOT NULL,
                    fecha       TIMESTAMP(6) NOT NULL DEFAULT NOW()
                )
                """
        };
        for (String sql : ddl) {
            try {
                jdbc.execute(sql);
            } catch (Exception e) {
                log.warn("Parche de esquema omitido: {}", e.getMessage());
            }
        }
    }
}
