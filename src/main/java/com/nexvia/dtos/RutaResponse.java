package com.nexvia.dtos;

import java.util.List;

public record RutaResponse(
        Long viajeId,
        long totalPuntos,
        double distanciaRecorridaKm,
        Long tiempoTranscurridoMinutos,
        PosicionViajeResponse posicionActual,
        List<PosicionViajeResponse> puntos
) {}
