package com.runsafe.api.actividad;

import java.time.LocalDateTime;

public record PuntoDto(
        Double latitud,
        Double longitud,
        Double altitud,
        Double velocidad,
        LocalDateTime timestampPunto
) {}
