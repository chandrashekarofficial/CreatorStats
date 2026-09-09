package com.creatorhub.service;

import com.creatorhub.entity.ChannelSnapshot;
import com.creatorhub.repository.ChannelSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChannelSnapshotService {

    private final ChannelSnapshotRepository snapshotRepository;
    private final PublicYouTubeService youtubeService;

    public void saveSnapshot(String channelId) throws Exception {

        if (channelId == null || channelId.isBlank()) {
            return;
        }

        channelId = channelId.trim();

        LocalDate today = LocalDate.now();

        Map<String, Object> channel =
                youtubeService.getChannel(channelId);

        ChannelSnapshot snapshot =
                snapshotRepository
                        .findByChannelIdAndSnapshotDate(channelId, today)
                        .orElseGet(ChannelSnapshot::new);

        snapshot.setChannelId(channelId);

        snapshot.setChannelName(
                String.valueOf(channel.getOrDefault("title", ""))
        );

        snapshot.setSubscribers(
                getLong(channel.get("subscribers"))
        );

        snapshot.setViews(
                getLong(channel.get("views"))
        );

        snapshot.setVideos(
                getLong(channel.get("videos"))
        );

        snapshot.setSnapshotDate(today);

        snapshotRepository.save(snapshot);
    }

    public void saveSnapshot(String channelId, Map<String, Object> channel) {

        if (channelId == null || channelId.isBlank() || channel == null) {
            return;
        }

        channelId = channelId.trim();

        LocalDate today = LocalDate.now();

        ChannelSnapshot snapshot =
                snapshotRepository
                        .findByChannelIdAndSnapshotDate(channelId, today)
                        .orElseGet(ChannelSnapshot::new);

        snapshot.setChannelId(channelId);

        snapshot.setChannelName(
                String.valueOf(channel.getOrDefault("title", ""))
        );

        snapshot.setSubscribers(
                getLong(channel.get("subscribers"))
        );

        snapshot.setViews(
                getLong(channel.get("views"))
        );

        snapshot.setVideos(
                getLong(channel.get("videos"))
        );

        snapshot.setSnapshotDate(today);

        snapshotRepository.save(snapshot);
    }
    private long getLong(Object value) {

        if (value instanceof Number number) {
            return number.longValue();
        }

        if (value == null) {
            return 0L;
        }

        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    public List<ChannelSnapshot> getSnapshots(String channelId) {

        return snapshotRepository
                .findByChannelIdOrderBySnapshotDateAsc(channelId);
    }

    @Scheduled(
            cron = "0 0 2 * * *",
            zone = "Asia/Kolkata"
    )
    public void dailySnapshotJob() {

        List<String> channelIds =
                snapshotRepository.findTrackedChannelIds();

        for (String channelId : channelIds) {

            try {
                saveSnapshot(channelId);

            } catch (Exception e) {

                System.err.println(
                        "CreatorStats snapshot failed for "
                                + channelId
                                + ": "
                                + e.getMessage()
                );
            }
        }
    }
}