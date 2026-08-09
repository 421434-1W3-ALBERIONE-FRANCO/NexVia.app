package com.nexvia.repositories;

import com.nexvia.domain.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    List<Notificacion> findByDestinatarioIdOrderByCreatedAtDesc(Long destinatarioId);
    List<Notificacion> findByDestinatarioIdAndLeidaFalseOrderByCreatedAtDesc(Long destinatarioId);
    long countByDestinatarioIdAndLeidaFalse(Long destinatarioId);
}
