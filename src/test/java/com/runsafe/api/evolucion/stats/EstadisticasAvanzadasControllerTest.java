package com.runsafe.api.evolucion.stats;

import com.runsafe.api.actividad.Actividad;
import com.runsafe.api.actividad.ActividadRepository;
import com.runsafe.api.security.AuthUser;
import com.runsafe.api.usuario.Usuario;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EstadisticasAvanzadasControllerTest {
    @Test
    void yearIncludesAllTwelveMonthsAndExcludesOtherYears() {
        int year = LocalDate.now(ZoneId.of("Europe/Madrid")).getYear();
        List<Actividad> activities = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            activities.add(activity(LocalDate.of(year, month, 15), month * 1000));
        }
        activities.add(activity(LocalDate.of(year - 1, 12, 31), 99000));
        activities.add(activity(LocalDate.of(year + 1, 1, 1), 99000));
        var controller = controller(activities);
        for (String period : List.of("anio", "AÑO")) {
            Map<?, ?> actual = (Map<?, ?>) controller.avanzadas(period).get("actual");
            assertEquals(12, actual.get("sesiones"));
            assertEquals(78000d, actual.get("distanciaM"));
            assertEquals(List.of(1d, 2d, 3d, 4d, 5d, 6d, 7d, 8d, 9d, 10d, 11d, 12d), actual.get("distanciaPorDiaKm"));
        }
    }

    @Test
    void monthKeepsEveryCalendarDayAndWeekKeepsSevenDays() {
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Madrid"));
        LocalDate last = today.with(TemporalAdjusters.lastDayOfMonth());
        var monthly = (Map<?, ?>) controller(List.of(activity(last, 3000))).avanzadas("mes").get("actual");
        List<?> days = (List<?>) monthly.get("distanciaPorDiaKm");
        assertEquals(today.lengthOfMonth(), days.size());
        assertEquals(3d, days.get(days.size() - 1));
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        var weekly = (Map<?, ?>) controller(List.of(activity(monday, 1000), activity(monday.plusDays(6), 2000), activity(monday.plusDays(7), 99000)))
                .avanzadas("semana").get("actual");
        assertEquals(List.of(1d, 0d, 0d, 0d, 0d, 0d, 2d), weekly.get("distanciaPorDiaKm"));
    }

    private EstadisticasAvanzadasController controller(List<Actividad> list) {
        var repo = mock(ActividadRepository.class);
        var auth = mock(AuthUser.class);
        var user = new Usuario(); user.setId(7L);
        when(auth.current()).thenReturn(user);
        when(repo.findByUsuarioIdOrderByFechaInicioDesc(7L)).thenReturn(list);
        return new EstadisticasAvanzadasController(repo, auth);
    }

    private Actividad activity(LocalDate date, double meters) {
        var a = new Actividad(); a.setFechaInicio(date.atStartOfDay()); a.setDistanciaM(meters); a.setDuracionS(3600);
        return a;
    }
}
