package com.nexvia.dtos;

import java.time.LocalDateTime;

public record LugarGuardadoResponse(
        Long id,
        String nombre,
        Double lat,
        Double lng,
        String tipo,
        Long usuarioId,
        LocalDateTime createdAt
) {}
