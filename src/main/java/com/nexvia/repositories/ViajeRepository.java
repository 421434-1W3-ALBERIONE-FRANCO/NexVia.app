package com.nexvia.repositories;

import com.nexvia.domain.EstadoViaje;
import com.nexvia.domain.Viaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ViajeRepository extends JpaRepository<Viaje, Long> {
    List<Viaje> findByEstado(EstadoViaje estado);
    List<Viaje> findByUsuarioIdOrderByCreatedAtDesc(Long usuarioId);
    List<Viaje> findByCamionIdOrderByCreatedAtDesc(Long camionId);
    List<Viaje> findByUsuarioIdAndEstadoIn(Long usuarioId, List<EstadoViaje> estados);
    List<Viaje> findByCamionIdAndEstadoIn(Long camionId, List<EstadoViaje> estados);

    long countByEstado(EstadoViaje estado);

    @Query("SELECT COALESCE(SUM(v.precio), 0) FROM Viaje v WHERE v.estado = :estado")
    Double sumPrecioByEstado(@Param("estado") EstadoViaje estado);

    @Query("SELECT COALESCE(SUM(v.distanciaKm), 0) FROM Viaje v WHERE v.estado = :estado")
    Double sumDistanciaByEstado(@Param("estado") EstadoViaje estado);

    @Query("SELECT COALESCE(SUM(v.toneladas), 0) FROM Viaje v WHERE v.estado = :estado")
    Double sumToneladasByEstado(@Param("estado") EstadoViaje estado);

    @Query("SELECT COALESCE(SUM(v.penalidad), 0) FROM Viaje v WHERE v.estado = :estado")
    Double sumPenalidadByEstado(@Param("estado") EstadoViaje estado);
}
