package com.runsafe.api.social;

import com.runsafe.api.actividad.Actividad;
import com.runsafe.api.actividad.ActividadRepository;
import com.runsafe.api.actividad.PuntoDto;
import com.runsafe.api.common.ApiException;
import com.runsafe.api.security.AuthUser;
import com.runsafe.api.usuario.Usuario;
import com.runsafe.api.usuario.UsuarioRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/social")
@Tag(name = "Social")
public class SocialController {

    private final PublicacionRepository publicaciones;
    private final ComentarioRepository comentarios;
    private final MeGustaRepository likes;
    private final SeguidorRepository seguidores;
    private final ActividadRepository actividades;
    private final UsuarioRepository usuarios;
    private final AuthUser authUser;

    public SocialController(
            PublicacionRepository publicaciones,
            ComentarioRepository comentarios,
            MeGustaRepository likes,
            SeguidorRepository seguidores,
            ActividadRepository actividades,
            UsuarioRepository usuarios,
            AuthUser authUser
    ) {
        this.publicaciones = publicaciones;
        this.comentarios = comentarios;
        this.likes = likes;
        this.seguidores = seguidores;
        this.actividades = actividades;
        this.usuarios = usuarios;
        this.authUser = authUser;
    }

    @GetMapping("/feed")
    @Transactional
    public List<Map<String, Object>> feed(@RequestParam(defaultValue = "descubrir") String tab) {
        Usuario me = authUser.current();
        sincronizarRutasPublicadas();
        List<Publicacion> all = publicaciones.findFeed().stream()
                .filter(p -> p.getActividad() != null)
                .toList();
        if ("siguiendo".equalsIgnoreCase(tab)) {
            List<Long> ids = seguidores.findBySeguidorId(me.getId()).stream()
                    .map(s -> s.getSeguido().getId()).toList();
            all = all.stream().filter(p -> ids.contains(p.getUsuario().getId())).toList();
        }
        return all.stream().map(p -> toPost(p, me.getId())).toList();
    }

    @GetMapping("/usuarios")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> usuarios(@RequestParam(defaultValue = "") String q) {
        Usuario me = authUser.current();
        String query = q == null ? "" : q.trim();
        List<Usuario> found = query.isBlank()
                ? usuarios.findOtros(me.getId())
                : usuarios.buscarOtros(me.getId(), query);
        return found.stream().limit(40).map(u -> toUser(u, me.getId())).toList();
    }

    @GetMapping("/siguiendo")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> siguiendo() {
        Usuario me = authUser.current();
        return seguidores.findBySeguidorId(me.getId()).stream()
                .map(s -> toUser(s.getSeguido(), me.getId()))
                .toList();
    }

    @PostMapping("/publicaciones")
    @Transactional
    public Map<String, Object> publicar(@RequestBody Map<String, Object> body) {
        Usuario me = authUser.current();
        Publicacion p = new Publicacion();
        p.setUsuario(me);
        p.setTexto(String.valueOf(body.getOrDefault("texto", "")));
        p.setFecha(LocalDateTime.now());
        Object actId = body.get("actividadId");
        if (actId != null && !String.valueOf(actId).isBlank() && !"null".equals(String.valueOf(actId))) {
            Long id = Long.valueOf(String.valueOf(actId).replace(".0", ""));
            Actividad a = actividades.findWithPuntosById(id)
                    .orElseThrow(() -> new ApiException("Actividad no encontrada"));
            if (!a.getUsuario().getId().equals(me.getId())) {
                throw new ApiException("Solo puedes publicar tus actividades");
            }
            if (publicaciones.existsByUsuarioIdAndActividadId(me.getId(), id)) {
                throw new ApiException("Esta ruta ya está publicada");
            }
            a.setPublica(true);
            p.setActividad(a);
        }
        publicaciones.save(p);
        return toPost(p, me.getId());
    }

    @PostMapping("/publicaciones/{id}/like")
    @Transactional
    public Map<String, Object> like(@PathVariable Long id) {
        Usuario me = authUser.current();
        Publicacion p = publicaciones.findDetailedById(id)
                .orElseThrow(() -> new ApiException("Publicación no encontrada"));
        likes.findByPublicacionIdAndUsuarioId(id, me.getId()).ifPresentOrElse(likes::delete, () -> {
            MeGusta like = new MeGusta();
            like.setPublicacion(p);
            like.setUsuario(me);
            like.setFecha(LocalDateTime.now());
            likes.save(like);
        });
        likes.flush();
        return toPost(p, me.getId());
    }

    @GetMapping("/publicaciones/{id}/comentarios")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarComentarios(@PathVariable Long id) {
        publicaciones.findById(id).orElseThrow(() -> new ApiException("Publicación no encontrada"));
        return comentarios.findByPublicacionIdOrderByFechaAsc(id).stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("texto", c.getTexto());
            m.put("fecha", c.getFecha());
            m.put("usuarioId", c.getUsuario().getId());
            m.put("nombre", c.getUsuario().getNombre());
            m.put("login", c.getUsuario().getLogin());
            return m;
        }).toList();
    }

    @PostMapping("/publicaciones/{id}/comentarios")
    @Transactional
    public Map<String, Object> comentar(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Usuario me = authUser.current();
        Publicacion p = publicaciones.findDetailedById(id)
                .orElseThrow(() -> new ApiException("Publicación no encontrada"));
        String texto = body.getOrDefault("texto", "").trim();
        if (texto.isBlank()) {
            throw new ApiException("Escribe un comentario");
        }
        Comentario c = new Comentario();
        c.setPublicacion(p);
        c.setUsuario(me);
        c.setTexto(texto);
        c.setFecha(LocalDateTime.now());
        comentarios.save(c);
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId());
        m.put("texto", c.getTexto());
        m.put("fecha", c.getFecha());
        m.put("usuarioId", me.getId());
        m.put("nombre", me.getNombre());
        m.put("login", me.getLogin());
        return m;
    }

    @PostMapping("/seguir/{userId}")
    @Transactional
    public Map<String, Boolean> seguir(@PathVariable Long userId) {
        Usuario me = authUser.current();
        if (me.getId().equals(userId)) {
            throw new ApiException("No puedes seguirte a ti mismo");
        }
        Usuario other = usuarios.findById(userId).orElseThrow(() -> new ApiException("Usuario no encontrado"));
        if (!seguidores.existsBySeguidorIdAndSeguidoId(me.getId(), other.getId())) {
            Seguidor s = new Seguidor();
            s.setSeguidor(me);
            s.setSeguido(other);
            s.setFecha(LocalDateTime.now());
            seguidores.save(s);
        }
        return Map.of("siguiendo", true);
    }

    @DeleteMapping("/seguir/{userId}")
    @Transactional
    public Map<String, Boolean> dejarDeSeguir(@PathVariable Long userId) {
        Usuario me = authUser.current();
        seguidores.deleteBySeguidorIdAndSeguidoId(me.getId(), userId);
        return Map.of("siguiendo", false);
    }

    private void sincronizarRutasPublicadas() {
        for (Actividad a : actividades.findByPublicaTrueOrderByFechaInicioDesc()) {
            Usuario autor = a.getUsuario();
            if (autor == null || publicaciones.existsByUsuarioIdAndActividadId(autor.getId(), a.getId())) {
                continue;
            }
            Publicacion p = new Publicacion();
            p.setUsuario(autor);
            p.setActividad(a);
            p.setFecha(a.getFechaFin() == null ? a.getFechaInicio() : a.getFechaFin());
            double km = a.getDistanciaM() == null ? 0 : a.getDistanciaM() / 1000.0;
            p.setTexto("He completado " + String.format(java.util.Locale.US, "%.2f", km) + " km.");
            publicaciones.save(p);
        }
        publicaciones.flush();
    }

    private Map<String, Object> toUser(Usuario u, Long meId) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.getId());
        m.put("login", u.getLogin());
        m.put("nombre", u.getNombre());
        m.put("apellidos", u.getApellidos());
        m.put("email", u.getEmail());
        m.put("siguiendo", seguidores.existsBySeguidorIdAndSeguidoId(meId, u.getId()));
        return m;
    }

    private Map<String, Object> toPost(Publicacion p, Long meId) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", p.getId());
        m.put("texto", p.getTexto());
        m.put("fecha", p.getFecha());
        m.put("usuarioId", p.getUsuario().getId());
        m.put("nombre", p.getUsuario().getNombre());
        m.put("login", p.getUsuario().getLogin());
        m.put("likes", likes.countByPublicacionId(p.getId()));
        m.put("comentarios", comentarios.countByPublicacionId(p.getId()));
        m.put("liked", likes.findByPublicacionIdAndUsuarioId(p.getId(), meId).isPresent());
        m.put("siguiendo", seguidores.existsBySeguidorIdAndSeguidoId(meId, p.getUsuario().getId()));
        m.put("esMia", p.getUsuario().getId().equals(meId));
        if (p.getActividad() != null) {
            Actividad a = actividades.findWithPuntosById(p.getActividad().getId()).orElse(p.getActividad());
            m.put("actividadId", a.getId());
            m.put("tipo", a.getTipo());
            m.put("distanciaM", a.getDistanciaM());
            m.put("duracionS", a.getDuracionS());
            m.put("ruta", a.getPuntos() == null ? List.of() : a.getPuntos().stream()
                    .map(pt -> new PuntoDto(pt.getLatitud(), pt.getLongitud(), pt.getAltitud(), pt.getVelocidad(), pt.getTimestampPunto()))
                    .toList());
        }
        return m;
    }
}
