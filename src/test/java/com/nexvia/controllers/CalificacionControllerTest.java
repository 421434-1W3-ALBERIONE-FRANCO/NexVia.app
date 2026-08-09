package com.nexvia.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexvia.config.JwtService;
import com.nexvia.dtos.CalificacionRequest;
import com.nexvia.dtos.CalificacionResponse;
import com.nexvia.dtos.PromedioCalificacionResponse;
import com.nexvia.exceptions.ForbiddenException;
import com.nexvia.exceptions.GlobalExceptionHandler;
import com.nexvia.services.CalificacionService;
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

@WebMvcTest(CalificacionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CalificacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CalificacionService calificacionService;

    @MockitoBean
    private JwtService jwtService;

    private UsernamePasswordAuthenticationToken userAuth() {
        return new UsernamePasswordAuthenticationToken(1L, null,
                List.of(new SimpleGrantedAuthority("ROLE_USUARIO")));
    }

    private final CalificacionResponse sampleResponse = new CalificacionResponse(
            1L, 10L, 1L, 2L, 5, "Excelente", LocalDateTime.of(2025, 6, 1, 12, 0)
    );

    @Test
    void crear_returns201() throws Exception {
        var request = new CalificacionRequest(10L, 2L, 5, "Excelente");
        when(calificacionService.crear(any(), eq(1L))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/calificaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(userAuth()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.puntuacion").value(5))
                .andExpect(jsonPath("$.destinatarioId").value(2));
    }

    @Test
    void crear_invalidPuntuacion_returns400() throws Exception {
        var request = new CalificacionRequest(10L, 2L, 6, null);

        mockMvc.perform(post("/api/v1/calificaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(userAuth()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crear_zeroPuntuacion_returns400() throws Exception {
        var request = new CalificacionRequest(10L, 2L, 0, null);

        mockMvc.perform(post("/api/v1/calificaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(userAuth()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crear_nullViajeId_returns400() throws Exception {
        var request = new CalificacionRequest(null, 2L, 5, null);

        mockMvc.perform(post("/api/v1/calificaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(userAuth()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crear_forbidden_returns403() throws Exception {
        var request = new CalificacionRequest(10L, 2L, 5, null);
        when(calificacionService.crear(any(), eq(1L)))
                .thenThrow(new ForbiddenException("No participás"));

        mockMvc.perform(post("/api/v1/calificaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(userAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    void porUsuario_returns200() throws Exception {
        when(calificacionService.obtenerPorDestinatario(2L)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/calificaciones/usuario/2").principal(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].destinatarioId").value(2));
    }

    @Test
    void porViaje_returns200() throws Exception {
        when(calificacionService.obtenerPorViaje(10L)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/calificaciones/viaje/10").principal(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].viajeId").value(10));
    }

    @Test
    void promedio_returns200() throws Exception {
        var promedio = new PromedioCalificacionResponse(2L, 4.5, 10);
        when(calificacionService.promedio(2L)).thenReturn(promedio);

        mockMvc.perform(get("/api/v1/calificaciones/promedio/2").principal(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(2))
                .andExpect(jsonPath("$.promedio").value(4.5))
                .andExpect(jsonPath("$.totalCalificaciones").value(10));
    }
}
