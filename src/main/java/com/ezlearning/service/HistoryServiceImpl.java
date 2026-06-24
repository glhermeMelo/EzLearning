package com.ezlearning.service;

import com.ezlearning.model.AudioRecord;
import com.ezlearning.model.ChatMessage;
import com.ezlearning.model.PdfExport;
import com.ezlearning.repository.AudioRecordRepository;
import com.ezlearning.repository.ChatMessageRepository;
import com.ezlearning.repository.PdfExportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class HistoryServiceImpl implements HistoryService {

    private static final Logger log = LoggerFactory.getLogger(HistoryServiceImpl.class);

    private final ChatMessageRepository chatMessageRepository;
    private final AudioRecordRepository audioRecordRepository;
    private final PdfExportRepository pdfExportRepository;

    public HistoryServiceImpl(ChatMessageRepository chatMessageRepository,
                              AudioRecordRepository audioRecordRepository,
                              PdfExportRepository pdfExportRepository) {
        this.chatMessageRepository = chatMessageRepository;
        this.audioRecordRepository = audioRecordRepository;
        this.pdfExportRepository = pdfExportRepository;
    }

    @Override
    public ChatMessage saveMessage(ChatMessage message) {
        log.debug("Saving chat message for user: {}", message.getUserId());
        return chatMessageRepository.save(message);
    }

    @Override
    public Optional<ChatMessage> findMessageById(UUID id) {
        return chatMessageRepository.findById(id);
    }

    @Override
    public Page<ChatMessage> getChatHistory(UUID userId, int page, int size) {
        log.debug("Fetching chat history for user: {}, page: {}, size: {}", userId, page, size);
        return chatMessageRepository.findByUserId(userId, PageRequest.of(page, size));
    }

    @Override
    public List<ChatMessage> getChatHistory(UUID userId) {
        log.debug("Fetching all chat history for user: {}", userId);
        return chatMessageRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public AudioRecord saveAudioRecord(AudioRecord record) {
        log.debug("Saving audio record for message: {}", record.getMessageId());
        return audioRecordRepository.save(record);
    }

    @Override
    public Optional<AudioRecord> findAudioByMessageId(UUID messageId) {
        return audioRecordRepository.findByMessageId(messageId);
    }

    @Override
    public List<AudioRecord> findAllAudioByMessageId(UUID messageId) {
        return audioRecordRepository.findAllByMessageId(messageId);
    }

    @Override
    public PdfExport savePdfExport(PdfExport export) {
        log.debug("Saving PDF export for message: {}", export.getMessageId());
        return pdfExportRepository.save(export);
    }

    @Override
    public Optional<PdfExport> findPdfByContentHash(String contentHash) {
        return pdfExportRepository.findByContentHash(contentHash);
    }

    @Override
    public List<PdfExport> findPdfsByMessageId(UUID messageId) {
        return pdfExportRepository.findByMessageId(messageId);
    }
}
