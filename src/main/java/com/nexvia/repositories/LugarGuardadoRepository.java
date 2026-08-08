package com.nexvia.repositories;

import com.nexvia.domain.LugarGuardado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LugarGuardadoRepository extends JpaRepository<LugarGuardado, Long> {
    List<LugarGuardado> findByUsuarioIdOrderByCreatedAtDesc(Long usuarioId);
}
