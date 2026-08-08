package com.nexvia.services;

import com.nexvia.domain.*;
import com.nexvia.dtos.LugarGuardadoRequest;
import com.nexvia.dtos.LugarGuardadoResponse;
import com.nexvia.exceptions.ForbiddenException;
import com.nexvia.exceptions.ResourceNotFoundException;
import com.nexvia.repositories.LugarGuardadoRepository;
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
class LugarGuardadoServiceTest {

    @Mock
    private LugarGuardadoRepository lugarGuardadoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private LugarGuardadoService lugarGuardadoService;

    private Usuario buildUsuario(Long id) {
        return Usuario.builder().id(id).email("user@mail.com").fullName("User " + id).role(Role.USUARIO).build();
    }

    private LugarGuardado buildLugar(Long id, Usuario usuario) {
        return LugarGuardado.builder()
                .id(id).nombre("Campo Norte").lat(-32.0).lng(-63.0)
                .tipo(TipoLugar.CAMPO).usuario(usuario).build();
    }

    @Test
    void listar_returnsUserPlaces() {
        Usuario u = buildUsuario(1L);
        when(lugarGuardadoRepository.findByUsuarioIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(buildLugar(1L, u)));

        List<LugarGuardadoResponse> result = lugarGuardadoService.listar(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).nombre()).isEqualTo("Campo Norte");
    }

    @Test
    void obtener_asOwner_success() {
        Usuario u = buildUsuario(1L);
        when(lugarGuardadoRepository.findById(1L)).thenReturn(Optional.of(buildLugar(1L, u)));

        LugarGuardadoResponse result = lugarGuardadoService.obtener(1L, 1L, Role.USUARIO);

        assertThat(result.nombre()).isEqualTo("Campo Norte");
        assertThat(result.tipo()).isEqualTo("CAMPO");
    }

    @Test
    void obtener_asAdmin_success() {
        Usuario u = buildUsuario(1L);
        when(lugarGuardadoRepository.findById(1L)).thenReturn(Optional.of(buildLugar(1L, u)));

        LugarGuardadoResponse result = lugarGuardadoService.obtener(1L, 99L, Role.ADMIN);

        assertThat(result.nombre()).isEqualTo("Campo Norte");
    }

    @Test
    void obtener_notOwner_throwsForbidden() {
        Usuario u = buildUsuario(1L);
        when(lugarGuardadoRepository.findById(1L)).thenReturn(Optional.of(buildLugar(1L, u)));

        assertThatThrownBy(() -> lugarGuardadoService.obtener(1L, 999L, Role.USUARIO))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void obtener_notFound() {
        when(lugarGuardadoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lugarGuardadoService.obtener(99L, 1L, Role.USUARIO))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void obtener_nullUsuario_throwsForbidden() {
        LugarGuardado lugar = buildLugar(1L, null);
        when(lugarGuardadoRepository.findById(1L)).thenReturn(Optional.of(lugar));

        assertThatThrownBy(() -> lugarGuardadoService.obtener(1L, 1L, Role.USUARIO))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void crear_success() {
        Usuario u = buildUsuario(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));
        when(lugarGuardadoRepository.save(any())).thenAnswer(inv -> {
            LugarGuardado l = inv.getArgument(0);
            l.setId(1L);
            return l;
        });

        var request = new LugarGuardadoRequest("Hacienda Sur", -33.0, -64.0, "hacienda");
        LugarGuardadoResponse result = lugarGuardadoService.crear(request, 1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.nombre()).isEqualTo("Hacienda Sur");
        assertThat(result.tipo()).isEqualTo("HACIENDA");
    }

    @Test
    void crear_nullTipo_defaultsToOtro() {
        Usuario u = buildUsuario(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));
        when(lugarGuardadoRepository.save(any())).thenAnswer(inv -> {
            LugarGuardado l = inv.getArgument(0);
            l.setId(1L);
            return l;
        });

        var request = new LugarGuardadoRequest("Lugar X", -32.0, -63.0, null);
        LugarGuardadoResponse result = lugarGuardadoService.crear(request, 1L);

        assertThat(result.tipo()).isEqualTo("OTRO");
    }

    @Test
    void crear_blankTipo_defaultsToOtro() {
        Usuario u = buildUsuario(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));
        when(lugarGuardadoRepository.save(any())).thenAnswer(inv -> {
            LugarGuardado l = inv.getArgument(0);
            l.setId(1L);
            return l;
        });

        var request = new LugarGuardadoRequest("Lugar Y", -32.0, -63.0, "  ");
        LugarGuardadoResponse result = lugarGuardadoService.crear(request, 1L);

        assertThat(result.tipo()).isEqualTo("OTRO");
    }

    @Test
    void crear_invalidTipo_throws() {
        Usuario u = buildUsuario(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));

        var request = new LugarGuardadoRequest("Lugar Z", -32.0, -63.0, "INVALIDO");

        assertThatThrownBy(() -> lugarGuardadoService.crear(request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tipo de lugar inválido");
    }

    @Test
    void crear_userNotFound() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        var request = new LugarGuardadoRequest("Lugar", -32.0, -63.0, null);

        assertThatThrownBy(() -> lugarGuardadoService.crear(request, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void actualizar_asOwner_success() {
        Usuario u = buildUsuario(1L);
        LugarGuardado lugar = buildLugar(1L, u);
        when(lugarGuardadoRepository.findById(1L)).thenReturn(Optional.of(lugar));
        when(lugarGuardadoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new LugarGuardadoRequest("Nuevo Nombre", -34.0, -58.0, "pueblo");
        LugarGuardadoResponse result = lugarGuardadoService.actualizar(1L, request, 1L, Role.USUARIO);

        assertThat(result.nombre()).isEqualTo("Nuevo Nombre");
        assertThat(result.lat()).isEqualTo(-34.0);
        assertThat(result.tipo()).isEqualTo("PUEBLO");
    }

    @Test
    void actualizar_asAdmin_success() {
        Usuario u = buildUsuario(1L);
        LugarGuardado lugar = buildLugar(1L, u);
        when(lugarGuardadoRepository.findById(1L)).thenReturn(Optional.of(lugar));
        when(lugarGuardadoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new LugarGuardadoRequest("Admin Edit", -34.0, -58.0, "campo");
        LugarGuardadoResponse result = lugarGuardadoService.actualizar(1L, request, 99L, Role.ADMIN);

        assertThat(result.nombre()).isEqualTo("Admin Edit");
    }

    @Test
    void actualizar_notOwner_throwsForbidden() {
        Usuario u = buildUsuario(1L);
        LugarGuardado lugar = buildLugar(1L, u);
        when(lugarGuardadoRepository.findById(1L)).thenReturn(Optional.of(lugar));

        var request = new LugarGuardadoRequest("X", -32.0, -63.0, null);

        assertThatThrownBy(() -> lugarGuardadoService.actualizar(1L, request, 999L, Role.USUARIO))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void actualizar_notFound() {
        when(lugarGuardadoRepository.findById(99L)).thenReturn(Optional.empty());

        var request = new LugarGuardadoRequest("X", -32.0, -63.0, null);

        assertThatThrownBy(() -> lugarGuardadoService.actualizar(99L, request, 1L, Role.USUARIO))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void eliminar_asOwner_success() {
        Usuario u = buildUsuario(1L);
        LugarGuardado lugar = buildLugar(1L, u);
        when(lugarGuardadoRepository.findById(1L)).thenReturn(Optional.of(lugar));

        lugarGuardadoService.eliminar(1L, 1L, Role.USUARIO);

        verify(lugarGuardadoRepository).deleteById(1L);
    }

    @Test
    void eliminar_asAdmin_success() {
        Usuario u = buildUsuario(1L);
        LugarGuardado lugar = buildLugar(1L, u);
        when(lugarGuardadoRepository.findById(1L)).thenReturn(Optional.of(lugar));

        lugarGuardadoService.eliminar(1L, 99L, Role.ADMIN);

        verify(lugarGuardadoRepository).deleteById(1L);
    }

    @Test
    void eliminar_notOwner_throwsForbidden() {
        Usuario u = buildUsuario(1L);
        LugarGuardado lugar = buildLugar(1L, u);
        when(lugarGuardadoRepository.findById(1L)).thenReturn(Optional.of(lugar));

        assertThatThrownBy(() -> lugarGuardadoService.eliminar(1L, 999L, Role.USUARIO))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void eliminar_notFound() {
        when(lugarGuardadoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lugarGuardadoService.eliminar(99L, 1L, Role.USUARIO))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void toResponse_nullUsuario_returnsNullUserId() {
        LugarGuardado lugar = buildLugar(1L, null);
        when(lugarGuardadoRepository.findById(1L)).thenReturn(Optional.of(lugar));

        LugarGuardadoResponse result = lugarGuardadoService.obtener(1L, 99L, Role.ADMIN);

        assertThat(result.usuarioId()).isNull();
    }

    @Test
    void crear_tipoPueblo_success() {
        Usuario u = buildUsuario(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));
        when(lugarGuardadoRepository.save(any())).thenAnswer(inv -> {
            LugarGuardado l = inv.getArgument(0);
            l.setId(1L);
            return l;
        });

        var request = new LugarGuardadoRequest("Villa María", -32.4, -63.2, "PUEBLO");
        LugarGuardadoResponse result = lugarGuardadoService.crear(request, 1L);

        assertThat(result.tipo()).isEqualTo("PUEBLO");
    }
}
