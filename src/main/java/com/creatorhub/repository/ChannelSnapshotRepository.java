package com.creatorhub.repository;

import com.creatorhub.entity.ChannelSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ChannelSnapshotRepository
        extends JpaRepository<ChannelSnapshot, Long> {

    Optional<ChannelSnapshot> findByChannelIdAndSnapshotDate(
            String channelId,
            LocalDate snapshotDate
    );

    List<ChannelSnapshot> findByChannelIdAndSnapshotDateGreaterThanEqualOrderBySnapshotDateAsc(
            String channelId,
            LocalDate date
    );

    List<ChannelSnapshot> findByChannelIdOrderBySnapshotDateAsc(
            String channelId
    );

    @Query("select distinct s.channelId from ChannelSnapshot s")
    List<String> findTrackedChannelIds();
}