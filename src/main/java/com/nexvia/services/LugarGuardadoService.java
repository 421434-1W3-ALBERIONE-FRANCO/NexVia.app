package com.nexvia.services;

import com.nexvia.domain.LugarGuardado;
import com.nexvia.domain.Role;
import com.nexvia.domain.TipoLugar;
import com.nexvia.domain.Usuario;
import com.nexvia.dtos.LugarGuardadoRequest;
import com.nexvia.dtos.LugarGuardadoResponse;
import com.nexvia.exceptions.ForbiddenException;
import com.nexvia.exceptions.ResourceNotFoundException;
import com.nexvia.repositories.LugarGuardadoRepository;
import com.nexvia.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LugarGuardadoService {

    private final LugarGuardadoRepository lugarGuardadoRepository;
    private final UsuarioRepository usuarioRepository;

    public List<LugarGuardadoResponse> listar(Long userId) {
        return lugarGuardadoRepository.findByUsuarioIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse).toList();
    }

    public LugarGuardadoResponse obtener(Long id, Long userId, Role userRole) {
        LugarGuardado lugar = findOrThrow(id);
        checkOwnershipOrAdmin(lugar, userId, userRole);
        return toResponse(lugar);
    }

    public LugarGuardadoResponse crear(LugarGuardadoRequest request, Long userId) {
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        TipoLugar tipo = parseTipoLugar(request.tipo());

        LugarGuardado lugar = LugarGuardado.builder()
                .nombre(request.nombre())
                .lat(request.lat())
                .lng(request.lng())
                .tipo(tipo)
                .usuario(usuario)
                .build();

        return toResponse(lugarGuardadoRepository.save(lugar));
    }

    public LugarGuardadoResponse actualizar(Long id, LugarGuardadoRequest request, Long userId, Role userRole) {
        LugarGuardado lugar = findOrThrow(id);
        checkOwnershipOrAdmin(lugar, userId, userRole);

        lugar.setNombre(request.nombre());
        lugar.setLat(request.lat());
        lugar.setLng(request.lng());
        lugar.setTipo(parseTipoLugar(request.tipo()));

        return toResponse(lugarGuardadoRepository.save(lugar));
    }

    public void eliminar(Long id, Long userId, Role userRole) {
        LugarGuardado lugar = findOrThrow(id);
        checkOwnershipOrAdmin(lugar, userId, userRole);
        lugarGuardadoRepository.deleteById(id);
    }

    private LugarGuardado findOrThrow(Long id) {
        return lugarGuardadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lugar guardado no encontrado con id: " + id));
    }

    private void checkOwnershipOrAdmin(LugarGuardado lugar, Long userId, Role userRole) {
        if (userRole == Role.ADMIN) return;
        if (lugar.getUsuario() == null || !lugar.getUsuario().getId().equals(userId)) {
            throw new ForbiddenException("No tenés permiso para acceder a este lugar guardado");
        }
    }

    private TipoLugar parseTipoLugar(String tipo) {
        if (tipo == null || tipo.isBlank()) return TipoLugar.OTRO;
        try {
            return TipoLugar.valueOf(tipo.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de lugar inválido: " + tipo);
        }
    }

    private LugarGuardadoResponse toResponse(LugarGuardado lugar) {
        return new LugarGuardadoResponse(
                lugar.getId(),
                lugar.getNombre(),
                lugar.getLat(),
                lugar.getLng(),
                lugar.getTipo().name(),
                lugar.getUsuario() != null ? lugar.getUsuario().getId() : null,
                lugar.getCreatedAt()
        );
    }
}
