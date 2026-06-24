package com.ezlearning.controller;

import com.ezlearning.config.RateLimitingInterceptor;
import com.ezlearning.model.dto.ChatStreamEvent;
import com.ezlearning.model.dto.ReasoningRequest;
import com.ezlearning.model.dto.ReasoningResponse;
import com.ezlearning.service.ReasoningService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ReasoningService reasoningService;
    private final RateLimitingInterceptor rateLimitingInterceptor;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public ChatController(ReasoningService reasoningService,
                          RateLimitingInterceptor rateLimitingInterceptor) {
        this.reasoningService = reasoningService;
        this.rateLimitingInterceptor = rateLimitingInterceptor;
    }

    @PostMapping("/reason")
    public ResponseEntity<ReasoningResponse> reason(
            @Valid @RequestBody ReasoningRequest request) {
        var response = reasoningService.reason(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @RequestParam String question,
            @RequestParam(required = false) String context,
            Principal principal) {
        var emitter = new SseEmitter(60_000L);
        var userId = principal.getName();

        if (rateLimitingInterceptor.isRateLimited(userId)) {
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(new ChatStreamEvent("error", null, "Rate limit exceeded. Max 5 requests per minute.")));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        rateLimitingInterceptor.incrementCounter(userId);

        emitter.onTimeout(() -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(new ChatStreamEvent("error", null, "Request timed out")));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        });

        executor.submit(() -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(new ChatStreamEvent("thinking", null, null)));

                var request = new ReasoningRequest(question, context);
                var response = reasoningService.reason(request);

                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(new ChatStreamEvent("result", Map.of(
                                "answer", response.answer(),
                                "steps", response.steps(),
                                "confidence", response.confidence()
                        ), null)));

                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(new ChatStreamEvent("complete", null, null)));

                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(new ChatStreamEvent("error", null, e.getMessage())));
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            }
        });

        return emitter;
    }
}
