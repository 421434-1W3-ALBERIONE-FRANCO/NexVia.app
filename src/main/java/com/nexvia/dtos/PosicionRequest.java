package com.nexvia.dtos;

import jakarta.validation.constraints.NotNull;

public record PosicionRequest(
        @NotNull(message = "La latitud es obligatoria")
        Double lat,

        @NotNull(message = "La longitud es obligatoria")
        Double lng
) {}
