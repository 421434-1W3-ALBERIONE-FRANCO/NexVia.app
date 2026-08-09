package com.nexvia.dtos;

import java.time.LocalDateTime;

public record PosicionViajeResponse(
        Long id,
        Long viajeId,
        Double lat,
        Double lng,
        Double velocidad,
        Double rumbo,
        LocalDateTime timestamp
) {}
