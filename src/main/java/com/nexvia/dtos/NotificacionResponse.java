package com.nexvia.dtos;

import java.time.LocalDateTime;

public record NotificacionResponse(
        Long id,
        String tipo,
        String mensaje,
        Long destinatarioId,
        Long viajeId,
        Boolean leida,
        LocalDateTime createdAt
) {}
