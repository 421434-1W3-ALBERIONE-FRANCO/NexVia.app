package com.nexvia.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "configuraciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Configuracion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tarifa_por_km", nullable = false)
    private Double tarifaPorKm;

    @Column(name = "tarifa_por_tonelada")
    private Double tarifaPorTonelada;

    @Column(name = "zona_nombre")
    private String zonaNombre;

    @Column(name = "centro_lat", nullable = false)
    private Double centroLat;

    @Column(name = "centro_lng", nullable = false)
    private Double centroLng;
}
