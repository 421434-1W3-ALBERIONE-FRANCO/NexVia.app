package com.nexvia.repositories;

import com.nexvia.domain.Camion;
import com.nexvia.domain.EstadoCamion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CamionRepository extends JpaRepository<Camion, Long> {
    List<Camion> findByEstado(EstadoCamion estado);
    List<Camion> findByUsuarioId(Long usuarioId);
    Optional<Camion> findFirstByUsuarioId(Long usuarioId);
}
