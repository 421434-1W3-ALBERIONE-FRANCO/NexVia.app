package com.nexvia.dtos;

import com.nexvia.validation.ValidLatitude;
import com.nexvia.validation.ValidLongitude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ViajeRequest(
        @NotNull(message = "La latitud de origen es obligatoria")
        @ValidLatitude
        Double origenLat,

        @NotNull(message = "La longitud de origen es obligatoria")
        @ValidLongitude
        Double origenLng,

        @NotNull(message = "La latitud de destino es obligatoria")
        @ValidLatitude
        Double destinoLat,

        @NotNull(message = "La longitud de destino es obligatoria")
        @ValidLongitude
        Double destinoLng,

        @NotNull(message = "La distancia es obligatoria")
        @Positive(message = "La distancia debe ser positiva")
        Double distanciaKm,

        Double toneladas,

        String tipoTarifa,

        @NotNull(message = "El precio es obligatorio")
        @Positive(message = "El precio debe ser positivo")
        Double precio,

        @NotBlank(message = "La carga es obligatoria")
        String carga
) {}
