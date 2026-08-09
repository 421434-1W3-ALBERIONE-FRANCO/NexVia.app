package com.nexvia.controllers;

import com.nexvia.dtos.NotificacionCountResponse;
import com.nexvia.dtos.NotificacionResponse;
import com.nexvia.services.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    @GetMapping
    public ResponseEntity<List<NotificacionResponse>> listar(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(notificacionService.listarPorUsuario(userId));
    }

    @GetMapping("/no-leidas")
    public ResponseEntity<List<NotificacionResponse>> noLeidas(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(notificacionService.listarNoLeidas(userId));
    }

    @GetMapping("/count")
    public ResponseEntity<NotificacionCountResponse> count(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(notificacionService.contarNoLeidas(userId));
    }

    @PatchMapping("/{id}/leer")
    public ResponseEntity<NotificacionResponse> marcarLeida(@PathVariable Long id,
                                                             Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(notificacionService.marcarLeida(id, userId));
    }

    @PatchMapping("/leer-todas")
    public ResponseEntity<Void> marcarTodasLeidas(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        notificacionService.marcarTodasLeidas(userId);
        return ResponseEntity.noContent().build();
    }
}
