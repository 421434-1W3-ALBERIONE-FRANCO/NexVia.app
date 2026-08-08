package com.nexvia.services;

import com.nexvia.domain.*;
import com.nexvia.dtos.CamionRequest;
import com.nexvia.dtos.CamionResponse;
import com.nexvia.dtos.PosicionRequest;
import com.nexvia.exceptions.ForbiddenException;
import com.nexvia.exceptions.ResourceNotFoundException;
import com.nexvia.repositories.CamionRepository;
import com.nexvia.repositories.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class CamionServiceTest {

    @Mock
    private CamionRepository camionRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private CamionService camionService;

    private Usuario buildUsuario(Long id) {
        return Usuario.builder().id(id).email("chofer@mail.com").fullName("Chofer").role(Role.CHOFER).build();
    }

    private Camion buildCamion(Long id, Usuario usuario) {
        return Camion.builder()
                .id(id).choferNombre("Juan").patente("ABC123")
                .capacidadKg(10000).lat(-32.0).lng(-63.0)
                .estado(EstadoCamion.DISPONIBLE).usuario(usuario)
                .build();
    }

    @Test
    void listar_returnsAll() {
        Usuario u = buildUsuario(1L);
        when(camionRepository.findAll()).thenReturn(List.of(buildCamion(1L, u)));

        List<CamionResponse> result = camionService.listar();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).choferNombre()).isEqualTo("Juan");
    }

    @Test
    void listarDisponibles_filtersCorrectly() {
        Usuario u = buildUsuario(1L);
        when(camionRepository.findByEstado(EstadoCamion.DISPONIBLE)).thenReturn(List.of(buildCamion(1L, u)));

        List<CamionResponse> result = camionService.listarDisponibles();

        assertThat(result).hasSize(1);
    }

    @Test
    void obtener_success() {
        Usuario u = buildUsuario(1L);
        when(camionRepository.findById(1L)).thenReturn(Optional.of(buildCamion(1L, u)));

        CamionResponse result = camionService.obtener(1L);

        assertThat(result.patente()).isEqualTo("ABC123");
        assertThat(result.userId()).isEqualTo(1L);
    }

    @Test
    void obtener_notFound() {
        when(camionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> camionService.obtener(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void miCamion_success() {
        Usuario u = buildUsuario(5L);
        when(camionRepository.findFirstByUsuarioId(5L)).thenReturn(Optional.of(buildCamion(1L, u)));

        CamionResponse result = camionService.miCamion(5L);

        assertThat(result.userId()).isEqualTo(5L);
    }

    @Test
    void miCamion_notFound() {
        when(camionRepository.findFirstByUsuarioId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> camionService.miCamion(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("camión asignado");
    }

    @Test
    void crear_success() {
        Usuario u = buildUsuario(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));
        when(camionRepository.save(any())).thenAnswer(inv -> {
            Camion c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        var request = new CamionRequest("Trans SA", "20-123", "Juan", "20-456", "ABC123", "XYZ789", "3512345", 10000, -32.0, -63.0);
        CamionResponse result = camionService.crear(request, 1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.estado()).isEqualTo("DISPONIBLE");
        assertThat(result.transporteNombre()).isEqualTo("Trans SA");
    }

    @Test
    void crear_nullCapacidadAndCoords_usesDefaults() {
        Usuario u = buildUsuario(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));
        when(camionRepository.save(any())).thenAnswer(inv -> {
            Camion c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        var request = new CamionRequest(null, null, "Juan", null, "ABC123", null, null, null, null, null);
        CamionResponse result = camionService.crear(request, 1L);

        assertThat(result.capacidadKg()).isEqualTo(0);
        assertThat(result.lat()).isEqualTo(-32.4341);
        assertThat(result.lng()).isEqualTo(-63.2433);
    }

    @Test
    void crear_userNotFound() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());
        var request = new CamionRequest(null, null, "Juan", null, "ABC123", null, null, null, null, null);

        assertThatThrownBy(() -> camionService.crear(request, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void actualizar_asOwner_success() {
        Usuario u = buildUsuario(1L);
        Camion camion = buildCamion(1L, u);
        when(camionRepository.findById(1L)).thenReturn(Optional.of(camion));
        when(camionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new CamionRequest("Nueva Trans", null, "Pedro", null, "NEW123", null, null, 20000, -33.0, -64.0);
        CamionResponse result = camionService.actualizar(1L, request, 1L, Role.CHOFER);

        assertThat(result.choferNombre()).isEqualTo("Pedro");
        assertThat(result.patente()).isEqualTo("NEW123");
        assertThat(result.capacidadKg()).isEqualTo(20000);
        assertThat(result.lat()).isEqualTo(-33.0);
    }

    @Test
    void actualizar_asAdmin_success() {
        Usuario u = buildUsuario(1L);
        Camion camion = buildCamion(1L, u);
        when(camionRepository.findById(1L)).thenReturn(Optional.of(camion));
        when(camionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new CamionRequest(null, null, "Admin Edit", null, "ADM123", null, null, null, null, null);
        CamionResponse result = camionService.actualizar(1L, request, 999L, Role.ADMIN);

        assertThat(result.choferNombre()).isEqualTo("Admin Edit");
        assertThat(result.capacidadKg()).isEqualTo(0);
    }

    @Test
    void actualizar_nullLatLng_keepsExisting() {
        Usuario u = buildUsuario(1L);
        Camion camion = buildCamion(1L, u);
        camion.setLat(-32.5);
        camion.setLng(-63.5);
        when(camionRepository.findById(1L)).thenReturn(Optional.of(camion));
        when(camionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new CamionRequest(null, null, "Juan", null, "ABC123", null, null, null, null, null);
        CamionResponse result = camionService.actualizar(1L, request, 1L, Role.CHOFER);

        assertThat(result.lat()).isEqualTo(-32.5);
        assertThat(result.lng()).isEqualTo(-63.5);
    }

    @Test
    void actualizar_notOwner_throwsForbidden() {
        Usuario u = buildUsuario(1L);
        Camion camion = buildCamion(1L, u);
        when(camionRepository.findById(1L)).thenReturn(Optional.of(camion));

        var request = new CamionRequest(null, null, "X", null, "X", null, null, null, null, null);

        assertThatThrownBy(() -> camionService.actualizar(1L, request, 999L, Role.CHOFER))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void actualizar_nullUsuario_throwsForbidden() {
        Camion camion = buildCamion(1L, null);
        when(camionRepository.findById(1L)).thenReturn(Optional.of(camion));

        var request = new CamionRequest(null, null, "X", null, "X", null, null, null, null, null);

        assertThatThrownBy(() -> camionService.actualizar(1L, request, 1L, Role.CHOFER))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void actualizar_notFound() {
        when(camionRepository.findById(99L)).thenReturn(Optional.empty());
        var request = new CamionRequest(null, null, "X", null, "X", null, null, null, null, null);

        assertThatThrownBy(() -> camionService.actualizar(99L, request, 1L, Role.CHOFER))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void actualizarPosicion_asOwner_success() {
        Usuario u = buildUsuario(1L);
        Camion camion = buildCamion(1L, u);
        when(camionRepository.findById(1L)).thenReturn(Optional.of(camion));
        when(camionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CamionResponse result = camionService.actualizarPosicion(1L, new PosicionRequest(-34.0, -58.0), 1L, Role.CHOFER);

        assertThat(result.lat()).isEqualTo(-34.0);
        assertThat(result.lng()).isEqualTo(-58.0);
    }

    @Test
    void actualizarPosicion_notOwner_throwsForbidden() {
        Usuario u = buildUsuario(1L);
        Camion camion = buildCamion(1L, u);
        when(camionRepository.findById(1L)).thenReturn(Optional.of(camion));

        assertThatThrownBy(() -> camionService.actualizarPosicion(1L, new PosicionRequest(-34.0, -58.0), 999L, Role.CHOFER))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void eliminar_success() {
        when(camionRepository.existsById(1L)).thenReturn(true);

        camionService.eliminar(1L);

        verify(camionRepository).deleteById(1L);
    }

    @Test
    void eliminar_notFound() {
        when(camionRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> camionService.eliminar(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void toResponse_nullUsuario_returnsNullUserId() {
        Camion camion = buildCamion(1L, null);
        when(camionRepository.findById(1L)).thenReturn(Optional.of(camion));

        CamionResponse result = camionService.obtener(1L);

        assertThat(result.userId()).isNull();
    }
}
