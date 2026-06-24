package com.ezlearning.model.dto;

import jakarta.validation.constraints.NotBlank;

public record TtsRequest(
    @NotBlank String text,
    String voice
) {
    public String voice() {
        return voice != null ? voice : "af_heart";
    }
}
