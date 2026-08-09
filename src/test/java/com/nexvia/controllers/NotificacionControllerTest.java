package com.nexvia.controllers;

import com.nexvia.config.JwtService;
import com.nexvia.dtos.NotificacionCountResponse;
import com.nexvia.dtos.NotificacionResponse;
import com.nexvia.exceptions.GlobalExceptionHandler;
import com.nexvia.services.NotificacionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificacionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class NotificacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificacionService notificacionService;

    @MockitoBean
    private JwtService jwtService;

    private UsernamePasswordAuthenticationToken userAuth() {
        return new UsernamePasswordAuthenticationToken(1L, null,
                List.of(new SimpleGrantedAuthority("ROLE_USUARIO")));
    }

    private final NotificacionResponse sampleNotif = new NotificacionResponse(
            1L, "VIAJE_ACEPTADO", "Tu viaje fue aceptado", 1L, 10L, false,
            LocalDateTime.of(2025, 6, 1, 12, 0)
    );

    @Test
    void listar_returns200() throws Exception {
        when(notificacionService.listarPorUsuario(1L)).thenReturn(List.of(sampleNotif));

        mockMvc.perform(get("/api/v1/notificaciones").principal(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("VIAJE_ACEPTADO"))
                .andExpect(jsonPath("$[0].mensaje").value("Tu viaje fue aceptado"));
    }

    @Test
    void noLeidas_returns200() throws Exception {
        when(notificacionService.listarNoLeidas(1L)).thenReturn(List.of(sampleNotif));

        mockMvc.perform(get("/api/v1/notificaciones/no-leidas").principal(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].leida").value(false));
    }

    @Test
    void count_returns200() throws Exception {
        when(notificacionService.contarNoLeidas(1L)).thenReturn(new NotificacionCountResponse(3));

        mockMvc.perform(get("/api/v1/notificaciones/count").principal(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noLeidas").value(3));
    }

    @Test
    void marcarLeida_returns200() throws Exception {
        var leida = new NotificacionResponse(1L, "VIAJE_ACEPTADO", "Tu viaje fue aceptado",
                1L, 10L, true, LocalDateTime.of(2025, 6, 1, 12, 0));
        when(notificacionService.marcarLeida(1L, 1L)).thenReturn(leida);

        mockMvc.perform(patch("/api/v1/notificaciones/1/leer").principal(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leida").value(true));
    }

    @Test
    void marcarTodasLeidas_returns204() throws Exception {
        mockMvc.perform(patch("/api/v1/notificaciones/leer-todas").principal(userAuth()))
                .andExpect(status().isNoContent());

        verify(notificacionService).marcarTodasLeidas(1L);
    }
}
