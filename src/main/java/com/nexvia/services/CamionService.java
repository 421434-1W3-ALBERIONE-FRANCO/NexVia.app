package com.nexvia.services;

import com.nexvia.domain.Camion;
import com.nexvia.domain.EstadoCamion;
import com.nexvia.domain.Role;
import com.nexvia.domain.Usuario;
import com.nexvia.dtos.CamionRequest;
import com.nexvia.dtos.CamionResponse;
import com.nexvia.dtos.PosicionRequest;
import com.nexvia.exceptions.ForbiddenException;
import com.nexvia.exceptions.ResourceNotFoundException;
import com.nexvia.repositories.CamionRepository;
import com.nexvia.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CamionService {

    private final CamionRepository camionRepository;
    private final UsuarioRepository usuarioRepository;

    public List<CamionResponse> listar() {
        return camionRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<CamionResponse> listarDisponibles() {
        return camionRepository.findByEstado(EstadoCamion.DISPONIBLE).stream()
                .map(this::toResponse)
                .toList();
    }

    public CamionResponse obtener(Long id) {
        return toResponse(findOrThrow(id));
    }

    public CamionResponse miCamion(Long userId) {
        Camion camion = camionRepository.findFirstByUsuarioId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No tenés un camión asignado"));
        return toResponse(camion);
    }

    public CamionResponse crear(CamionRequest request, Long userId) {
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Camion camion = Camion.builder()
                .transporteNombre(request.transporteNombre())
                .transporteCuit(request.transporteCuit())
                .choferNombre(request.choferNombre())
                .choferCuit(request.choferCuit())
                .patente(request.patente())
                .patenteAcoplado(request.patenteAcoplado())
                .telefono(request.telefono())
                .capacidadKg(request.capacidadKg() != null ? request.capacidadKg() : 0)
                .lat(request.lat() != null ? request.lat() : -32.4341)
                .lng(request.lng() != null ? request.lng() : -63.2433)
                .estado(EstadoCamion.DISPONIBLE)
                .usuario(usuario)
                .build();

        return toResponse(camionRepository.save(camion));
    }

    public CamionResponse actualizar(Long id, CamionRequest request, Long userId, Role userRole) {
        Camion camion = findOrThrow(id);
        checkOwnershipOrAdmin(camion, userId, userRole);

        camion.setTransporteNombre(request.transporteNombre());
        camion.setTransporteCuit(request.transporteCuit());
        camion.setChoferNombre(request.choferNombre());
        camion.setChoferCuit(request.choferCuit());
        camion.setPatente(request.patente());
        camion.setPatenteAcoplado(request.patenteAcoplado());
        camion.setTelefono(request.telefono());
        camion.setCapacidadKg(request.capacidadKg() != null ? request.capacidadKg() : 0);
        if (request.lat() != null) camion.setLat(request.lat());
        if (request.lng() != null) camion.setLng(request.lng());

        return toResponse(camionRepository.save(camion));
    }

    public CamionResponse actualizarPosicion(Long id, PosicionRequest request, Long userId, Role userRole) {
        Camion camion = findOrThrow(id);
        checkOwnershipOrAdmin(camion, userId, userRole);

        camion.setLat(request.lat());
        camion.setLng(request.lng());

        return toResponse(camionRepository.save(camion));
    }

    public void eliminar(Long id) {
        if (!camionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Camión no encontrado con id: " + id);
        }
        camionRepository.deleteById(id);
    }

    private Camion findOrThrow(Long id) {
        return camionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Camión no encontrado con id: " + id));
    }

    private void checkOwnershipOrAdmin(Camion camion, Long userId, Role userRole) {
        if (userRole == Role.ADMIN) return;
        if (camion.getUsuario() == null || !camion.getUsuario().getId().equals(userId)) {
            throw new ForbiddenException("No tenés permiso para modificar este camión");
        }
    }

    private CamionResponse toResponse(Camion camion) {
        return new CamionResponse(
                camion.getId(),
                camion.getTransporteNombre(),
                camion.getTransporteCuit(),
                camion.getChoferNombre(),
                camion.getChoferCuit(),
                camion.getPatente(),
                camion.getPatenteAcoplado(),
                camion.getTelefono(),
                camion.getCapacidadKg(),
                camion.getLat(),
                camion.getLng(),
                camion.getEstado().name(),
                camion.getUsuario() != null ? camion.getUsuario().getId() : null
        );
    }
}
