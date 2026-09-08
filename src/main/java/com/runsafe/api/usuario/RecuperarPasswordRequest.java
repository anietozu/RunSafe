package com.runsafe.api.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecuperarPasswordRequest(
        @NotBlank @Size(max = 20) String telefono,
        @NotBlank @Size(min = 6, max = 72) String password
) {}
