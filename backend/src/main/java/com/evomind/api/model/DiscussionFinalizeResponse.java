package com.evomind.api.model;

public record DiscussionFinalizeResponse(String discussionId, String summary, String unresolvedQuestion, String tag) {}
