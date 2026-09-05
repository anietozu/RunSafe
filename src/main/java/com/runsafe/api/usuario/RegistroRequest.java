package com.runsafe.api.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @NotBlank @Size(max = 45) String nombre,
        @Size(max = 100) String apellidos,
        @NotBlank @Email String email,
        @Size(max = 255) String login,
        @NotBlank @Size(min = 6, max = 72) String password,
        @Size(max = 20) String telefono
) {}
