package com.creatorhub.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "connected_channels", uniqueConstraints = {
        @UniqueConstraint(name = "uk_connected_channel_user", columnNames = "user_id"),
        @UniqueConstraint(name = "uk_connected_channel_youtube_id", columnNames = "youtube_channel_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectedChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "connected_channel_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "youtube_channel_id", nullable = false, length = 100)
    private String youtubeChannelId;

    @Column(name = "channel_name", nullable = false, length = 200)
    private String channelName;

    @Column(name = "channel_handle", length = 200)
    private String channelHandle;

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    @Column(name = "connected_at", nullable = false)
    private LocalDateTime connectedAt;

    @PrePersist
    void prePersist() {
        if (connectedAt == null) connectedAt = LocalDateTime.now();
    }
}
