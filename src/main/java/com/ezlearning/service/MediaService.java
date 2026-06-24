package com.ezlearning.service;

import com.ezlearning.model.dto.MediaGenerationRequest;
import com.ezlearning.model.dto.MediaGenerationResponse;
import com.ezlearning.model.dto.MediaRequest;
import com.ezlearning.model.dto.MediaResponse;

import java.io.IOException;
import java.util.UUID;

public interface MediaService {

    MediaGenerationResponse generateMedia(MediaGenerationRequest request, UUID userId);

    MediaResponse generateDiagram(MediaRequest request);

    byte[] loadMedia(UUID id) throws IOException;

    String getMimeType(UUID id);

    void cleanupUnreferencedMedia();
}
