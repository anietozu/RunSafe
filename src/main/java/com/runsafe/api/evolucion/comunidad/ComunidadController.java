package com.runsafe.api.evolucion.comunidad;

import com.runsafe.api.evolucion.EvolucionStore;
import com.runsafe.api.security.AuthUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Comunidad")
public class ComunidadController {

    private final EvolucionStore store;
    private final AuthUser authUser;

    public ComunidadController(EvolucionStore store, AuthUser authUser) {
        this.store = store;
        this.authUser = authUser;
    }

    @GetMapping("/grupos")
    public List<Map<String, Object>> grupos() {
        return store.grupos(authUser.current().getId());
    }

    @PostMapping("/grupos")
    public Map<String, Object> crearGrupo(@RequestBody Map<String, Object> body) {
        return store.crearGrupo(authUser.current().getId(), body);
    }

    @PostMapping("/grupos/{id}/unirse")
    public Map<String, Boolean> unirseGrupo(@PathVariable Long id) {
        store.unirseGrupo(authUser.current().getId(), id);
        return Map.of("ok", true);
    }

    @PostMapping("/grupos/{id}/salir")
    public Map<String, Boolean> salirGrupo(@PathVariable Long id) {
        store.salirGrupo(authUser.current().getId(), id);
        return Map.of("ok", true);
    }

    @GetMapping("/grupos/{id}")
    public Map<String, Object> grupo(@PathVariable Long id) {
        return store.grupoDetalle(authUser.current().getId(), id);
    }

    @GetMapping("/grupos/{id}/miembros")
    public List<Map<String, Object>> miembros(@PathVariable Long id) {
        return store.miembrosGrupo(id);
    }

    @GetMapping("/grupos/{id}/mensajes")
    public List<Map<String, Object>> mensajesGrupo(@PathVariable Long id) {
        return store.mensajesGrupo(authUser.current().getId(), id);
    }

    @PostMapping("/grupos/{id}/mensajes")
    public Map<String, Object> enviarMensajeGrupo(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return store.enviarMensajeGrupo(authUser.current().getId(), id, body);
    }

    @GetMapping("/retos")
    public List<Map<String, Object>> retos() {
        return store.retos(authUser.current().getId());
    }

    @PostMapping("/retos")
    public Map<String, Object> crearReto(@RequestBody Map<String, Object> body) {
        return store.crearReto(authUser.current().getId(), body);
    }

    @PostMapping("/retos/{id}/unirse")
    public Map<String, Boolean> unirseReto(@PathVariable Long id) {
        store.unirseReto(authUser.current().getId(), id);
        return Map.of("ok", true);
    }

    @PostMapping("/retos/{id}/salir")
    public Map<String, Boolean> salirReto(@PathVariable Long id) {
        store.salirReto(authUser.current().getId(), id);
        return Map.of("ok", true);
    }

    @GetMapping("/retos/{id}/clasificacion")
    public List<Map<String, Object>> clasificacionReto(@PathVariable Long id) {
        return store.clasificacionReto(id);
    }

    @GetMapping("/retos/{id}")
    public Map<String, Object> reto(@PathVariable Long id) {
        return store.retoDetalle(authUser.current().getId(), id);
    }

    @GetMapping("/eventos")
    public List<Map<String, Object>> eventos() {
        return store.eventos(authUser.current().getId());
    }

    @PostMapping("/eventos")
    public Map<String, Object> crearEvento(@RequestBody Map<String, Object> body) {
        return store.crearEvento(authUser.current().getId(), body);
    }

    @PostMapping("/eventos/{id}/unirse")
    public Map<String, Boolean> unirseEvento(@PathVariable Long id) {
        store.unirseEvento(authUser.current().getId(), id);
        return Map.of("ok", true);
    }

    @PostMapping("/eventos/{id}/salir")
    public Map<String, Boolean> salirEvento(@PathVariable Long id) {
        store.salirEvento(authUser.current().getId(), id);
        return Map.of("ok", true);
    }

    @GetMapping("/eventos/{id}")
    public Map<String, Object> evento(@PathVariable Long id) {
        return store.eventoDetalle(authUser.current().getId(), id);
    }

    @GetMapping("/eventos/{id}/mensajes")
    public List<Map<String, Object>> mensajesEvento(@PathVariable Long id) {
        return store.mensajesEvento(authUser.current().getId(), id);
    }

    @PostMapping("/eventos/{id}/mensajes")
    public Map<String, Object> enviarMensajeEvento(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return store.enviarMensajeEvento(authUser.current().getId(), id, body);
    }
}
