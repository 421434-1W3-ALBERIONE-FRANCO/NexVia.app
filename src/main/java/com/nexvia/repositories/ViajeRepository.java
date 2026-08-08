package com.nexvia.repositories;

import com.nexvia.domain.EstadoViaje;
import com.nexvia.domain.Viaje;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ViajeRepository extends JpaRepository<Viaje, Long> {
    List<Viaje> findByEstado(EstadoViaje estado);
    List<Viaje> findByUsuarioIdOrderByCreatedAtDesc(Long usuarioId);
    List<Viaje> findByCamionIdOrderByCreatedAtDesc(Long camionId);
    List<Viaje> findByUsuarioIdAndEstadoIn(Long usuarioId, List<EstadoViaje> estados);
    List<Viaje> findByCamionIdAndEstadoIn(Long camionId, List<EstadoViaje> estados);
}
