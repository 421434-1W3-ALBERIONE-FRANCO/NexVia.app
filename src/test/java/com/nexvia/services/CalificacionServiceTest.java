package com.nexvia.services;

import com.nexvia.domain.*;
import com.nexvia.dtos.CalificacionRequest;
import com.nexvia.dtos.CalificacionResponse;
import com.nexvia.dtos.PromedioCalificacionResponse;
import com.nexvia.exceptions.ForbiddenException;
import com.nexvia.exceptions.ResourceNotFoundException;
import com.nexvia.repositories.CalificacionRepository;
import com.nexvia.repositories.ViajeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalificacionServiceTest {

    @Mock
    private CalificacionRepository calificacionRepository;

    @Mock
    private ViajeRepository viajeRepository;

    @InjectMocks
    private CalificacionService calificacionService;

    private Usuario buildUsuario(Long id) {
        return Usuario.builder().id(id).email("u@mail.com").fullName("User " + id).role(Role.USUARIO).build();
    }

    private Viaje buildViajeCompletado(Long id, Usuario usuario, Long choferId) {
        return Viaje.builder().id(id)
                .origenLat(-32.0).origenLng(-63.0)
                .destinoLat(-34.0).destinoLng(-58.0)
                .distanciaKm(400.0).precio(50000.0)
                .tipoTarifa(TipoTarifa.POR_KM)
                .estado(EstadoViaje.COMPLETADO)
                .usuario(usuario).choferId(choferId)
                .build();
    }

    private Calificacion buildCalificacion(Long id, Viaje viaje, Long autorId, Long destinatarioId) {
        Calificacion c = Calificacion.builder()
                .id(id).viaje(viaje).autorId(autorId)
                .destinatarioId(destinatarioId).puntuacion(5)
                .comentario("Excelente").build();
        c.setCreatedAt(LocalDateTime.now());
        return c;
    }

    @Test
    void crear_asUsuario_success() {
        Usuario u = buildUsuario(1L);
        Viaje viaje = buildViajeCompletado(10L, u, 2L);
        when(viajeRepository.findById(10L)).thenReturn(Optional.of(viaje));
        when(calificacionRepository.existsByViajeIdAndAutorId(10L, 1L)).thenReturn(false);
        when(calificacionRepository.save(any())).thenAnswer(inv -> {
            Calificacion c = inv.getArgument(0);
            c.setId(1L);
            c.setCreatedAt(LocalDateTime.now());
            return c;
        });

        var request = new CalificacionRequest(10L, 2L, 5, "Buen viaje");
        CalificacionResponse result = calificacionService.crear(request, 1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.puntuacion()).isEqualTo(5);
        assertThat(result.autorId()).isEqualTo(1L);
        assertThat(result.destinatarioId()).isEqualTo(2L);
    }

    @Test
    void crear_asChofer_success() {
        Usuario u = buildUsuario(1L);
        Viaje viaje = buildViajeCompletado(10L, u, 2L);
        when(viajeRepository.findById(10L)).thenReturn(Optional.of(viaje));
        when(calificacionRepository.existsByViajeIdAndAutorId(10L, 2L)).thenReturn(false);
        when(calificacionRepository.save(any())).thenAnswer(inv -> {
            Calificacion c = inv.getArgument(0);
            c.setId(2L);
            c.setCreatedAt(LocalDateTime.now());
            return c;
        });

        var request = new CalificacionRequest(10L, 1L, 4, "Buen cliente");
        CalificacionResponse result = calificacionService.crear(request, 2L);

        assertThat(result.destinatarioId()).isEqualTo(1L);
        assertThat(result.autorId()).isEqualTo(2L);
    }

    @Test
    void crear_viajeNoCompletado_throws() {
        Usuario u = buildUsuario(1L);
        Viaje viaje = Viaje.builder().id(10L).estado(EstadoViaje.EN_CAMINO)
                .origenLat(-32.0).origenLng(-63.0).destinoLat(-34.0).destinoLng(-58.0)
                .distanciaKm(400.0).precio(50000.0).tipoTarifa(TipoTarifa.POR_KM)
                .usuario(u).choferId(2L).build();
        when(viajeRepository.findById(10L)).thenReturn(Optional.of(viaje));

        var request = new CalificacionRequest(10L, 2L, 5, null);

        assertThatThrownBy(() -> calificacionService.crear(request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("completado");
    }

    @Test
    void crear_viajeNotFound_throws() {
        when(viajeRepository.findById(99L)).thenReturn(Optional.empty());

        var request = new CalificacionRequest(99L, 2L, 5, null);

        assertThatThrownBy(() -> calificacionService.crear(request, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void crear_notParticipante_throwsForbidden() {
        Usuario u = buildUsuario(1L);
        Viaje viaje = buildViajeCompletado(10L, u, 2L);
        when(viajeRepository.findById(10L)).thenReturn(Optional.of(viaje));

        var request = new CalificacionRequest(10L, 1L, 5, null);

        assertThatThrownBy(() -> calificacionService.crear(request, 999L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void crear_selfRating_throws() {
        Usuario u = buildUsuario(1L);
        Viaje viaje = buildViajeCompletado(10L, u, 2L);
        when(viajeRepository.findById(10L)).thenReturn(Optional.of(viaje));

        var request = new CalificacionRequest(10L, 1L, 5, null);

        assertThatThrownBy(() -> calificacionService.crear(request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vos mismo");
    }

    @Test
    void crear_alreadyRated_throws() {
        Usuario u = buildUsuario(1L);
        Viaje viaje = buildViajeCompletado(10L, u, 2L);
        when(viajeRepository.findById(10L)).thenReturn(Optional.of(viaje));
        when(calificacionRepository.existsByViajeIdAndAutorId(10L, 1L)).thenReturn(true);

        var request = new CalificacionRequest(10L, 2L, 5, null);

        assertThatThrownBy(() -> calificacionService.crear(request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya calificaste");
    }

    @Test
    void crear_nullComentario_success() {
        Usuario u = buildUsuario(1L);
        Viaje viaje = buildViajeCompletado(10L, u, 2L);
        when(viajeRepository.findById(10L)).thenReturn(Optional.of(viaje));
        when(calificacionRepository.existsByViajeIdAndAutorId(10L, 1L)).thenReturn(false);
        when(calificacionRepository.save(any())).thenAnswer(inv -> {
            Calificacion c = inv.getArgument(0);
            c.setId(1L);
            c.setCreatedAt(LocalDateTime.now());
            return c;
        });

        var request = new CalificacionRequest(10L, 2L, 3, null);
        CalificacionResponse result = calificacionService.crear(request, 1L);

        assertThat(result.comentario()).isNull();
        assertThat(result.puntuacion()).isEqualTo(3);
    }

    @Test
    void obtenerPorDestinatario_returnsList() {
        Usuario u = buildUsuario(1L);
        Viaje viaje = buildViajeCompletado(10L, u, 2L);
        Calificacion c = buildCalificacion(1L, viaje, 1L, 2L);
        when(calificacionRepository.findByDestinatarioIdOrderByCreatedAtDesc(2L))
                .thenReturn(List.of(c));

        List<CalificacionResponse> result = calificacionService.obtenerPorDestinatario(2L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).destinatarioId()).isEqualTo(2L);
    }

    @Test
    void obtenerPorViaje_returnsList() {
        Usuario u = buildUsuario(1L);
        Viaje viaje = buildViajeCompletado(10L, u, 2L);
        Calificacion c = buildCalificacion(1L, viaje, 1L, 2L);
        when(calificacionRepository.findByViajeId(10L)).thenReturn(List.of(c));

        List<CalificacionResponse> result = calificacionService.obtenerPorViaje(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).viajeId()).isEqualTo(10L);
    }

    @Test
    void promedio_withData() {
        when(calificacionRepository.promedioByDestinatarioId(2L)).thenReturn(4.333);
        when(calificacionRepository.countByDestinatarioId(2L)).thenReturn(3L);

        PromedioCalificacionResponse result = calificacionService.promedio(2L);

        assertThat(result.usuarioId()).isEqualTo(2L);
        assertThat(result.promedio()).isEqualTo(4.33);
        assertThat(result.totalCalificaciones()).isEqualTo(3);
    }

    @Test
    void promedio_noData() {
        when(calificacionRepository.promedioByDestinatarioId(99L)).thenReturn(0.0);
        when(calificacionRepository.countByDestinatarioId(99L)).thenReturn(0L);

        PromedioCalificacionResponse result = calificacionService.promedio(99L);

        assertThat(result.promedio()).isZero();
        assertThat(result.totalCalificaciones()).isZero();
    }

    @Test
    void crear_nullUsuarioInViaje_throwsForbidden() {
        Viaje viaje = Viaje.builder().id(10L).estado(EstadoViaje.COMPLETADO)
                .origenLat(-32.0).origenLng(-63.0).destinoLat(-34.0).destinoLng(-58.0)
                .distanciaKm(400.0).precio(50000.0).tipoTarifa(TipoTarifa.POR_KM)
                .choferId(2L).build();
        when(viajeRepository.findById(10L)).thenReturn(Optional.of(viaje));

        var request = new CalificacionRequest(10L, 2L, 5, null);

        assertThatThrownBy(() -> calificacionService.crear(request, 999L))
                .isInstanceOf(ForbiddenException.class);
    }
}
