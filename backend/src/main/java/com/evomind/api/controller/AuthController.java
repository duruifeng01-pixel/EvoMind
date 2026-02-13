package com.evomind.api.controller;

import com.evomind.api.model.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @PostMapping("/sms/send")
    public ApiResponse<SmsSendResponse> sendSms(@Valid @RequestBody SmsSendRequest req) {
        return ApiResponse.ok(new SmsSendResponse(req.phone(), "BIZ" + System.currentTimeMillis(), "验证码已发送（演示环境固定123456）"));
    }

    @PostMapping("/sms/login")
    public ApiResponse<LoginResponse> smsLogin(@Valid @RequestBody LoginRequest req) {
        String userId = "u_" + req.phone().substring(Math.max(0, req.phone().length() - 4));
        LoginResponse data = new LoginResponse(
                userId,
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                "欢迎来到EvoMind（进化意志）"
        );
        return ApiResponse.ok(data);
    }

    @PostMapping("/password/login")
    public ApiResponse<LoginResponse> passwordLogin(@Valid @RequestBody PasswordLoginRequest req) {
        String userId = "u_" + req.phone().substring(Math.max(0, req.phone().length() - 4));
        return ApiResponse.ok(new LoginResponse(userId, UUID.randomUUID().toString(), UUID.randomUUID().toString(), "密码登录成功"));
    }

    @PostMapping("/wechat/login")
    public ApiResponse<LoginResponse> wechatLogin(@Valid @RequestBody WechatLoginRequest req) {
        String userId = "wx_" + Math.abs(req.openid().hashCode());
        return ApiResponse.ok(new LoginResponse(userId, UUID.randomUUID().toString(), UUID.randomUUID().toString(), "微信登录成功，欢迎" + req.nickname()));
    }

    @PostMapping("/password/reset")
    public ApiResponse<Map<String, String>> resetPassword(@Valid @RequestBody PasswordResetRequest req) {
        return ApiResponse.ok(Map.of("status", "ok", "message", "密码重置成功"));
    }

    @PostMapping("/logout")
    public ApiResponse<Map<String, String>> logout(@Valid @RequestBody LogoutRequest req) {
        return ApiResponse.ok(Map.of("status", "ok", "message", "已退出登录"));
    }
}
