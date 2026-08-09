package com.nexvia.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones", indexes = {
        @Index(name = "idx_notif_destinatario", columnList = "destinatario_id"),
        @Index(name = "idx_notif_leida", columnList = "destinatario_id, leida")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoNotificacion tipo;

    @Column(nullable = false)
    private String mensaje;

    @Column(name = "destinatario_id", nullable = false)
    private Long destinatarioId;

    @Column(name = "viaje_id")
    private Long viajeId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean leida = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
