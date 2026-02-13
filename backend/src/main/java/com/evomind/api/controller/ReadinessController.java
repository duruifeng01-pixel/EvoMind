package com.evomind.api.controller;

import com.evomind.api.model.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
public class ReadinessController {

    @GetMapping("/readiness")
    public ApiResponse<Map<String, Object>> readiness() {
        return ApiResponse.ok(Map.of(
                "implemented", List.of(
                        "认证演示接口", "信息源导入演示", "认知卡片/脑图演示", "讨论接口演示", "订单与退款工单演示", "隐私导出/注销受理演示"
                ),
                "pending", List.of(
                        "MySQL/Redis持久化", "真实OCR/语音/AIGC SDK", "微信支付/支付宝真实验签和清结算", "完整安卓页面和本地数据库", "上架合规材料"
                )
        ));
    }
}
