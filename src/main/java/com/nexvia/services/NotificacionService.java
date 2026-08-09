package com.nexvia.services;

import com.nexvia.domain.Notificacion;
import com.nexvia.domain.TipoNotificacion;
import com.nexvia.dtos.NotificacionCountResponse;
import com.nexvia.dtos.NotificacionResponse;
import com.nexvia.exceptions.ForbiddenException;
import com.nexvia.exceptions.ResourceNotFoundException;
import com.nexvia.repositories.NotificacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;

    public void crearNotificacion(TipoNotificacion tipo, String mensaje,
                                   Long destinatarioId, Long viajeId) {
        Notificacion notificacion = Notificacion.builder()
                .tipo(tipo)
                .mensaje(mensaje)
                .destinatarioId(destinatarioId)
                .viajeId(viajeId)
                .build();
        notificacionRepository.save(notificacion);
    }

    public List<NotificacionResponse> listarPorUsuario(Long userId) {
        return notificacionRepository.findByDestinatarioIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    public List<NotificacionResponse> listarNoLeidas(Long userId) {
        return notificacionRepository.findByDestinatarioIdAndLeidaFalseOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    public NotificacionCountResponse contarNoLeidas(Long userId) {
        long count = notificacionRepository.countByDestinatarioIdAndLeidaFalse(userId);
        return new NotificacionCountResponse(count);
    }

    public NotificacionResponse marcarLeida(Long notificacionId, Long userId) {
        Notificacion notif = notificacionRepository.findById(notificacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada"));

        if (!notif.getDestinatarioId().equals(userId)) {
            throw new ForbiddenException("No tenés permiso para modificar esta notificación");
        }

        notif.setLeida(true);
        return toResponse(notificacionRepository.save(notif));
    }

    public void marcarTodasLeidas(Long userId) {
        List<Notificacion> noLeidas = notificacionRepository
                .findByDestinatarioIdAndLeidaFalseOrderByCreatedAtDesc(userId);
        noLeidas.forEach(n -> n.setLeida(true));
        notificacionRepository.saveAll(noLeidas);
    }

    private NotificacionResponse toResponse(Notificacion n) {
        return new NotificacionResponse(
                n.getId(), n.getTipo().name(), n.getMensaje(),
                n.getDestinatarioId(), n.getViajeId(),
                n.getLeida(), n.getCreatedAt()
        );
    }
}
