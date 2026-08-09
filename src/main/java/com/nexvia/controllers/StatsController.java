package com.nexvia.controllers;

import com.nexvia.dtos.DashboardStatsResponse;
import com.nexvia.dtos.EstadoCountResponse;
import com.nexvia.services.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStatsResponse> dashboard() {
        return ResponseEntity.ok(statsService.getDashboard());
    }

    @GetMapping("/viajes-por-estado")
    public ResponseEntity<List<EstadoCountResponse>> viajesPorEstado() {
        return ResponseEntity.ok(statsService.getViajesPorEstado());
    }
}
