package com.runsafe.api.evolucion.objetivos;

import com.runsafe.api.evolucion.EvolucionStore;
import com.runsafe.api.security.AuthUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@Tag(name = "Objetivos y planes")
public class ObjetivosController {

    private final EvolucionStore store;
    private final AuthUser authUser;

    public ObjetivosController(EvolucionStore store, AuthUser authUser) {
        this.store = store;
        this.authUser = authUser;
    }

    @GetMapping("/objetivos")
    public List<Map<String, Object>> objetivos() {
        return store.objetivos(authUser.current().getId());
    }

    @PostMapping("/objetivos")
    public Map<String, Object> crear(@RequestBody Map<String, Object> body) {
        return store.crearObjetivo(authUser.current().getId(), body);
    }

    @DeleteMapping("/objetivos/{id}")
    public Map<String, Boolean> borrar(@PathVariable Long id) {
        store.borrarObjetivo(authUser.current().getId(), id);
        return Map.of("ok", true);
    }

    @PostMapping("/objetivos/{id}/eliminar")
    public Map<String, Boolean> borrarPost(@PathVariable Long id) {
        store.borrarObjetivo(authUser.current().getId(), id);
        return Map.of("ok", true);
    }

    @GetMapping("/planes")
    public List<Map<String, Object>> planes() {
        return store.planes(authUser.current().getId());
    }

    @PostMapping("/planes")
    public Map<String, Object> crearPlan(@RequestBody Map<String, Object> body) {
        return store.crearPlan(authUser.current().getId(), body);
    }

    @DeleteMapping("/planes/{id}")
    public Map<String, Boolean> borrarPlan(@PathVariable Long id) {
        store.borrarPlan(authUser.current().getId(), id);
        return Map.of("ok", true);
    }

    @PostMapping("/planes/{id}/eliminar")
    public Map<String, Boolean> borrarPlanPost(@PathVariable Long id) {
        store.borrarPlan(authUser.current().getId(), id);
        return Map.of("ok", true);
    }
}
