package com.runsafe.api.evolucion;

import com.runsafe.api.common.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EvolucionStore {

    private final JdbcTemplate jdbc;

    public EvolucionStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> grupos(Long userId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                """
                SELECT g.*, u.nombre AS creador_nombre, u.login AS creador_login,
                       (SELECT COUNT(*) FROM grupo_miembros m WHERE m.grupo_id = g.id) AS miembros,
                       EXISTS (SELECT 1 FROM grupo_miembros m WHERE m.grupo_id = g.id AND m.usuario_id = ?) AS soy_miembro
                FROM grupos g
                JOIN usuarios u ON u.id = g.creador_id
                ORDER BY g.fecha_alta DESC
                """,
                userId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> g = new HashMap<>();
            g.put("id", row.get("id"));
            g.put("nombre", row.get("nombre"));
            g.put("descripcion", row.get("descripcion"));
            g.put("creadorId", row.get("creador_id"));
            g.put("creadorNombre", row.get("creador_nombre"));
            g.put("creadorLogin", row.get("creador_login"));
            g.put("miembros", ((Number) row.get("miembros")).intValue());
            g.put("soyMiembro", bool(row.get("soy_miembro")));
            g.put("esMio", userId.equals(asLong(row.get("creador_id"))));
            g.put("admin", userId.equals(asLong(row.get("creador_id"))));
            out.add(g);
        }
        return out;
    }

    public Map<String, Object> crearGrupo(Long userId, Map<String, Object> body) {
        String nombre = str(body.get("nombre"), "").trim();
        if (nombre.isEmpty()) {
            throw new ApiException("El nombre del grupo es obligatorio");
        }
        String desc = emptyToNull(body.get("descripcion"));
        Long id = jdbc.queryForObject(
                "INSERT INTO grupos (creador_id, nombre, descripcion) VALUES (?, ?, ?) RETURNING id",
                Long.class, userId, nombre, desc);
        jdbc.update("INSERT INTO grupo_miembros (grupo_id, usuario_id) VALUES (?, ?) ON CONFLICT DO NOTHING", id, userId);
        return grupos(userId).stream().filter(g -> id.equals(asLong(g.get("id")))).findFirst()
                .orElseGet(() -> Map.of("id", id));
    }

    public void unirseGrupo(Long userId, Long grupoId) {
        requireGrupo(grupoId);
        jdbc.update("INSERT INTO grupo_miembros (grupo_id, usuario_id) VALUES (?, ?) ON CONFLICT DO NOTHING", grupoId, userId);
    }

    public void salirGrupo(Long userId, Long grupoId) {
        if (userId.equals(creadorDeGrupo(grupoId))) {
            throw new ApiException("El administrador no puede salir. Elimina el grupo si quieres quitarlo.");
        }
        jdbc.update("DELETE FROM grupo_miembros WHERE grupo_id = ? AND usuario_id = ?", grupoId, userId);
    }

    public void borrarGrupo(Long userId, Long grupoId) {
        if (!userId.equals(creadorDeGrupo(grupoId))) {
            throw new ApiException("Solo el administrador puede eliminar el grupo");
        }
        jdbc.update("DELETE FROM grupos WHERE id = ?", grupoId);
    }

    public Map<String, Object> grupoDetalle(Long userId, Long grupoId) {
        ensureAdminGrupo(grupoId);
        Map<String, Object> grupo = grupos(userId).stream()
                .filter(g -> grupoId.equals(asLong(g.get("id"))))
                .findFirst()
                .orElseThrow(() -> new ApiException("Grupo no encontrado"));
        grupo.put("integrantes", miembrosGrupo(grupoId));
        return grupo;
    }

    private List<Map<String, Object>> miembrosGrupo(Long grupoId) {
        requireGrupo(grupoId);
        ensureAdminGrupo(grupoId);
        List<Map<String, Object>> rows = jdbc.queryForList(
                """
                SELECT u.id, u.nombre, u.apellidos, u.login,
                       (u.id = g.creador_id) AS admin
                FROM grupo_miembros m
                JOIN usuarios u ON u.id = m.usuario_id
                JOIN grupos g ON g.id = m.grupo_id
                WHERE m.grupo_id = ?
                ORDER BY (u.id = g.creador_id) DESC, m.fecha ASC
                """,
                grupoId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", row.get("id"));
            m.put("nombre", row.get("nombre"));
            m.put("apellidos", row.get("apellidos"));
            m.put("login", row.get("login"));
            m.put("admin", bool(row.get("admin")));
            out.add(m);
        }
        return out;
    }

    public List<Map<String, Object>> mensajesGrupo(Long userId, Long grupoId) {
        requireMiembroGrupo(userId, grupoId);
        return mensajes("grupo_mensajes", "grupo_id", grupoId, userId);
    }

    public Map<String, Object> enviarMensajeGrupo(Long userId, Long grupoId, Map<String, Object> body) {
        requireMiembroGrupo(userId, grupoId);
        return enviarMensaje("grupo_mensajes", "grupo_id", grupoId, userId, body);
    }

    public Map<String, Object> retoDetalle(Long userId, Long retoId) {
        ensureAdminReto(retoId);
        Map<String, Object> reto = retos(userId).stream()
                .filter(r -> retoId.equals(asLong(r.get("id"))))
                .findFirst()
                .orElseThrow(() -> new ApiException("Reto no encontrado"));
        reto.put("clasificacion", clasificacionReto(retoId));
        return reto;
    }

    public Map<String, Object> eventoDetalle(Long userId, Long eventoId) {
        ensureAdminEvento(eventoId);
        Map<String, Object> evento = eventos(userId).stream()
                .filter(e -> eventoId.equals(asLong(e.get("id"))))
                .findFirst()
                .orElseThrow(() -> new ApiException("Evento no encontrado"));
        evento.put("participantesList", participantesEvento(eventoId));
        return evento;
    }

    public List<Map<String, Object>> mensajesEvento(Long userId, Long eventoId) {
        requireParticipanteEvento(userId, eventoId);
        return mensajes("evento_mensajes", "evento_id", eventoId, userId);
    }

    public Map<String, Object> enviarMensajeEvento(Long userId, Long eventoId, Map<String, Object> body) {
        requireParticipanteEvento(userId, eventoId);
        return enviarMensaje("evento_mensajes", "evento_id", eventoId, userId, body);
    }

    public List<Map<String, Object>> retos(Long userId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                """
                SELECT r.*, u.nombre AS creador_nombre, u.login AS creador_login,
                       EXISTS (SELECT 1 FROM reto_inscripciones i WHERE i.reto_id = r.id AND i.usuario_id = ?) AS inscrito,
                       (SELECT COUNT(*) FROM reto_inscripciones i WHERE i.reto_id = r.id) AS participantes
                FROM retos r
                JOIN usuarios u ON u.id = r.creador_id
                ORDER BY r.fecha_fin ASC
                """,
                userId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> r = retoDto(row);
            r.put("inscrito", bool(row.get("inscrito")));
            r.put("participantes", ((Number) row.get("participantes")).intValue());
            r.put("creadorNombre", row.get("creador_nombre"));
            r.put("creadorLogin", row.get("creador_login"));
            r.put("esMio", userId.equals(asLong(row.get("creador_id"))));
            r.put("admin", userId.equals(asLong(row.get("creador_id"))));
            if (Boolean.TRUE.equals(r.get("inscrito"))) {
                r.put("progreso", progresoReto(userId, r));
            }
            r.remove("fechaAltaRaw");
            r.remove("fechaFinRaw");
            out.add(r);
        }
        return out;
    }

    public Map<String, Object> crearReto(Long userId, Map<String, Object> body) {
        String titulo = str(body.get("titulo"), "").trim();
        if (titulo.isEmpty()) {
            throw new ApiException("El título del reto es obligatorio");
        }
        String metrica = str(body.get("metrica"), "DISTANCIA_KM");
        double objetivo = num(body.get("objetivo"), 50);
        Timestamp fin = ts(body.get("fechaFin"), LocalDateTime.now().plusDays(14));
        Long grupoId = body.get("grupoId") == null ? null : asLong(body.get("grupoId"));
        Long id = jdbc.queryForObject(
                """
                INSERT INTO retos (creador_id, grupo_id, titulo, metrica, objetivo, fecha_fin)
                VALUES (?, ?, ?, ?, ?, ?) RETURNING id
                """,
                Long.class, userId, grupoId, titulo, metrica, objetivo, fin);
        jdbc.update("INSERT INTO reto_inscripciones (reto_id, usuario_id) VALUES (?, ?) ON CONFLICT DO NOTHING", id, userId);
        return retos(userId).stream().filter(r -> id.equals(asLong(r.get("id")))).findFirst()
                .orElseGet(() -> Map.of("id", id));
    }

    public void unirseReto(Long userId, Long retoId) {
        jdbc.update("INSERT INTO reto_inscripciones (reto_id, usuario_id) VALUES (?, ?) ON CONFLICT DO NOTHING", retoId, userId);
    }

    public void salirReto(Long userId, Long retoId) {
        if (userId.equals(creadorDeReto(retoId))) {
            throw new ApiException("El administrador no puede abandonar. Elimina el reto si quieres quitarlo.");
        }
        jdbc.update("DELETE FROM reto_inscripciones WHERE reto_id = ? AND usuario_id = ?", retoId, userId);
    }

    public void borrarReto(Long userId, Long retoId) {
        if (!userId.equals(creadorDeReto(retoId))) {
            throw new ApiException("Solo el administrador puede eliminar el reto");
        }
        jdbc.update("DELETE FROM retos WHERE id = ?", retoId);
    }

    private List<Map<String, Object>> clasificacionReto(Long retoId) {
        ensureAdminReto(retoId);
        Map<String, Object> reto = jdbc.queryForMap("SELECT * FROM retos WHERE id = ?", retoId);
        Map<String, Object> dto = retoDto(reto);
        Long creadorId = asLong(reto.get("creador_id"));
        List<Map<String, Object>> inscritos = jdbc.queryForList(
                """
                SELECT u.id, u.nombre, u.login FROM reto_inscripciones i
                JOIN usuarios u ON u.id = i.usuario_id
                WHERE i.reto_id = ?
                """,
                retoId);
        List<Map<String, Object>> ranking = new ArrayList<>();
        for (Map<String, Object> u : inscritos) {
            Long uid = asLong(u.get("id"));
            double progreso = progresoReto(uid, dto);
            Map<String, Object> row = new HashMap<>();
            row.put("usuarioId", uid);
            row.put("nombre", u.get("nombre"));
            row.put("login", u.get("login"));
            row.put("progreso", progreso);
            row.put("objetivo", dto.get("objetivo"));
            row.put("admin", creadorId.equals(uid));
            ranking.add(row);
        }
        ranking.sort((a, b) -> Double.compare((Double) b.get("progreso"), (Double) a.get("progreso")));
        return ranking;
    }

    public List<Map<String, Object>> eventos(Long userId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                """
                SELECT e.*, u.nombre AS organizador_nombre, u.login AS organizador_login,
                       EXISTS (SELECT 1 FROM eventos_participantes p WHERE p.evento_id = e.id AND p.usuario_id = ?) AS inscrito,
                       (SELECT COUNT(*) FROM eventos_participantes p WHERE p.evento_id = e.id) AS participantes
                FROM eventos_grupo e
                JOIN usuarios u ON u.id = e.organizador_id
                ORDER BY e.fecha_evento ASC
                """,
                userId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> e = new HashMap<>();
            e.put("id", row.get("id"));
            e.put("titulo", row.get("titulo"));
            e.put("descripcion", row.get("descripcion"));
            e.put("tipo", row.get("tipo"));
            e.put("fechaEvento", row.get("fecha_evento"));
            e.put("latitud", row.get("latitud"));
            e.put("longitud", row.get("longitud"));
            e.put("puntoEncuentro", row.get("punto_encuentro"));
            e.put("organizadorId", row.get("organizador_id"));
            e.put("organizadorNombre", row.get("organizador_nombre"));
            e.put("organizadorLogin", row.get("organizador_login"));
            e.put("inscrito", bool(row.get("inscrito")));
            e.put("participantes", ((Number) row.get("participantes")).intValue());
            e.put("esMio", userId.equals(asLong(row.get("organizador_id"))));
            e.put("admin", userId.equals(asLong(row.get("organizador_id"))));
            out.add(e);
        }
        return out;
    }

    public Map<String, Object> crearEvento(Long userId, Map<String, Object> body) {
        String titulo = str(body.get("titulo"), "").trim();
        if (titulo.isEmpty()) {
            throw new ApiException("El título del evento es obligatorio");
        }
        String tipo = str(body.get("tipo"), "CORRER");
        Timestamp fecha = ts(body.get("fechaEvento"), LocalDateTime.now().plusDays(7));
        String desc = emptyToNull(body.get("descripcion"));
        String punto = emptyToNull(body.get("puntoEncuentro"));
        Double lat = body.get("latitud") == null ? null : num(body.get("latitud"), 0);
        Double lng = body.get("longitud") == null ? null : num(body.get("longitud"), 0);
        Long id = jdbc.queryForObject(
                """
                INSERT INTO eventos_grupo (organizador_id, titulo, descripcion, tipo, fecha_evento, latitud, longitud, punto_encuentro)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                """,
                Long.class, userId, titulo, desc, tipo, fecha, lat, lng, punto);
        jdbc.update("INSERT INTO eventos_participantes (evento_id, usuario_id) VALUES (?, ?) ON CONFLICT DO NOTHING", id, userId);
        return eventos(userId).stream().filter(e -> id.equals(asLong(e.get("id")))).findFirst()
                .orElseGet(() -> Map.of("id", id));
    }

    public void unirseEvento(Long userId, Long eventoId) {
        jdbc.update("INSERT INTO eventos_participantes (evento_id, usuario_id) VALUES (?, ?) ON CONFLICT DO NOTHING", eventoId, userId);
    }

    public void salirEvento(Long userId, Long eventoId) {
        if (userId.equals(organizadorDeEvento(eventoId))) {
            throw new ApiException("El administrador no puede salir. Elimina el evento si quieres quitarlo.");
        }
        jdbc.update("DELETE FROM eventos_participantes WHERE evento_id = ? AND usuario_id = ?", eventoId, userId);
    }

    public void borrarEvento(Long userId, Long eventoId) {
        if (!userId.equals(organizadorDeEvento(eventoId))) {
            throw new ApiException("Solo el administrador puede eliminar el evento");
        }
        jdbc.update("DELETE FROM eventos_grupo WHERE id = ?", eventoId);
    }

    private List<Map<String, Object>> participantesEvento(Long eventoId) {
        ensureAdminEvento(eventoId);
        List<Map<String, Object>> rows = jdbc.queryForList(
                """
                SELECT u.id, u.nombre, u.apellidos, u.login,
                       (u.id = e.organizador_id) AS admin
                FROM eventos_participantes p
                JOIN usuarios u ON u.id = p.usuario_id
                JOIN eventos_grupo e ON e.id = p.evento_id
                WHERE p.evento_id = ?
                ORDER BY (u.id = e.organizador_id) DESC, u.nombre ASC
                """,
                eventoId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", row.get("id"));
            m.put("nombre", row.get("nombre"));
            m.put("apellidos", row.get("apellidos"));
            m.put("login", row.get("login"));
            m.put("admin", bool(row.get("admin")));
            out.add(m);
        }
        return out;
    }

    public Map<String, Object> heartbeat(Long userId, int operaciones) {
        jdbc.update(
                """
                INSERT INTO sincronizacion (usuario_id, ultima_sync, operaciones)
                VALUES (?, NOW(), ?)
                ON CONFLICT (usuario_id) DO UPDATE SET ultima_sync = NOW(), operaciones = EXCLUDED.operaciones
                """,
                userId, operaciones);
        return Map.of("ok", true);
    }

    private double progresoReto(Long userId, Map<String, Object> reto) {
        Timestamp alta = (Timestamp) reto.get("fechaAltaRaw");
        Timestamp fin = (Timestamp) reto.get("fechaFinRaw");
        LocalDateTime from = alta == null ? LocalDateTime.now().minusDays(30) : alta.toLocalDateTime();
        String tipo = String.valueOf(reto.get("metrica"));
        Stats s = statsBetween(userId, from, fin == null ? LocalDateTime.now().plusYears(1) : fin.toLocalDateTime());
        return switch (tipo) {
            case "TIEMPO_MIN" -> s.duracionS / 60.0;
            case "SESIONES" -> s.sesiones;
            default -> s.distanciaM / 1000.0;
        };
    }

    private Stats statsBetween(Long userId, LocalDateTime from, LocalDateTime to) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                """
                SELECT COALESCE(SUM(distancia_m),0) d, COALESCE(SUM(duracion_s),0) t, COUNT(*) n
                FROM actividades WHERE usuario_id = ? AND fecha_inicio >= ? AND fecha_inicio < ?
                """,
                userId, Timestamp.valueOf(from), Timestamp.valueOf(to));
        Map<String, Object> r = rows.get(0);
        return new Stats(
                ((Number) r.get("d")).doubleValue(),
                ((Number) r.get("t")).intValue(),
                ((Number) r.get("n")).intValue());
    }

    private Long creadorDeGrupo(Long grupoId) {
        requireGrupo(grupoId);
        return asLong(jdbc.queryForMap("SELECT creador_id FROM grupos WHERE id = ?", grupoId).get("creador_id"));
    }

    private Long creadorDeReto(Long retoId) {
        List<Map<String, Object>> found = jdbc.queryForList("SELECT creador_id FROM retos WHERE id = ?", retoId);
        if (found.isEmpty()) {
            throw new ApiException("Reto no encontrado");
        }
        return asLong(found.get(0).get("creador_id"));
    }

    private Long organizadorDeEvento(Long eventoId) {
        List<Map<String, Object>> found = jdbc.queryForList("SELECT organizador_id FROM eventos_grupo WHERE id = ?", eventoId);
        if (found.isEmpty()) {
            throw new ApiException("Evento no encontrado");
        }
        return asLong(found.get(0).get("organizador_id"));
    }

    private void ensureAdminGrupo(Long grupoId) {
        jdbc.update(
                """
                INSERT INTO grupo_miembros (grupo_id, usuario_id)
                SELECT id, creador_id FROM grupos WHERE id = ?
                ON CONFLICT DO NOTHING
                """,
                grupoId);
    }

    private void ensureAdminReto(Long retoId) {
        jdbc.update(
                """
                INSERT INTO reto_inscripciones (reto_id, usuario_id)
                SELECT id, creador_id FROM retos WHERE id = ?
                ON CONFLICT DO NOTHING
                """,
                retoId);
    }

    private void ensureAdminEvento(Long eventoId) {
        jdbc.update(
                """
                INSERT INTO eventos_participantes (evento_id, usuario_id)
                SELECT id, organizador_id FROM eventos_grupo WHERE id = ?
                ON CONFLICT DO NOTHING
                """,
                eventoId);
    }

    private void requireGrupo(Long id) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM grupos WHERE id = ?", Integer.class, id);
        if (n == null || n == 0) {
            throw new ApiException("Grupo no encontrado");
        }
    }

    private void requireMiembroGrupo(Long userId, Long grupoId) {
        requireGrupo(grupoId);
        ensureAdminGrupo(grupoId);
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM grupo_miembros WHERE grupo_id = ? AND usuario_id = ?",
                Integer.class, grupoId, userId);
        if (n == null || n == 0) {
            throw new ApiException("Únete al grupo para ver el chat");
        }
    }

    private void requireParticipanteEvento(Long userId, Long eventoId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM eventos_grupo WHERE id = ?", Integer.class, eventoId);
        if (n == null || n == 0) {
            throw new ApiException("Evento no encontrado");
        }
        ensureAdminEvento(eventoId);
        Integer p = jdbc.queryForObject(
                "SELECT COUNT(*) FROM eventos_participantes WHERE evento_id = ? AND usuario_id = ?",
                Integer.class, eventoId, userId);
        if (p == null || p == 0) {
            throw new ApiException("Apúntate al evento para comentar");
        }
    }

    private List<Map<String, Object>> mensajes(String table, String fk, Long ownerId, Long userId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT m.id, m.texto, m.fecha, u.id AS usuario_id, u.nombre, u.login FROM "
                        + table + " m JOIN usuarios u ON u.id = m.usuario_id WHERE m." + fk + " = ? ORDER BY m.fecha ASC",
                ownerId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", row.get("id"));
            m.put("texto", row.get("texto"));
            m.put("fecha", row.get("fecha"));
            m.put("usuarioId", row.get("usuario_id"));
            m.put("nombre", row.get("nombre"));
            m.put("login", row.get("login"));
            m.put("esMio", userId.equals(asLong(row.get("usuario_id"))));
            out.add(m);
        }
        return out;
    }

    private Map<String, Object> enviarMensaje(String table, String fk, Long ownerId, Long userId, Map<String, Object> body) {
        String texto = str(body.get("texto"), "").trim();
        if (texto.isEmpty()) {
            throw new ApiException("Escribe un mensaje");
        }
        if (texto.length() > 1000) {
            texto = texto.substring(0, 1000);
        }
        Long id = jdbc.queryForObject(
                "INSERT INTO " + table + " (" + fk + ", usuario_id, texto) VALUES (?, ?, ?) RETURNING id",
                Long.class, ownerId, userId, texto);
        return mensajes(table, fk, ownerId, userId).stream()
                .filter(m -> id.equals(asLong(m.get("id"))))
                .findFirst()
                .orElseGet(() -> Map.of("id", id, "texto", texto));
    }

    private Map<String, Object> retoDto(Map<String, Object> row) {
        Map<String, Object> r = new HashMap<>();
        r.put("id", row.get("id"));
        r.put("titulo", row.get("titulo"));
        r.put("metrica", row.get("metrica"));
        r.put("objetivo", row.get("objetivo"));
        r.put("fechaFin", row.get("fecha_fin"));
        r.put("grupoId", row.get("grupo_id"));
        r.put("creadorId", row.get("creador_id"));
        r.put("fechaAltaRaw", row.get("fecha_alta"));
        r.put("fechaFinRaw", row.get("fecha_fin"));
        return r;
    }

    private static String str(Object v, String fallback) {
        if (v == null) {
            return fallback;
        }
        String s = v.toString().trim();
        return s.isEmpty() ? fallback : s;
    }

    private static String emptyToNull(Object v) {
        if (v == null) {
            return null;
        }
        String s = v.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static double num(Object v, double fallback) {
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        if (v == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(v.toString());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static Long asLong(Object v) {
        if (v instanceof Number n) {
            return n.longValue();
        }
        return v == null ? null : Long.parseLong(v.toString());
    }

    private static boolean bool(Object v) {
        if (v instanceof Boolean b) {
            return b;
        }
        if (v instanceof Number n) {
            return n.intValue() != 0;
        }
        return Boolean.parseBoolean(String.valueOf(v));
    }

    private static Timestamp ts(Object v, LocalDateTime fallback) {
        if (v == null) {
            return Timestamp.valueOf(fallback);
        }
        String s = v.toString().trim();
        try {
            return Timestamp.from(Instant.parse(s));
        } catch (Exception ignored) {
            try {
                return Timestamp.valueOf(LocalDateTime.parse(s.replace("Z", "")));
            } catch (Exception e) {
                return Timestamp.valueOf(fallback);
            }
        }
    }

    private record Stats(double distanciaM, int duracionS, int sesiones) {}
}
