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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Estadísticas avanzadas")
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
        List<Actividad> previo = filter(todas, range.prevFrom, range.from);
        Map<String, Object> body = new HashMap<>();
        body.put("periodo", periodo);
        body.put("actual", resumen(actual, range.from, range.to));
        body.put("previo", resumen(previo, range.prevFrom, range.from));
        body.put("comparativa", comparativa(actual, previo));
        body.put("evolucionSemanal", evolucion(todas, 8));
        body.put("porTipo", porTipo(actual));
        body.put("totalHistorico", todas.size());
        return body;
    }

    private static Map<String, Object> resumen(List<Actividad> list, LocalDateTime from, LocalDateTime to) {
        double dist = list.stream().mapToDouble(a -> nz(a.getDistanciaM())).sum();
        int tiempo = list.stream().mapToInt(a -> a.getDuracionS() == null ? 0 : a.getDuracionS()).sum();
        int calorias = list.stream().mapToInt(a -> a.getCalorias() == null ? 0 : a.getCalorias()).sum();
        double desnivel = list.stream().mapToDouble(a -> nz(a.getDesnivelM())).sum();
        double vel = tiempo > 0 ? (dist / 1000.0) / (tiempo / 3600.0) : 0;
        Map<String, Object> m = new HashMap<>();
        m.put("distanciaM", dist);
        m.put("duracionS", tiempo);
        m.put("sesiones", list.size());
        m.put("calorias", calorias);
        m.put("desnivelM", desnivel);
        m.put("velocidadMedia", vel);
        m.put("desde", from);
        m.put("hasta", to);
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

    private static Map<String, Object> comparativa(List<Actividad> actual, List<Actividad> previo) {
        double d1 = actual.stream().mapToDouble(a -> nz(a.getDistanciaM())).sum();
        double d0 = previo.stream().mapToDouble(a -> nz(a.getDistanciaM())).sum();
        int s1 = actual.size();
        int s0 = previo.size();
        Map<String, Object> m = new HashMap<>();
        m.put("deltaDistanciaPct", pct(d1, d0));
        m.put("deltaSesionesPct", pct(s1, s0));
        m.put("mejora", d1 >= d0 && s1 >= s0);
        return m;
    }

    private static List<Map<String, Object>> evolucion(List<Actividad> todas, int semanas) {
        LocalDate monday = LocalDate.now(ZONE).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = semanas - 1; i >= 0; i--) {
            LocalDate start = monday.minusWeeks(i);
            LocalDateTime from = start.atStartOfDay();
            LocalDateTime to = start.plusWeeks(1).atStartOfDay();
            List<Actividad> week = filter(todas, from, to);
            Map<String, Object> w = new HashMap<>();
            w.put("semana", start.toString());
            w.put("distanciaKm", week.stream().mapToDouble(a -> nz(a.getDistanciaM())).sum() / 1000.0);
            w.put("sesiones", week.size());
            w.put("calorias", week.stream().mapToInt(a -> a.getCalorias() == null ? 0 : a.getCalorias()).sum());
            out.add(w);
        }
        return out;
    }

    private static List<Map<String, Object>> porTipo(List<Actividad> list) {
        Map<String, double[]> acc = new LinkedHashMap<>();
        for (Actividad a : list) {
            String tipo = a.getTipo() == null ? "CORRER" : a.getTipo();
            double[] v = acc.computeIfAbsent(tipo, k -> new double[4]);
            v[0] += 1;
            v[1] += nz(a.getDistanciaM());
            v[2] += a.getDuracionS() == null ? 0 : a.getDuracionS();
            v[3] += a.getCalorias() == null ? 0 : a.getCalorias();
        }
        List<Map<String, Object>> out = new ArrayList<>();
        acc.forEach((tipo, v) -> {
            Map<String, Object> m = new HashMap<>();
            m.put("tipo", tipo);
            m.put("sesiones", (int) v[0]);
            m.put("distanciaM", v[1]);
            m.put("duracionS", (int) v[2]);
            m.put("calorias", (int) v[3]);
            m.put("velocidadMedia", v[2] > 0 ? (v[1] / 1000.0) / (v[2] / 3600.0) : 0);
            out.add(m);
        });
        return out;
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
            LocalDateTime to = from.plusMonths(1);
            return new Range(from, to, from.minusMonths(1));
        }
        if ("anio".equalsIgnoreCase(periodo) || "año".equalsIgnoreCase(periodo)) {
            LocalDateTime from = today.withDayOfYear(1).atStartOfDay();
            LocalDateTime to = from.plusYears(1);
            return new Range(from, to, from.minusYears(1));
        }
        LocalDateTime from = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime to = from.plusWeeks(1);
        return new Range(from, to, from.minusWeeks(1));
    }

    private static double pct(double now, double prev) {
        if (prev <= 0) {
            return now > 0 ? 100 : 0;
        }
        return ((now - prev) / prev) * 100;
    }

    private static double nz(Double v) {
        return v == null ? 0 : v;
    }

    private record Range(LocalDateTime from, LocalDateTime to, LocalDateTime prevFrom) {}
}
