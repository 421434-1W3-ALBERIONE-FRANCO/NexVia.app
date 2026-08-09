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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrackingService {

    private final PosicionViajeRepository posicionRepository;
    private final ViajeRepository viajeRepository;
    private final CamionRepository camionRepository;

    @Transactional
    public PosicionViajeResponse registrarPosicion(Long viajeId, PosicionViajeRequest request,
                                                    Long userId, Role userRole) {
        Viaje viaje = findViajeOrThrow(viajeId);

        if (viaje.getEstado() != EstadoViaje.EN_CAMINO && viaje.getEstado() != EstadoViaje.ACEPTADO) {
            throw new IllegalArgumentException(
                    "Solo se puede registrar posición en viajes ACEPTADO o EN_CAMINO");
        }

        checkChoferOrAdmin(viaje, userId, userRole);

        PosicionViaje posicion = PosicionViaje.builder()
                .viaje(viaje)
                .lat(request.lat())
                .lng(request.lng())
                .velocidad(request.velocidad())
                .rumbo(request.rumbo())
                .build();

        PosicionViaje saved = posicionRepository.save(posicion);

        if (viaje.getCamion() != null) {
            Camion camion = viaje.getCamion();
            camion.setLat(request.lat());
            camion.setLng(request.lng());
            camionRepository.save(camion);
        }

        return toResponse(saved);
    }

    public PosicionViajeResponse obtenerUltimaPosicion(Long viajeId) {
        findViajeOrThrow(viajeId);
        PosicionViaje posicion = posicionRepository.findFirstByViajeIdOrderByTimestampDesc(viajeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No hay posiciones registradas para el viaje: " + viajeId));
        return toResponse(posicion);
    }

    public RutaResponse obtenerRuta(Long viajeId) {
        Viaje viaje = findViajeOrThrow(viajeId);
        List<PosicionViaje> puntos = posicionRepository.findByViajeIdOrderByTimestampAsc(viajeId);

        List<PosicionViajeResponse> puntosResponse = puntos.stream()
                .map(this::toResponse).toList();

        double distanciaRecorrida = calcularDistanciaRecorrida(puntos);

        Long tiempoMinutos = null;
        if (puntos.size() >= 2) {
            Duration duracion = Duration.between(
                    puntos.getFirst().getTimestamp(),
                    puntos.getLast().getTimestamp());
            tiempoMinutos = duracion.toMinutes();
        }

        PosicionViajeResponse posicionActual = puntos.isEmpty() ? null
                : toResponse(puntos.getLast());

        return new RutaResponse(
                viajeId,
                puntos.size(),
                Math.round(distanciaRecorrida * 100.0) / 100.0,
                tiempoMinutos,
                posicionActual,
                puntosResponse
        );
    }

    double calcularDistanciaRecorrida(List<PosicionViaje> puntos) {
        if (puntos.size() < 2) return 0.0;

        double totalKm = 0.0;
        for (int i = 1; i < puntos.size(); i++) {
            totalKm += haversine(
                    puntos.get(i - 1).getLat(), puntos.get(i - 1).getLng(),
                    puntos.get(i).getLat(), puntos.get(i).getLng()
            );
        }
        return totalKm;
    }

    static double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private Viaje findViajeOrThrow(Long viajeId) {
        return viajeRepository.findById(viajeId)
                .orElseThrow(() -> new ResourceNotFoundException("Viaje no encontrado con id: " + viajeId));
    }

    private void checkChoferOrAdmin(Viaje viaje, Long userId, Role userRole) {
        if (userRole == Role.ADMIN) return;
        if (viaje.getChoferId() == null || !viaje.getChoferId().equals(userId)) {
            throw new ForbiddenException("Solo el chofer asignado puede registrar posición");
        }
    }

    private PosicionViajeResponse toResponse(PosicionViaje p) {
        return new PosicionViajeResponse(
                p.getId(), p.getViaje().getId(),
                p.getLat(), p.getLng(),
                p.getVelocidad(), p.getRumbo(),
                p.getTimestamp()
        );
    }
}
