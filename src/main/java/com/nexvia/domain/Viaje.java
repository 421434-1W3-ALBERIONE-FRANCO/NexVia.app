package com.nexvia.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "viajes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Viaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "origen_lat", nullable = false)
    private Double origenLat;

    @Column(name = "origen_lng", nullable = false)
    private Double origenLng;

    @Column(name = "destino_lat", nullable = false)
    private Double destinoLat;

    @Column(name = "destino_lng", nullable = false)
    private Double destinoLng;

    @Column(name = "distancia_km", nullable = false)
    private Double distanciaKm;

    private Double toneladas;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_tarifa", nullable = false)
    @Builder.Default
    private TipoTarifa tipoTarifa = TipoTarifa.POR_KM;

    @Column(nullable = false)
    private Double precio;

    private String carga;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoViaje estado = EstadoViaje.SOLICITADO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "usuario_nombre")
    private String usuarioNombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "camion_id")
    private Camion camion;

    @Column(name = "chofer_nombre")
    private String choferNombre;

    @Column(name = "chofer_id")
    private Long choferId;

    @Column(name = "motivo_cancelacion")
    private String motivoCancelacion;

    @Column(name = "cancelado_por_id")
    private Long canceladoPorId;

    @Column(name = "cancelado_at")
    private LocalDateTime canceladoAt;

    @Column(name = "penalidad")
    @Builder.Default
    private Double penalidad = 0.0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
