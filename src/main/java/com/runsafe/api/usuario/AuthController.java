package com.runsafe.api.usuario;

import com.runsafe.api.common.ApiException;
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
    private final RecuperacionPasswordService recuperacion;
    private final AuthUser authUser;
    private final UsuarioRepository usuarios;

    public AuthController(
            AuthService authService,
            RecuperacionPasswordService recuperacion,
            AuthUser authUser,
            UsuarioRepository usuarios
    ) {
        this.authService = authService;
        this.recuperacion = recuperacion;
        this.authUser = authUser;
        this.usuarios = usuarios;
    }

    @PostMapping("/auth/registro")
    @Operation(summary = "Crear cuenta")
    public AuthResponse registro(@Valid @RequestBody RegistroRequest request) {
        return authService.registrar(request);
    }

    @PostMapping("/auth/login")
    @Operation(summary = "Iniciar sesión con usuario, email o teléfono")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/auth/password/codigo")
    @Operation(summary = "Enviar código SMS para recuperar la contraseña")
    public Map<String, String> pedirCodigo(@Valid @RequestBody PedirCodigoRequest request) {
        recuperacion.pedirCodigo(request.telefono());
        return Map.of("ok", "true");
    }

    @PostMapping("/auth/password/restablecer")
    @Operation(summary = "Validar el código SMS y establecer una contraseña nueva")
    public Map<String, String> restablecer(@Valid @RequestBody RestablecerPasswordRequest request) {
        recuperacion.restablecer(request);
        return Map.of("ok", "true");
    }

    @PutMapping("/usuarios/me/password")
    @Operation(summary = "Cambiar contraseña verificando la contraseña actual")
    public Map<String, String> actualizarPassword(@Valid @RequestBody CambiarPasswordRequest request) {
        authService.actualizarPassword(authUser.current().getId(), request);
        return Map.of("ok", "true");
    }

    @GetMapping("/usuarios/me")
    public UsuarioResponse me() {
        return UsuarioResponse.from(authUser.current());
    }

    @PutMapping("/usuarios/me")
    public UsuarioResponse actualizar(@RequestBody ActualizarPerfilRequest request) {
        Usuario u = authUser.current();
        if (request.nombre() != null && !request.nombre().isBlank()) {
            u.setNombre(request.nombre().trim());
        }
        if (request.apellidos() != null) {
            u.setApellidos(request.apellidos().trim());
        }
        if (request.email() != null && !request.email().isBlank()) {
            String email = request.email().trim().toLowerCase();
            if (!email.equalsIgnoreCase(u.getEmail()) && usuarios.existsByEmailIgnoreCase(email)) {
                throw new ApiException("Ese email ya está en uso");
            }
            u.setEmail(email);
        }
        if (request.telefono() != null) {
            u.setTelefono(request.telefono().trim());
        }
        return UsuarioResponse.from(usuarios.save(u));
    }
}
