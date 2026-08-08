package com.nexvia.dtos;

public record AuthResponse(
        String token,
        long expiresIn
) {}
