package com.evomind.api.model;

import jakarta.validation.constraints.NotBlank;

public record WechatLoginRequest(@NotBlank String openid, @NotBlank String nickname) {}
