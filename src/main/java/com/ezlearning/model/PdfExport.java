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
@Table(name = "pdf_exports")
public class PdfExport {

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
    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Setter
    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Setter
    @Column(name = "content_hash", nullable = false)
    private String contentHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public PdfExport() {}

    public PdfExport(UUID messageId, String fileName, long fileSize, String filePath, String contentHash) {
        this.messageId = messageId;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.filePath = filePath;
        this.contentHash = contentHash;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
