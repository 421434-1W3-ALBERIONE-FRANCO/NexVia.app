package com.nexvia.services;

import com.nexvia.domain.*;
import com.nexvia.dtos.PosicionViajeRequest;
import com.nexvia.dtos.PosicionViajeResponse;
import com.nexvia.dtos.RutaResponse;
import com.nexvia.exceptions.ForbiddenException;
import com.nexvia.exceptions.ResourceNotFoundException;
import com.nexvia.repositories.CamionRepository;
import com.nexvia.repositories.PosicionViajeRepository;
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
class TrackingServiceTest {

    @Mock
    private PosicionViajeRepository posicionRepository;

    @Mock
    private ViajeRepository viajeRepository;

    @Mock
    private CamionRepository camionRepository;

    @InjectMocks
    private TrackingService trackingService;

    private Usuario buildUsuario(Long id) {
        return Usuario.builder().id(id).email("u@mail.com").fullName("User " + id).role(Role.USUARIO).build();
    }

    private Viaje buildViaje(Long id, EstadoViaje estado, Long choferId) {
        Usuario u = buildUsuario(1L);
        Camion camion = Camion.builder().id(5L).choferNombre("C").patente("ABC")
                .estado(EstadoCamion.OCUPADO).usuario(buildUsuario(choferId)).build();
        return Viaje.builder().id(id)
                .origenLat(-32.0).origenLng(-63.0).destinoLat(-34.0).destinoLng(-58.0)
                .distanciaKm(400.0).precio(50000.0).tipoTarifa(TipoTarifa.POR_KM)
                .estado(estado).usuario(u).choferId(choferId).camion(camion).build();
    }

    private PosicionViaje buildPosicion(Long id, Viaje viaje, double lat, double lng, LocalDateTime ts) {
        return PosicionViaje.builder().id(id).viaje(viaje).lat(lat).lng(lng)
                .velocidad(60.0).rumbo(180.0).timestamp(ts).build();
    }

    @Test
    void registrarPosicion_enCamino_success() {
        Viaje viaje = buildViaje(1L, EstadoViaje.EN_CAMINO, 2L);
        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));
        when(posicionRepository.save(any())).thenAnswer(inv -> {
            PosicionViaje p = inv.getArgument(0);
            p.setId(1L);
            p.setTimestamp(LocalDateTime.now());
            return p;
        });
        when(camionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new PosicionViajeRequest(-33.0, -64.0, 80.0, 90.0);
        PosicionViajeResponse result = trackingService.registrarPosicion(1L, request, 2L, Role.CHOFER);

        assertThat(result.lat()).isEqualTo(-33.0);
        assertThat(result.lng()).isEqualTo(-64.0);
        assertThat(result.velocidad()).isEqualTo(80.0);
        verify(camionRepository).save(any());
    }

    @Test
    void registrarPosicion_aceptado_success() {
        Viaje viaje = buildViaje(1L, EstadoViaje.ACEPTADO, 2L);
        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));
        when(posicionRepository.save(any())).thenAnswer(inv -> {
            PosicionViaje p = inv.getArgument(0);
            p.setId(1L);
            p.setTimestamp(LocalDateTime.now());
            return p;
        });
        when(camionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new PosicionViajeRequest(-33.0, -64.0, null, null);
        PosicionViajeResponse result = trackingService.registrarPosicion(1L, request, 2L, Role.CHOFER);

        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    void registrarPosicion_asAdmin_success() {
        Viaje viaje = buildViaje(1L, EstadoViaje.EN_CAMINO, 2L);
        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));
        when(posicionRepository.save(any())).thenAnswer(inv -> {
            PosicionViaje p = inv.getArgument(0);
            p.setId(1L);
            p.setTimestamp(LocalDateTime.now());
            return p;
        });
        when(camionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new PosicionViajeRequest(-33.0, -64.0, null, null);
        PosicionViajeResponse result = trackingService.registrarPosicion(1L, request, 99L, Role.ADMIN);

        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    void registrarPosicion_completado_throws() {
        Viaje viaje = buildViaje(1L, EstadoViaje.COMPLETADO, 2L);
        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));

        var request = new PosicionViajeRequest(-33.0, -64.0, null, null);

        assertThatThrownBy(() -> trackingService.registrarPosicion(1L, request, 2L, Role.CHOFER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ACEPTADO o EN_CAMINO");
    }

    @Test
    void registrarPosicion_solicitado_throws() {
        Viaje viaje = buildViaje(1L, EstadoViaje.SOLICITADO, 2L);
        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));

        var request = new PosicionViajeRequest(-33.0, -64.0, null, null);

        assertThatThrownBy(() -> trackingService.registrarPosicion(1L, request, 2L, Role.CHOFER))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void registrarPosicion_notChofer_throwsForbidden() {
        Viaje viaje = buildViaje(1L, EstadoViaje.EN_CAMINO, 2L);
        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));

        var request = new PosicionViajeRequest(-33.0, -64.0, null, null);

        assertThatThrownBy(() -> trackingService.registrarPosicion(1L, request, 999L, Role.CHOFER))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void registrarPosicion_viajeNotFound_throws() {
        when(viajeRepository.findById(99L)).thenReturn(Optional.empty());

        var request = new PosicionViajeRequest(-33.0, -64.0, null, null);

        assertThatThrownBy(() -> trackingService.registrarPosicion(99L, request, 2L, Role.CHOFER))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void registrarPosicion_noCamion_doesNotUpdateCamion() {
        Viaje viaje = Viaje.builder().id(1L)
                .origenLat(-32.0).origenLng(-63.0).destinoLat(-34.0).destinoLng(-58.0)
                .distanciaKm(400.0).precio(50000.0).tipoTarifa(TipoTarifa.POR_KM)
                .estado(EstadoViaje.EN_CAMINO).choferId(2L).build();
        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));
        when(posicionRepository.save(any())).thenAnswer(inv -> {
            PosicionViaje p = inv.getArgument(0);
            p.setId(1L);
            p.setTimestamp(LocalDateTime.now());
            return p;
        });

        var request = new PosicionViajeRequest(-33.0, -64.0, null, null);
        trackingService.registrarPosicion(1L, request, 2L, Role.CHOFER);

        verify(camionRepository, never()).save(any());
    }

    @Test
    void obtenerUltimaPosicion_success() {
        Viaje viaje = buildViaje(1L, EstadoViaje.EN_CAMINO, 2L);
        PosicionViaje p = buildPosicion(1L, viaje, -33.5, -64.5, LocalDateTime.now());
        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));
        when(posicionRepository.findFirstByViajeIdOrderByTimestampDesc(1L)).thenReturn(Optional.of(p));

        PosicionViajeResponse result = trackingService.obtenerUltimaPosicion(1L);

        assertThat(result.lat()).isEqualTo(-33.5);
        assertThat(result.lng()).isEqualTo(-64.5);
    }

    @Test
    void obtenerUltimaPosicion_noPosiciones_throws() {
        Viaje viaje = buildViaje(1L, EstadoViaje.EN_CAMINO, 2L);
        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));
        when(posicionRepository.findFirstByViajeIdOrderByTimestampDesc(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trackingService.obtenerUltimaPosicion(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No hay posiciones");
    }

    @Test
    void obtenerRuta_withPoints() {
        Viaje viaje = buildViaje(1L, EstadoViaje.EN_CAMINO, 2L);
        LocalDateTime t1 = LocalDateTime.of(2025, 6, 1, 10, 0);
        LocalDateTime t2 = LocalDateTime.of(2025, 6, 1, 11, 30);
        PosicionViaje p1 = buildPosicion(1L, viaje, -32.0, -63.0, t1);
        PosicionViaje p2 = buildPosicion(2L, viaje, -33.0, -64.0, t2);

        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));
        when(posicionRepository.findByViajeIdOrderByTimestampAsc(1L)).thenReturn(List.of(p1, p2));

        RutaResponse result = trackingService.obtenerRuta(1L);

        assertThat(result.viajeId()).isEqualTo(1L);
        assertThat(result.totalPuntos()).isEqualTo(2);
        assertThat(result.distanciaRecorridaKm()).isGreaterThan(0);
        assertThat(result.tiempoTranscurridoMinutos()).isEqualTo(90);
        assertThat(result.posicionActual().lat()).isEqualTo(-33.0);
        assertThat(result.puntos()).hasSize(2);
    }

    @Test
    void obtenerRuta_empty() {
        Viaje viaje = buildViaje(1L, EstadoViaje.SOLICITADO, 2L);
        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));
        when(posicionRepository.findByViajeIdOrderByTimestampAsc(1L)).thenReturn(List.of());

        RutaResponse result = trackingService.obtenerRuta(1L);

        assertThat(result.totalPuntos()).isZero();
        assertThat(result.distanciaRecorridaKm()).isZero();
        assertThat(result.tiempoTranscurridoMinutos()).isNull();
        assertThat(result.posicionActual()).isNull();
    }

    @Test
    void obtenerRuta_singlePoint() {
        Viaje viaje = buildViaje(1L, EstadoViaje.EN_CAMINO, 2L);
        PosicionViaje p = buildPosicion(1L, viaje, -32.0, -63.0, LocalDateTime.now());
        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));
        when(posicionRepository.findByViajeIdOrderByTimestampAsc(1L)).thenReturn(List.of(p));

        RutaResponse result = trackingService.obtenerRuta(1L);

        assertThat(result.totalPuntos()).isEqualTo(1);
        assertThat(result.distanciaRecorridaKm()).isZero();
        assertThat(result.tiempoTranscurridoMinutos()).isNull();
        assertThat(result.posicionActual()).isNotNull();
    }

    @Test
    void haversine_knownDistance() {
        double dist = TrackingService.haversine(-32.0, -63.0, -33.0, -64.0);
        assertThat(dist).isBetween(140.0, 150.0);
    }

    @Test
    void haversine_samePoint_returnsZero() {
        double dist = TrackingService.haversine(-32.0, -63.0, -32.0, -63.0);
        assertThat(dist).isEqualTo(0.0);
    }

    @Test
    void registrarPosicion_nullChoferId_throwsForbidden() {
        Viaje viaje = Viaje.builder().id(1L)
                .origenLat(-32.0).origenLng(-63.0).destinoLat(-34.0).destinoLng(-58.0)
                .distanciaKm(400.0).precio(50000.0).tipoTarifa(TipoTarifa.POR_KM)
                .estado(EstadoViaje.EN_CAMINO).build();
        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));

        var request = new PosicionViajeRequest(-33.0, -64.0, null, null);

        assertThatThrownBy(() -> trackingService.registrarPosicion(1L, request, 2L, Role.CHOFER))
                .isInstanceOf(ForbiddenException.class);
    }
}
