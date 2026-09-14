package com.runsafe.api.evolucion.dispositivos;

import com.runsafe.api.evolucion.EvolucionStore;
import com.runsafe.api.security.AuthUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Dispositivos")
public class DispositivosController {

    private final EvolucionStore store;
    private final AuthUser authUser;

    public DispositivosController(EvolucionStore store, AuthUser authUser) {
        this.store = store;
        this.authUser = authUser;
    }

    @GetMapping("/dispositivos")
    public List<Map<String, Object>> listar() {
        return store.dispositivos(authUser.current().getId());
    }

    @PostMapping("/dispositivos")
    public Map<String, Object> crear(@RequestBody Map<String, Object> body) {
        return store.crearDispositivo(authUser.current().getId(), body);
    }

    @PutMapping("/dispositivos/{id}")
    public Map<String, Object> actualizar(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return store.actualizarDispositivo(authUser.current().getId(), id, body);
    }

    @PostMapping("/dispositivos/{id}")
    public Map<String, Object> actualizarPost(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return store.actualizarDispositivo(authUser.current().getId(), id, body);
    }

    @DeleteMapping("/dispositivos/{id}")
    public Map<String, Boolean> borrar(@PathVariable Long id) {
        store.borrarDispositivo(authUser.current().getId(), id);
        return Map.of("ok", true);
    }

    @PostMapping("/dispositivos/{id}/eliminar")
    public Map<String, Boolean> borrarPost(@PathVariable Long id) {
        store.borrarDispositivo(authUser.current().getId(), id);
        return Map.of("ok", true);
    }
}
