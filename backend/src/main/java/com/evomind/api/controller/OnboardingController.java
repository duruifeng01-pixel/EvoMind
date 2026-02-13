package com.evomind.api.controller;

import com.evomind.api.model.ApiResponse;
import com.evomind.api.model.OnboardingStateResponse;
import com.evomind.api.store.InMemoryStore;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/onboarding")
public class OnboardingController {
    private final InMemoryStore store;

    public OnboardingController(InMemoryStore store) {
        this.store = store;
    }

    @GetMapping("/state")
    public ApiResponse<OnboardingStateResponse> state(@RequestParam String userId) {
        boolean completed = store.isOnboardingDone(userId);
        return ApiResponse.ok(new OnboardingStateResponse(completed, 5, completed ? 5 : 0, "完成后赠送7天基础体验套餐"));
    }

    @PostMapping("/complete")
    public ApiResponse<Map<String, String>> complete(@RequestParam String userId) {
        store.completeOnboarding(userId);
        return ApiResponse.ok(Map.of("message", "新手引导已完成，7天体验权益已发放"));
    }
}
