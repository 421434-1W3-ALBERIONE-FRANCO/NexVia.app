package com.nexvia.dtos;

import jakarta.validation.constraints.NotBlank;

public record CamionRequest(
        String transporteNombre,
        String transporteCuit,

        @NotBlank(message = "El nombre del chofer es obligatorio")
        String choferNombre,

        String choferCuit,

        @NotBlank(message = "La patente es obligatoria")
        String patente,

        String patenteAcoplado,
        String telefono,
        Integer capacidadKg,
        Double lat,
        Double lng
) {}
