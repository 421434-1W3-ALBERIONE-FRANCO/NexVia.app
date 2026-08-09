package com.nexvia.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "calificaciones", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"viaje_id", "autor_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Calificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viaje_id", nullable = false)
    private Viaje viaje;

    @Column(name = "autor_id", nullable = false)
    private Long autorId;

    @Column(name = "destinatario_id", nullable = false)
    private Long destinatarioId;

    @Column(nullable = false)
    private Integer puntuacion;

    private String comentario;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
