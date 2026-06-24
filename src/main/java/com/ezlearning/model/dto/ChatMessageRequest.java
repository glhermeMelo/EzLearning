package com.ezlearning.model.dto;

public record ChatMessageRequest(
    String question,
    String context,
    String token
) {}
