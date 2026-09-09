package com.creatorhub.controller;

import com.creatorhub.entity.FavoriteChannel;
import com.creatorhub.service.FavoriteChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteChannelController {

    private final FavoriteChannelService favoriteService;

    @GetMapping
    public List<FavoriteChannel> list(Authentication authentication) {
        return favoriteService.list(authentication.getName());
    }

    @PostMapping
    public FavoriteChannel add(Authentication authentication, @RequestBody Map<String, Object> data) {
        return favoriteService.add(authentication.getName(), data);
    }

    @DeleteMapping("/{channelId}")
    public ResponseEntity<Void> remove(Authentication authentication, @PathVariable String channelId) {
        favoriteService.remove(authentication.getName(), channelId);
        return ResponseEntity.noContent().build();
    }
}
