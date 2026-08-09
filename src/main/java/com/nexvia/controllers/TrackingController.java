package com.nexvia.controllers;

import com.nexvia.domain.Role;
import com.nexvia.dtos.PosicionViajeRequest;
import com.nexvia.dtos.PosicionViajeResponse;
import com.nexvia.dtos.RutaResponse;
import com.nexvia.services.TrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tracking")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;

    @PostMapping("/{viajeId}/posicion")
    public ResponseEntity<PosicionViajeResponse> registrarPosicion(
            @PathVariable Long viajeId,
            @Valid @RequestBody PosicionViajeRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Role role = extractRole(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(trackingService.registrarPosicion(viajeId, request, userId, role));
    }

    @GetMapping("/{viajeId}/posicion-actual")
    public ResponseEntity<PosicionViajeResponse> posicionActual(@PathVariable Long viajeId) {
        return ResponseEntity.ok(trackingService.obtenerUltimaPosicion(viajeId));
    }

    @GetMapping("/{viajeId}/ruta")
    public ResponseEntity<RutaResponse> ruta(@PathVariable Long viajeId) {
        return ResponseEntity.ok(trackingService.obtenerRuta(viajeId));
    }

    private Role extractRole(Authentication authentication) {
        String authority = authentication.getAuthorities().iterator().next().getAuthority();
        return Role.valueOf(authority.replace("ROLE_", ""));
    }
}
