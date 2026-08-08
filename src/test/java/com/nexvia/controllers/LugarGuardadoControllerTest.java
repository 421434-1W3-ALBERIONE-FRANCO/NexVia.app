package com.nexvia.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexvia.config.JwtService;
import com.nexvia.dtos.LugarGuardadoRequest;
import com.nexvia.dtos.LugarGuardadoResponse;
import com.nexvia.exceptions.ForbiddenException;
import com.nexvia.exceptions.GlobalExceptionHandler;
import com.nexvia.exceptions.ResourceNotFoundException;
import com.nexvia.services.LugarGuardadoService;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LugarGuardadoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class LugarGuardadoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LugarGuardadoService lugarGuardadoService;

    @MockitoBean
    private JwtService jwtService;

    private final LugarGuardadoResponse sampleResponse = new LugarGuardadoResponse(
            1L, "Campo Norte", -32.0, -63.0, "CAMPO", 1L, LocalDateTime.of(2025, 1, 1, 12, 0)
    );

    private UsernamePasswordAuthenticationToken userAuth() {
        return new UsernamePasswordAuthenticationToken(1L, null,
                List.of(new SimpleGrantedAuthority("ROLE_USUARIO")));
    }

    private UsernamePasswordAuthenticationToken adminAuth() {
        return new UsernamePasswordAuthenticationToken(99L, null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void listar_returns200() throws Exception {
        when(lugarGuardadoService.listar(1L)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/lugares").principal(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Campo Norte"));
    }

    @Test
    void obtener_returns200() throws Exception {
        when(lugarGuardadoService.obtener(eq(1L), eq(1L), any())).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/lugares/1").principal(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("CAMPO"));
    }

    @Test
    void obtener_notFound_returns404() throws Exception {
        when(lugarGuardadoService.obtener(eq(99L), eq(1L), any()))
                .thenThrow(new ResourceNotFoundException("No encontrado"));

        mockMvc.perform(get("/api/v1/lugares/99").principal(userAuth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void obtener_forbidden_returns403() throws Exception {
        when(lugarGuardadoService.obtener(eq(1L), eq(1L), any()))
                .thenThrow(new ForbiddenException("No permitido"));

        mockMvc.perform(get("/api/v1/lugares/1").principal(userAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    void crear_returns201() throws Exception {
        var request = new LugarGuardadoRequest("Hacienda Sur", -33.0, -64.0, "hacienda");
        when(lugarGuardadoService.crear(any(), eq(1L))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/lugares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(userAuth()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void crear_blankNombre_returns400() throws Exception {
        var request = new LugarGuardadoRequest("", -32.0, -63.0, null);

        mockMvc.perform(post("/api/v1/lugares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(userAuth()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crear_nullLat_returns400() throws Exception {
        var request = new LugarGuardadoRequest("Campo", null, -63.0, null);

        mockMvc.perform(post("/api/v1/lugares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(userAuth()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crear_nullLng_returns400() throws Exception {
        var request = new LugarGuardadoRequest("Campo", -32.0, null, null);

        mockMvc.perform(post("/api/v1/lugares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(userAuth()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizar_returns200() throws Exception {
        var request = new LugarGuardadoRequest("Nuevo Nombre", -34.0, -58.0, "pueblo");
        var updated = new LugarGuardadoResponse(1L, "Nuevo Nombre", -34.0, -58.0, "PUEBLO", 1L,
                LocalDateTime.of(2025, 1, 1, 12, 0));
        when(lugarGuardadoService.actualizar(eq(1L), any(), eq(1L), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/lugares/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nuevo Nombre"));
    }

    @Test
    void actualizar_forbidden_returns403() throws Exception {
        var request = new LugarGuardadoRequest("X", -32.0, -63.0, null);
        when(lugarGuardadoService.actualizar(eq(1L), any(), eq(1L), any()))
                .thenThrow(new ForbiddenException("No permitido"));

        mockMvc.perform(put("/api/v1/lugares/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(userAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    void eliminar_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/lugares/1").principal(userAuth()))
                .andExpect(status().isNoContent());
    }

    @Test
    void eliminar_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("No encontrado"))
                .when(lugarGuardadoService).eliminar(eq(99L), eq(1L), any());

        mockMvc.perform(delete("/api/v1/lugares/99").principal(userAuth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminar_forbidden_returns403() throws Exception {
        doThrow(new ForbiddenException("No permitido"))
                .when(lugarGuardadoService).eliminar(eq(1L), eq(1L), any());

        mockMvc.perform(delete("/api/v1/lugares/1").principal(userAuth()))
                .andExpect(status().isForbidden());
    }
}
