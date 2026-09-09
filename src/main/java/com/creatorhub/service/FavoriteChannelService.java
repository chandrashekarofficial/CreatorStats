package com.creatorhub.service;

import com.creatorhub.entity.FavoriteChannel;
import com.creatorhub.entity.User;
import com.creatorhub.repository.FavoriteChannelRepository;
import com.creatorhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FavoriteChannelService {

    private static final int MAX_FAVORITES = 5;

    private final FavoriteChannelRepository favoriteRepository;
    private final UserRepository userRepository;

    public List<FavoriteChannel> list(String principalName) {
        Long userId = userId(principalName);
        return favoriteRepository.findTop5ByUserUserIdOrderByCreatedAtAsc(userId);
    }

    public FavoriteChannel add(String principalName, Map<String, Object> data) {
        Long userId = userId(principalName);
        String channelId = text(data.get("channelId"));
        if (channelId == null || channelId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Channel ID is required.");
        }

        if (favoriteRepository.findByUserUserIdAndChannelId(userId, channelId).isPresent()) {
            return favoriteRepository.findByUserUserIdAndChannelId(userId, channelId).orElseThrow();
        }

        if (favoriteRepository.countByUserUserId(userId) >= MAX_FAVORITES) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You can save up to 5 favorite channels.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));

        FavoriteChannel favorite = FavoriteChannel.builder()
                .user(user)
                .channelId(channelId)
                .title(textOrDefault(data.get("title"), "YouTube Channel"))
                .handle(text(data.get("handle")))
                .thumbnail(text(data.get("thumbnail")))
                .youtubeUrl(text(data.get("youtubeUrl")))
                .build();

        return favoriteRepository.save(favorite);
    }

    @Transactional
    public void remove(String principalName, String channelId) {
        favoriteRepository.deleteByUserUserIdAndChannelId(userId(principalName), channelId);
    }

    private Long userId(String principalName) {
        try {
            return Long.valueOf(principalName);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authenticated user.");
        }
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private String textOrDefault(Object value, String fallback) {
        String result = text(value);
        return result == null || result.isBlank() ? fallback : result;
    }
}
