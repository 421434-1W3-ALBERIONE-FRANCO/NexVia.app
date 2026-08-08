package com.nexvia.dtos;

import java.time.LocalDateTime;

public record ViajeResponse(
        Long id,
        Double origenLat,
        Double origenLng,
        Double destinoLat,
        Double destinoLng,
        Double distanciaKm,
        Double toneladas,
        String tipoTarifa,
        Double precio,
        String carga,
        String estado,
        Long usuarioId,
        String usuarioNombre,
        Long camionId,
        String choferNombre,
        Long choferId,
        LocalDateTime createdAt
) {}
