package com.runsafe.api.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PedirCodigoRequest(@NotBlank @Size(min = 3, max = 255) String login) {}
