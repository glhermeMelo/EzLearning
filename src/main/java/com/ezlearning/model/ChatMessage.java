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
import java.util.List;
import java.util.UUID;

@Getter
@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Setter
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Setter
    @Column(name = "user_id")
    private UUID userId;

    @Setter
    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Setter
    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Setter
    @Column(nullable = false)
    private double confidence;

    @Setter
    @Column(columnDefinition = "JSONB")
    private List<String> steps;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ChatMessage() {}

    public ChatMessage(UUID userId, String question, String answer, double confidence, List<String> steps) {
        this.userId = userId;
        this.question = question;
        this.answer = answer;
        this.confidence = confidence;
        this.steps = steps;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
