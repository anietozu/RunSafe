package com.runsafe.api.usuario;

import java.time.LocalDateTime;

public record UsuarioResponse(
        Long id,
        String nombre,
        String apellidos,
        String email,
        String login,
        String telefono,
        Boolean activo,
        Boolean activoUsuario,
        LocalDateTime fechaAlta
) {
    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(
                u.getId(), u.getNombre(), u.getApellidos(), u.getEmail(), u.getLogin(),
                u.getTelefono(), u.getActivo(), u.getActivoUsuario(), u.getFechaAlta());
    }
}
