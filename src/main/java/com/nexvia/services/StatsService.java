package com.nexvia.services;

import com.nexvia.domain.EstadoViaje;
import com.nexvia.dtos.DashboardStatsResponse;
import com.nexvia.dtos.EstadoCountResponse;
import com.nexvia.repositories.ViajeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final ViajeRepository viajeRepository;

    public DashboardStatsResponse getDashboard() {
        long total = viajeRepository.count();
        long completados = viajeRepository.countByEstado(EstadoViaje.COMPLETADO);
        long cancelados = viajeRepository.countByEstado(EstadoViaje.CANCELADO);
        long activos = viajeRepository.countByEstado(EstadoViaje.ACEPTADO)
                + viajeRepository.countByEstado(EstadoViaje.EN_CAMINO);

        double ingresos = viajeRepository.sumPrecioByEstado(EstadoViaje.COMPLETADO);
        double penalidades = viajeRepository.sumPenalidadByEstado(EstadoViaje.CANCELADO);
        double distancia = viajeRepository.sumDistanciaByEstado(EstadoViaje.COMPLETADO);
        double toneladas = viajeRepository.sumToneladasByEstado(EstadoViaje.COMPLETADO);
        double precioPromedio = completados > 0 ? Math.round(ingresos / completados * 100.0) / 100.0 : 0.0;
        double tasaCancelacion = total > 0 ? Math.round((double) cancelados / total * 10000.0) / 100.0 : 0.0;

        List<EstadoCountResponse> porEstado = Arrays.stream(EstadoViaje.values())
                .map(e -> new EstadoCountResponse(e.name(), viajeRepository.countByEstado(e)))
                .toList();

        return new DashboardStatsResponse(
                total, completados, cancelados, activos,
                ingresos, penalidades, distancia, toneladas,
                precioPromedio, tasaCancelacion, porEstado
        );
    }

    public List<EstadoCountResponse> getViajesPorEstado() {
        return Arrays.stream(EstadoViaje.values())
                .map(e -> new EstadoCountResponse(e.name(), viajeRepository.countByEstado(e)))
                .toList();
    }
}
