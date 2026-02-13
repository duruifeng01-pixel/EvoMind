package com.evomind.api.controller;

import com.evomind.api.model.*;
import com.evomind.api.store.InMemoryStore;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/discussion")
public class DiscussionController {

    private final InMemoryStore store;

    public DiscussionController(InMemoryStore store) {
        this.store = store;
    }

    @PostMapping("/daily-question/generate")
    public ApiResponse<DailyQuestionResponse> generate() {
        return ApiResponse.ok(store.dailyQuestion());
    }

    @PostMapping("/{id}/reply")
    public ApiResponse<DiscussionReplyResponse> reply(@PathVariable String id, @Valid @RequestBody DiscussionReplyRequest req) {
        return ApiResponse.ok(store.followUp(id, req.answer()));
    }

    @PostMapping("/{id}/finalize")
    public ApiResponse<DiscussionFinalizeResponse> finalizeDiscussion(@PathVariable String id, @Valid @RequestBody DiscussionFinalizeRequest req) {
        return ApiResponse.ok(store.finalizeDiscussion(id));
    }
}
