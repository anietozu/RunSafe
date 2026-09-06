package com.runsafe.api.usuario;

import com.runsafe.api.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Usuarios")
public class AuthController {

    private final AuthService authService;
    private final AuthUser authUser;
    private final UsuarioRepository usuarios;

    public AuthController(AuthService authService, AuthUser authUser, UsuarioRepository usuarios) {
        this.authService = authService;
        this.authUser = authUser;
        this.usuarios = usuarios;
    }

    @PostMapping("/auth/registro")
    @Operation(summary = "Crear cuenta")
    public AuthResponse registro(@Valid @RequestBody RegistroRequest request) {
        return authService.registrar(request);
    }

    @PostMapping("/auth/login")
    @Operation(summary = "Iniciar sesión con email o login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PutMapping("/auth/password")
    @Operation(summary = "Actualizar contraseña con el email de la cuenta")
    public Map<String, String> actualizarPassword(@Valid @RequestBody RecuperarPasswordRequest request) {
        authService.actualizarPassword(request);
        return Map.of("ok", "true");
    }

    @GetMapping("/usuarios/me")
    public UsuarioResponse me() {
        return UsuarioResponse.from(authUser.current());
    }

    @PutMapping("/usuarios/me")
    public UsuarioResponse actualizar(@RequestBody RegistroRequest request) {
        Usuario u = authUser.current();
        if (request.nombre() != null) {
            u.setNombre(request.nombre());
        }
        if (request.apellidos() != null) {
            u.setApellidos(request.apellidos());
        }
        if (request.telefono() != null) {
            u.setTelefono(request.telefono());
        }
        return UsuarioResponse.from(usuarios.save(u));
    }
}
