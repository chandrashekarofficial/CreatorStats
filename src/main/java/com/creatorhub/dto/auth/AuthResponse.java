package com.creatorhub.dto.auth;

public record AuthResponse(
        Long userId,
        String name,
        String email,
        String token,
        String profileImageUrl,
        String loginProvider
) {}
