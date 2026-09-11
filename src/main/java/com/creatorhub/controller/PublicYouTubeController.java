package com.creatorhub.controller;

import org.springframework.http.ResponseEntity;
import com.creatorhub.service.ChannelSnapshotService;
import com.creatorhub.service.PublicYouTubeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/public/youtube")
@RequiredArgsConstructor
public class PublicYouTubeController {

    private final PublicYouTubeService service;
    private final ChannelSnapshotService snapshotService;

    @GetMapping("/channel")
    public Map<String, Object> getChannel(
            @RequestParam String query) throws Exception {

        Map<String, Object> channel = service.getChannel(query);

        String channelId = String.valueOf(
                channel.getOrDefault("channelId", "")
        );

        snapshotService.saveSnapshot(channelId, channel);

        return channel;
    }

    @GetMapping("/videos")
    public Map<String, Object> getVideos(
            @RequestParam String channelId) throws Exception {

        return service.getVideos(channelId);
    }
  @GetMapping("/popular")
  public ResponseEntity<?> popular() throws Exception {
    return ResponseEntity.ok(service.getPopularChannels());
  }
}
