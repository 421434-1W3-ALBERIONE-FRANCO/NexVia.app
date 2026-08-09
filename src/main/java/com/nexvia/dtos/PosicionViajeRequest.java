package com.nexvia.dtos;

import com.nexvia.validation.ValidLatitude;
import com.nexvia.validation.ValidLongitude;
import jakarta.validation.constraints.NotNull;

public record PosicionViajeRequest(
        @NotNull @ValidLatitude Double lat,
        @NotNull @ValidLongitude Double lng,
        Double velocidad,
        Double rumbo
) {}
