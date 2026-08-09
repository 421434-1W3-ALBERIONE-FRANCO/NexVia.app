package com.nexvia.dtos;

public record PromedioCalificacionResponse(
        Long usuarioId,
        double promedio,
        long totalCalificaciones
) {}
