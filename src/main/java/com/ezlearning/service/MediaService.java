package com.ezlearning.service;

import com.ezlearning.model.dto.MediaGenerationRequest;
import com.ezlearning.model.dto.MediaGenerationResponse;

import java.io.IOException;
import java.util.UUID;

public interface MediaService {

    /**
     * Gera uma imagem a partir de um prompt, utilizando cache quando possível.
     *
     * @param request dados do prompt e opções
     * @param userId  ID do usuário solicitante (pode ser null)
     * @return resposta com URL pública da imagem gerada
     */
    MediaGenerationResponse generateMedia(MediaGenerationRequest request, UUID userId);

    /**
     * Carrega os bytes de uma imagem gerada pelo ID.
     *
     * @param id ID da imagem
     * @return bytes da imagem
     * @throws IOException se houver erro de leitura
     */
    byte[] loadMedia(UUID id) throws IOException;

    /**
     * Remove mídias não referenciadas mais antigas que o período configurado.
     */
    void cleanupUnreferencedMedia();
}
