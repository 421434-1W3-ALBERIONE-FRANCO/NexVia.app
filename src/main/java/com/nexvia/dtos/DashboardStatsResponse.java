package com.nexvia.dtos;

import java.util.List;

public record DashboardStatsResponse(
        long totalViajes,
        long viajesCompletados,
        long viajesCancelados,
        long viajesActivos,
        double ingresosTotales,
        double penalidadesTotales,
        double distanciaTotalKm,
        double toneladasTotales,
        double precioPromedio,
        double tasaCancelacion,
        List<EstadoCountResponse> viajesPorEstado
) {}
