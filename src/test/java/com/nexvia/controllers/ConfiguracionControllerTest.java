package com.nexvia.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexvia.config.JwtService;
import com.nexvia.dtos.ConfiguracionRequest;
import com.nexvia.dtos.ConfiguracionResponse;
import com.nexvia.exceptions.GlobalExceptionHandler;
import com.nexvia.exceptions.ResourceNotFoundException;
import com.nexvia.services.ConfiguracionService;
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

@WebMvcTest(ConfiguracionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ConfiguracionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ConfiguracionService configuracionService;

    @MockitoBean
    private JwtService jwtService;

    private final ConfiguracionResponse sampleResponse = new ConfiguracionResponse(
            1L, 500.0, 1200.0, "Zona Agrícola", -32.4341, -63.2433
    );

    @Test
    void listar_returns200() throws Exception {
        when(configuracionService.listar()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/configuraciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].tarifaPorKm").value(500.0));
    }

    @Test
    void obtener_returns200() throws Exception {
        when(configuracionService.obtener(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/configuraciones/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zonaNombre").value("Zona Agrícola"));
    }

    @Test
    void obtener_notFound_returns404() throws Exception {
        when(configuracionService.obtener(99L)).thenThrow(new ResourceNotFoundException("No encontrada"));

        mockMvc.perform(get("/api/v1/configuraciones/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void crear_returns201() throws Exception {
        var request = new ConfiguracionRequest(500.0, 1200.0, "Zona", -32.0, -63.0);
        when(configuracionService.crear(any())).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/configuraciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void crear_missingTarifaPorKm_returns400() throws Exception {
        var request = new ConfiguracionRequest(null, 0.0, "Zona", -32.0, -63.0);

        mockMvc.perform(post("/api/v1/configuraciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crear_missingCentroLat_returns400() throws Exception {
        var request = new ConfiguracionRequest(500.0, 0.0, "Zona", null, -63.0);

        mockMvc.perform(post("/api/v1/configuraciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crear_missingCentroLng_returns400() throws Exception {
        var request = new ConfiguracionRequest(500.0, 0.0, "Zona", -32.0, null);

        mockMvc.perform(post("/api/v1/configuraciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizar_returns200() throws Exception {
        var request = new ConfiguracionRequest(800.0, 1500.0, "Nueva Zona", -33.0, -64.0);
        var updated = new ConfiguracionResponse(1L, 800.0, 1500.0, "Nueva Zona", -33.0, -64.0);
        when(configuracionService.actualizar(eq(1L), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/configuraciones/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tarifaPorKm").value(800.0));
    }

    @Test
    void eliminar_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/configuraciones/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void eliminar_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("No encontrada")).when(configuracionService).eliminar(99L);

        mockMvc.perform(delete("/api/v1/configuraciones/99"))
                .andExpect(status().isNotFound());
    }
}
