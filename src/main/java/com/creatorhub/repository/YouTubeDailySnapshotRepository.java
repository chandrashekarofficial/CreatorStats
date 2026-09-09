package com.creatorhub.repository;

import com.creatorhub.entity.YouTubeDailySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface YouTubeDailySnapshotRepository
        extends JpaRepository<YouTubeDailySnapshot, Long> {

    List<YouTubeDailySnapshot> findByChannelIdOrderBySnapshotDateAsc(
            String channelId
    );

    Optional<YouTubeDailySnapshot> findByChannelIdAndSnapshotDate(
            String channelId,
            LocalDate snapshotDate
    );
}
