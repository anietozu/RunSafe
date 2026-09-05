package com.runsafe.api.actividad;

import com.runsafe.api.common.ApiException;
import com.runsafe.api.security.AuthUser;
import com.runsafe.api.usuario.Usuario;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Actividades")
public class ActividadController {

    private final ActividadRepository actividades;
    private final AuthUser authUser;

    public ActividadController(ActividadRepository actividades, AuthUser authUser) {
        this.actividades = actividades;
        this.authUser = authUser;
    }

    @GetMapping("/actividades")
    public List<ActividadResponse> listar() {
        return actividades.findByUsuarioIdOrderByFechaInicioDesc(authUser.current().getId())
                .stream().map(a -> ActividadResponse.from(a, false)).toList();
    }

    @GetMapping("/actividades/{id}")
    public ActividadResponse detalle(@PathVariable Long id) {
        Actividad a = actividades.findById(id).orElseThrow(() -> new ApiException("Actividad no encontrada"));
        return ActividadResponse.from(a, true);
    }

    @PostMapping("/actividades")
    public ActividadResponse guardar(@RequestBody ActividadRequest req) {
        Usuario u = authUser.current();
        Actividad a = new Actividad();
        a.setUsuario(u);
        a.setTipo(req.tipo() == null ? "CORRER" : req.tipo());
        a.setEstado("COMPLETADA");
        a.setFechaInicio(req.fechaInicio() == null ? LocalDateTime.now() : req.fechaInicio());
        a.setFechaFin(req.fechaFin() == null ? LocalDateTime.now() : req.fechaFin());
        a.setDistanciaM(nz(req.distanciaM()));
        a.setDuracionS(req.duracionS() == null ? 0 : req.duracionS());
        a.setVelocidadMedia(nz(req.velocidadMedia()));
        a.setCalorias(req.calorias() == null ? 0 : req.calorias());
        a.setDesnivelM(nz(req.desnivelM()));
        a.setPublica(req.publica() == null || req.publica());
        a.setNotas(req.notas());
        if (req.puntos() != null) {
            int i = 0;
            for (PuntoDto dto : req.puntos()) {
                PuntoRuta p = new PuntoRuta();
                p.setActividad(a);
                p.setLatitud(dto.latitud());
                p.setLongitud(dto.longitud());
                p.setAltitud(dto.altitud());
                p.setVelocidad(dto.velocidad());
                p.setTimestampPunto(dto.timestampPunto() == null ? LocalDateTime.now() : dto.timestampPunto());
                p.setOrden(i++);
                a.getPuntos().add(p);
            }
        }
        return ActividadResponse.from(actividades.save(a), true);
    }

    @GetMapping("/estadisticas")
    public Map<String, Object> estadisticas() {
        Long userId = authUser.current().getId();
        LocalDateTime startWeek = LocalDateTime.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
        List<Actividad> semana = actividades.findByUsuarioIdAndFechaInicioAfter(userId, startWeek);
        List<Actividad> todas = actividades.findByUsuarioIdOrderByFechaInicioDesc(userId);

        double dist = semana.stream().mapToDouble(a -> nz(a.getDistanciaM())).sum();
        int tiempo = semana.stream().mapToInt(a -> a.getDuracionS() == null ? 0 : a.getDuracionS()).sum();
        int calorias = semana.stream().mapToInt(a -> a.getCalorias() == null ? 0 : a.getCalorias()).sum();
        double vel = semana.isEmpty() ? 0 : semana.stream().mapToDouble(a -> nz(a.getVelocidadMedia())).average().orElse(0);

        double[] porDia = new double[7];
        for (Actividad a : semana) {
            int idx = a.getFechaInicio().getDayOfWeek().getValue() - 1;
            porDia[idx] += nz(a.getDistanciaM()) / 1000.0;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("distanciaM", dist);
        body.put("duracionS", tiempo);
        body.put("sesiones", semana.size());
        body.put("velocidadMedia", vel);
        body.put("calorias", calorias);
        body.put("distanciaPorDiaKm", porDia);
        body.put("totalHistorico", todas.size());
        return body;
    }

    private static double nz(Double v) {
        return v == null ? 0 : v;
    }
}
