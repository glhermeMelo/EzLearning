package com.ezlearning.model.dto;

public record TtsResponse(
    String audioUrl,
    double durationSeconds,
    String format
) {}
