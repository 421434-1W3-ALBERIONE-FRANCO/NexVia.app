package com.nexvia.services;

import com.nexvia.domain.Configuracion;
import com.nexvia.domain.TipoTarifa;
import com.nexvia.dtos.CotizacionRequest;
import com.nexvia.dtos.CotizacionResponse;
import com.nexvia.exceptions.ResourceNotFoundException;
import com.nexvia.repositories.ConfiguracionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CotizacionService {

    private final ConfiguracionRepository configuracionRepository;

    public CotizacionResponse cotizar(CotizacionRequest request) {
        Configuracion config = resolveConfiguracion(request.configuracionId());
        TipoTarifa tipo = parseTipoTarifa(request.tipoTarifa());
        double toneladas = request.toneladas() != null ? request.toneladas() : 0.0;

        double tarifaAplicada;
        double precio;

        if (tipo == TipoTarifa.POR_TONELADA && toneladas > 0) {
            tarifaAplicada = config.getTarifaPorTonelada();
            precio = toneladas * tarifaAplicada;
        } else {
            tarifaAplicada = config.getTarifaPorKm();
            precio = request.distanciaKm() * tarifaAplicada;
        }

        precio = Math.round(precio * 100.0) / 100.0;

        return new CotizacionResponse(
                precio,
                request.distanciaKm(),
                toneladas,
                tipo.name(),
                tarifaAplicada,
                config.getZonaNombre()
        );
    }

    public double calcularPrecio(Double distanciaKm, Double toneladas, String tipoTarifaStr, Long configuracionId) {
        Configuracion config = resolveConfiguracion(configuracionId);
        TipoTarifa tipo = parseTipoTarifa(tipoTarifaStr);
        double tons = toneladas != null ? toneladas : 0.0;

        double precio;
        if (tipo == TipoTarifa.POR_TONELADA && tons > 0) {
            precio = tons * config.getTarifaPorTonelada();
        } else {
            precio = distanciaKm * config.getTarifaPorKm();
        }

        return Math.round(precio * 100.0) / 100.0;
    }

    private Configuracion resolveConfiguracion(Long configuracionId) {
        if (configuracionId != null) {
            return configuracionRepository.findById(configuracionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Configuración no encontrada con id: " + configuracionId));
        }
        List<Configuracion> configs = configuracionRepository.findAll();
        if (configs.isEmpty()) {
            throw new ResourceNotFoundException("No hay configuraciones de tarifa disponibles");
        }
        return configs.get(0);
    }

    private TipoTarifa parseTipoTarifa(String tipoTarifa) {
        if (tipoTarifa == null || tipoTarifa.isBlank()) return TipoTarifa.POR_KM;
        try {
            return TipoTarifa.valueOf(tipoTarifa.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de tarifa inválido: " + tipoTarifa);
        }
    }
}
