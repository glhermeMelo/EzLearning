package com.ezlearning.controller;

import com.ezlearning.model.dto.ReasoningRequest;
import com.ezlearning.model.dto.ReasoningResponse;
import com.ezlearning.service.ReasoningService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ReasoningController {

    private final ReasoningService reasoningService;

    public ReasoningController(ReasoningService reasoningService) {
        this.reasoningService = reasoningService;
    }

    @PostMapping("/reason")
    public ResponseEntity<ReasoningResponse> reason(
            @Valid @RequestBody ReasoningRequest request) {
        var response = reasoningService.reason(request);
        return ResponseEntity.ok(response);
    }
}
