package com.nexvia.controllers;

import com.nexvia.dtos.CalificacionRequest;
import com.nexvia.dtos.CalificacionResponse;
import com.nexvia.dtos.PromedioCalificacionResponse;
import com.nexvia.services.CalificacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/calificaciones")
@RequiredArgsConstructor
public class CalificacionController {

    private final CalificacionService calificacionService;

    @PostMapping
    public ResponseEntity<CalificacionResponse> crear(@Valid @RequestBody CalificacionRequest request,
                                                       Authentication authentication) {
        Long autorId = (Long) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(calificacionService.crear(request, autorId));
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<CalificacionResponse>> porUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(calificacionService.obtenerPorDestinatario(usuarioId));
    }

    @GetMapping("/viaje/{viajeId}")
    public ResponseEntity<List<CalificacionResponse>> porViaje(@PathVariable Long viajeId) {
        return ResponseEntity.ok(calificacionService.obtenerPorViaje(viajeId));
    }

    @GetMapping("/promedio/{usuarioId}")
    public ResponseEntity<PromedioCalificacionResponse> promedio(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(calificacionService.promedio(usuarioId));
    }
}
