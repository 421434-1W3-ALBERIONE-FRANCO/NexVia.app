package com.nexvia.services;

import com.nexvia.config.JwtService;
import com.nexvia.domain.RefreshToken;
import com.nexvia.domain.Role;
import com.nexvia.domain.Usuario;
import com.nexvia.dtos.FullAuthResponse;
import com.nexvia.dtos.LoginRequest;
import com.nexvia.dtos.RegisterRequest;
import com.nexvia.dtos.UsuarioResponse;
import com.nexvia.exceptions.DuplicateResourceException;
import com.nexvia.exceptions.ResourceNotFoundException;
import com.nexvia.exceptions.TokenRefreshException;
import com.nexvia.repositories.RefreshTokenRepository;
import com.nexvia.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshExpirationMs", 604800000L);
    }

    private Usuario buildUsuario(Long id, String email, Role role) {
        return Usuario.builder().id(id).email(email).password("hashed").fullName("User").role(role).build();
    }

    @Test
    void register_success() {
        var request = new RegisterRequest("test@mail.com", "123456", "Juan Pérez", "usuario");
        when(usuarioRepository.existsByEmail("test@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("hashed");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtService.generateToken(1L, "test@mail.com", "USUARIO")).thenReturn("jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FullAuthResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.expiresIn()).isEqualTo(86400000L);
        assertThat(response.refreshToken()).isNotNull();

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("hashed");
        assertThat(captor.getValue().getRole()).isEqualTo(Role.USUARIO);
    }

    @Test
    void register_duplicateEmail_throwsDuplicateResourceException() {
        var request = new RegisterRequest("dup@mail.com", "123456", "Nombre", "usuario");
        when(usuarioRepository.existsByEmail("dup@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("dup@mail.com");
    }

    @Test
    void register_invalidRole_throwsIllegalArgument() {
        var request = new RegisterRequest("test@mail.com", "123456", "Nombre", "piloto");
        when(usuarioRepository.existsByEmail("test@mail.com")).thenReturn(false);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("piloto");
    }

    @Test
    void register_roleChofer_success() {
        var request = new RegisterRequest("chofer@mail.com", "123456", "Chofer", "chofer");
        when(usuarioRepository.existsByEmail("chofer@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("hashed");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(2L);
            return u;
        });
        when(jwtService.generateToken(2L, "chofer@mail.com", "CHOFER")).thenReturn("jwt-chofer");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FullAuthResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("jwt-chofer");
    }

    @Test
    void register_roleAdmin_success() {
        var request = new RegisterRequest("admin@mail.com", "123456", "Admin", "ADMIN");
        when(usuarioRepository.existsByEmail("admin@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("hashed");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(3L);
            return u;
        });
        when(jwtService.generateToken(3L, "admin@mail.com", "ADMIN")).thenReturn("jwt-admin");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FullAuthResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("jwt-admin");
    }

    @Test
    void login_success() {
        var request = new LoginRequest("test@mail.com", "123456");
        var usuario = buildUsuario(1L, "test@mail.com", Role.USUARIO);
        when(usuarioRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("123456", "hashed")).thenReturn(true);
        when(jwtService.generateToken(1L, "test@mail.com", "USUARIO")).thenReturn("jwt-login");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FullAuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("jwt-login");
        assertThat(response.refreshToken()).isNotNull();
    }

    @Test
    void login_emailNotFound_throwsBadCredentials() {
        var request = new LoginRequest("noexiste@mail.com", "123456");
        when(usuarioRepository.findByEmail("noexiste@mail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_wrongPassword_throwsBadCredentials() {
        var request = new LoginRequest("test@mail.com", "wrong");
        var usuario = buildUsuario(1L, "test@mail.com", Role.USUARIO);
        when(usuarioRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refresh_success() {
        var usuario = buildUsuario(1L, "test@mail.com", Role.USUARIO);
        var rt = RefreshToken.builder().id(1L).token("valid-rt").usuario(usuario)
                .expiryDate(Instant.now().plusSeconds(3600)).build();

        when(refreshTokenRepository.findByToken("valid-rt")).thenReturn(Optional.of(rt));
        when(jwtService.generateToken(1L, "test@mail.com", "USUARIO")).thenReturn("new-jwt");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FullAuthResponse response = authService.refresh("valid-rt");

        assertThat(response.token()).isEqualTo("new-jwt");
        verify(refreshTokenRepository).delete(rt);
    }

    @Test
    void refresh_invalidToken_throws() {
        when(refreshTokenRepository.findByToken("bad")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("bad"))
                .isInstanceOf(TokenRefreshException.class)
                .hasMessageContaining("inválido");
    }

    @Test
    void refresh_expiredToken_throws() {
        var usuario = buildUsuario(1L, "test@mail.com", Role.USUARIO);
        var rt = RefreshToken.builder().id(1L).token("expired-rt").usuario(usuario)
                .expiryDate(Instant.now().minusSeconds(3600)).build();

        when(refreshTokenRepository.findByToken("expired-rt")).thenReturn(Optional.of(rt));

        assertThatThrownBy(() -> authService.refresh("expired-rt"))
                .isInstanceOf(TokenRefreshException.class)
                .hasMessageContaining("expirado");
        verify(refreshTokenRepository).delete(rt);
    }

    @Test
    void logout_success() {
        authService.logout(1L);
        verify(refreshTokenRepository).deleteByUsuarioId(1L);
    }

    @Test
    void me_success() {
        var usuario = buildUsuario(1L, "test@mail.com", Role.USUARIO);
        usuario.setFullName("Juan");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        UsuarioResponse response = authService.me(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("test@mail.com");
        assertThat(response.fullName()).isEqualTo("Juan");
        assertThat(response.role()).isEqualTo("USUARIO");
    }

    @Test
    void me_notFound_throwsResourceNotFoundException() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.me(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
