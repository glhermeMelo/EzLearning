package com.ezlearning.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "generated_media")
public class GeneratedMedia {

    @Setter
    @Id
    private UUID id;

    @Setter
    @Column(nullable = false, columnDefinition = "TEXT")
    private String prompt;

    @Setter
    @Column(name = "prompt_hash", nullable = false)
    private String promptHash;

    @Setter
    @Column(name = "stored_path", nullable = false)
    private String storedPath;

    @Setter
    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Setter
    @Column(nullable = false)
    private long size;

    @Setter
    @Column(name = "referenced", nullable = false)
    private boolean referenced;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Setter
    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @Setter
    @Column(name = "user_id")
    private UUID userId;

    public GeneratedMedia() {}

    public GeneratedMedia(UUID id, String prompt, String promptHash, String storedPath,
                          String mimeType, long size, UUID userId) {
        this.id = id;
        this.prompt = prompt;
        this.promptHash = promptHash;
        this.storedPath = storedPath;
        this.mimeType = mimeType;
        this.size = size;
        this.referenced = false;
        this.userId = userId;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.lastAccessedAt = LocalDateTime.now();
    }
}
