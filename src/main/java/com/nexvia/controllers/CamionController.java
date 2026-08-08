package com.nexvia.controllers;

import com.nexvia.domain.Role;
import com.nexvia.dtos.CamionRequest;
import com.nexvia.dtos.CamionResponse;
import com.nexvia.dtos.PosicionRequest;
import com.nexvia.services.CamionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/camiones")
@RequiredArgsConstructor
public class CamionController {

    private final CamionService camionService;

    @GetMapping
    public ResponseEntity<List<CamionResponse>> listar() {
        return ResponseEntity.ok(camionService.listar());
    }

    @GetMapping("/disponibles")
    public ResponseEntity<List<CamionResponse>> listarDisponibles() {
        return ResponseEntity.ok(camionService.listarDisponibles());
    }

    @GetMapping("/mi-camion")
    public ResponseEntity<CamionResponse> miCamion(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(camionService.miCamion(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CamionResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(camionService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<CamionResponse> crear(@Valid @RequestBody CamionRequest request,
                                                 Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(camionService.crear(request, userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CamionResponse> actualizar(@PathVariable Long id,
                                                      @Valid @RequestBody CamionRequest request,
                                                      Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Role role = extractRole(authentication);
        return ResponseEntity.ok(camionService.actualizar(id, request, userId, role));
    }

    @PatchMapping("/{id}/posicion")
    public ResponseEntity<CamionResponse> actualizarPosicion(@PathVariable Long id,
                                                              @Valid @RequestBody PosicionRequest request,
                                                              Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Role role = extractRole(authentication);
        return ResponseEntity.ok(camionService.actualizarPosicion(id, request, userId, role));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        camionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    private Role extractRole(Authentication authentication) {
        String authority = authentication.getAuthorities().iterator().next().getAuthority();
        return Role.valueOf(authority.replace("ROLE_", ""));
    }
}
