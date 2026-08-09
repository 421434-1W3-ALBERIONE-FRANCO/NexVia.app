package com.nexvia.services;

import com.nexvia.domain.EstadoViaje;
import com.nexvia.dtos.DashboardStatsResponse;
import com.nexvia.dtos.EstadoCountResponse;
import com.nexvia.repositories.ViajeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private ViajeRepository viajeRepository;

    @InjectMocks
    private StatsService statsService;

    @Test
    void getDashboard_withData() {
        when(viajeRepository.count()).thenReturn(100L);
        when(viajeRepository.countByEstado(EstadoViaje.COMPLETADO)).thenReturn(60L);
        when(viajeRepository.countByEstado(EstadoViaje.CANCELADO)).thenReturn(10L);
        when(viajeRepository.countByEstado(EstadoViaje.ACEPTADO)).thenReturn(15L);
        when(viajeRepository.countByEstado(EstadoViaje.EN_CAMINO)).thenReturn(5L);
        when(viajeRepository.countByEstado(EstadoViaje.SOLICITADO)).thenReturn(10L);

        when(viajeRepository.sumPrecioByEstado(EstadoViaje.COMPLETADO)).thenReturn(3000000.0);
        when(viajeRepository.sumPenalidadByEstado(EstadoViaje.CANCELADO)).thenReturn(50000.0);
        when(viajeRepository.sumDistanciaByEstado(EstadoViaje.COMPLETADO)).thenReturn(24000.0);
        when(viajeRepository.sumToneladasByEstado(EstadoViaje.COMPLETADO)).thenReturn(600.0);

        DashboardStatsResponse result = statsService.getDashboard();

        assertThat(result.totalViajes()).isEqualTo(100);
        assertThat(result.viajesCompletados()).isEqualTo(60);
        assertThat(result.viajesCancelados()).isEqualTo(10);
        assertThat(result.viajesActivos()).isEqualTo(20);
        assertThat(result.ingresosTotales()).isEqualTo(3000000.0);
        assertThat(result.penalidadesTotales()).isEqualTo(50000.0);
        assertThat(result.distanciaTotalKm()).isEqualTo(24000.0);
        assertThat(result.toneladasTotales()).isEqualTo(600.0);
        assertThat(result.precioPromedio()).isEqualTo(50000.0);
        assertThat(result.tasaCancelacion()).isEqualTo(10.0);
        assertThat(result.viajesPorEstado()).hasSize(EstadoViaje.values().length);
    }

    @Test
    void getDashboard_empty() {
        when(viajeRepository.count()).thenReturn(0L);
        when(viajeRepository.countByEstado(EstadoViaje.COMPLETADO)).thenReturn(0L);
        when(viajeRepository.countByEstado(EstadoViaje.CANCELADO)).thenReturn(0L);
        when(viajeRepository.countByEstado(EstadoViaje.ACEPTADO)).thenReturn(0L);
        when(viajeRepository.countByEstado(EstadoViaje.EN_CAMINO)).thenReturn(0L);
        when(viajeRepository.countByEstado(EstadoViaje.SOLICITADO)).thenReturn(0L);

        when(viajeRepository.sumPrecioByEstado(EstadoViaje.COMPLETADO)).thenReturn(0.0);
        when(viajeRepository.sumPenalidadByEstado(EstadoViaje.CANCELADO)).thenReturn(0.0);
        when(viajeRepository.sumDistanciaByEstado(EstadoViaje.COMPLETADO)).thenReturn(0.0);
        when(viajeRepository.sumToneladasByEstado(EstadoViaje.COMPLETADO)).thenReturn(0.0);

        DashboardStatsResponse result = statsService.getDashboard();

        assertThat(result.totalViajes()).isZero();
        assertThat(result.precioPromedio()).isZero();
        assertThat(result.tasaCancelacion()).isZero();
    }

    @Test
    void getDashboard_precioPromedioRounding() {
        when(viajeRepository.count()).thenReturn(3L);
        when(viajeRepository.countByEstado(EstadoViaje.COMPLETADO)).thenReturn(3L);
        when(viajeRepository.countByEstado(EstadoViaje.CANCELADO)).thenReturn(0L);
        when(viajeRepository.countByEstado(EstadoViaje.ACEPTADO)).thenReturn(0L);
        when(viajeRepository.countByEstado(EstadoViaje.EN_CAMINO)).thenReturn(0L);
        when(viajeRepository.countByEstado(EstadoViaje.SOLICITADO)).thenReturn(0L);

        when(viajeRepository.sumPrecioByEstado(EstadoViaje.COMPLETADO)).thenReturn(100000.0);
        when(viajeRepository.sumPenalidadByEstado(EstadoViaje.CANCELADO)).thenReturn(0.0);
        when(viajeRepository.sumDistanciaByEstado(EstadoViaje.COMPLETADO)).thenReturn(1200.0);
        when(viajeRepository.sumToneladasByEstado(EstadoViaje.COMPLETADO)).thenReturn(90.0);

        DashboardStatsResponse result = statsService.getDashboard();

        assertThat(result.precioPromedio()).isEqualTo(33333.33);
    }

    @Test
    void getDashboard_tasaCancelacionRounding() {
        when(viajeRepository.count()).thenReturn(3L);
        when(viajeRepository.countByEstado(EstadoViaje.COMPLETADO)).thenReturn(2L);
        when(viajeRepository.countByEstado(EstadoViaje.CANCELADO)).thenReturn(1L);
        when(viajeRepository.countByEstado(EstadoViaje.ACEPTADO)).thenReturn(0L);
        when(viajeRepository.countByEstado(EstadoViaje.EN_CAMINO)).thenReturn(0L);
        when(viajeRepository.countByEstado(EstadoViaje.SOLICITADO)).thenReturn(0L);

        when(viajeRepository.sumPrecioByEstado(EstadoViaje.COMPLETADO)).thenReturn(100000.0);
        when(viajeRepository.sumPenalidadByEstado(EstadoViaje.CANCELADO)).thenReturn(5000.0);
        when(viajeRepository.sumDistanciaByEstado(EstadoViaje.COMPLETADO)).thenReturn(800.0);
        when(viajeRepository.sumToneladasByEstado(EstadoViaje.COMPLETADO)).thenReturn(40.0);

        DashboardStatsResponse result = statsService.getDashboard();

        assertThat(result.tasaCancelacion()).isEqualTo(33.33);
    }

    @Test
    void getViajesPorEstado_returnsAllEstados() {
        for (EstadoViaje e : EstadoViaje.values()) {
            when(viajeRepository.countByEstado(e)).thenReturn(5L);
        }

        List<EstadoCountResponse> result = statsService.getViajesPorEstado();

        assertThat(result).hasSize(EstadoViaje.values().length);
        assertThat(result).allMatch(r -> r.cantidad() == 5);
    }

    @Test
    void getViajesPorEstado_correctEstadoNames() {
        for (EstadoViaje e : EstadoViaje.values()) {
            when(viajeRepository.countByEstado(e)).thenReturn(0L);
        }

        List<EstadoCountResponse> result = statsService.getViajesPorEstado();

        List<String> nombres = result.stream().map(EstadoCountResponse::estado).toList();
        assertThat(nombres).contains("SOLICITADO", "ACEPTADO", "EN_CAMINO", "COMPLETADO", "CANCELADO");
    }
}
