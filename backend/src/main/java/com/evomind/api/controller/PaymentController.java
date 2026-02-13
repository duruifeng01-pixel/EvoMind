package com.evomind.api.controller;

import com.evomind.api.integration.PaymentSdkClient;
import com.evomind.api.model.ApiResponse;
import com.evomind.api.model.PayCallbackRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/pay")
public class PaymentController {

    private final PaymentSdkClient paymentSdkClient;

    public PaymentController(PaymentSdkClient paymentSdkClient) {
        this.paymentSdkClient = paymentSdkClient;
    }

    @PostMapping("/wechat/callback")
    public ApiResponse<Map<String, String>> wechatCallback(@Valid @RequestBody PayCallbackRequest req) {
        boolean ok = paymentSdkClient.verifyWechat(req);
        return ApiResponse.ok(Map.of("orderNo", req.orderNo(), "tradeNo", req.tradeNo(), "status", ok ? req.status() : "FAILED", "message", ok ? "微信回调验签通过（演示）" : "微信回调验签失败"));
    }

    @PostMapping("/alipay/callback")
    public ApiResponse<Map<String, String>> alipayCallback(@Valid @RequestBody PayCallbackRequest req) {
        boolean ok = paymentSdkClient.verifyAlipay(req);
        return ApiResponse.ok(Map.of("orderNo", req.orderNo(), "tradeNo", req.tradeNo(), "status", ok ? req.status() : "FAILED", "message", ok ? "支付宝回调验签通过（演示）" : "支付宝回调验签失败"));
    }
}
