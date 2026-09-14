package com.runsafe.api.usuario;

import com.runsafe.api.common.ApiException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class RecuperacionPasswordService {

    private static final int CADUCIDAD_MIN = 10;
    private static final int MAX_INTENTOS = 5;
    private static final int REENVIO_SEGUNDOS = 60;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarios;
    private final RecuperacionPasswordRepository recuperaciones;
    private final PasswordEncoder encoder;
    private final EmailSender email;

    public RecuperacionPasswordService(
            UsuarioRepository usuarios,
            RecuperacionPasswordRepository recuperaciones,
            PasswordEncoder encoder,
            EmailSender email
    ) {
        this.usuarios = usuarios;
        this.recuperaciones = recuperaciones;
        this.encoder = encoder;
        this.email = email;
    }

    @Transactional
    public void pedirCodigo(String login) {
        Usuario u = buscarPorLogin(login);
        recuperaciones.findFirstByUsuarioIdAndUsadaFalseOrderByFechaAltaDesc(u.getId()).ifPresent(prev -> {
            if (prev.getFechaAlta() != null && prev.getFechaAlta().isAfter(LocalDateTime.now().minusSeconds(REENVIO_SEGUNDOS))) {
                throw new ApiException("Espera un minuto para pedir otro código");
            }
        });
        recuperaciones.invalidarPendientes(u.getId());
        String codigo = String.format("%06d", RANDOM.nextInt(1_000_000));
        RecuperacionPassword row = new RecuperacionPassword();
        row.setUsuario(u);
        row.setCodigoHash(encoder.encode(codigo));
        row.setCaduca(LocalDateTime.now().plusMinutes(CADUCIDAD_MIN));
        row.setIntentos(0);
        row.setUsada(false);
        row.setFechaAlta(LocalDateTime.now());
        recuperaciones.save(row);
        try {
            email.enviar(
                    u.getEmail(),
                    "Código RunSafe para recuperar tu contraseña",
                    "Hola " + (u.getNombre() == null ? u.getLogin() : u.getNombre()) + ",\n\n"
                            + "Tu código para restablecer la contraseña es: " + codigo + "\n\n"
                            + "Caduca en 10 minutos. Si no has pedido este código, ignora este correo.\n\n"
                            + "RunSafe"
            );
        } catch (RuntimeException e) {
            row.setUsada(true);
            recuperaciones.save(row);
            throw e;
        }
    }

    @Transactional
    public void restablecer(RestablecerPasswordRequest req) {
        Usuario u = buscarPorLogin(req.login());
        RecuperacionPassword row = recuperaciones.findFirstByUsuarioIdAndUsadaFalseOrderByFechaAltaDesc(u.getId())
                .orElseThrow(() -> new ApiException("Pide un código antes de cambiar la contraseña"));
        if (row.getCaduca() == null || row.getCaduca().isBefore(LocalDateTime.now())) {
            row.setUsada(true);
            recuperaciones.save(row);
            throw new ApiException("El código ha caducado. Pide uno nuevo.");
        }
        if (row.getIntentos() >= MAX_INTENTOS) {
            row.setUsada(true);
            recuperaciones.save(row);
            throw new ApiException("Demasiados intentos. Pide un código nuevo.");
        }
        if (!encoder.matches(req.codigo(), row.getCodigoHash())) {
            row.setIntentos(row.getIntentos() + 1);
            recuperaciones.save(row);
            throw new ApiException("El código no es correcto");
        }
        if (req.passwordNueva().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException("La contraseña nueva no puede superar 72 bytes UTF-8");
        }
        u.setPassword(encoder.encode(req.passwordNueva()));
        usuarios.save(u);
        row.setUsada(true);
        recuperaciones.save(row);
        recuperaciones.invalidarPendientes(u.getId());
    }

    private Usuario buscarPorLogin(String login) {
        String id = login == null ? "" : login.trim().toLowerCase();
        if (id.isBlank()) {
            throw new ApiException("Indica tu usuario");
        }
        Usuario u = usuarios.findByLoginIgnoreCase(id)
                .filter(x -> Boolean.TRUE.equals(x.getActivo()))
                .orElseThrow(() -> new ApiException("No hay ninguna cuenta con ese usuario"));
        if (u.getEmail() == null || u.getEmail().isBlank()) {
            throw new ApiException("Esta cuenta no tiene email");
        }
        return u;
    }
}
