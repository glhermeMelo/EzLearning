package com.ezlearning.websocket;

import com.ezlearning.config.RateLimitingInterceptor;
import com.ezlearning.model.dto.ChatMessageRequest;
import com.ezlearning.model.dto.ChatStreamEvent;
import com.ezlearning.model.dto.ReasoningRequest;
import com.ezlearning.service.ReasoningService;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
public class ChatWebSocketHandler {

    private final ReasoningService reasoningService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RateLimitingInterceptor rateLimitingInterceptor;

    public ChatWebSocketHandler(ReasoningService reasoningService,
                                SimpMessagingTemplate messagingTemplate,
                                RateLimitingInterceptor rateLimitingInterceptor) {
        this.reasoningService = reasoningService;
        this.messagingTemplate = messagingTemplate;
        this.rateLimitingInterceptor = rateLimitingInterceptor;
    }

    @MessageMapping("/chat")
    public void handleMessage(@Payload ChatMessageRequest message, Principal principal) {
        var userId = principal.getName();

        if (rateLimitingInterceptor.isRateLimited(userId)) {
            messagingTemplate.convertAndSendToUser(userId, "/topic/chat",
                    new ChatStreamEvent("error", null, "Rate limit exceeded. Max 5 requests per minute."));
            return;
        }

        rateLimitingInterceptor.incrementCounter(userId);

        messagingTemplate.convertAndSendToUser(userId, "/topic/chat",
                new ChatStreamEvent("thinking", null, null));

        try {
            var request = new ReasoningRequest(message.question(), message.context());
            var response = reasoningService.reason(request);

            messagingTemplate.convertAndSendToUser(userId, "/topic/chat",
                    new ChatStreamEvent("result", Map.of(
                            "answer", response.answer(),
                            "steps", response.steps(),
                            "confidence", response.confidence()
                    ), null));
            messagingTemplate.convertAndSendToUser(userId, "/topic/chat",
                    new ChatStreamEvent("complete", null, null));
        } catch (Exception e) {
            messagingTemplate.convertAndSendToUser(userId, "/topic/chat",
                    new ChatStreamEvent("error", null, e.getMessage()));
        }
    }

    @MessageExceptionHandler
    public void handleException(Exception e, Principal principal) {
        messagingTemplate.convertAndSendToUser(principal.getName(), "/topic/chat",
                new ChatStreamEvent("error", null, e.getMessage()));
    }
}
