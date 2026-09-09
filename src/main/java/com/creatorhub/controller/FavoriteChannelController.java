package com.creatorhub.controller;

import com.creatorhub.entity.FavoriteChannel;
import com.creatorhub.service.FavoriteChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteChannelController {

    private final FavoriteChannelService favoriteService;

    @GetMapping
    public List<Map<String, Object>> list(Authentication authentication) {
        return favoriteService.list(authentication.getName()).stream().map(this::toResponse).toList();
    }

    @PostMapping
    public Map<String, Object> add(Authentication authentication, @RequestBody Map<String, Object> data) {
        return toResponse(favoriteService.add(authentication.getName(), data));
    }

    @DeleteMapping("/{channelId}")
    public ResponseEntity<Void> remove(Authentication authentication, @PathVariable String channelId) {
        favoriteService.remove(authentication.getName(), channelId);
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> toResponse(FavoriteChannel favorite) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("favoriteId", favorite.getFavoriteId());
        item.put("channelId", favorite.getChannelId());
        item.put("title", favorite.getTitle());
        item.put("handle", favorite.getHandle());
        item.put("thumbnail", favorite.getThumbnail());
        item.put("youtubeUrl", favorite.getYoutubeUrl());
        item.put("createdAt", favorite.getCreatedAt());
        return item;
    }
}
