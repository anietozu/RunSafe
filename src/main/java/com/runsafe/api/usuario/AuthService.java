package com.runsafe.api.usuario;

import com.runsafe.api.common.ApiException;
import com.runsafe.api.security.JwtService;
import com.runsafe.api.seguridad.ConfiguracionSeguridad;
import com.runsafe.api.seguridad.ConfiguracionSeguridadRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final ConfiguracionSeguridadRepository configs;

    public AuthService(
            UsuarioRepository usuarios,
            PasswordEncoder encoder,
            JwtService jwtService,
            ConfiguracionSeguridadRepository configs
    ) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.configs = configs;
    }

    @Transactional
    public AuthResponse registrar(RegistroRequest req) {
        String login = req.login().trim().toLowerCase();
        if (login.contains("@")) {
            throw new ApiException("El usuario no puede ser un email");
        }
        if (usuarios.existsByEmailIgnoreCase(req.email())) {
            throw new ApiException("El email ya está registrado");
        }
        if (usuarios.existsByLoginIgnoreCase(login)) {
            throw new ApiException("El login ya está en uso");
        }
        Usuario u = new Usuario();
        u.setNombre(req.nombre());
        u.setApellidos(req.apellidos());
        u.setEmail(req.email().toLowerCase());
        u.setLogin(login);
        u.setPassword(encoder.encode(req.password()));
        u.setTelefono(req.telefono() == null ? null : req.telefono().trim());
        u.setActivo(true);
        u.setFechaAlta(LocalDateTime.now());
        usuarios.save(u);

        ConfiguracionSeguridad cfg = new ConfiguracionSeguridad();
        cfg.setUsuario(u);
        cfg.setDeteccionCaidas(true);
        cfg.setGpsDuranteActividad(true);
        configs.save(cfg);

        return new AuthResponse(jwtService.generate(u.getId(), u.getLogin()), UsuarioResponse.from(u));
    }

    public AuthResponse login(LoginRequest req) {
        String id = req.identificador().trim();
        Usuario u = usuarios.findByLoginIgnoreCase(id)
                .or(() -> usuarios.findByEmailIgnoreCase(id))
                .or(() -> usuarios.findAllByTelefonoNorm(normPhone(id)).stream().findFirst())
                .orElseThrow(() -> new ApiException("Credenciales incorrectas"));
        if (!Boolean.TRUE.equals(u.getActivo()) || !encoder.matches(req.password(), u.getPassword())) {
            throw new ApiException("Credenciales incorrectas");
        }
        return new AuthResponse(jwtService.generate(u.getId(), u.getLogin()), UsuarioResponse.from(u));
    }

    @Transactional
    public void actualizarPassword(Long userId, CambiarPasswordRequest req) {
        Usuario u = usuarios.findById(userId)
                .orElseThrow(() -> new ApiException("Cuenta no disponible"));
        if (!Boolean.TRUE.equals(u.getActivo())) {
            throw new ApiException("La cuenta no está activa");
        }
        if (!encoder.matches(req.passwordActual(), u.getPassword())) {
            throw new ApiException("La contraseña actual no es correcta");
        }
        if (req.passwordNueva().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new ApiException("La contraseña nueva no puede superar 72 bytes UTF-8");
        }
        u.setPassword(encoder.encode(req.passwordNueva()));
        usuarios.save(u);
    }

    static String normPhone(String value) {
        return value == null ? "" : value.replaceAll("[\\s\\-()]", "");
    }
}
