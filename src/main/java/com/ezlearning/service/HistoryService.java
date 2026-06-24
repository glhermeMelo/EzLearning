package com.ezlearning.service;

import com.ezlearning.model.AudioRecord;
import com.ezlearning.model.ChatMessage;
import com.ezlearning.model.PdfExport;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HistoryService {

    ChatMessage saveMessage(ChatMessage message);

    Optional<ChatMessage> findMessageById(UUID id);

    Page<ChatMessage> getChatHistory(UUID userId, int page, int size);

    List<ChatMessage> getChatHistory(UUID userId);

    AudioRecord saveAudioRecord(AudioRecord record);

    Optional<AudioRecord> findAudioByMessageId(UUID messageId);

    List<AudioRecord> findAllAudioByMessageId(UUID messageId);

    PdfExport savePdfExport(PdfExport export);

    Optional<PdfExport> findPdfByContentHash(String contentHash);

    List<PdfExport> findPdfsByMessageId(UUID messageId);
}
