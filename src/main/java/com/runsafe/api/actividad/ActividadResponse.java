package com.runsafe.api.actividad;

import java.time.LocalDateTime;
import java.util.List;

public record ActividadResponse(
        Long id,
        String tipo,
        String estado,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        Double distanciaM,
        Integer duracionS,
        Double velocidadMedia,
        Integer calorias,
        Double desnivelM,
        Boolean publica,
        String notas,
        Long usuarioId,
        String usuarioNombre,
        List<PuntoDto> puntos
) {
    public static ActividadResponse from(Actividad a, boolean includePuntos) {
        List<PuntoDto> puntos = includePuntos
                ? a.getPuntos().stream().map(p -> new PuntoDto(
                        p.getLatitud(), p.getLongitud(), p.getAltitud(), p.getVelocidad(), p.getTimestampPunto()))
                .toList()
                : List.of();
        return new ActividadResponse(
                a.getId(), a.getTipo(), a.getEstado(), a.getFechaInicio(), a.getFechaFin(),
                a.getDistanciaM(), a.getDuracionS(), a.getVelocidadMedia(), a.getCalorias(),
                a.getDesnivelM(), a.getPublica(), a.getNotas(),
                a.getUsuario().getId(), a.getUsuario().getNombre(), puntos);
    }
}
