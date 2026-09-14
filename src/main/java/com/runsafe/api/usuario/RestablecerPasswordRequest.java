package com.runsafe.api.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RestablecerPasswordRequest(
        @NotBlank @Size(min = 3, max = 255) String login,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "El código debe tener 6 dígitos") String codigo,
        @NotBlank @Size(min = 6, max = 72) String passwordNueva
) {}
