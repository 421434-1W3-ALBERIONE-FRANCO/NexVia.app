package com.nexvia.services;

import com.nexvia.domain.*;
import com.nexvia.dtos.ViajeRequest;
import com.nexvia.dtos.ViajeResponse;
import com.nexvia.exceptions.ForbiddenException;
import com.nexvia.exceptions.ResourceNotFoundException;
import com.nexvia.repositories.CamionRepository;
import com.nexvia.repositories.UsuarioRepository;
import com.nexvia.repositories.ViajeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ViajeService {

    private final ViajeRepository viajeRepository;
    private final UsuarioRepository usuarioRepository;
    private final CamionRepository camionRepository;

    @Value("${nexvia.cancelacion.ventana-gratis-minutos:30}")
    private int ventanaGratisMinutos;

    @Value("${nexvia.cancelacion.penalidad-porcentaje:10}")
    private int penalidadPorcentaje;

    public List<ViajeResponse> listar() {
        return viajeRepository.findAll().stream().map(this::toResponse).toList();
    }

    public List<ViajeResponse> listarPorEstado(EstadoViaje estado) {
        return viajeRepository.findByEstado(estado).stream().map(this::toResponse).toList();
    }

    public List<ViajeResponse> misViajes(Long userId) {
        return viajeRepository.findByUsuarioIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse).toList();
    }

    public List<ViajeResponse> viajesDelCamion(Long camionId) {
        return viajeRepository.findByCamionIdOrderByCreatedAtDesc(camionId).stream()
                .map(this::toResponse).toList();
    }

    public ViajeResponse obtener(Long id) {
        return toResponse(findOrThrow(id));
    }

    public ViajeResponse crear(ViajeRequest request, Long userId) {
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        TipoTarifa tipoTarifa = parseTipoTarifa(request.tipoTarifa());

        Viaje viaje = Viaje.builder()
                .origenLat(request.origenLat())
                .origenLng(request.origenLng())
                .destinoLat(request.destinoLat())
                .destinoLng(request.destinoLng())
                .distanciaKm(request.distanciaKm())
                .toneladas(request.toneladas() != null ? request.toneladas() : 0.0)
                .tipoTarifa(tipoTarifa)
                .precio(request.precio())
                .carga(request.carga())
                .estado(EstadoViaje.SOLICITADO)
                .usuario(usuario)
                .usuarioNombre(usuario.getFullName())
                .build();

        return toResponse(viajeRepository.save(viaje));
    }

    @Transactional
    public ViajeResponse aceptar(Long viajeId, Long camionId, Long choferId) {
        Viaje viaje = findOrThrow(viajeId);
        checkEstado(viaje, EstadoViaje.SOLICITADO, "aceptar");

        Camion camion = camionRepository.findById(camionId)
                .orElseThrow(() -> new ResourceNotFoundException("Camión no encontrado"));

        if (camion.getUsuario() == null || !camion.getUsuario().getId().equals(choferId)) {
            throw new ForbiddenException("No sos el dueño de este camión");
        }

        Usuario chofer = usuarioRepository.findById(choferId)
                .orElseThrow(() -> new ResourceNotFoundException("Chofer no encontrado"));

        viaje.setEstado(EstadoViaje.ACEPTADO);
        viaje.setCamion(camion);
        viaje.setChoferNombre(chofer.getFullName());
        viaje.setChoferId(choferId);

        camion.setEstado(EstadoCamion.OCUPADO);
        camionRepository.save(camion);

        return toResponse(viajeRepository.save(viaje));
    }

    @Transactional
    public ViajeResponse avanzarEnCamino(Long viajeId, Long userId, Role userRole) {
        Viaje viaje = findOrThrow(viajeId);
        checkEstado(viaje, EstadoViaje.ACEPTADO, "marcar en camino");
        checkChoferOrAdmin(viaje, userId, userRole);

        viaje.setEstado(EstadoViaje.EN_CAMINO);
        return toResponse(viajeRepository.save(viaje));
    }

    @Transactional
    public ViajeResponse completar(Long viajeId, Long userId, Role userRole) {
        Viaje viaje = findOrThrow(viajeId);
        checkEstado(viaje, EstadoViaje.EN_CAMINO, "completar");
        checkChoferOrAdmin(viaje, userId, userRole);

        viaje.setEstado(EstadoViaje.COMPLETADO);

        if (viaje.getCamion() != null) {
            Camion camion = viaje.getCamion();
            camion.setEstado(EstadoCamion.DISPONIBLE);
            camionRepository.save(camion);
        }

        return toResponse(viajeRepository.save(viaje));
    }

    @Transactional
    public ViajeResponse cancelar(Long viajeId, String motivo, Long userId, Role userRole) {
        Viaje viaje = findOrThrow(viajeId);

        if (viaje.getEstado() == EstadoViaje.COMPLETADO || viaje.getEstado() == EstadoViaje.CANCELADO) {
            throw new IllegalArgumentException("No se puede cancelar un viaje en estado: " + viaje.getEstado());
        }

        checkParticipanteOrAdmin(viaje, userId, userRole);

        double penalidad = calcularPenalidad(viaje);

        viaje.setEstado(EstadoViaje.CANCELADO);
        viaje.setMotivoCancelacion(motivo);
        viaje.setCanceladoPorId(userId);
        viaje.setCanceladoAt(LocalDateTime.now());
        viaje.setPenalidad(penalidad);

        if (viaje.getCamion() != null) {
            Camion camion = viaje.getCamion();
            camion.setEstado(EstadoCamion.DISPONIBLE);
            camionRepository.save(camion);
        }

        return toResponse(viajeRepository.save(viaje));
    }

    double calcularPenalidad(Viaje viaje) {
        if (viaje.getEstado() == EstadoViaje.SOLICITADO) {
            return 0.0;
        }
        if (viaje.getCreatedAt() != null) {
            LocalDateTime limite = viaje.getCreatedAt().plusMinutes(ventanaGratisMinutos);
            if (LocalDateTime.now().isBefore(limite)) {
                return 0.0;
            }
        }
        return Math.round(viaje.getPrecio() * penalidadPorcentaje / 100.0 * 100.0) / 100.0;
    }

    private Viaje findOrThrow(Long id) {
        return viajeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Viaje no encontrado con id: " + id));
    }

    private void checkEstado(Viaje viaje, EstadoViaje esperado, String accion) {
        if (viaje.getEstado() != esperado) {
            throw new IllegalArgumentException(
                    "No se puede " + accion + " un viaje en estado: " + viaje.getEstado()
            );
        }
    }

    private void checkChoferOrAdmin(Viaje viaje, Long userId, Role userRole) {
        if (userRole == Role.ADMIN) return;
        if (viaje.getChoferId() == null || !viaje.getChoferId().equals(userId)) {
            throw new ForbiddenException("No tenés permiso para modificar este viaje");
        }
    }

    private void checkParticipanteOrAdmin(Viaje viaje, Long userId, Role userRole) {
        if (userRole == Role.ADMIN) return;
        boolean isUsuario = viaje.getUsuario() != null && viaje.getUsuario().getId().equals(userId);
        boolean isChofer = viaje.getChoferId() != null && viaje.getChoferId().equals(userId);
        if (!isUsuario && !isChofer) {
            throw new ForbiddenException("No tenés permiso para cancelar este viaje");
        }
    }

    private TipoTarifa parseTipoTarifa(String tipoTarifa) {
        if (tipoTarifa == null || tipoTarifa.isBlank()) return TipoTarifa.POR_KM;
        try {
            return TipoTarifa.valueOf(tipoTarifa.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de tarifa inválido: " + tipoTarifa);
        }
    }

    private ViajeResponse toResponse(Viaje viaje) {
        return new ViajeResponse(
                viaje.getId(),
                viaje.getOrigenLat(),
                viaje.getOrigenLng(),
                viaje.getDestinoLat(),
                viaje.getDestinoLng(),
                viaje.getDistanciaKm(),
                viaje.getToneladas(),
                viaje.getTipoTarifa().name(),
                viaje.getPrecio(),
                viaje.getCarga(),
                viaje.getEstado().name(),
                viaje.getUsuario() != null ? viaje.getUsuario().getId() : null,
                viaje.getUsuarioNombre(),
                viaje.getCamion() != null ? viaje.getCamion().getId() : null,
                viaje.getChoferNombre(),
                viaje.getChoferId(),
                viaje.getMotivoCancelacion(),
                viaje.getCanceladoPorId(),
                viaje.getCanceladoAt(),
                viaje.getPenalidad(),
                viaje.getCreatedAt()
        );
    }
}
