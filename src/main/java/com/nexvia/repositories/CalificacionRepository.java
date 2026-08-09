package com.nexvia.repositories;

import com.nexvia.domain.Calificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CalificacionRepository extends JpaRepository<Calificacion, Long> {
    List<Calificacion> findByDestinatarioIdOrderByCreatedAtDesc(Long destinatarioId);
    List<Calificacion> findByViajeId(Long viajeId);
    Optional<Calificacion> findByViajeIdAndAutorId(Long viajeId, Long autorId);
    boolean existsByViajeIdAndAutorId(Long viajeId, Long autorId);

    @Query("SELECT COALESCE(AVG(c.puntuacion), 0) FROM Calificacion c WHERE c.destinatarioId = :destinatarioId")
    Double promedioByDestinatarioId(@Param("destinatarioId") Long destinatarioId);

    long countByDestinatarioId(Long destinatarioId);
}
