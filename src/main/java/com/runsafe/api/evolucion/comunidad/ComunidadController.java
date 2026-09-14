package com.runsafe.api.evolucion.comunidad;

import com.runsafe.api.evolucion.EvolucionStore;
import com.runsafe.api.security.AuthUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/social/ranking")
    public List<Map<String, Object>> ranking(@RequestParam(defaultValue = "semana") String periodo) {
        return store.ranking(periodo);
    }
}
