package com.ezlearning.model.dto;

public record ChatStreamEvent(
    String type,
    Object data,
    String message
) {}
