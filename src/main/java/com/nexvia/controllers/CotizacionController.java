package com.nexvia.controllers;

import com.nexvia.dtos.CotizacionRequest;
import com.nexvia.dtos.CotizacionResponse;
import com.nexvia.services.CotizacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cotizaciones")
@RequiredArgsConstructor
public class CotizacionController {

    private final CotizacionService cotizacionService;

    @PostMapping
    public ResponseEntity<CotizacionResponse> cotizar(@Valid @RequestBody CotizacionRequest request) {
        return ResponseEntity.ok(cotizacionService.cotizar(request));
    }
}
