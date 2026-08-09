package com.nexvia.services;

import com.nexvia.domain.Configuracion;
import com.nexvia.dtos.CotizacionRequest;
import com.nexvia.dtos.CotizacionResponse;
import com.nexvia.exceptions.ResourceNotFoundException;
import com.nexvia.repositories.ConfiguracionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CotizacionServiceTest {

    @Mock
    private ConfiguracionRepository configuracionRepository;

    @InjectMocks
    private CotizacionService cotizacionService;

    private Configuracion buildConfig() {
        return Configuracion.builder()
                .id(1L).tarifaPorKm(150.0).tarifaPorTonelada(500.0).zonaNombre("Córdoba").build();
    }

    @Test
    void cotizar_porKm_success() {
        when(configuracionRepository.findAll()).thenReturn(List.of(buildConfig()));

        var request = new CotizacionRequest(400.0, null, "POR_KM", null);
        CotizacionResponse result = cotizacionService.cotizar(request);

        assertThat(result.precioCalculado()).isEqualTo(60000.0);
        assertThat(result.tipoTarifa()).isEqualTo("POR_KM");
        assertThat(result.tarifaAplicada()).isEqualTo(150.0);
        assertThat(result.zonaNombre()).isEqualTo("Córdoba");
    }

    @Test
    void cotizar_porTonelada_success() {
        when(configuracionRepository.findAll()).thenReturn(List.of(buildConfig()));

        var request = new CotizacionRequest(400.0, 30.0, "POR_TONELADA", null);
        CotizacionResponse result = cotizacionService.cotizar(request);

        assertThat(result.precioCalculado()).isEqualTo(15000.0);
        assertThat(result.tipoTarifa()).isEqualTo("POR_TONELADA");
        assertThat(result.tarifaAplicada()).isEqualTo(500.0);
    }

    @Test
    void cotizar_porToneladaWithZeroToneladas_fallsBackToKm() {
        when(configuracionRepository.findAll()).thenReturn(List.of(buildConfig()));

        var request = new CotizacionRequest(400.0, 0.0, "POR_TONELADA", null);
        CotizacionResponse result = cotizacionService.cotizar(request);

        assertThat(result.precioCalculado()).isEqualTo(60000.0);
        assertThat(result.tipoTarifa()).isEqualTo("POR_TONELADA");
    }

    @Test
    void cotizar_nullTipoTarifa_defaultsToKm() {
        when(configuracionRepository.findAll()).thenReturn(List.of(buildConfig()));

        var request = new CotizacionRequest(100.0, null, null, null);
        CotizacionResponse result = cotizacionService.cotizar(request);

        assertThat(result.precioCalculado()).isEqualTo(15000.0);
        assertThat(result.tipoTarifa()).isEqualTo("POR_KM");
    }

    @Test
    void cotizar_withConfigId_success() {
        when(configuracionRepository.findById(1L)).thenReturn(Optional.of(buildConfig()));

        var request = new CotizacionRequest(200.0, null, null, 1L);
        CotizacionResponse result = cotizacionService.cotizar(request);

        assertThat(result.precioCalculado()).isEqualTo(30000.0);
    }

    @Test
    void cotizar_configNotFound_throws() {
        when(configuracionRepository.findById(99L)).thenReturn(Optional.empty());

        var request = new CotizacionRequest(100.0, null, null, 99L);

        assertThatThrownBy(() -> cotizacionService.cotizar(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Configuración");
    }

    @Test
    void cotizar_noConfigAvailable_throws() {
        when(configuracionRepository.findAll()).thenReturn(List.of());

        var request = new CotizacionRequest(100.0, null, null, null);

        assertThatThrownBy(() -> cotizacionService.cotizar(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("configuraciones");
    }

    @Test
    void cotizar_invalidTipoTarifa_throws() {
        when(configuracionRepository.findAll()).thenReturn(List.of(buildConfig()));

        var request = new CotizacionRequest(100.0, null, "INVALIDO", null);

        assertThatThrownBy(() -> cotizacionService.cotizar(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tipo de tarifa inválido");
    }

    @Test
    void calcularPrecio_porKm() {
        when(configuracionRepository.findAll()).thenReturn(List.of(buildConfig()));

        double precio = cotizacionService.calcularPrecio(100.0, null, null, null);

        assertThat(precio).isEqualTo(15000.0);
    }

    @Test
    void calcularPrecio_porTonelada() {
        when(configuracionRepository.findAll()).thenReturn(List.of(buildConfig()));

        double precio = cotizacionService.calcularPrecio(100.0, 20.0, "POR_TONELADA", null);

        assertThat(precio).isEqualTo(10000.0);
    }

    @Test
    void cotizar_roundsCorrectly() {
        var config = Configuracion.builder()
                .id(1L).tarifaPorKm(33.33).tarifaPorTonelada(0.0).zonaNombre("Test").build();
        when(configuracionRepository.findAll()).thenReturn(List.of(config));

        var request = new CotizacionRequest(3.0, null, null, null);
        CotizacionResponse result = cotizacionService.cotizar(request);

        assertThat(result.precioCalculado()).isEqualTo(99.99);
    }
}
