package com.nexvia.services;

import com.nexvia.config.JwtService;
import com.nexvia.domain.RefreshToken;
import com.nexvia.domain.Role;
import com.nexvia.domain.Usuario;
import com.nexvia.dtos.AuthResponse;
import com.nexvia.dtos.FullAuthResponse;
import com.nexvia.dtos.LoginRequest;
import com.nexvia.dtos.RegisterRequest;
import com.nexvia.dtos.UsuarioResponse;
import com.nexvia.exceptions.DuplicateResourceException;
import com.nexvia.exceptions.ResourceNotFoundException;
import com.nexvia.exceptions.TokenRefreshException;
import com.nexvia.repositories.RefreshTokenRepository;
import com.nexvia.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${nexvia.jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    public FullAuthResponse register(RegisterRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Ya existe un usuario con el email: " + request.email());
        }

        Role role = parseRole(request.role());

        Usuario usuario = Usuario.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(role)
                .build();

        usuario = usuarioRepository.save(usuario);

        String token = jwtService.generateToken(usuario.getId(), usuario.getEmail(), usuario.getRole().name());
        String refreshToken = createRefreshToken(usuario).getToken();
        return new FullAuthResponse(token, refreshToken, jwtService.getExpirationMs());
    }

    public FullAuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Email o contraseña incorrectos"));

        if (!passwordEncoder.matches(request.password(), usuario.getPassword())) {
            throw new BadCredentialsException("Email o contraseña incorrectos");
        }

        String token = jwtService.generateToken(usuario.getId(), usuario.getEmail(), usuario.getRole().name());
        String refreshToken = createRefreshToken(usuario).getToken();
        return new FullAuthResponse(token, refreshToken, jwtService.getExpirationMs());
    }

    @Transactional
    public FullAuthResponse refresh(String refreshTokenStr) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new TokenRefreshException("Refresh token inválido"));

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new TokenRefreshException("Refresh token expirado. Iniciá sesión de nuevo.");
        }

        Usuario usuario = refreshToken.getUsuario();
        refreshTokenRepository.delete(refreshToken);

        String newAccessToken = jwtService.generateToken(usuario.getId(), usuario.getEmail(), usuario.getRole().name());
        String newRefreshToken = createRefreshToken(usuario).getToken();
        return new FullAuthResponse(newAccessToken, newRefreshToken, jwtService.getExpirationMs());
    }

    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.deleteByUsuarioId(userId);
    }

    public UsuarioResponse me(Long userId) {
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        return toResponse(usuario);
    }

    private RefreshToken createRefreshToken(Usuario usuario) {
        RefreshToken refreshToken = RefreshToken.builder()
                .usuario(usuario)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    private Role parseRole(String role) {
        try {
            return Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Rol inválido: " + role + ". Roles válidos: USUARIO, CHOFER, ADMIN");
        }
    }

    private UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getFullName(),
                usuario.getRole().name()
        );
    }
}
