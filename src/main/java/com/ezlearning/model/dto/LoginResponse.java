package com.ezlearning.model.dto;

public record LoginResponse(
    String token,
    String refreshToken,
    long expiresIn,
    UserInfo user
) {
    public record UserInfo(String name, String email) {}
}
