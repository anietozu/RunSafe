package com.runsafe.api.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambiarPasswordRequest(
        @NotBlank @Size(max = 72) String passwordActual,
        @NotBlank @Size(min = 6, max = 72) String passwordNueva
) {}
