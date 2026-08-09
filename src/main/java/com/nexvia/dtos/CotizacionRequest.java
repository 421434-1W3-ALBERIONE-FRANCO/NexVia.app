package com.nexvia.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CotizacionRequest(
        @NotNull(message = "La distancia es obligatoria")
        @Positive(message = "La distancia debe ser positiva")
        Double distanciaKm,

        Double toneladas,

        String tipoTarifa,

        Long configuracionId
) {}
