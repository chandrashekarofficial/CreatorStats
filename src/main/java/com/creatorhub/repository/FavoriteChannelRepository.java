package com.creatorhub.repository;

import com.creatorhub.entity.FavoriteChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteChannelRepository extends JpaRepository<FavoriteChannel, Long> {
    List<FavoriteChannel> findTop5ByUserUserIdOrderByCreatedAtAsc(Long userId);
    long countByUserUserId(Long userId);
    Optional<FavoriteChannel> findByUserUserIdAndChannelId(Long userId, String channelId);
    void deleteByUserUserIdAndChannelId(Long userId, String channelId);
}
