package com.runsafe.api.evolucion.stats;

import com.runsafe.api.actividad.Actividad;
import com.runsafe.api.actividad.ActividadRepository;
import com.runsafe.api.security.AuthUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Estadísticas")
public class EstadisticasAvanzadasController {

    private static final ZoneId ZONE = ZoneId.of("Europe/Madrid");
    private final ActividadRepository actividades;
    private final AuthUser authUser;

    public EstadisticasAvanzadasController(ActividadRepository actividades, AuthUser authUser) {
        this.actividades = actividades;
        this.authUser = authUser;
    }

    @GetMapping("/estadisticas/avanzadas")
    @Transactional(readOnly = true)
    public Map<String, Object> avanzadas(@RequestParam(defaultValue = "semana") String periodo) {
        Long userId = authUser.current().getId();
        List<Actividad> todas = actividades.findByUsuarioIdOrderByFechaInicioDesc(userId);
        Range range = range(periodo);
        List<Actividad> actual = filter(todas, range.from, range.to);
        Map<String, Object> body = new HashMap<>();
        body.put("periodo", periodo);
        body.put("actual", resumen(actual, range.from, range.to));
        return body;
    }

    private static Map<String, Object> resumen(List<Actividad> list, LocalDateTime from, LocalDateTime to) {
        double dist = list.stream().mapToDouble(a -> nz(a.getDistanciaM())).sum();
        int tiempo = list.stream().mapToInt(a -> a.getDuracionS() == null ? 0 : a.getDuracionS()).sum();
        int calorias = list.stream().mapToInt(a -> a.getCalorias() == null ? 0 : a.getCalorias()).sum();
        double vel = tiempo > 0 ? (dist / 1000.0) / (tiempo / 3600.0) : 0;
        Map<String, Object> m = new HashMap<>();
        m.put("distanciaM", dist);
        m.put("duracionS", tiempo);
        m.put("sesiones", list.size());
        m.put("calorias", calorias);
        m.put("velocidadMedia", vel);
        m.put("distanciaPorDiaKm", porDia(list, from, to));
        return m;
    }

    private static List<Double> porDia(List<Actividad> list, LocalDateTime from, LocalDateTime to) {
        long days = Math.max(1, java.time.Duration.between(from, to).toDays());
        int n = (int) Math.min(days, 31);
        List<Double> bins = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            bins.add(0d);
        }
        for (Actividad a : list) {
            if (a.getFechaInicio() == null) {
                continue;
            }
            int idx = (int) java.time.Duration.between(from, a.getFechaInicio()).toDays();
            if (idx >= 0 && idx < n) {
                bins.set(idx, bins.get(idx) + nz(a.getDistanciaM()) / 1000.0);
            }
        }
        return bins;
    }

    private static List<Actividad> filter(List<Actividad> list, LocalDateTime from, LocalDateTime to) {
        return list.stream()
                .filter(a -> a.getFechaInicio() != null && !a.getFechaInicio().isBefore(from) && a.getFechaInicio().isBefore(to))
                .toList();
    }

    private static Range range(String periodo) {
        LocalDate today = LocalDate.now(ZONE);
        if ("mes".equalsIgnoreCase(periodo)) {
            LocalDateTime from = today.withDayOfMonth(1).atStartOfDay();
            return new Range(from, from.plusMonths(1));
        }
        if ("anio".equalsIgnoreCase(periodo) || "año".equalsIgnoreCase(periodo)) {
            LocalDateTime from = today.withDayOfYear(1).atStartOfDay();
            return new Range(from, from.plusYears(1));
        }
        LocalDateTime from = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        return new Range(from, from.plusWeeks(1));
    }

    private static double nz(Double v) {
        return v == null ? 0 : v;
    }

    private record Range(LocalDateTime from, LocalDateTime to) {}
}
