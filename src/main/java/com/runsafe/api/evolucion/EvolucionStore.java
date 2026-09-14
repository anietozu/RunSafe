package com.runsafe.api.evolucion;

import com.runsafe.api.common.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EvolucionStore {

    private static final ZoneId ZONE = ZoneId.of("Europe/Madrid");
    private final JdbcTemplate jdbc;

    public EvolucionStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> objetivos(Long userId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM objetivos WHERE usuario_id = ? ORDER BY activo DESC, id DESC", userId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = objetivoDto(row);
            item.putAll(progresoObjetivo(userId, item));
            out.add(item);
        }
        return out;
    }

    public Map<String, Object> crearObjetivo(Long userId, Map<String, Object> body) {
        String tipo = str(body.get("tipo"), "DISTANCIA_KM");
        String periodo = str(body.get("periodo"), "SEMANA");
        double valor = num(body.get("valor"), 20);
        String tipoAct = emptyToNull(body.get("tipoActividad"));
        Long id = jdbc.queryForObject(
                """
                INSERT INTO objetivos (usuario_id, tipo, periodo, valor, tipo_actividad)
                VALUES (?, ?, ?, ?, ?) RETURNING id
                """,
                Long.class, userId, tipo, periodo, valor, tipoAct);
        return objetivos(userId).stream().filter(o -> id.equals(asLong(o.get("id")))).findFirst()
                .orElseGet(() -> Map.of("id", id));
    }

    public void borrarObjetivo(Long userId, Long id) {
        int n = jdbc.update("DELETE FROM objetivos WHERE id = ? AND usuario_id = ?", id, userId);
        if (n == 0) {
            throw new ApiException("Objetivo no encontrado");
        }
    }

    public List<Map<String, Object>> planes(Long userId) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : jdbc.queryForList(
                "SELECT * FROM planes_entrenamiento WHERE usuario_id = ? ORDER BY activo DESC, id DESC", userId)) {
            Map<String, Object> plan = planDto(row);
            plan.putAll(progresoPlan(userId, plan));
            out.add(plan);
        }
        return out;
    }

    public Map<String, Object> crearPlan(Long userId, Map<String, Object> body) {
        String nombre = str(body.get("nombre"), "Plan semanal");
        String tipo = str(body.get("tipoActividad"), "CORRER");
        int ses = (int) num(body.get("sesionesSemana"), 3);
        double km = num(body.get("kmSemana"), 20);
        String notas = emptyToNull(body.get("notas"));
        Long id = jdbc.queryForObject(
                """
                INSERT INTO planes_entrenamiento (usuario_id, nombre, tipo_actividad, sesiones_semana, km_semana, notas)
                VALUES (?, ?, ?, ?, ?, ?) RETURNING id
                """,
                Long.class, userId, nombre, tipo, ses, km, notas);
        return planes(userId).stream().filter(p -> id.equals(asLong(p.get("id")))).findFirst()
                .orElseGet(() -> Map.of("id", id));
    }

    public void borrarPlan(Long userId, Long id) {
        int n = jdbc.update("DELETE FROM planes_entrenamiento WHERE id = ? AND usuario_id = ?", id, userId);
        if (n == 0) {
            throw new ApiException("Plan no encontrado");
        }
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
        jdbc.update("DELETE FROM grupo_miembros WHERE grupo_id = ? AND usuario_id = ?", grupoId, userId);
    }

    public Map<String, Object> grupoDetalle(Long userId, Long grupoId) {
        Map<String, Object> grupo = grupos(userId).stream()
                .filter(g -> grupoId.equals(asLong(g.get("id"))))
                .findFirst()
                .orElseThrow(() -> new ApiException("Grupo no encontrado"));
        grupo.put("integrantes", miembrosGrupo(grupoId));
        return grupo;
    }

    public List<Map<String, Object>> miembrosGrupo(Long grupoId) {
        requireGrupo(grupoId);
        List<Map<String, Object>> rows = jdbc.queryForList(
                """
                SELECT u.id, u.nombre, u.apellidos, u.login
                FROM grupo_miembros m
                JOIN usuarios u ON u.id = m.usuario_id
                WHERE m.grupo_id = ?
                ORDER BY m.fecha ASC
                """,
                grupoId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", row.get("id"));
            m.put("nombre", row.get("nombre"));
            m.put("apellidos", row.get("apellidos"));
            m.put("login", row.get("login"));
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
        Map<String, Object> reto = retos(userId).stream()
                .filter(r -> retoId.equals(asLong(r.get("id"))))
                .findFirst()
                .orElseThrow(() -> new ApiException("Reto no encontrado"));
        reto.put("clasificacion", clasificacionReto(retoId));
        return reto;
    }

    public Map<String, Object> eventoDetalle(Long userId, Long eventoId) {
        Map<String, Object> evento = eventos(userId).stream()
                .filter(e -> eventoId.equals(asLong(e.get("id"))))
                .findFirst()
                .orElseThrow(() -> new ApiException("Evento no encontrado"));
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
                SELECT r.*, u.nombre AS creador_nombre,
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
        jdbc.update("DELETE FROM reto_inscripciones WHERE reto_id = ? AND usuario_id = ?", retoId, userId);
    }

    public List<Map<String, Object>> clasificacionReto(Long retoId) {
        Map<String, Object> reto = jdbc.queryForMap("SELECT * FROM retos WHERE id = ?", retoId);
        Map<String, Object> dto = retoDto(reto);
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
            ranking.add(row);
        }
        ranking.sort((a, b) -> Double.compare((Double) b.get("progreso"), (Double) a.get("progreso")));
        return ranking;
    }

    public List<Map<String, Object>> eventos(Long userId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                """
                SELECT e.*, u.nombre AS organizador_nombre,
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
            e.put("inscrito", bool(row.get("inscrito")));
            e.put("participantes", ((Number) row.get("participantes")).intValue());
            e.put("esMio", userId.equals(asLong(row.get("organizador_id"))));
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
        jdbc.update("DELETE FROM eventos_participantes WHERE evento_id = ? AND usuario_id = ?", eventoId, userId);
    }

    public List<Map<String, Object>> ranking(String periodo) {
        LocalDateTime from = inicioPeriodo(periodo);
        List<Map<String, Object>> rows = jdbc.queryForList(
                """
                SELECT u.id AS usuario_id, u.nombre, u.login,
                       COALESCE(SUM(a.distancia_m), 0) AS distancia_m,
                       COUNT(a.id) AS sesiones,
                       COALESCE(SUM(a.duracion_s), 0) AS duracion_s,
                       COALESCE(SUM(a.calorias), 0) AS calorias
                FROM usuarios u
                JOIN actividades a ON a.usuario_id = u.id AND a.fecha_inicio >= ? AND a.publica = TRUE
                GROUP BY u.id, u.nombre, u.login
                ORDER BY distancia_m DESC
                LIMIT 30
                """,
                Timestamp.valueOf(from));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("usuarioId", row.get("usuario_id"));
            m.put("nombre", row.get("nombre"));
            m.put("login", row.get("login"));
            m.put("distanciaM", row.get("distancia_m"));
            m.put("sesiones", row.get("sesiones"));
            m.put("duracionS", row.get("duracion_s"));
            m.put("calorias", row.get("calorias"));
            out.add(m);
        }
        return out;
    }

    public Map<String, Object> recomendaciones(Long userId) {
        List<Map<String, Object>> tipos = jdbc.queryForList(
                """
                SELECT tipo, COUNT(*) AS n, COALESCE(AVG(distancia_m), 0) AS dist
                FROM actividades WHERE usuario_id = ?
                GROUP BY tipo ORDER BY n DESC
                """,
                userId);
        String preferido = tipos.isEmpty() ? "CORRER" : String.valueOf(tipos.get(0).get("tipo"));
        double distMedia = tipos.isEmpty() ? 5000 : ((Number) tipos.get(0).get("dist")).doubleValue();
        List<Map<String, Object>> rutas = jdbc.queryForList(
                """
                SELECT a.id, a.tipo, a.distancia_m AS "distanciaM", a.duracion_s AS "duracionS",
                       a.desnivel_m AS "desnivelM", a.fecha_inicio AS "fechaInicio",
                       u.nombre AS "usuarioNombre", u.login
                FROM actividades a
                JOIN usuarios u ON u.id = a.usuario_id
                WHERE a.publica = TRUE AND a.usuario_id <> ? AND a.tipo = ?
                ORDER BY ABS(a.distancia_m - ?) ASC, a.fecha_inicio DESC
                LIMIT 8
                """,
                userId, preferido, distMedia);
        List<Map<String, Object>> eventos = jdbc.queryForList(
                """
                SELECT id, titulo, tipo, fecha_evento AS "fechaEvento", punto_encuentro AS "puntoEncuentro"
                FROM eventos_grupo
                WHERE fecha_evento >= NOW() AND tipo = ?
                ORDER BY fecha_evento ASC LIMIT 5
                """,
                preferido);
        Map<String, Object> out = new HashMap<>();
        out.put("tipoPreferido", preferido);
        out.put("distanciaMediaM", distMedia);
        out.put("rutas", rutas);
        out.put("eventos", eventos);
        out.put("sugerencia", sugerenciaTexto(preferido, distMedia));
        return out;
    }

    public List<Map<String, Object>> dispositivos(Long userId) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : jdbc.queryForList(
                "SELECT * FROM dispositivos WHERE usuario_id = ? ORDER BY id DESC", userId)) {
            Map<String, Object> d = new HashMap<>();
            d.put("id", row.get("id"));
            d.put("nombre", row.get("nombre"));
            d.put("tipo", row.get("tipo"));
            d.put("modelo", row.get("modelo"));
            d.put("conectado", bool(row.get("conectado")));
            d.put("fechaAlta", row.get("fecha_alta"));
            out.add(d);
        }
        return out;
    }

    public Map<String, Object> crearDispositivo(Long userId, Map<String, Object> body) {
        String nombre = str(body.get("nombre"), "").trim();
        if (nombre.isEmpty()) {
            throw new ApiException("Indica el nombre del dispositivo");
        }
        String tipo = str(body.get("tipo"), "RELOJ");
        String modelo = emptyToNull(body.get("modelo"));
        Long id = jdbc.queryForObject(
                "INSERT INTO dispositivos (usuario_id, nombre, tipo, modelo) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class, userId, nombre, tipo, modelo);
        return dispositivos(userId).stream().filter(d -> id.equals(asLong(d.get("id")))).findFirst()
                .orElseGet(() -> Map.of("id", id));
    }

    public Map<String, Object> actualizarDispositivo(Long userId, Long id, Map<String, Object> body) {
        List<Map<String, Object>> found = jdbc.queryForList(
                "SELECT * FROM dispositivos WHERE id = ? AND usuario_id = ?", id, userId);
        if (found.isEmpty()) {
            throw new ApiException("Dispositivo no encontrado");
        }
        Map<String, Object> cur = found.get(0);
        boolean conectado = body.containsKey("conectado") ? bool(body.get("conectado")) : bool(cur.get("conectado"));
        jdbc.update("UPDATE dispositivos SET conectado = ? WHERE id = ?", conectado, id);
        return dispositivos(userId).stream().filter(d -> id.equals(asLong(d.get("id")))).findFirst()
                .orElseGet(() -> Map.of("id", id, "conectado", conectado));
    }

    public void borrarDispositivo(Long userId, Long id) {
        int n = jdbc.update("DELETE FROM dispositivos WHERE id = ? AND usuario_id = ?", id, userId);
        if (n == 0) {
            throw new ApiException("Dispositivo no encontrado");
        }
    }

    public Map<String, Object> heartbeat(Long userId, int operaciones) {
        jdbc.update(
                """
                INSERT INTO sincronizacion (usuario_id, ultima_sync, operaciones)
                VALUES (?, NOW(), ?)
                ON CONFLICT (usuario_id) DO UPDATE SET ultima_sync = NOW(), operaciones = EXCLUDED.operaciones
                """,
                userId, operaciones);
        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM sincronizacion WHERE usuario_id = ?", userId);
        Map<String, Object> out = new HashMap<>();
        out.put("ok", true);
        out.put("servidor", Instant.now().toString());
        out.put("ultimaSync", row.get("ultima_sync"));
        out.put("operaciones", row.get("operaciones"));
        return out;
    }

    public Map<String, Object> estadoSync(Long userId) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM sincronizacion WHERE usuario_id = ?", userId);
        Map<String, Object> out = new HashMap<>();
        out.put("ok", true);
        out.put("servidor", Instant.now().toString());
        if (rows.isEmpty()) {
            out.put("ultimaSync", null);
            out.put("operaciones", 0);
            return out;
        }
        out.put("ultimaSync", rows.get(0).get("ultima_sync"));
        out.put("operaciones", rows.get(0).get("operaciones"));
        return out;
    }

    private Map<String, Object> progresoObjetivo(Long userId, Map<String, Object> obj) {
        String periodo = String.valueOf(obj.get("periodo"));
        String tipo = String.valueOf(obj.get("tipo"));
        String tipoAct = obj.get("tipoActividad") == null ? null : String.valueOf(obj.get("tipoActividad"));
        LocalDateTime from = inicioPeriodo(periodo);
        Stats s = stats(userId, from, tipoAct);
        double actual = switch (tipo) {
            case "TIEMPO_MIN" -> s.duracionS / 60.0;
            case "SESIONES", "FRECUENCIA" -> s.sesiones;
            default -> s.distanciaM / 1000.0;
        };
        double meta = num(obj.get("valor"), 1);
        Map<String, Object> p = new HashMap<>();
        p.put("actual", actual);
        p.put("cumplimiento", meta <= 0 ? 0 : Math.min(100, (actual / meta) * 100));
        return p;
    }

    private Map<String, Object> progresoPlan(Long userId, Map<String, Object> plan) {
        String tipoAct = String.valueOf(plan.get("tipoActividad"));
        Stats s = stats(userId, inicioPeriodo("SEMANA"), tipoAct);
        Map<String, Object> p = new HashMap<>();
        p.put("sesionesHechas", s.sesiones);
        p.put("kmHechos", s.distanciaM / 1000.0);
        double kmMeta = num(plan.get("kmSemana"), 1);
        int sesMeta = (int) num(plan.get("sesionesSemana"), 1);
        p.put("cumplimientoKm", kmMeta <= 0 ? 0 : Math.min(100, (s.distanciaM / 1000.0 / kmMeta) * 100));
        p.put("cumplimientoSesiones", sesMeta <= 0 ? 0 : Math.min(100, (s.sesiones * 100.0 / sesMeta)));
        return p;
    }

    private double progresoReto(Long userId, Map<String, Object> reto) {
        Timestamp alta = (Timestamp) reto.get("fechaAltaRaw");
        Timestamp fin = (Timestamp) reto.get("fechaFinRaw");
        LocalDateTime from = alta == null ? LocalDateTime.now().minusDays(30) : alta.toLocalDateTime();
        String tipo = String.valueOf(reto.get("metrica"));
        Stats s = statsBetween(userId, from, fin == null ? LocalDateTime.now().plusYears(1) : fin.toLocalDateTime(), null);
        return switch (tipo) {
            case "TIEMPO_MIN" -> s.duracionS / 60.0;
            case "SESIONES" -> s.sesiones;
            default -> s.distanciaM / 1000.0;
        };
    }

    private Stats stats(Long userId, LocalDateTime from, String tipoAct) {
        return statsBetween(userId, from, LocalDateTime.now().plusDays(1), tipoAct);
    }

    private Stats statsBetween(Long userId, LocalDateTime from, LocalDateTime to, String tipoAct) {
        List<Map<String, Object>> rows;
        if (tipoAct == null || tipoAct.isBlank()) {
            rows = jdbc.queryForList(
                    """
                    SELECT COALESCE(SUM(distancia_m),0) d, COALESCE(SUM(duracion_s),0) t, COUNT(*) n
                    FROM actividades WHERE usuario_id = ? AND fecha_inicio >= ? AND fecha_inicio < ?
                    """,
                    userId, Timestamp.valueOf(from), Timestamp.valueOf(to));
        } else {
            rows = jdbc.queryForList(
                    """
                    SELECT COALESCE(SUM(distancia_m),0) d, COALESCE(SUM(duracion_s),0) t, COUNT(*) n
                    FROM actividades WHERE usuario_id = ? AND fecha_inicio >= ? AND fecha_inicio < ? AND tipo = ?
                    """,
                    userId, Timestamp.valueOf(from), Timestamp.valueOf(to), tipoAct);
        }
        Map<String, Object> r = rows.get(0);
        return new Stats(
                ((Number) r.get("d")).doubleValue(),
                ((Number) r.get("t")).intValue(),
                ((Number) r.get("n")).intValue());
    }

    private void requireGrupo(Long id) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM grupos WHERE id = ?", Integer.class, id);
        if (n == null || n == 0) {
            throw new ApiException("Grupo no encontrado");
        }
    }

    private void requireMiembroGrupo(Long userId, Long grupoId) {
        requireGrupo(grupoId);
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

    private Map<String, Object> objetivoDto(Map<String, Object> row) {
        Map<String, Object> o = new HashMap<>();
        o.put("id", row.get("id"));
        o.put("tipo", row.get("tipo"));
        o.put("periodo", row.get("periodo"));
        o.put("valor", row.get("valor"));
        o.put("tipoActividad", row.get("tipo_actividad"));
        o.put("activo", bool(row.get("activo")));
        return o;
    }

    private Map<String, Object> planDto(Map<String, Object> row) {
        Map<String, Object> p = new HashMap<>();
        p.put("id", row.get("id"));
        p.put("nombre", row.get("nombre"));
        p.put("tipoActividad", row.get("tipo_actividad"));
        p.put("sesionesSemana", row.get("sesiones_semana"));
        p.put("kmSemana", row.get("km_semana"));
        p.put("notas", row.get("notas"));
        p.put("activo", bool(row.get("activo")));
        return p;
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

    private static LocalDateTime inicioPeriodo(String periodo) {
        LocalDate today = LocalDate.now(ZONE);
        if ("MES".equalsIgnoreCase(periodo) || "mes".equalsIgnoreCase(periodo)) {
            return today.withDayOfMonth(1).atStartOfDay();
        }
        if ("ANIO".equalsIgnoreCase(periodo) || "año".equalsIgnoreCase(periodo) || "anio".equalsIgnoreCase(periodo)) {
            return today.withDayOfYear(1).atStartOfDay();
        }
        return today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
    }

    private static String sugerenciaTexto(String tipo, double distMedia) {
        double km = distMedia / 1000.0;
        return "Suele encajarte " + tipo.toLowerCase() + " alrededor de "
                + String.format(java.util.Locale.US, "%.1f", km) + " km. Te proponemos rutas similares y eventos de esa disciplina.";
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
