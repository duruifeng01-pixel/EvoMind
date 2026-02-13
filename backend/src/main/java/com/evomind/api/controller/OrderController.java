package com.evomind.api.controller;

import com.evomind.api.model.*;
import com.evomind.api.store.InMemoryStore;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class OrderController {

    private final InMemoryStore store;

    public OrderController(InMemoryStore store) {
        this.store = store;
    }

    @PostMapping("/orders/create")
    public ApiResponse<OrderItem> createOrder(@Valid @RequestBody OrderCreateRequest req) {
        return ApiResponse.ok(store.createOrder(req));
    }

    @GetMapping("/orders/history")
    public ApiResponse<List<OrderItem>> orderHistory(@RequestParam String userId) {
        return ApiResponse.ok(store.orders(userId));
    }

    @PostMapping("/refund/apply")
    public ApiResponse<Map<String, String>> refundApply(@Valid @RequestBody RefundRequest req) {
        return ApiResponse.ok(Map.of("ticketNo", "RF" + System.currentTimeMillis(), "status", "待审核"));
    }

    @PostMapping("/privacy/export")
    public ApiResponse<Map<String, String>> export(@Valid @RequestBody PrivacyRequest req) {
        return ApiResponse.ok(Map.of("status", "已受理", "message", "导出文件将在24小时内生成"));
    }

    @PostMapping("/privacy/delete-account")
    public ApiResponse<Map<String, String>> deleteAccount(@Valid @RequestBody PrivacyRequest req) {
        return ApiResponse.ok(Map.of("status", "已受理", "message", "账号注销申请已提交，T+7完成删除"));
    }
}
