package com.runsafe.api.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "recuperacion_password")
public class RecuperacionPassword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "codigo_hash", nullable = false, length = 60)
    private String codigoHash;

    @Column(nullable = false)
    private LocalDateTime caduca;

    @Column(nullable = false)
    private int intentos = 0;

    @Column(nullable = false)
    private boolean usada = false;

    @Column(name = "fecha_alta", nullable = false)
    private LocalDateTime fechaAlta;
}
