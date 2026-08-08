package com.nexvia.dtos;

public record CamionResponse(
        Long id,
        String transporteNombre,
        String transporteCuit,
        String choferNombre,
        String choferCuit,
        String patente,
        String patenteAcoplado,
        String telefono,
        Integer capacidadKg,
        Double lat,
        Double lng,
        String estado,
        Long userId
) {}
