package com.ezlearning.service;

import com.ezlearning.model.dto.ReasoningRequest;
import com.ezlearning.model.dto.ReasoningResponse;

import java.util.List;
import java.util.UUID;

public interface PdfExportService {

    byte[] exportChat(UUID messageId, ReasoningRequest request, ReasoningResponse response, List<UUID> mediaIds);

    byte[] exportWithImages(UUID messageId, ReasoningRequest request, ReasoningResponse response, List<UUID> mediaIds);
}
