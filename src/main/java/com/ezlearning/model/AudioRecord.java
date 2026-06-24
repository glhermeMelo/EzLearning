package com.ezlearning.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "audio_records")
public class AudioRecord {

    @Setter
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Setter
    @Column(name = "message_id")
    private UUID messageId;

    @Setter
    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Setter
    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Setter
    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Setter
    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Setter
    @Column(name = "duration_seconds", nullable = false)
    private double durationSeconds;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public AudioRecord() {}

    public AudioRecord(UUID messageId, String fileName, String mimeType, long fileSize, String filePath, double durationSeconds) {
        this.messageId = messageId;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.filePath = filePath;
        this.durationSeconds = durationSeconds;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
