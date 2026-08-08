package com.nexvia.controllers;

import com.nexvia.domain.Role;
import com.nexvia.dtos.LugarGuardadoRequest;
import com.nexvia.dtos.LugarGuardadoResponse;
import com.nexvia.services.LugarGuardadoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lugares")
@RequiredArgsConstructor
public class LugarGuardadoController {

    private final LugarGuardadoService lugarGuardadoService;

    @GetMapping
    public ResponseEntity<List<LugarGuardadoResponse>> listar(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(lugarGuardadoService.listar(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LugarGuardadoResponse> obtener(@PathVariable Long id,
                                                          Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Role role = extractRole(authentication);
        return ResponseEntity.ok(lugarGuardadoService.obtener(id, userId, role));
    }

    @PostMapping
    public ResponseEntity<LugarGuardadoResponse> crear(@Valid @RequestBody LugarGuardadoRequest request,
                                                        Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(lugarGuardadoService.crear(request, userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LugarGuardadoResponse> actualizar(@PathVariable Long id,
                                                             @Valid @RequestBody LugarGuardadoRequest request,
                                                             Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Role role = extractRole(authentication);
        return ResponseEntity.ok(lugarGuardadoService.actualizar(id, request, userId, role));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id,
                                          Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Role role = extractRole(authentication);
        lugarGuardadoService.eliminar(id, userId, role);
        return ResponseEntity.noContent().build();
    }

    private Role extractRole(Authentication authentication) {
        String authority = authentication.getAuthorities().iterator().next().getAuthority();
        return Role.valueOf(authority.replace("ROLE_", ""));
    }
}
