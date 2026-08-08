package com.nexvia.dtos;

public record ConfiguracionResponse(
        Long id,
        Double tarifaPorKm,
        Double tarifaPorTonelada,
        String zonaNombre,
        Double centroLat,
        Double centroLng
) {}
