package com.evomind.api.controller;

import com.evomind.api.model.ApiResponse;
import com.evomind.api.model.PayCallbackRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/pay")
public class PaymentController {

    @PostMapping("/wechat/callback")
    public ApiResponse<Map<String, String>> wechatCallback(@Valid @RequestBody PayCallbackRequest req) {
        return ApiResponse.ok(Map.of("orderNo", req.orderNo(), "tradeNo", req.tradeNo(), "status", req.status(), "message", "微信回调验签通过（演示）"));
    }

    @PostMapping("/alipay/callback")
    public ApiResponse<Map<String, String>> alipayCallback(@Valid @RequestBody PayCallbackRequest req) {
        return ApiResponse.ok(Map.of("orderNo", req.orderNo(), "tradeNo", req.tradeNo(), "status", req.status(), "message", "支付宝回调验签通过（演示）"));
    }
}
