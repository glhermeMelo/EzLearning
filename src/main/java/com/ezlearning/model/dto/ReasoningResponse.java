package com.ezlearning.model.dto;

import java.util.List;

public record ReasoningResponse(
    String answer,
    List<String> steps,
    double confidence
) {}
