package com.nexvia.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexvia.config.JwtService;
import com.nexvia.dtos.CamionRequest;
import com.nexvia.dtos.CamionResponse;
import com.nexvia.dtos.PosicionRequest;
import com.nexvia.exceptions.ForbiddenException;
import com.nexvia.exceptions.GlobalExceptionHandler;
import com.nexvia.exceptions.ResourceNotFoundException;
import com.nexvia.services.CamionService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CamionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CamionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CamionService camionService;

    @MockitoBean
    private JwtService jwtService;

    private final CamionResponse sampleResponse = new CamionResponse(
            1L, "Trans SA", "20-123", "Juan", "20-456", "ABC123", "XYZ789",
            "3512345", 10000, -32.0, -63.0, "DISPONIBLE", 1L
    );

    private UsernamePasswordAuthenticationToken choferAuth() {
        return new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("ROLE_CHOFER")));
    }

    private UsernamePasswordAuthenticationToken adminAuth() {
        return new UsernamePasswordAuthenticationToken(99L, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void listar_returns200() throws Exception {
        when(camionService.listar()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/camiones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].patente").value("ABC123"));
    }

    @Test
    void listarDisponibles_returns200() throws Exception {
        when(camionService.listarDisponibles()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/camiones/disponibles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("DISPONIBLE"));
    }

    @Test
    void miCamion_returns200() throws Exception {
        when(camionService.miCamion(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/camiones/mi-camion").principal(choferAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.choferNombre").value("Juan"));
    }

    @Test
    void miCamion_notFound_returns404() throws Exception {
        when(camionService.miCamion(1L)).thenThrow(new ResourceNotFoundException("No tenés camión"));

        mockMvc.perform(get("/api/v1/camiones/mi-camion").principal(choferAuth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void obtener_returns200() throws Exception {
        when(camionService.obtener(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/camiones/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void crear_returns201() throws Exception {
        var request = new CamionRequest("Trans", null, "Juan", null, "ABC123", null, null, 10000, null, null);
        when(camionService.crear(any(), eq(1L))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/camiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(choferAuth()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void crear_blankChoferNombre_returns400() throws Exception {
        var request = new CamionRequest(null, null, "", null, "ABC123", null, null, null, null, null);

        mockMvc.perform(post("/api/v1/camiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(choferAuth()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crear_blankPatente_returns400() throws Exception {
        var request = new CamionRequest(null, null, "Juan", null, "", null, null, null, null, null);

        mockMvc.perform(post("/api/v1/camiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(choferAuth()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizar_returns200() throws Exception {
        var request = new CamionRequest(null, null, "Pedro", null, "NEW123", null, null, null, null, null);
        var updated = new CamionResponse(1L, null, null, "Pedro", null, "NEW123", null, null, 0, -32.0, -63.0, "DISPONIBLE", 1L);
        when(camionService.actualizar(eq(1L), any(), eq(1L), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/camiones/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(choferAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.choferNombre").value("Pedro"));
    }

    @Test
    void actualizar_forbidden_returns403() throws Exception {
        var request = new CamionRequest(null, null, "X", null, "X", null, null, null, null, null);
        when(camionService.actualizar(eq(1L), any(), eq(1L), any())).thenThrow(new ForbiddenException("No permitido"));

        mockMvc.perform(put("/api/v1/camiones/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(choferAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    void actualizarPosicion_returns200() throws Exception {
        var request = new PosicionRequest(-34.0, -58.0);
        var updated = new CamionResponse(1L, null, null, "Juan", null, "ABC123", null, null, 0, -34.0, -58.0, "DISPONIBLE", 1L);
        when(camionService.actualizarPosicion(eq(1L), any(), eq(1L), any())).thenReturn(updated);

        mockMvc.perform(patch("/api/v1/camiones/1/posicion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(choferAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lat").value(-34.0));
    }

    @Test
    void actualizarPosicion_nullLat_returns400() throws Exception {
        var request = new PosicionRequest(null, -58.0);

        mockMvc.perform(patch("/api/v1/camiones/1/posicion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(choferAuth()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizarPosicion_nullLng_returns400() throws Exception {
        var request = new PosicionRequest(-34.0, null);

        mockMvc.perform(patch("/api/v1/camiones/1/posicion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(choferAuth()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void eliminar_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/camiones/1").principal(adminAuth()))
                .andExpect(status().isNoContent());
    }

    @Test
    void eliminar_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("No encontrado")).when(camionService).eliminar(99L);

        mockMvc.perform(delete("/api/v1/camiones/99").principal(adminAuth()))
                .andExpect(status().isNotFound());
    }
}
