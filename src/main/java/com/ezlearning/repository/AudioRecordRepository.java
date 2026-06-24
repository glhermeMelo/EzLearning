package com.ezlearning.repository;

import com.ezlearning.model.AudioRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AudioRecordRepository extends JpaRepository<AudioRecord, UUID> {

    Optional<AudioRecord> findByMessageId(UUID messageId);

    List<AudioRecord> findAllByMessageId(UUID messageId);
}
