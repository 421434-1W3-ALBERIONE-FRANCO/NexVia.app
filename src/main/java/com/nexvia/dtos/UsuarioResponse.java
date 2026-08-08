package com.nexvia.dtos;

public record UsuarioResponse(
        Long id,
        String email,
        String fullName,
        String role
) {}
