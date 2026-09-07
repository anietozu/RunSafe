package com.runsafe.api.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record ActualizarPerfilRequest(
        @Size(max = 45) String nombre,
        @Size(max = 100) String apellidos,
        @Email @Size(max = 255) String email,
        @Size(max = 20) String telefono
) {}
