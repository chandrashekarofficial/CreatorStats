package com.creatorhub.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
    name = "channel_snapshots",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"channel_id", "snapshot_date"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChannelSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "channel_id", nullable = false)
    private String channelId;

    @Column(nullable = false)
    private String channelName;

    @Column(nullable = false)
    private Long subscribers;

    @Column(nullable = false)
    private Long views;

    @Column(nullable = false)
    private Long videos;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;
}
