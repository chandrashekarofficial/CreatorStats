package com.creatorhub.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "favorite_channels", uniqueConstraints = @UniqueConstraint(name = "uk_favorite_user_channel", columnNames = {"user_id", "channel_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FavoriteChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "favorite_id")
    private Long favoriteId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "channel_id", nullable = false, length = 100)
    private String channelId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 200)
    private String handle;

    @Column(length = 500)
    private String thumbnail;

    @Column(name = "youtube_url", length = 500)
    private String youtubeUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
