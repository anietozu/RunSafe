package com.runsafe.api.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecuperarPasswordRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6, max = 72) String password
) {}
