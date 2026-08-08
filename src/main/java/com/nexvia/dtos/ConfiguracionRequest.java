package com.nexvia.dtos;

import jakarta.validation.constraints.NotNull;

public record ConfiguracionRequest(
        @NotNull(message = "La tarifa por km es obligatoria")
        Double tarifaPorKm,

        Double tarifaPorTonelada,

        String zonaNombre,

        @NotNull(message = "La latitud del centro es obligatoria")
        Double centroLat,

        @NotNull(message = "La longitud del centro es obligatoria")
        Double centroLng
) {}
