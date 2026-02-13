package com.evomind.api.controller;

import com.evomind.api.model.ApiResponse;
import com.evomind.api.model.ArtifactRequest;
import com.evomind.api.model.ChallengeTask;
import com.evomind.api.model.TaskStatusRequest;
import com.evomind.api.store.InMemoryStore;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/challenges")
public class ChallengeController {

    private final InMemoryStore store;

    public ChallengeController(InMemoryStore store) {
        this.store = store;
    }

    @GetMapping("/current")
    public ApiResponse<ChallengeTask> current(@RequestParam String userId) {
        return ApiResponse.ok(store.getOrInitTask(userId));
    }

    @PostMapping("/{id}/status")
    public ApiResponse<ChallengeTask> updateStatus(@PathVariable String id, @Valid @RequestBody TaskStatusRequest req) {
        return ApiResponse.ok(store.updateTaskStatus(req.userId(), req.status()));
    }

    @PostMapping("/{id}/artifact")
    public ApiResponse<Map<String, String>> uploadArtifact(@PathVariable String id, @Valid @RequestBody ArtifactRequest req) {
        return ApiResponse.ok(Map.of(
                "taskId", id,
                "result", "作品已上传并归档",
                "type", req.type()
        ));
    }
}
