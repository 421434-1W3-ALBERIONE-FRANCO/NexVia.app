package com.nexvia.services;

import com.nexvia.domain.Configuracion;
import com.nexvia.dtos.ConfiguracionRequest;
import com.nexvia.dtos.ConfiguracionResponse;
import com.nexvia.exceptions.ResourceNotFoundException;
import com.nexvia.repositories.ConfiguracionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConfiguracionService {

    private final ConfiguracionRepository configuracionRepository;

    public List<ConfiguracionResponse> listar() {
        return configuracionRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public ConfiguracionResponse obtener(Long id) {
        return toResponse(findOrThrow(id));
    }

    public ConfiguracionResponse crear(ConfiguracionRequest request) {
        Configuracion config = Configuracion.builder()
                .tarifaPorKm(request.tarifaPorKm())
                .tarifaPorTonelada(request.tarifaPorTonelada() != null ? request.tarifaPorTonelada() : 0.0)
                .zonaNombre(request.zonaNombre())
                .centroLat(request.centroLat())
                .centroLng(request.centroLng())
                .build();
        return toResponse(configuracionRepository.save(config));
    }

    public ConfiguracionResponse actualizar(Long id, ConfiguracionRequest request) {
        Configuracion config = findOrThrow(id);
        config.setTarifaPorKm(request.tarifaPorKm());
        config.setTarifaPorTonelada(request.tarifaPorTonelada() != null ? request.tarifaPorTonelada() : 0.0);
        config.setZonaNombre(request.zonaNombre());
        config.setCentroLat(request.centroLat());
        config.setCentroLng(request.centroLng());
        return toResponse(configuracionRepository.save(config));
    }

    public void eliminar(Long id) {
        if (!configuracionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Configuración no encontrada con id: " + id);
        }
        configuracionRepository.deleteById(id);
    }

    private Configuracion findOrThrow(Long id) {
        return configuracionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Configuración no encontrada con id: " + id));
    }

    private ConfiguracionResponse toResponse(Configuracion config) {
        return new ConfiguracionResponse(
                config.getId(),
                config.getTarifaPorKm(),
                config.getTarifaPorTonelada(),
                config.getZonaNombre(),
                config.getCentroLat(),
                config.getCentroLng()
        );
    }
}
