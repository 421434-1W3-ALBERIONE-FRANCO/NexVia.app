package com.nexvia.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexvia.config.JwtService;
import com.nexvia.config.RateLimitFilter;
import com.nexvia.dtos.*;
import com.nexvia.exceptions.DuplicateResourceException;
import com.nexvia.exceptions.GlobalExceptionHandler;
import com.nexvia.exceptions.TokenRefreshException;
import com.nexvia.services.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private RateLimitFilter rateLimitFilter;

    private UsernamePasswordAuthenticationToken userAuth() {
        return new UsernamePasswordAuthenticationToken(1L, null,
                List.of(new SimpleGrantedAuthority("ROLE_USUARIO")));
    }

    @Test
    void register_returns201() throws Exception {
        var request = new RegisterRequest("test@mail.com", "123456", "Juan Pérez", "usuario");
        when(authService.register(any())).thenReturn(new FullAuthResponse("jwt-token", "refresh-token", 86400000L));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.expiresIn").value(86400000));
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        var request = new RegisterRequest("not-an-email", "123456", "Juan", "usuario");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_blankPassword_returns400() throws Exception {
        var request = new RegisterRequest("test@mail.com", "", "Juan", "usuario");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_shortPassword_returns400() throws Exception {
        var request = new RegisterRequest("test@mail.com", "123", "Juan", "usuario");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_blankFullName_returns400() throws Exception {
        var request = new RegisterRequest("test@mail.com", "123456", "", "usuario");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_blankRole_returns400() throws Exception {
        var request = new RegisterRequest("test@mail.com", "123456", "Juan", "");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        var request = new RegisterRequest("dup@mail.com", "123456", "Juan", "usuario");
        when(authService.register(any())).thenThrow(new DuplicateResourceException("Ya existe"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void login_returns200() throws Exception {
        var request = new LoginRequest("test@mail.com", "123456");
        when(authService.login(any())).thenReturn(new FullAuthResponse("jwt-login", "rt", 86400000L));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-login"));
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        var request = new LoginRequest("test@mail.com", "wrong");
        when(authService.login(any())).thenThrow(new BadCredentialsException("bad"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_blankEmail_returns400() throws Exception {
        var request = new LoginRequest("", "123456");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_blankPassword_returns400() throws Exception {
        var request = new LoginRequest("test@mail.com", "");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refresh_returns200() throws Exception {
        var request = new RefreshTokenRequest("valid-rt");
        when(authService.refresh("valid-rt")).thenReturn(new FullAuthResponse("new-jwt", "new-rt", 86400000L));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("new-jwt"))
                .andExpect(jsonPath("$.refreshToken").value("new-rt"));
    }

    @Test
    void refresh_invalidToken_returns401() throws Exception {
        var request = new RefreshTokenRequest("bad-rt");
        when(authService.refresh("bad-rt")).thenThrow(new TokenRefreshException("Token inválido"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_blankToken_returns400() throws Exception {
        var request = new RefreshTokenRequest("");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logout_returns204() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout").principal(userAuth()))
                .andExpect(status().isNoContent());
    }

    @Test
    void me_returns200() throws Exception {
        when(authService.me(1L)).thenReturn(new UsuarioResponse(1L, "test@mail.com", "Juan", "USUARIO"));

        mockMvc.perform(get("/api/v1/auth/me").principal(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@mail.com"));
    }
}
