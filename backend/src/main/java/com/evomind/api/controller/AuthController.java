package com.evomind.api.controller;

import com.evomind.api.model.ApiResponse;
import com.evomind.api.model.LoginRequest;
import com.evomind.api.model.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

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
}
