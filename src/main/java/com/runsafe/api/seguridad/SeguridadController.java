package com.runsafe.api.seguridad;

import com.runsafe.api.common.ApiException;
import com.runsafe.api.security.AuthUser;
import com.runsafe.api.usuario.Usuario;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Seguridad")
public class SeguridadController {

    private final ContactoEmergenciaRepository contactos;
    private final ConfiguracionSeguridadRepository configs;
    private final AlertaSosRepository alertas;
    private final AuthUser authUser;

    public SeguridadController(
            ContactoEmergenciaRepository contactos,
            ConfiguracionSeguridadRepository configs,
            AlertaSosRepository alertas,
            AuthUser authUser
    ) {
        this.contactos = contactos;
        this.configs = configs;
        this.alertas = alertas;
        this.authUser = authUser;
    }

    @GetMapping("/seguridad")
    public Map<String, Object> estado() {
        Usuario me = authUser.current();
        ConfiguracionSeguridad cfg = configs.findByUsuarioId(me.getId()).orElseGet(() -> {
            ConfiguracionSeguridad c = new ConfiguracionSeguridad();
            c.setUsuario(me);
            return configs.save(c);
        });
        Map<String, Object> body = new HashMap<>();
        body.put("protegido", !contactos.findByUsuarioId(me.getId()).isEmpty() && Boolean.TRUE.equals(cfg.getDeteccionCaidas()));
        body.put("deteccionCaidas", cfg.getDeteccionCaidas());
        body.put("gpsDuranteActividad", cfg.getGpsDuranteActividad());
        body.put("contactos", contactos.findByUsuarioId(me.getId()).stream().map(this::toContacto).toList());
        return body;
    }

    @PutMapping("/seguridad")
    public Map<String, Object> actualizar(@RequestBody Map<String, Boolean> body) {
        Usuario me = authUser.current();
        ConfiguracionSeguridad cfg = configs.findByUsuarioId(me.getId()).orElseGet(() -> {
            ConfiguracionSeguridad c = new ConfiguracionSeguridad();
            c.setUsuario(me);
            return c;
        });
        if (body.containsKey("deteccionCaidas")) {
            cfg.setDeteccionCaidas(body.get("deteccionCaidas"));
        }
        if (body.containsKey("gpsDuranteActividad")) {
            cfg.setGpsDuranteActividad(body.get("gpsDuranteActividad"));
        }
        configs.save(cfg);
        return estado();
    }

    @PostMapping("/contactos-emergencia")
    public Map<String, Object> crearContacto(@RequestBody Map<String, String> body) {
        ContactoEmergencia c = new ContactoEmergencia();
        c.setUsuario(authUser.current());
        c.setNombre(body.getOrDefault("nombre", "Contacto"));
        c.setTelefono(body.getOrDefault("telefono", ""));
        c.setRelacion(body.get("relacion"));
        contactos.save(c);
        return toContacto(c);
    }

    @DeleteMapping("/contactos-emergencia/{id}")
    public Map<String, Boolean> borrar(@PathVariable Long id) {
        ContactoEmergencia c = contactos.findById(id).orElseThrow(() -> new ApiException("Contacto no encontrado"));
        if (!c.getUsuario().getId().equals(authUser.current().getId())) {
            throw new ApiException("No autorizado");
        }
        contactos.delete(c);
        return Map.of("ok", true);
    }

    @PostMapping("/alertas-sos")
    public Map<String, Object> sos(@RequestBody Map<String, Object> body) {
        Usuario me = authUser.current();
        AlertaSos a = new AlertaSos();
        a.setUsuario(me);
        a.setMotivo(String.valueOf(body.getOrDefault("motivo", "MANUAL")));
        a.setMensaje(body.get("mensaje") == null ? null : String.valueOf(body.get("mensaje")));
        a.setFecha(LocalDateTime.now());
        a.setEnviada(true);
        if (body.get("latitud") != null) {
            a.setLatitud(Double.valueOf(body.get("latitud").toString()));
        }
        if (body.get("longitud") != null) {
            a.setLongitud(Double.valueOf(body.get("longitud").toString()));
        }
        alertas.save(a);
        List<Map<String, Object>> destinos = contactos.findByUsuarioId(me.getId()).stream().map(this::toContacto).toList();
        Map<String, Object> res = new HashMap<>();
        res.put("id", a.getId());
        res.put("fecha", a.getFecha());
        res.put("contactosNotificados", destinos);
        return res;
    }

    private Map<String, Object> toContacto(ContactoEmergencia c) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId());
        m.put("nombre", c.getNombre());
        m.put("telefono", c.getTelefono());
        m.put("relacion", c.getRelacion());
        return m;
    }
}
