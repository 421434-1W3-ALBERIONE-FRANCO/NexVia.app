package com.nexvia.services;

import com.nexvia.domain.Notificacion;
import com.nexvia.domain.TipoNotificacion;
import com.nexvia.dtos.NotificacionCountResponse;
import com.nexvia.dtos.NotificacionResponse;
import com.nexvia.exceptions.ForbiddenException;
import com.nexvia.exceptions.ResourceNotFoundException;
import com.nexvia.repositories.NotificacionRepository;
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
class NotificacionServiceTest {

    @Mock
    private NotificacionRepository notificacionRepository;

    @InjectMocks
    private NotificacionService notificacionService;

    private Notificacion buildNotificacion(Long id, Long destinatarioId, boolean leida) {
        Notificacion n = Notificacion.builder()
                .id(id).tipo(TipoNotificacion.VIAJE_ACEPTADO)
                .mensaje("Viaje aceptado").destinatarioId(destinatarioId)
                .viajeId(10L).leida(leida).build();
        n.setCreatedAt(LocalDateTime.now());
        return n;
    }

    @Test
    void crearNotificacion_saves() {
        when(notificacionRepository.save(any())).thenAnswer(inv -> {
            Notificacion n = inv.getArgument(0);
            n.setId(1L);
            return n;
        });

        notificacionService.crearNotificacion(
                TipoNotificacion.VIAJE_ACEPTADO, "Test", 1L, 10L);

        verify(notificacionRepository).save(any());
    }

    @Test
    void listarPorUsuario_returnsAll() {
        when(notificacionRepository.findByDestinatarioIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(buildNotificacion(1L, 1L, false), buildNotificacion(2L, 1L, true)));

        List<NotificacionResponse> result = notificacionService.listarPorUsuario(1L);

        assertThat(result).hasSize(2);
    }

    @Test
    void listarNoLeidas_returnsUnread() {
        when(notificacionRepository.findByDestinatarioIdAndLeidaFalseOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(buildNotificacion(1L, 1L, false)));

        List<NotificacionResponse> result = notificacionService.listarNoLeidas(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).leida()).isFalse();
    }

    @Test
    void contarNoLeidas_returnsCount() {
        when(notificacionRepository.countByDestinatarioIdAndLeidaFalse(1L)).thenReturn(5L);

        NotificacionCountResponse result = notificacionService.contarNoLeidas(1L);

        assertThat(result.noLeidas()).isEqualTo(5);
    }

    @Test
    void marcarLeida_success() {
        Notificacion n = buildNotificacion(1L, 1L, false);
        when(notificacionRepository.findById(1L)).thenReturn(Optional.of(n));
        when(notificacionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NotificacionResponse result = notificacionService.marcarLeida(1L, 1L);

        assertThat(result.leida()).isTrue();
    }

    @Test
    void marcarLeida_notFound_throws() {
        when(notificacionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificacionService.marcarLeida(99L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void marcarLeida_wrongUser_throwsForbidden() {
        Notificacion n = buildNotificacion(1L, 1L, false);
        when(notificacionRepository.findById(1L)).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> notificacionService.marcarLeida(1L, 999L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void marcarTodasLeidas_updatesAll() {
        Notificacion n1 = buildNotificacion(1L, 1L, false);
        Notificacion n2 = buildNotificacion(2L, 1L, false);
        when(notificacionRepository.findByDestinatarioIdAndLeidaFalseOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(n1, n2));

        notificacionService.marcarTodasLeidas(1L);

        assertThat(n1.getLeida()).isTrue();
        assertThat(n2.getLeida()).isTrue();
        verify(notificacionRepository).saveAll(any());
    }

    @Test
    void marcarTodasLeidas_noUnread_savesEmpty() {
        when(notificacionRepository.findByDestinatarioIdAndLeidaFalseOrderByCreatedAtDesc(1L))
                .thenReturn(List.of());

        notificacionService.marcarTodasLeidas(1L);

        verify(notificacionRepository).saveAll(List.of());
    }
}
