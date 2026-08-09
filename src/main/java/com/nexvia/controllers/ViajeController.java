package com.nexvia.controllers;

import com.nexvia.domain.EstadoViaje;
import com.nexvia.domain.Role;
import com.nexvia.dtos.CancelacionRequest;
import com.nexvia.dtos.ViajeRequest;
import com.nexvia.dtos.ViajeResponse;
import com.nexvia.services.ViajeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/viajes")
@RequiredArgsConstructor
public class ViajeController {

    private final ViajeService viajeService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ViajeResponse>> listar() {
        return ResponseEntity.ok(viajeService.listar());
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<ViajeResponse>> listarPorEstado(@PathVariable String estado) {
        EstadoViaje estadoViaje = EstadoViaje.valueOf(estado.toUpperCase());
        return ResponseEntity.ok(viajeService.listarPorEstado(estadoViaje));
    }

    @GetMapping("/mis-viajes")
    public ResponseEntity<List<ViajeResponse>> misViajes(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(viajeService.misViajes(userId));
    }

    @GetMapping("/camion/{camionId}")
    public ResponseEntity<List<ViajeResponse>> viajesDelCamion(@PathVariable Long camionId) {
        return ResponseEntity.ok(viajeService.viajesDelCamion(camionId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ViajeResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(viajeService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<ViajeResponse> crear(@Valid @RequestBody ViajeRequest request,
                                                Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(viajeService.crear(request, userId));
    }

    @PatchMapping("/{id}/aceptar")
    public ResponseEntity<ViajeResponse> aceptar(@PathVariable Long id,
                                                  @RequestParam Long camionId,
                                                  Authentication authentication) {
        Long choferId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(viajeService.aceptar(id, camionId, choferId));
    }

    @PatchMapping("/{id}/en-camino")
    public ResponseEntity<ViajeResponse> enCamino(@PathVariable Long id,
                                                   Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Role role = extractRole(authentication);
        return ResponseEntity.ok(viajeService.avanzarEnCamino(id, userId, role));
    }

    @PatchMapping("/{id}/completar")
    public ResponseEntity<ViajeResponse> completar(@PathVariable Long id,
                                                    Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Role role = extractRole(authentication);
        return ResponseEntity.ok(viajeService.completar(id, userId, role));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<ViajeResponse> cancelar(@PathVariable Long id,
                                                   @Valid @RequestBody CancelacionRequest request,
                                                   Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Role role = extractRole(authentication);
        return ResponseEntity.ok(viajeService.cancelar(id, request.motivo(), userId, role));
    }

    private Role extractRole(Authentication authentication) {
        String authority = authentication.getAuthorities().iterator().next().getAuthority();
        return Role.valueOf(authority.replace("ROLE_", ""));
    }
}
