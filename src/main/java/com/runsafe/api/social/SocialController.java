package com.runsafe.api.social;

import com.runsafe.api.actividad.Actividad;
import com.runsafe.api.actividad.ActividadRepository;
import com.runsafe.api.actividad.PuntoDto;
import com.runsafe.api.common.ApiException;
import com.runsafe.api.security.AuthUser;
import com.runsafe.api.usuario.Usuario;
import com.runsafe.api.usuario.UsuarioRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    public List<Map<String, Object>> feed(@RequestParam(defaultValue = "descubrir") String tab) {
        Usuario me = authUser.current();
        List<Publicacion> all = publicaciones.findAllByOrderByFechaDesc();
        if ("siguiendo".equalsIgnoreCase(tab)) {
            List<Long> ids = seguidores.findBySeguidorId(me.getId()).stream()
                    .map(s -> s.getSeguido().getId()).toList();
            all = all.stream().filter(p -> ids.contains(p.getUsuario().getId())).toList();
        }
        return all.stream().map(p -> toPost(p, me.getId())).toList();
    }

    @PostMapping("/publicaciones")
    public Map<String, Object> publicar(@RequestBody Map<String, Object> body) {
        Usuario me = authUser.current();
        Publicacion p = new Publicacion();
        p.setUsuario(me);
        p.setTexto(String.valueOf(body.getOrDefault("texto", "")));
        p.setFecha(LocalDateTime.now());
        Object actId = body.get("actividadId");
        if (actId != null) {
            Long id = Long.valueOf(actId.toString());
            Actividad a = actividades.findById(id).orElseThrow(() -> new ApiException("Actividad no encontrada"));
            p.setActividad(a);
        }
        publicaciones.save(p);
        return toPost(p, me.getId());
    }

    @PostMapping("/publicaciones/{id}/like")
    public Map<String, Object> like(@PathVariable Long id) {
        Usuario me = authUser.current();
        Publicacion p = publicaciones.findById(id).orElseThrow(() -> new ApiException("Publicación no encontrada"));
        likes.findByPublicacionIdAndUsuarioId(id, me.getId()).ifPresentOrElse(likes::delete, () -> {
            MeGusta like = new MeGusta();
            like.setPublicacion(p);
            like.setUsuario(me);
            likes.save(like);
        });
        return toPost(p, me.getId());
    }

    @PostMapping("/publicaciones/{id}/comentarios")
    public Map<String, Object> comentar(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Usuario me = authUser.current();
        Publicacion p = publicaciones.findById(id).orElseThrow(() -> new ApiException("Publicación no encontrada"));
        Comentario c = new Comentario();
        c.setPublicacion(p);
        c.setUsuario(me);
        c.setTexto(body.getOrDefault("texto", ""));
        comentarios.save(c);
        return toPost(p, me.getId());
    }

    @PostMapping("/seguir/{userId}")
    public Map<String, Boolean> seguir(@PathVariable Long userId) {
        Usuario me = authUser.current();
        Usuario other = usuarios.findById(userId).orElseThrow(() -> new ApiException("Usuario no encontrado"));
        if (seguidores.existsBySeguidorIdAndSeguidoId(me.getId(), other.getId())) {
            return Map.of("siguiendo", true);
        }
        Seguidor s = new Seguidor();
        s.setSeguidor(me);
        s.setSeguido(other);
        seguidores.save(s);
        return Map.of("siguiendo", true);
    }

    private Map<String, Object> toPost(Publicacion p, Long meId) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", p.getId());
        m.put("texto", p.getTexto());
        m.put("fecha", p.getFecha());
        m.put("usuarioId", p.getUsuario().getId());
        m.put("nombre", p.getUsuario().getNombre());
        m.put("likes", likes.countByPublicacionId(p.getId()));
        m.put("comentarios", comentarios.countByPublicacionId(p.getId()));
        m.put("liked", likes.findByPublicacionIdAndUsuarioId(p.getId(), meId).isPresent());
        if (p.getActividad() != null) {
            m.put("tipo", p.getActividad().getTipo());
            m.put("distanciaM", p.getActividad().getDistanciaM());
            m.put("ruta", p.getActividad().getPuntos().stream()
                    .map(pt -> new PuntoDto(pt.getLatitud(), pt.getLongitud(), pt.getAltitud(), pt.getVelocidad(), pt.getTimestampPunto()))
                    .toList());
        }
        return m;
    }
}
