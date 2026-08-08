package com.nexvia.services;

import com.nexvia.domain.Configuracion;
import com.nexvia.dtos.ConfiguracionRequest;
import com.nexvia.dtos.ConfiguracionResponse;
import com.nexvia.exceptions.ResourceNotFoundException;
import com.nexvia.repositories.ConfiguracionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfiguracionServiceTest {

    @Mock
    private ConfiguracionRepository configuracionRepository;

    @InjectMocks
    private ConfiguracionService configuracionService;

    private Configuracion buildConfig(Long id) {
        return Configuracion.builder()
                .id(id)
                .tarifaPorKm(500.0)
                .tarifaPorTonelada(1200.0)
                .zonaNombre("Zona Agrícola")
                .centroLat(-32.4341)
                .centroLng(-63.2433)
                .build();
    }

    @Test
    void listar_returnsAll() {
        when(configuracionRepository.findAll()).thenReturn(List.of(buildConfig(1L), buildConfig(2L)));

        List<ConfiguracionResponse> result = configuracionService.listar();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(1L);
    }

    @Test
    void listar_empty() {
        when(configuracionRepository.findAll()).thenReturn(List.of());

        List<ConfiguracionResponse> result = configuracionService.listar();

        assertThat(result).isEmpty();
    }

    @Test
    void obtener_success() {
        when(configuracionRepository.findById(1L)).thenReturn(Optional.of(buildConfig(1L)));

        ConfiguracionResponse result = configuracionService.obtener(1L);

        assertThat(result.tarifaPorKm()).isEqualTo(500.0);
        assertThat(result.zonaNombre()).isEqualTo("Zona Agrícola");
    }

    @Test
    void obtener_notFound() {
        when(configuracionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> configuracionService.obtener(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void crear_success() {
        var request = new ConfiguracionRequest(500.0, 1200.0, "Zona Test", -32.0, -63.0);
        when(configuracionRepository.save(any())).thenAnswer(inv -> {
            Configuracion c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        ConfiguracionResponse result = configuracionService.crear(request);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.tarifaPorKm()).isEqualTo(500.0);
        assertThat(result.tarifaPorTonelada()).isEqualTo(1200.0);
    }

    @Test
    void crear_nullTarifaTonelada_defaultsToZero() {
        var request = new ConfiguracionRequest(500.0, null, "Zona", -32.0, -63.0);
        when(configuracionRepository.save(any())).thenAnswer(inv -> {
            Configuracion c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        ConfiguracionResponse result = configuracionService.crear(request);

        assertThat(result.tarifaPorTonelada()).isEqualTo(0.0);
    }

    @Test
    void actualizar_success() {
        Configuracion existing = buildConfig(1L);
        when(configuracionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(configuracionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new ConfiguracionRequest(800.0, 1500.0, "Nueva Zona", -33.0, -64.0);
        ConfiguracionResponse result = configuracionService.actualizar(1L, request);

        assertThat(result.tarifaPorKm()).isEqualTo(800.0);
        assertThat(result.zonaNombre()).isEqualTo("Nueva Zona");
        assertThat(result.centroLat()).isEqualTo(-33.0);
    }

    @Test
    void actualizar_nullTarifaTonelada_defaultsToZero() {
        Configuracion existing = buildConfig(1L);
        when(configuracionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(configuracionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new ConfiguracionRequest(500.0, null, "Zona", -32.0, -63.0);
        ConfiguracionResponse result = configuracionService.actualizar(1L, request);

        assertThat(result.tarifaPorTonelada()).isEqualTo(0.0);
    }

    @Test
    void actualizar_notFound() {
        when(configuracionRepository.findById(99L)).thenReturn(Optional.empty());
        var request = new ConfiguracionRequest(500.0, 0.0, "Zona", -32.0, -63.0);

        assertThatThrownBy(() -> configuracionService.actualizar(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void eliminar_success() {
        when(configuracionRepository.existsById(1L)).thenReturn(true);

        configuracionService.eliminar(1L);

        verify(configuracionRepository).deleteById(1L);
    }

    @Test
    void eliminar_notFound() {
        when(configuracionRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> configuracionService.eliminar(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }
}
