package com.creatorhub.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(
    name = "channel_snapshots",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"channel_id", "snapshot_date"}
    )
)
public class ChannelSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "channel_id", nullable = false, length = 100)
    private String channelId;

    @Column(name = "channel_name", length = 255)
    private String channelName;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(nullable = false)
    private long subscribers;

    @Column(nullable = false)
    private long views;

    @Column(nullable = false)
    private long videos;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final ChannelSnapshot snapshot = new ChannelSnapshot();

        public Builder channelId(String channelId) {
            snapshot.setChannelId(channelId);
            return this;
        }

        public Builder channelName(String channelName) {
            snapshot.setChannelName(channelName);
            return this;
        }

        public Builder snapshotDate(LocalDate snapshotDate) {
            snapshot.setSnapshotDate(snapshotDate);
            return this;
        }

        public Builder subscribers(long subscribers) {
            snapshot.setSubscribers(subscribers);
            return this;
        }

        public Builder views(long views) {
            snapshot.setViews(views);
            return this;
        }

        public Builder videos(long videos) {
            snapshot.setVideos(videos);
            return this;
        }

        public ChannelSnapshot build() {
            return snapshot;
        }
    }

    public Long getId() {
        return id;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public LocalDate getSnapshotDate() {
        return snapshotDate;
    }

    public void setSnapshotDate(LocalDate snapshotDate) {
        this.snapshotDate = snapshotDate;
    }

    public long getSubscribers() {
        return subscribers;
    }

    public void setSubscribers(long subscribers) {
        this.subscribers = subscribers;
    }

    public long getViews() {
        return views;
    }

    public void setViews(long views) {
        this.views = views;
    }

    public long getVideos() {
        return videos;
    }

    public void setVideos(long videos) {
        this.videos = videos;
    }
}