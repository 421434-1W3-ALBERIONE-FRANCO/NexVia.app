package com.nexvia.services;

import com.nexvia.domain.Calificacion;
import com.nexvia.domain.EstadoViaje;
import com.nexvia.domain.Viaje;
import com.nexvia.dtos.CalificacionRequest;
import com.nexvia.dtos.CalificacionResponse;
import com.nexvia.dtos.PromedioCalificacionResponse;
import com.nexvia.exceptions.ForbiddenException;
import com.nexvia.exceptions.ResourceNotFoundException;
import com.nexvia.repositories.CalificacionRepository;
import com.nexvia.repositories.ViajeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CalificacionService {

    private final CalificacionRepository calificacionRepository;
    private final ViajeRepository viajeRepository;

    public CalificacionResponse crear(CalificacionRequest request, Long autorId) {
        Viaje viaje = viajeRepository.findById(request.viajeId())
                .orElseThrow(() -> new ResourceNotFoundException("Viaje no encontrado"));

        if (viaje.getEstado() != EstadoViaje.COMPLETADO) {
            throw new IllegalArgumentException("Solo se puede calificar un viaje completado");
        }

        boolean isParticipante = (viaje.getUsuario() != null && viaje.getUsuario().getId().equals(autorId))
                || (viaje.getChoferId() != null && viaje.getChoferId().equals(autorId));

        if (!isParticipante) {
            throw new ForbiddenException("Solo los participantes del viaje pueden calificar");
        }

        if (autorId.equals(request.destinatarioId())) {
            throw new IllegalArgumentException("No podés calificarte a vos mismo");
        }

        if (calificacionRepository.existsByViajeIdAndAutorId(request.viajeId(), autorId)) {
            throw new IllegalArgumentException("Ya calificaste este viaje");
        }

        Calificacion calificacion = Calificacion.builder()
                .viaje(viaje)
                .autorId(autorId)
                .destinatarioId(request.destinatarioId())
                .puntuacion(request.puntuacion())
                .comentario(request.comentario())
                .build();

        return toResponse(calificacionRepository.save(calificacion));
    }

    public List<CalificacionResponse> obtenerPorDestinatario(Long destinatarioId) {
        return calificacionRepository.findByDestinatarioIdOrderByCreatedAtDesc(destinatarioId)
                .stream().map(this::toResponse).toList();
    }

    public List<CalificacionResponse> obtenerPorViaje(Long viajeId) {
        return calificacionRepository.findByViajeId(viajeId)
                .stream().map(this::toResponse).toList();
    }

    public PromedioCalificacionResponse promedio(Long usuarioId) {
        double promedio = calificacionRepository.promedioByDestinatarioId(usuarioId);
        long total = calificacionRepository.countByDestinatarioId(usuarioId);
        return new PromedioCalificacionResponse(usuarioId,
                Math.round(promedio * 100.0) / 100.0, total);
    }

    private CalificacionResponse toResponse(Calificacion c) {
        return new CalificacionResponse(
                c.getId(),
                c.getViaje().getId(),
                c.getAutorId(),
                c.getDestinatarioId(),
                c.getPuntuacion(),
                c.getComentario(),
                c.getCreatedAt()
        );
    }
}
