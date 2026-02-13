package com.evomind.api.controller;

import com.evomind.api.model.ApiResponse;
import com.evomind.api.model.PlanItem;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscription")
public class PlanController {

    @GetMapping("/plans")
    public ApiResponse<List<PlanItem>> plans() {
        return ApiResponse.ok(List.of(
                new PlanItem("BASIC", "基础套餐", "WEEK/MONTH", 20, 5, "无", List.of("信息源<=20", "无观点冲突标记", "摘要token<=1000/天")),
                new PlanItem("ADVANCED", "进阶套餐", "WEEK/MONTH", 50, 20, "3次/周", List.of("信息源<=50", "观点冲突标记无限", "摘要token<=5000/天")),
                new PlanItem("CUSTOM", "定制套餐", "WEEK/MONTH", -1, -1, "无限", List.of("信息源无限", "全功能无限", "按实时算力动态计费"))
        ));
    }
}
