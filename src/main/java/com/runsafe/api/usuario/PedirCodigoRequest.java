package com.runsafe.api.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PedirCodigoRequest(@NotBlank @Size(max = 20) String telefono) {}
