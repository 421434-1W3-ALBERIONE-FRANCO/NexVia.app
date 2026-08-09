package com.nexvia.dtos;

import java.time.LocalDateTime;

public record CalificacionResponse(
        Long id,
        Long viajeId,
        Long autorId,
        Long destinatarioId,
        Integer puntuacion,
        String comentario,
        LocalDateTime createdAt
) {}
