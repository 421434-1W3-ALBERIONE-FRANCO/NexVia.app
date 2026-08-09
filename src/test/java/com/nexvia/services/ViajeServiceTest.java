package com.nexvia.services;

import com.nexvia.domain.*;
import com.nexvia.dtos.ViajeRequest;
import com.nexvia.dtos.ViajeResponse;
import com.nexvia.exceptions.ForbiddenException;
import com.nexvia.exceptions.ResourceNotFoundException;
import com.nexvia.repositories.CamionRepository;
import com.nexvia.repositories.UsuarioRepository;
import com.nexvia.repositories.ViajeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ViajeServiceTest {

    @Mock
    private ViajeRepository viajeRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CamionRepository camionRepository;

    @Mock
    private NotificacionService notificacionService;

    @InjectMocks
    private ViajeService viajeService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(viajeService, "ventanaGratisMinutos", 30);
        ReflectionTestUtils.setField(viajeService, "penalidadPorcentaje", 10);
    }

    private Usuario buildUsuario(Long id, Role role) {
        return Usuario.builder().id(id).email("user@mail.com").fullName("User " + id).role(role).build();
    }

    private Camion buildCamion(Long id, Usuario owner) {
        return Camion.builder().id(id).choferNombre("Chofer").patente("ABC123")
                .estado(EstadoCamion.DISPONIBLE).usuario(owner).build();
    }

    private Viaje buildViaje(Long id, EstadoViaje estado, Usuario usuario) {
        return Viaje.builder().id(id)
                .origenLat(-32.0).origenLng(-63.0)
                .destinoLat(-34.0).destinoLng(-58.0)
                .distanciaKm(400.0).toneladas(10.0)
                .tipoTarifa(TipoTarifa.POR_KM).precio(50000.0)
                .carga("Soja").estado(estado).usuario(usuario)
                .usuarioNombre(usuario.getFullName())
                .build();
    }

    private ViajeRequest sampleRequest() {
        return new ViajeRequest(-32.0, -63.0, -34.0, -58.0, 400.0, 10.0, "POR_KM", 50000.0, "Soja");
    }

    @Test
    void listar_returnsAll() {
        Usuario u = buildUsuario(1L, Role.USUARIO);
        when(viajeRepository.findAll()).thenReturn(List.of(buildViaje(1L, EstadoViaje.SOLICITADO, u)));

        List<ViajeResponse> result = viajeService.listar();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).carga()).isEqualTo("Soja");
    }

    @Test
    void listarPorEstado_filtersCorrectly() {
        Usuario u = buildUsuario(1L, Role.USUARIO);
        when(viajeRepository.findByEstado(EstadoViaje.SOLICITADO))
                .thenReturn(List.of(buildViaje(1L, EstadoViaje.SOLICITADO, u)));

        List<ViajeResponse> result = viajeService.listarPorEstado(EstadoViaje.SOLICITADO);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).estado()).isEqualTo("SOLICITADO");
    }

    @Test
    void misViajes_returnsUserTrips() {
        Usuario u = buildUsuario(1L, Role.USUARIO);
        when(viajeRepository.findByUsuarioIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(buildViaje(1L, EstadoViaje.SOLICITADO, u)));

        List<ViajeResponse> result = viajeService.misViajes(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void viajesDelCamion_returnsTruckTrips() {
        Usuario u = buildUsuario(1L, Role.USUARIO);
        when(viajeRepository.findByCamionIdOrderByCreatedAtDesc(5L))
                .thenReturn(List.of(buildViaje(1L, EstadoViaje.ACEPTADO, u)));

        List<ViajeResponse> result = viajeService.viajesDelCamion(5L);

        assertThat(result).hasSize(1);
    }

    @Test
    void obtener_success() {
        Usuario u = buildUsuario(1L, Role.USUARIO);
        when(viajeRepository.findById(1L)).thenReturn(Optional.of(buildViaje(1L, EstadoViaje.SOLICITADO, u)));

        ViajeResponse result = viajeService.obtener(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.carga()).isEqualTo("Soja");
    }

    @Test
    void obtener_notFound() {
        when(viajeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> viajeService.obtener(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void crear_success() {
        Usuario u = buildUsuario(1L, Role.USUARIO);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));
        when(viajeRepository.save(any())).thenAnswer(inv -> {
            Viaje v = inv.getArgument(0);
            v.setId(1L);
            return v;
        });

        ViajeResponse result = viajeService.crear(sampleRequest(), 1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.estado()).isEqualTo("SOLICITADO");
        assertThat(result.usuarioNombre()).isEqualTo("User 1");
    }

    @Test
    void crear_nullToneladasAndTarifa_usesDefaults() {
        Usuario u = buildUsuario(1L, Role.USUARIO);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));
        when(viajeRepository.save(any())).thenAnswer(inv -> {
            Viaje v = inv.getArgument(0);
            v.setId(1L);
            return v;
        });

        var request = new ViajeRequest(-32.0, -63.0, -34.0, -58.0, 400.0, null, null, 50000.0, "Soja");
        ViajeResponse result = viajeService.crear(request, 1L);

        assertThat(result.toneladas()).isEqualTo(0.0);
        assertThat(result.tipoTarifa()).isEqualTo("POR_KM");
    }

    @Test
    void crear_invalidTipoTarifa_throws() {
        Usuario u = buildUsuario(1L, Role.USUARIO);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));

        var request = new ViajeRequest(-32.0, -63.0, -34.0, -58.0, 400.0, null, "INVALIDO", 50000.0, "Soja");

        assertThatThrownBy(() -> viajeService.crear(request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tipo de tarifa inválido");
    }

    @Test
    void crear_userNotFound() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> viajeService.crear(sampleRequest(), 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void aceptar_success() {
        Usuario usuario = buildUsuario(1L, Role.USUARIO);
        Usuario chofer = buildUsuario(2L, Role.CHOFER);
        Viaje viaje = buildViaje(1L, EstadoViaje.SOLICITADO, usuario);
        Camion camion = buildCamion(5L, chofer);

        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));
        when(camionRepository.findById(5L)).thenReturn(Optional.of(camion));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(chofer));
        when(viajeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(camionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ViajeResponse result = viajeService.aceptar(1L, 5L, 2L);

        assertThat(result.estado()).isEqualTo("ACEPTADO");
        assertThat(result.camionId()).isEqualTo(5L);
        assertThat(result.choferId()).isEqualTo(2L);
        assertThat(camion.getEstado()).isEqualTo(EstadoCamion.OCUPADO);
    }

    @Test
    void aceptar_wrongState_throws() {
        Usuario u = buildUsuario(1L, Role.USUARIO);
        Viaje viaje = buildViaje(1L, EstadoViaje.ACEPTADO, u);
        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));

        assertThatThrownBy(() -> viajeService.aceptar(1L, 5L, 2L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aceptar_notOwnerOfCamion_throwsForbidden() {
        Usuario usuario = buildUsuario(1L, Role.USUARIO);
        Usuario chofer = buildUsuario(2L, Role.CHOFER);
        Viaje viaje = buildViaje(1L, EstadoViaje.SOLICITADO, usuario);
        Camion camion = buildCamion(5L, chofer);

        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));
        when(camionRepository.findById(5L)).thenReturn(Optional.of(camion));

        assertThatThrownBy(() -> viajeService.aceptar(1L, 5L, 999L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void aceptar_camionNotFound() {
        Usuario u = buildUsuario(1L, Role.USUARIO);
        Viaje viaje = buildViaje(1L, EstadoViaje.SOLICITADO, u);
        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));
        when(camionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> viajeService.aceptar(1L, 99L, 2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void aceptar_camionNullUsuario_throwsForbidden() {
        Usuario usuario = buildUsuario(1L, Role.USUARIO);
        Viaje viaje = buildViaje(1L, EstadoViaje.SOLICITADO, usuario);
        Camion camion = buildCamion(5L, null);

        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));
        when(camionRepository.findById(5L)).thenReturn(Optional.of(camion));

        assertThatThrownBy(() -> viajeService.aceptar(1L, 5L, 2L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void avanzarEnCamino_asChofer_success() {
        Usuario u = buildUsuario(1L, Role.USUARIO);
        Viaje viaje = buildViaje(1L, EstadoViaje.ACEPTADO, u);
        viaje.setChoferId(2L);

        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));
        when(viajeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ViajeResponse result = viajeService.avanzarEnCamino(1L, 2L, Role.CHOFER);

        assertThat(result.estado()).isEqualTo("EN_CAMINO");
    }

    @Test
    void avanzarEnCamino_asAdmin_success() {
        Usuario u = buildUsuario(1L, Role.USUARIO);
        Viaje viaje = buildViaje(1L, EstadoViaje.ACEPTADO, u);

        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));
        when(viajeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ViajeResponse result = viajeService.avanzarEnCamino(1L, 99L, Role.ADMIN);

        assertThat(result.estado()).isEqualTo("EN_CAMINO");
    }

    @Test
    void avanzarEnCamino_wrongState_throws() {
        Usuario u = buildUsuario(1L, Role.USUARIO);
        Viaje viaje = buildViaje(1L, EstadoViaje.SOLICITADO, u);
        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));

        assertThatThrownBy(() -> viajeService.avanzarEnCamino(1L, 2L, Role.CHOFER))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void avanzarEnCamino_notChofer_throwsForbidden() {
        Usuario u = buildUsuario(1L, Role.USUARIO);
        Viaje viaje = buildViaje(1L, EstadoViaje.ACEPTADO, u);
        viaje.setChoferId(2L);
        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));

        assertThatThrownBy(() -> viajeService.avanzarEnCamino(1L, 999L, Role.CHOFER))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void avanzarEnCamino_nullChoferId_throwsForbidden() {
        Usuario u = buildUsuario(1L, Role.USUARIO);
        Viaje viaje = buildViaje(1L, EstadoViaje.ACEPTADO, u);
        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));

        assertThatThrownBy(() -> viajeService.avanzarEnCamino(1L, 2L, Role.CHOFER))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void completar_asChofer_success() {
        Usuario u = buildUsuario(1L, Role.USUARIO);
        Camion camion = buildCamion(5L, buildUsuario(2L, Role.CHOFER));
        camion.setEstado(EstadoCamion.OCUPADO);
        Viaje viaje = buildViaje(1L, EstadoViaje.EN_CAMINO, u);
        viaje.setChoferId(2L);
        viaje.setCamion(camion);

        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));
        when(viajeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(camionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ViajeResponse result = viajeService.completar(1L, 2L, Role.CHOFER);

        assertThat(result.estado()).isEqualTo("COMPLETADO");
        assertThat(camion.getEstado()).isEqualTo(EstadoCamion.DISPONIBLE);
    }

    @Test
    void completar_wrongState_throws() {
        Usuario u = buildUsuario(1L, Role.USUARIO);
        Viaje viaje = buildViaje(1L, EstadoViaje.ACEPTADO, u);
        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));

        assertThatThrownBy(() -> viajeService.completar(1L, 2L, Role.CHOFER))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void completar_noCamion_success() {
        Usuario u = buildUsuario(1L, Role.USUARIO);
        Viaje viaje = buildViaje(1L, EstadoViaje.EN_CAMINO, u);
        viaje.setChoferId(2L);

        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));
        when(viajeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ViajeResponse result = viajeService.completar(1L, 2L, Role.CHOFER);

        assertThat(result.estado()).isEqualTo("COMPLETADO");
        verify(camionRepository, never()).save(any());
    }

    @Test
    void cancelar_solicitado_noPenalty() {
        Usuario u = buildUsuario(1L, Role.USUARIO);
        Viaje viaje = buildViaje(1L, EstadoViaje.SOLICITADO, u);

        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));
        when(viajeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ViajeResponse result = viajeService.cancelar(1L, "No quiero", 1L, Role.USUARIO);

        assertThat(result.estado()).isEqualTo("CANCELADO");
        assertThat(result.motivoCancelacion()).isEqualTo("No quiero");
        assertThat(result.canceladoPorId()).isEqualTo(1L);
        assertThat(result.canceladoAt()).isNotNull();
        assertThat(result.penalidad()).isEqualTo(0.0);
    }

    @Test
    void cancelar_aceptado_withinWindow_noPenalty() {
        Usuario u = buildUsuario(1L, Role.USUARIO);
        Viaje viaje = buildViaje(1L, EstadoViaje.ACEPTADO, u);
        viaje.setChoferId(2L);
        viaje.setCreatedAt(LocalDateTime.now().minusMinutes(10));

        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));
        when(viajeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ViajeResponse result = viajeService.cancelar(1L, "Cambio de planes", 2L, Role.CHOFER);

        assertThat(result.penalidad()).isEqualTo(0.0);
    }

    @Test
    void cancelar_aceptado_outsideWindow_hasPenalty() {
        Usuario u = buildUsuario(1L, Role.USUARIO);
        Viaje viaje = buildViaje(1L, EstadoViaje.ACEPTADO, u);
        viaje.setChoferId(2L);
        viaje.setCreatedAt(LocalDateTime.now().minusMinutes(60));

        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));
        when(viajeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ViajeResponse result = viajeService.cancelar(1L, "Emergencia", 2L, Role.CHOFER);

        assertThat(result.penalidad()).isEqualTo(5000.0);
    }

    @Test
    void cancelar_enCamino_liberaCamion() {
        Usuario u = buildUsuario(1L, Role.USUARIO);
        Camion camion = buildCamion(5L, buildUsuario(2L, Role.CHOFER));
        camion.setEstado(EstadoCamion.OCUPADO);
        Viaje viaje = buildViaje(1L, EstadoViaje.EN_CAMINO, u);
        viaje.setChoferId(2L);
        viaje.setCamion(camion);
        viaje.setCreatedAt(LocalDateTime.now().minusHours(2));

        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));
        when(viajeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(camionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ViajeResponse result = viajeService.cancelar(1L, "Problema mecánico", 2L, Role.CHOFER);

        assertThat(result.estado()).isEqualTo("CANCELADO");
        assertThat(camion.getEstado()).isEqualTo(EstadoCamion.DISPONIBLE);
        assertThat(result.penalidad()).isEqualTo(5000.0);
    }

    @Test
    void cancelar_asAdmin_success() {
        Usuario u = buildUsuario(1L, Role.USUARIO);
        Viaje viaje = buildViaje(1L, EstadoViaje.EN_CAMINO, u);
        viaje.setCreatedAt(LocalDateTime.now().minusHours(1));

        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));
        when(viajeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ViajeResponse result = viajeService.cancelar(1L, "Admin cancel", 99L, Role.ADMIN);

        assertThat(result.estado()).isEqualTo("CANCELADO");
    }

    @Test
    void cancelar_completado_throws() {
        Usuario u = buildUsuario(1L, Role.USUARIO);
        Viaje viaje = buildViaje(1L, EstadoViaje.COMPLETADO, u);
        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));

        assertThatThrownBy(() -> viajeService.cancelar(1L, "motivo", 1L, Role.USUARIO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cancelar_cancelado_throws() {
        Usuario u = buildUsuario(1L, Role.USUARIO);
        Viaje viaje = buildViaje(1L, EstadoViaje.CANCELADO, u);
        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));

        assertThatThrownBy(() -> viajeService.cancelar(1L, "motivo", 1L, Role.USUARIO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cancelar_notParticipant_throwsForbidden() {
        Usuario u = buildUsuario(1L, Role.USUARIO);
        Viaje viaje = buildViaje(1L, EstadoViaje.SOLICITADO, u);
        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));

        assertThatThrownBy(() -> viajeService.cancelar(1L, "motivo", 999L, Role.CHOFER))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void cancelar_nullUsuarioAndNullChofer_throwsForbidden() {
        Viaje viaje = Viaje.builder().id(1L)
                .origenLat(-32.0).origenLng(-63.0)
                .destinoLat(-34.0).destinoLng(-58.0)
                .distanciaKm(400.0).precio(50000.0)
                .tipoTarifa(TipoTarifa.POR_KM)
                .estado(EstadoViaje.SOLICITADO)
                .build();
        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));

        assertThatThrownBy(() -> viajeService.cancelar(1L, "motivo", 1L, Role.CHOFER))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void toResponse_nullUsuarioAndCamion() {
        Viaje viaje = Viaje.builder().id(1L)
                .origenLat(-32.0).origenLng(-63.0)
                .destinoLat(-34.0).destinoLng(-58.0)
                .distanciaKm(400.0).toneladas(0.0).precio(50000.0)
                .tipoTarifa(TipoTarifa.POR_KM).carga("Soja")
                .estado(EstadoViaje.SOLICITADO)
                .build();
        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));

        ViajeResponse result = viajeService.obtener(1L);

        assertThat(result.usuarioId()).isNull();
        assertThat(result.camionId()).isNull();
    }

    @Test
    void crear_tipoTarifaPorTonelada_success() {
        Usuario u = buildUsuario(1L, Role.USUARIO);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));
        when(viajeRepository.save(any())).thenAnswer(inv -> {
            Viaje v = inv.getArgument(0);
            v.setId(1L);
            return v;
        });

        var request = new ViajeRequest(-32.0, -63.0, -34.0, -58.0, 400.0, 20.0, "por_tonelada", 80000.0, "Maíz");
        ViajeResponse result = viajeService.crear(request, 1L);

        assertThat(result.tipoTarifa()).isEqualTo("POR_TONELADA");
    }

    @Test
    void calcularPenalidad_nullCreatedAt_hasPenalty() {
        Viaje viaje = Viaje.builder().id(1L).precio(10000.0)
                .estado(EstadoViaje.ACEPTADO)
                .tipoTarifa(TipoTarifa.POR_KM).build();

        double penalidad = viajeService.calcularPenalidad(viaje);

        assertThat(penalidad).isEqualTo(1000.0);
    }
}
