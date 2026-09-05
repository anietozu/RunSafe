package com.runsafe.api.actividad;

import com.runsafe.api.usuario.Usuario;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "actividades")
public class Actividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(nullable = false, length = 20)
    private String estado = "EN_CURSO";

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "distancia_m", nullable = false)
    private Double distanciaM = 0d;

    @Column(name = "duracion_s", nullable = false)
    private Integer duracionS = 0;

    @Column(name = "velocidad_media", nullable = false)
    private Double velocidadMedia = 0d;

    @Column(nullable = false)
    private Integer calorias = 0;

    @Column(name = "desnivel_m", nullable = false)
    private Double desnivelM = 0d;

    @Column(nullable = false)
    private Boolean publica = true;

    @Column(length = 500)
    private String notas;

    @OneToMany(mappedBy = "actividad", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<PuntoRuta> puntos = new ArrayList<>();
}
