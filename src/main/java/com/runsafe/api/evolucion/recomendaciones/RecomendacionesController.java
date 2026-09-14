package com.runsafe.api.evolucion.recomendaciones;

import com.runsafe.api.evolucion.EvolucionStore;
import com.runsafe.api.security.AuthUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Recomendaciones")
public class RecomendacionesController {

    private final EvolucionStore store;
    private final AuthUser authUser;

    public RecomendacionesController(EvolucionStore store, AuthUser authUser) {
        this.store = store;
        this.authUser = authUser;
    }

    @GetMapping("/recomendaciones")
    public Map<String, Object> recomendaciones() {
        return store.recomendaciones(authUser.current().getId());
    }
}
