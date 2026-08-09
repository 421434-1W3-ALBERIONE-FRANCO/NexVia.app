package com.nexvia.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CalificacionRequest(
        @NotNull Long viajeId,
        @NotNull Long destinatarioId,
        @NotNull @Min(1) @Max(5) Integer puntuacion,
        String comentario
) {}
