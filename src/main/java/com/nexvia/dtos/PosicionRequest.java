package com.nexvia.dtos;

import com.nexvia.validation.ValidLatitude;
import com.nexvia.validation.ValidLongitude;
import jakarta.validation.constraints.NotNull;

public record PosicionRequest(
        @NotNull(message = "La latitud es obligatoria")
        @ValidLatitude
        Double lat,

        @NotNull(message = "La longitud es obligatoria")
        @ValidLongitude
        Double lng
) {}
