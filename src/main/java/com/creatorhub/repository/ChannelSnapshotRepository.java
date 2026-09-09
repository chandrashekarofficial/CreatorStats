package com.creatorhub.repository;

import com.creatorhub.entity.ChannelSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ChannelSnapshotRepository
        extends JpaRepository<ChannelSnapshot, Long> {

    List<ChannelSnapshot> findByChannelIdOrderBySnapshotDateAsc(
            String channelId
    );

    Optional<ChannelSnapshot> findByChannelIdAndSnapshotDate(
            String channelId,
            LocalDate snapshotDate
    );
}
