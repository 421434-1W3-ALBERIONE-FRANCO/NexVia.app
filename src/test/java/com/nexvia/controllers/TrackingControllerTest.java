package com.nexvia.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexvia.config.JwtService;
import com.nexvia.dtos.PosicionViajeRequest;
import com.nexvia.dtos.PosicionViajeResponse;
import com.nexvia.dtos.RutaResponse;
import com.nexvia.exceptions.ForbiddenException;
import com.nexvia.exceptions.GlobalExceptionHandler;
import com.nexvia.exceptions.ResourceNotFoundException;
import com.nexvia.services.TrackingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TrackingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TrackingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TrackingService trackingService;

    @MockitoBean
    private JwtService jwtService;

    private UsernamePasswordAuthenticationToken choferAuth() {
        return new UsernamePasswordAuthenticationToken(2L, null,
                List.of(new SimpleGrantedAuthority("ROLE_CHOFER")));
    }

    private UsernamePasswordAuthenticationToken userAuth() {
        return new UsernamePasswordAuthenticationToken(1L, null,
                List.of(new SimpleGrantedAuthority("ROLE_USUARIO")));
    }

    @Test
    void registrarPosicion_returns201() throws Exception {
        var response = new PosicionViajeResponse(1L, 10L, -33.0, -64.0, 80.0, 90.0,
                LocalDateTime.of(2025, 6, 1, 12, 0));
        when(trackingService.registrarPosicion(eq(10L), any(), eq(2L), any())).thenReturn(response);

        var request = new PosicionViajeRequest(-33.0, -64.0, 80.0, 90.0);

        mockMvc.perform(post("/api/v1/tracking/10/posicion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(choferAuth()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lat").value(-33.0))
                .andExpect(jsonPath("$.velocidad").value(80.0));
    }

    @Test
    void registrarPosicion_invalidLat_returns400() throws Exception {
        var request = new PosicionViajeRequest(91.0, -64.0, null, null);

        mockMvc.perform(post("/api/v1/tracking/10/posicion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(choferAuth()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registrarPosicion_forbidden_returns403() throws Exception {
        when(trackingService.registrarPosicion(eq(10L), any(), eq(2L), any()))
                .thenThrow(new ForbiddenException("No permitido"));

        var request = new PosicionViajeRequest(-33.0, -64.0, null, null);

        mockMvc.perform(post("/api/v1/tracking/10/posicion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(choferAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    void posicionActual_returns200() throws Exception {
        var response = new PosicionViajeResponse(1L, 10L, -33.5, -64.5, 60.0, 180.0,
                LocalDateTime.of(2025, 6, 1, 12, 30));
        when(trackingService.obtenerUltimaPosicion(10L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/tracking/10/posicion-actual").principal(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lat").value(-33.5))
                .andExpect(jsonPath("$.viajeId").value(10));
    }

    @Test
    void posicionActual_notFound_returns404() throws Exception {
        when(trackingService.obtenerUltimaPosicion(10L))
                .thenThrow(new ResourceNotFoundException("No hay posiciones"));

        mockMvc.perform(get("/api/v1/tracking/10/posicion-actual").principal(userAuth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void ruta_returns200() throws Exception {
        var p1 = new PosicionViajeResponse(1L, 10L, -32.0, -63.0, 60.0, 180.0,
                LocalDateTime.of(2025, 6, 1, 10, 0));
        var p2 = new PosicionViajeResponse(2L, 10L, -33.0, -64.0, 70.0, 190.0,
                LocalDateTime.of(2025, 6, 1, 11, 30));
        var ruta = new RutaResponse(10L, 2, 135.5, 90L, p2, List.of(p1, p2));
        when(trackingService.obtenerRuta(10L)).thenReturn(ruta);

        mockMvc.perform(get("/api/v1/tracking/10/ruta").principal(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viajeId").value(10))
                .andExpect(jsonPath("$.totalPuntos").value(2))
                .andExpect(jsonPath("$.distanciaRecorridaKm").value(135.5))
                .andExpect(jsonPath("$.tiempoTranscurridoMinutos").value(90))
                .andExpect(jsonPath("$.puntos.length()").value(2));
    }

    @Test
    void ruta_emptyTrip_returns200() throws Exception {
        var ruta = new RutaResponse(10L, 0, 0.0, null, null, List.of());
        when(trackingService.obtenerRuta(10L)).thenReturn(ruta);

        mockMvc.perform(get("/api/v1/tracking/10/ruta").principal(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPuntos").value(0))
                .andExpect(jsonPath("$.posicionActual").doesNotExist());
    }
}
