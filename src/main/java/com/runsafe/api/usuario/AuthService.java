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
        String login = (req.login() == null || req.login().isBlank()) ? req.email() : req.login();
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
        u.setLogin(login.toLowerCase());
        u.setPassword(encoder.encode(req.password()));
        u.setTelefono(req.telefono());
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
        Usuario u = usuarios.findByEmailIgnoreCase(req.identificador())
                .or(() -> usuarios.findByLoginIgnoreCase(req.identificador()))
                .orElseThrow(() -> new ApiException("Credenciales incorrectas"));
        if (!Boolean.TRUE.equals(u.getActivo()) || !encoder.matches(req.password(), u.getPassword())) {
            throw new ApiException("Credenciales incorrectas");
        }
        return new AuthResponse(jwtService.generate(u.getId(), u.getLogin()), UsuarioResponse.from(u));
    }

    @Transactional
    public void actualizarPassword(RecuperarPasswordRequest req) {
        Usuario u = usuarios.findByEmailIgnoreCase(req.email().trim())
                .orElseThrow(() -> new ApiException("No hay ninguna cuenta con ese email"));
        if (!Boolean.TRUE.equals(u.getActivo())) {
            throw new ApiException("La cuenta no está activa");
        }
        u.setPassword(encoder.encode(req.password()));
        usuarios.save(u);
    }
}
