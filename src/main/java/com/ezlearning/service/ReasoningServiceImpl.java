package com.ezlearning.service;

import com.ezlearning.integration.ReasoningApiClient;
import com.ezlearning.model.dto.ReasoningRequest;
import com.ezlearning.model.dto.ReasoningResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReasoningServiceImpl implements ReasoningService {

    private static final Logger log = LoggerFactory.getLogger(ReasoningServiceImpl.class);

    private final ReasoningApiClient reasoningApiClient;

    public ReasoningServiceImpl(ReasoningApiClient reasoningApiClient) {
        this.reasoningApiClient = reasoningApiClient;
    }

    @Override
    public ReasoningResponse reason(ReasoningRequest request) {
        if (request.question() == null || request.question().isBlank()) {
            throw new IllegalArgumentException("A pergunta não pode estar vazia");
        }

        log.debug("Processing reasoning request - question length: {}, has context: {}",
                request.question().length(),
                request.context() != null && !request.context().isBlank());

        ReasoningResponse raw = reasoningApiClient.ask(request);

        return parseMarkdown(raw.answer());
    }

    @Override
    public ReasoningResponse askQuestion(String question, String context) {
        return reason(new ReasoningRequest(question, context));
    }

    private ReasoningResponse parseMarkdown(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return new ReasoningResponse("", List.of(), 0.0);
        }

        String[] lines = markdown.split("\n");
        List<String> steps = new ArrayList<>();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.matches("^\\d+\\.\\s+.*") || trimmed.startsWith("**")) {
                steps.add(trimmed);
            }
        }

        double confidence = calculateConfidence(markdown, steps);

        return new ReasoningResponse(markdown, steps, confidence);
    }

    private double calculateConfidence(String answer, List<String> steps) {
        double score = 0.5;
        score += Math.min(0.25, steps.size() * 0.05);
        score += Math.min(0.2, answer.length() / 5000.0 * 0.2);
        return Math.min(0.95, score);
    }
}
