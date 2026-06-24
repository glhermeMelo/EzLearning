package com.ezlearning.service;

import com.ezlearning.model.dto.ReasoningRequest;
import com.ezlearning.model.dto.ReasoningResponse;

public interface ReasoningService {

    ReasoningResponse reason(ReasoningRequest request);

    ReasoningResponse askQuestion(String question, String context);
}
