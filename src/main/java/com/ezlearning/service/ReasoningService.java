package com.ezlearning.service;

import com.ezlearning.model.dto.ReasoningRequest;
import com.ezlearning.model.dto.ReasoningResponse;

public interface ReasoningService {

    /**
     * Envia uma pergunta para a API de raciocínio e retorna a resposta estruturada.
     *
     * @param request dados da pergunta e contexto opcional
     * @return resposta com texto formatado, passos e confiança
     * @throws IllegalArgumentException se a pergunta for inválida ou a API falhar
     */
    ReasoningResponse reason(ReasoningRequest request);
}
