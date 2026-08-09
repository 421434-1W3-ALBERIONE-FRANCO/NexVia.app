package com.nexvia.dtos;

public record CotizacionResponse(
        Double precioCalculado,
        Double distanciaKm,
        Double toneladas,
        String tipoTarifa,
        Double tarifaAplicada,
        String zonaNombre
) {}
