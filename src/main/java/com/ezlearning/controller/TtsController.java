package com.ezlearning.controller;

import com.ezlearning.model.dto.TtsRequest;
import com.ezlearning.model.dto.TtsResponse;
import com.ezlearning.service.TtsService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tts")
public class TtsController {

    private final TtsService ttsService;

    public TtsController(TtsService ttsService) {
        this.ttsService = ttsService;
    }

    @PostMapping("/synthesize")
    public ResponseEntity<TtsResponse> synthesize(
            @Valid @RequestBody TtsRequest request) {
        var response = ttsService.synthesize(request.text(), request.voice());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> serveAudio(@PathVariable String id) {
        var data = ttsService.loadAudio(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .body(data);
    }
}
