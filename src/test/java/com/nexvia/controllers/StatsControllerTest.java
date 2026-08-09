package com.nexvia.controllers;

import com.nexvia.config.JwtService;
import com.nexvia.dtos.DashboardStatsResponse;
import com.nexvia.dtos.EstadoCountResponse;
import com.nexvia.exceptions.GlobalExceptionHandler;
import com.nexvia.services.StatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatsService statsService;

    @MockitoBean
    private JwtService jwtService;

    private UsernamePasswordAuthenticationToken adminAuth() {
        return new UsernamePasswordAuthenticationToken(99L, null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void dashboard_returns200() throws Exception {
        var stats = new DashboardStatsResponse(
                100, 60, 10, 20, 3000000.0, 50000.0,
                24000.0, 600.0, 50000.0, 10.0,
                List.of(
                        new EstadoCountResponse("SOLICITADO", 10),
                        new EstadoCountResponse("ACEPTADO", 15),
                        new EstadoCountResponse("EN_CAMINO", 5),
                        new EstadoCountResponse("COMPLETADO", 60),
                        new EstadoCountResponse("CANCELADO", 10)
                )
        );
        when(statsService.getDashboard()).thenReturn(stats);

        mockMvc.perform(get("/api/v1/stats/dashboard").principal(adminAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalViajes").value(100))
                .andExpect(jsonPath("$.viajesCompletados").value(60))
                .andExpect(jsonPath("$.ingresosTotales").value(3000000.0))
                .andExpect(jsonPath("$.tasaCancelacion").value(10.0))
                .andExpect(jsonPath("$.viajesPorEstado").isArray())
                .andExpect(jsonPath("$.viajesPorEstado.length()").value(5));
    }

    @Test
    void viajesPorEstado_returns200() throws Exception {
        var counts = List.of(
                new EstadoCountResponse("SOLICITADO", 10),
                new EstadoCountResponse("COMPLETADO", 50)
        );
        when(statsService.getViajesPorEstado()).thenReturn(counts);

        mockMvc.perform(get("/api/v1/stats/viajes-por-estado").principal(adminAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("SOLICITADO"))
                .andExpect(jsonPath("$[0].cantidad").value(10))
                .andExpect(jsonPath("$[1].estado").value("COMPLETADO"))
                .andExpect(jsonPath("$[1].cantidad").value(50));
    }

    @Test
    void dashboard_emptyData_returns200() throws Exception {
        var stats = new DashboardStatsResponse(
                0, 0, 0, 0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, List.of()
        );
        when(statsService.getDashboard()).thenReturn(stats);

        mockMvc.perform(get("/api/v1/stats/dashboard").principal(adminAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalViajes").value(0))
                .andExpect(jsonPath("$.precioPromedio").value(0.0));
    }
}
