package com.ezlearning.service;

import com.ezlearning.integration.ReasoningApiClient;
import com.ezlearning.model.dto.ReasoningApiResponse;
import com.ezlearning.model.dto.ReasoningRequest;
import com.ezlearning.model.dto.ReasoningResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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

        ReasoningApiResponse apiResponse = reasoningApiClient.sendReasoningRequest(request);

        log.debug("Reasoning API responded - confidence: {}, steps: {}",
                apiResponse.confidence(),
                apiResponse.steps() != null ? apiResponse.steps().size() : 0);

        return mapToResponse(apiResponse);
    }

    private ReasoningResponse mapToResponse(ReasoningApiResponse apiResponse) {
        String answer = apiResponse.answer() != null ? apiResponse.answer() : "";
        List<String> steps = apiResponse.steps() != null ? apiResponse.steps() : List.of();
        double confidence = apiResponse.confidence() != null ? apiResponse.confidence() : 0.0;

        return new ReasoningResponse(answer, steps, confidence);
    }
}
