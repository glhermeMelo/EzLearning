package com.ezlearning.controller;

import com.ezlearning.model.dto.MediaGenerationRequest;
import com.ezlearning.model.dto.MediaGenerationResponse;
import com.ezlearning.service.MediaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping("/generate")
    public ResponseEntity<MediaGenerationResponse> generate(
            @Valid @RequestBody MediaGenerationRequest request,
            Authentication authentication) {
        UUID userId = authentication != null ? (UUID) authentication.getPrincipal() : null;
        var response = mediaService.generateMedia(request, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> serveMedia(@PathVariable UUID id) throws IOException {
        var data = mediaService.loadMedia(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + id + ".png\"")
                .contentType(MediaType.IMAGE_PNG)
                .body(data);
    }
}
