package com.nexvia.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexvia.config.JwtService;
import com.nexvia.config.RateLimitFilter;
import com.nexvia.dtos.CotizacionRequest;
import com.nexvia.dtos.CotizacionResponse;
import com.nexvia.exceptions.GlobalExceptionHandler;
import com.nexvia.exceptions.ResourceNotFoundException;
import com.nexvia.services.CotizacionService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CotizacionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CotizacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CotizacionService cotizacionService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private RateLimitFilter rateLimitFilter;

    private UsernamePasswordAuthenticationToken userAuth() {
        return new UsernamePasswordAuthenticationToken(1L, null,
                List.of(new SimpleGrantedAuthority("ROLE_USUARIO")));
    }

    @Test
    void cotizar_returns200() throws Exception {
        var request = new CotizacionRequest(400.0, 10.0, "POR_KM", null);
        var response = new CotizacionResponse(60000.0, 400.0, 10.0, "POR_KM", 150.0, "Córdoba");
        when(cotizacionService.cotizar(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/cotizaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precioCalculado").value(60000.0))
                .andExpect(jsonPath("$.tarifaAplicada").value(150.0));
    }

    @Test
    void cotizar_nullDistancia_returns400() throws Exception {
        var request = new CotizacionRequest(null, null, null, null);

        mockMvc.perform(post("/api/v1/cotizaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(userAuth()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cotizar_negativeDistancia_returns400() throws Exception {
        var request = new CotizacionRequest(-10.0, null, null, null);

        mockMvc.perform(post("/api/v1/cotizaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(userAuth()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cotizar_noConfig_returns404() throws Exception {
        var request = new CotizacionRequest(100.0, null, null, null);
        when(cotizacionService.cotizar(any())).thenThrow(new ResourceNotFoundException("No hay config"));

        mockMvc.perform(post("/api/v1/cotizaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(userAuth()))
                .andExpect(status().isNotFound());
    }
}
