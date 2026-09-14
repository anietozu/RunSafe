package com.runsafe.api.evolucion.sync;

import com.runsafe.api.evolucion.EvolucionStore;
import com.runsafe.api.security.AuthUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/sync")
@Tag(name = "Sincronización")
public class SyncController {

    private final EvolucionStore store;
    private final AuthUser authUser;

    public SyncController(EvolucionStore store, AuthUser authUser) {
        this.store = store;
        this.authUser = authUser;
    }

    @GetMapping("/estado")
    public Map<String, Object> estado() {
        return store.estadoSync(authUser.current().getId());
    }

    @PostMapping("/heartbeat")
    public Map<String, Object> heartbeat(@RequestBody(required = false) Map<String, Object> body) {
        int ops = 0;
        if (body != null && body.get("operaciones") instanceof Number n) {
            ops = n.intValue();
        }
        return store.heartbeat(authUser.current().getId(), ops);
    }
}
