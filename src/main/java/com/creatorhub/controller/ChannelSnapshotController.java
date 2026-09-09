package com.creatorhub.controller;

import com.creatorhub.entity.ChannelSnapshot;
import com.creatorhub.service.ChannelSnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/youtube")
@RequiredArgsConstructor
public class ChannelSnapshotController {

    private final ChannelSnapshotService snapshotService;

    @GetMapping("/snapshots")
    public List<ChannelSnapshot> getSnapshots(
            @RequestParam String channelId) {

        return snapshotService.getSnapshots(channelId);
    }
}
