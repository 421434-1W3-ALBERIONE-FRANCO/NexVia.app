package com.nexvia.dtos;

public record FullAuthResponse(
        String token,
        String refreshToken,
        long expiresIn
) {}
