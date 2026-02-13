package com.evomind.api.model;

public record OnboardingStateResponse(boolean completed, int totalSteps, int finishedSteps, String reward) {}
