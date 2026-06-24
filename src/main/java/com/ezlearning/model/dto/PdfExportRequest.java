package com.ezlearning.model.dto;

import java.util.List;
import java.util.UUID;

public record PdfExportRequest(
    String question,
    String context,
    String answer,
    List<String> steps,
    double confidence,
    List<UUID> mediaIds
) {}
