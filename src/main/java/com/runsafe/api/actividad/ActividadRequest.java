package com.runsafe.api.actividad;

import java.time.LocalDateTime;
import java.util.List;

public record ActividadRequest(
        String tipo,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        Double distanciaM,
        Integer duracionS,
        Double velocidadMedia,
        Integer calorias,
        Double desnivelM,
        Boolean publica,
        String notas,
        Boolean compartir,
        List<PuntoDto> puntos
) {}
