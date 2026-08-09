package com.nexvia.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexvia.config.JwtService;
import com.nexvia.dtos.CancelacionRequest;
import com.nexvia.dtos.ViajeRequest;
import com.nexvia.dtos.ViajeResponse;
import com.nexvia.exceptions.ForbiddenException;
import com.nexvia.exceptions.GlobalExceptionHandler;
import com.nexvia.exceptions.ResourceNotFoundException;
import com.nexvia.services.ViajeService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ViajeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ViajeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ViajeService viajeService;

    @MockitoBean
    private JwtService jwtService;

    private final ViajeResponse sampleResponse = new ViajeResponse(
            1L, -32.0, -63.0, -34.0, -58.0, 400.0, 10.0,
            "POR_KM", 50000.0, "Soja", "SOLICITADO",
            1L, "User 1", null, null, null,
            null, null, null, 0.0,
            LocalDateTime.of(2025, 1, 1, 12, 0)
    );

    private UsernamePasswordAuthenticationToken userAuth() {
        return new UsernamePasswordAuthenticationToken(1L, null,
                List.of(new SimpleGrantedAuthority("ROLE_USUARIO")));
    }

    private UsernamePasswordAuthenticationToken choferAuth() {
        return new UsernamePasswordAuthenticationToken(2L, null,
                List.of(new SimpleGrantedAuthority("ROLE_CHOFER")));
    }

    private UsernamePasswordAuthenticationToken adminAuth() {
        return new UsernamePasswordAuthenticationToken(99L, null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void listar_returns200() throws Exception {
        when(viajeService.listar()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/viajes").principal(adminAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].carga").value("Soja"));
    }

    @Test
    void listarPorEstado_returns200() throws Exception {
        when(viajeService.listarPorEstado(any())).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/viajes/estado/SOLICITADO").principal(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("SOLICITADO"));
    }

    @Test
    void misViajes_returns200() throws Exception {
        when(viajeService.misViajes(1L)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/viajes/mis-viajes").principal(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usuarioId").value(1));
    }

    @Test
    void viajesDelCamion_returns200() throws Exception {
        when(viajeService.viajesDelCamion(5L)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/viajes/camion/5").principal(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void obtener_returns200() throws Exception {
        when(viajeService.obtener(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/viajes/1").principal(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distanciaKm").value(400.0));
    }

    @Test
    void obtener_notFound_returns404() throws Exception {
        when(viajeService.obtener(99L)).thenThrow(new ResourceNotFoundException("No encontrado"));

        mockMvc.perform(get("/api/v1/viajes/99").principal(userAuth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void crear_returns201() throws Exception {
        var request = new ViajeRequest(-32.0, -63.0, -34.0, -58.0, 400.0, 10.0, "POR_KM", 50000.0, "Soja");
        when(viajeService.crear(any(), eq(1L))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/viajes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(userAuth()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void crear_nullOrigenLat_returns400() throws Exception {
        var request = new ViajeRequest(null, -63.0, -34.0, -58.0, 400.0, null, null, 50000.0, "Soja");

        mockMvc.perform(post("/api/v1/viajes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(userAuth()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crear_blankCarga_returns400() throws Exception {
        var request = new ViajeRequest(-32.0, -63.0, -34.0, -58.0, 400.0, null, null, 50000.0, "");

        mockMvc.perform(post("/api/v1/viajes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(userAuth()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crear_negativePrecio_returns400() throws Exception {
        var request = new ViajeRequest(-32.0, -63.0, -34.0, -58.0, 400.0, null, null, -100.0, "Soja");

        mockMvc.perform(post("/api/v1/viajes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(userAuth()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crear_negativeDistancia_returns400() throws Exception {
        var request = new ViajeRequest(-32.0, -63.0, -34.0, -58.0, -10.0, null, null, 50000.0, "Soja");

        mockMvc.perform(post("/api/v1/viajes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(userAuth()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aceptar_returns200() throws Exception {
        var accepted = new ViajeResponse(1L, -32.0, -63.0, -34.0, -58.0, 400.0, 10.0,
                "POR_KM", 50000.0, "Soja", "ACEPTADO", 1L, "User 1", 5L, "Chofer", 2L,
                null, null, null, 0.0,
                LocalDateTime.of(2025, 1, 1, 12, 0));
        when(viajeService.aceptar(1L, 5L, 2L)).thenReturn(accepted);

        mockMvc.perform(patch("/api/v1/viajes/1/aceptar")
                        .param("camionId", "5")
                        .principal(choferAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACEPTADO"))
                .andExpect(jsonPath("$.camionId").value(5));
    }

    @Test
    void aceptar_wrongState_returns400() throws Exception {
        when(viajeService.aceptar(1L, 5L, 2L))
                .thenThrow(new IllegalArgumentException("No se puede aceptar"));

        mockMvc.perform(patch("/api/v1/viajes/1/aceptar")
                        .param("camionId", "5")
                        .principal(choferAuth()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void enCamino_returns200() throws Exception {
        var enCamino = new ViajeResponse(1L, -32.0, -63.0, -34.0, -58.0, 400.0, 10.0,
                "POR_KM", 50000.0, "Soja", "EN_CAMINO", 1L, "User 1", 5L, "Chofer", 2L,
                null, null, null, 0.0,
                LocalDateTime.of(2025, 1, 1, 12, 0));
        when(viajeService.avanzarEnCamino(eq(1L), eq(2L), any())).thenReturn(enCamino);

        mockMvc.perform(patch("/api/v1/viajes/1/en-camino").principal(choferAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_CAMINO"));
    }

    @Test
    void enCamino_forbidden_returns403() throws Exception {
        when(viajeService.avanzarEnCamino(eq(1L), eq(2L), any()))
                .thenThrow(new ForbiddenException("No permitido"));

        mockMvc.perform(patch("/api/v1/viajes/1/en-camino").principal(choferAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    void completar_returns200() throws Exception {
        var completado = new ViajeResponse(1L, -32.0, -63.0, -34.0, -58.0, 400.0, 10.0,
                "POR_KM", 50000.0, "Soja", "COMPLETADO", 1L, "User 1", 5L, "Chofer", 2L,
                null, null, null, 0.0,
                LocalDateTime.of(2025, 1, 1, 12, 0));
        when(viajeService.completar(eq(1L), eq(2L), any())).thenReturn(completado);

        mockMvc.perform(patch("/api/v1/viajes/1/completar").principal(choferAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADO"));
    }

    @Test
    void cancelar_returns200() throws Exception {
        var cancelado = new ViajeResponse(1L, -32.0, -63.0, -34.0, -58.0, 400.0, 10.0,
                "POR_KM", 50000.0, "Soja", "CANCELADO", 1L, "User 1", null, null, null,
                "No quiero", 1L, LocalDateTime.of(2025, 1, 1, 12, 30), 0.0,
                LocalDateTime.of(2025, 1, 1, 12, 0));
        when(viajeService.cancelar(eq(1L), eq("No quiero"), eq(1L), any())).thenReturn(cancelado);

        var cancelRequest = new CancelacionRequest("No quiero");

        mockMvc.perform(patch("/api/v1/viajes/1/cancelar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelRequest))
                        .principal(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"))
                .andExpect(jsonPath("$.motivoCancelacion").value("No quiero"))
                .andExpect(jsonPath("$.penalidad").value(0.0));
    }

    @Test
    void cancelar_forbidden_returns403() throws Exception {
        when(viajeService.cancelar(eq(1L), eq("motivo"), eq(1L), any()))
                .thenThrow(new ForbiddenException("No tenés permiso"));

        var cancelRequest = new CancelacionRequest("motivo");

        mockMvc.perform(patch("/api/v1/viajes/1/cancelar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelRequest))
                        .principal(userAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelar_blankMotivo_returns400() throws Exception {
        var cancelRequest = new CancelacionRequest("");

        mockMvc.perform(patch("/api/v1/viajes/1/cancelar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelRequest))
                        .principal(userAuth()))
                .andExpect(status().isBadRequest());
    }
}
