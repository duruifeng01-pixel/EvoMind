package com.evomind.api.model;

public record LoginResponse(String userId, String accessToken, String refreshToken, String welcome) {}
