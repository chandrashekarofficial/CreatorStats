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

        LocalDate today = LocalDate.now();

        if (snapshotRepository
                .findByChannelIdAndSnapshotDate(channelId, today)
                .isPresent()) {
            return;
        }

        Map<String, Object> channel =
                youtubeService.getChannel(channelId);

        ChannelSnapshot snapshot = ChannelSnapshot.builder()
                .channelId(channelId)
                .channelName(String.valueOf(channel.get("title")))
                .subscribers(((Number) channel.get("subscribers")).longValue())
                .views(((Number) channel.get("views")).longValue())
                .videos(((Number) channel.get("videos")).longValue())
                .snapshotDate(today)
                .build();

        snapshotRepository.save(snapshot);
    }

    public List<ChannelSnapshot> getSnapshots(String channelId) {
        return snapshotRepository
                .findByChannelIdOrderBySnapshotDateAsc(channelId);
    }

    /*
     * Runs every day at 2:00 AM.
     *
     * NOTE:
     * We cannot automatically discover every YouTube channel ever
     * searched by users without storing those channels first.
     *
     * For now, snapshots are created when a channel is analyzed.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void dailySnapshotJob() {
        // Daily scheduler enabled.
        // Individual channels are saved when analyzed.
    }
}
