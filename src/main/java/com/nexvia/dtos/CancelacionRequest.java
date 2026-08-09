package com.nexvia.dtos;

import jakarta.validation.constraints.NotBlank;

public record CancelacionRequest(
        @NotBlank(message = "El motivo de cancelación es obligatorio")
        String motivo
) {}
