package com.nexvia.controllers;

import com.nexvia.dtos.ConfiguracionRequest;
import com.nexvia.dtos.ConfiguracionResponse;
import com.nexvia.services.ConfiguracionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/configuraciones")
@RequiredArgsConstructor
public class ConfiguracionController {

    private final ConfiguracionService configuracionService;

    @GetMapping
    public ResponseEntity<List<ConfiguracionResponse>> listar() {
        return ResponseEntity.ok(configuracionService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConfiguracionResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(configuracionService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ConfiguracionResponse> crear(@Valid @RequestBody ConfiguracionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(configuracionService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ConfiguracionResponse> actualizar(@PathVariable Long id,
                                                            @Valid @RequestBody ConfiguracionRequest request) {
        return ResponseEntity.ok(configuracionService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        configuracionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
