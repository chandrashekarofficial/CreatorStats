package com.creatorhub.repository;

import com.creatorhub.entity.ConnectedChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConnectedChannelRepository extends JpaRepository<ConnectedChannel, Long> {
    Optional<ConnectedChannel> findByUserUserId(Long userId);
    Optional<ConnectedChannel> findByYoutubeChannelId(String youtubeChannelId);
    void deleteByUserUserId(Long userId);
}
