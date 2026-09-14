package com.runsafe.api.usuario;

import com.runsafe.api.security.*;
import com.runsafe.api.seguridad.ConfiguracionSeguridadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AuthController.class, com.runsafe.api.common.HealthController.class})
@Import({SecurityConfig.class, JwtAuthFilter.class, AuthUser.class, AuthService.class})
class PasswordSecurityTest {
    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;
    @MockBean UsuarioRepository usuarios;
    @MockBean JwtService jwt;
    @MockBean ConfiguracionSeguridadRepository configs;
    @MockBean RecuperacionPasswordService recuperacion;
    Usuario user;

    @BeforeEach
    void setup() {
        user = new Usuario();
        user.setId(7L);
        user.setLogin("runner");
        user.setActivo(true);
        user.setPassword(encoder.encode("actual123"));
        when(jwt.parseUserId("valid-token")).thenReturn(7L);
        when(jwt.parseUserId("invalid-token")).thenThrow(new IllegalArgumentException("Invalid JWT"));
        when(usuarios.findById(7L)).thenReturn(Optional.of(user));
    }

    @Test
    void phoneRecoveryIsNotPublicAndCannotMutatePasswords() throws Exception {
        mvc.perform(put("/api/auth/password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"telefono\":\"600000000\",\"password\":\"hacked123\"}"))
                .andExpect(status().isUnauthorized());
        verify(usuarios, never()).save(any());
    }

    @Test
    void changeRequiresAuthentication() throws Exception {
        mvc.perform(put("/api/usuarios/me/password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"passwordActual\":\"actual123\",\"passwordNueva\":\"nueva123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void incorrectCurrentPasswordDoesNotChangeHash() throws Exception {
        String hash = user.getPassword();
        change("incorrecta", "nueva123").andExpect(status().isBadRequest());
        assertEquals(hash, user.getPassword());
        verify(usuarios, never()).save(any());
    }

    @Test
    void currentPasswordIsCheckedAndNewPasswordIsHashed() throws Exception {
        change("actual123", "nueva123").andExpect(status().isOk());
        assertTrue(encoder.matches("nueva123", user.getPassword()));
        assertFalse(encoder.matches("actual123", user.getPassword()));
        assertNotEquals("nueva123", user.getPassword());
        verify(usuarios).save(user);
    }

    @Test
    void rejectsShortBlankAndOverlongUtf8Passwords() throws Exception {
        change("actual123", "123").andExpect(status().isBadRequest());
        change("actual123", "      ").andExpect(status().isBadRequest());
        change("actual123", "é".repeat(40)).andExpect(status().isBadRequest());
        verify(usuarios, never()).save(any());
    }

    @Test
    void sameTokenStopsWorkingAfterUserIsDeactivated() throws Exception {
        mvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk());
        user.setActivo(false);
        mvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isUnauthorized());
        change("actual123", "nueva123").andExpect(status().isUnauthorized());
    }

    @Test
    void missingUserAndInvalidTokenAreUnauthorized() throws Exception {
        when(usuarios.findById(7L)).thenReturn(Optional.empty());
        mvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginAndRegistrationRemainPublicButOtherAuthRoutesDoNot() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/password/codigo").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/password/restablecer").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/auth/anything")).andExpect(status().isUnauthorized());
    }

    @Test
    void healthRemainsPublic() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions change(String current, String next) throws Exception {
        return mvc.perform(put("/api/usuarios/me/password").header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"passwordActual\":\"" + current + "\",\"passwordNueva\":\"" + next + "\"}"));
    }
}
