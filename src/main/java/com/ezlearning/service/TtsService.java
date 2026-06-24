package com.ezlearning.service;

import com.ezlearning.model.dto.TtsResponse;

import java.io.OutputStream;

public interface TtsService {

    TtsResponse synthesize(String text, String voice);

    void synthesizeAndStream(String text, String voice, OutputStream out);

    byte[] loadAudio(String id);
}
