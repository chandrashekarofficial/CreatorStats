package com.creatorhub.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
    name = "youtube_daily_snapshots",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"channel_id", "snapshot_date"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YouTubeDailySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "channel_id", nullable = false, length = 100)
    private String channelId;

    @Column(name = "channel_name", length = 255)
    private String channelName;

    @Column(nullable = false)
    private Long subscribers;

    @Column(nullable = false)
    private Long views;

    @Column(nullable = false)
    private Long videos;

    @Column(name = "estimated_earnings")
    private Long estimatedEarnings;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;
}
