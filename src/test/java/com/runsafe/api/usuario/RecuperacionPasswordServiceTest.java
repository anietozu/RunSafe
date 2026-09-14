package com.runsafe.api.usuario;

import com.runsafe.api.common.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RecuperacionPasswordServiceTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final RecuperacionPasswordRepository recuperaciones = mock(RecuperacionPasswordRepository.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private final EmailSender email = mock(EmailSender.class);
    private final RecuperacionPasswordService service =
            new RecuperacionPasswordService(usuarios, recuperaciones, encoder, email);
    private Usuario user;

    @BeforeEach
    void setup() {
        user = new Usuario();
        user.setId(3L);
        user.setActivo(true);
        user.setLogin("runner");
        user.setEmail("runner@runsafe.app");
        user.setNombre("Alex");
        user.setPassword(encoder.encode("antigua123"));
        when(usuarios.findByLoginIgnoreCase("runner")).thenReturn(Optional.of(user));
        when(recuperaciones.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void pedirCodigoEnviaEmailYNoGuardaElCodigoEnClaro() {
        service.pedirCodigo("runner");
        verify(email).enviar(eq("runner@runsafe.app"), contains("Código RunSafe"), contains("código"));
        verify(recuperaciones).save(argThat(row -> row.getCodigoHash() != null && !row.getCodigoHash().matches("\\d{6}")));
    }

    @Test
    void usuarioDesconocidoNoEnviaEmail() {
        when(usuarios.findByLoginIgnoreCase("nadie")).thenReturn(Optional.empty());
        ApiException ex = assertThrows(ApiException.class, () -> service.pedirCodigo("nadie"));
        assertEquals("No hay ninguna cuenta con ese usuario", ex.getMessage());
        verify(email, never()).enviar(any(), any(), any());
    }

    @Test
    void codigoIncorrectoNoCambiaLaPassword() {
        RecuperacionPassword row = pendiente("123456");
        when(recuperaciones.findFirstByUsuarioIdAndUsadaFalseOrderByFechaAltaDesc(3L)).thenReturn(Optional.of(row));
        String hash = user.getPassword();
        assertThrows(ApiException.class, () -> service.restablecer(new RestablecerPasswordRequest("runner", "000000", "nueva123")));
        assertEquals(hash, user.getPassword());
        assertEquals(1, row.getIntentos());
        verify(usuarios, never()).save(any());
    }

    @Test
    void codigoCorrectoCambiaLaPasswordYMarcaUsado() {
        RecuperacionPassword row = pendiente("654321");
        when(recuperaciones.findFirstByUsuarioIdAndUsadaFalseOrderByFechaAltaDesc(3L)).thenReturn(Optional.of(row));
        service.restablecer(new RestablecerPasswordRequest("runner", "654321", "nueva123"));
        assertTrue(encoder.matches("nueva123", user.getPassword()));
        assertTrue(row.isUsada());
        verify(usuarios).save(user);
    }

    private RecuperacionPassword pendiente(String codigo) {
        RecuperacionPassword row = new RecuperacionPassword();
        row.setUsuario(user);
        row.setCodigoHash(encoder.encode(codigo));
        row.setCaduca(LocalDateTime.now().plusMinutes(10));
        row.setFechaAlta(LocalDateTime.now());
        row.setUsada(false);
        row.setIntentos(0);
        return row;
    }
}
