package com.creatorhub.controller;

import com.creatorhub.entity.ConnectedChannel;
import com.creatorhub.service.YouTubeConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/youtube")
@RequiredArgsConstructor
public class YouTubeConnectionController {

    private final YouTubeConnectionService connectionService;

    @GetMapping("/connect")
    public ResponseEntity<Map<String, String>> connect(Authentication authentication) {
        String url = connectionService.buildAuthorizationUrl(authentication.getName());
        return ResponseEntity.ok(Map.of("authorizationUrl", url));
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error
    ) {
        if (error != null || code == null || state == null) {
            log.warn("YouTube OAuth callback did not complete. error={}, codePresent={}, statePresent={}",
                    error, code != null, state != null);
            return ResponseEntity.status(302)
                    .header("Location", "/profile.html?youtube=error")
                    .build();
        }

        try {
            connectionService.completeConnection(code, state);
            return ResponseEntity.status(302)
                    .header("Location", "/profile.html?youtube=connected")
                    .build();
        } catch (RuntimeException ex) {
            log.error("YouTube OAuth callback failed: {}", ex.getMessage(), ex);
            return ResponseEntity.status(302)
                    .header("Location", "/profile.html?youtube=error")
                    .build();
        }
    }

    @GetMapping("/connected")
    public ResponseEntity<?> connected(Authentication authentication) {
        ConnectedChannel channel = connectionService.getConnectedChannel(authentication.getName());
        if (channel == null) {
            return ResponseEntity.ok(Map.of("connected", false));
        }

        return ResponseEntity.ok(Map.of(
                "connected", true,
                "channelId", channel.getYoutubeChannelId(),
                "channelName", channel.getChannelName(),
                "handle", channel.getChannelHandle() == null ? "" : channel.getChannelHandle(),
                "thumbnail", channel.getThumbnailUrl() == null ? "" : channel.getThumbnailUrl()
        ));
    }

    @DeleteMapping("/connected")
    public ResponseEntity<Void> disconnect(Authentication authentication) {
        connectionService.disconnect(authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
