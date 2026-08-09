package com.nexvia.repositories;

import com.nexvia.domain.PosicionViaje;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PosicionViajeRepository extends JpaRepository<PosicionViaje, Long> {
    List<PosicionViaje> findByViajeIdOrderByTimestampAsc(Long viajeId);
    Optional<PosicionViaje> findFirstByViajeIdOrderByTimestampDesc(Long viajeId);
    long countByViajeId(Long viajeId);
}
