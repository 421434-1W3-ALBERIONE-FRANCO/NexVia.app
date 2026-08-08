package com.nexvia.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "camiones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Camion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transporte_nombre")
    private String transporteNombre;

    @Column(name = "transporte_cuit")
    private String transporteCuit;

    @Column(name = "chofer_nombre", nullable = false)
    private String choferNombre;

    @Column(name = "chofer_cuit")
    private String choferCuit;

    @Column(nullable = false)
    private String patente;

    @Column(name = "patente_acoplado")
    private String patenteAcoplado;

    private String telefono;

    @Column(name = "capacidad_kg")
    private Integer capacidadKg;

    private Double lat;

    private Double lng;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoCamion estado = EstadoCamion.DISPONIBLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Usuario usuario;
}
