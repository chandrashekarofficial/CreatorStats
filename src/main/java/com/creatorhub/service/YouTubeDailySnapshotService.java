package com.creatorhub.service;

import com.creatorhub.entity.YouTubeDailySnapshot;
import com.creatorhub.repository.YouTubeDailySnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class YouTubeDailySnapshotService {

    private final YouTubeDailySnapshotRepository repository;
    private final PublicYouTubeService youtubeService;

    public void saveSnapshot(String channelId) throws Exception {

        LocalDate today = LocalDate.now();

        if (repository.findByChannelIdAndSnapshotDate(channelId, today).isPresent()) {
            return;
        }

        var channel = youtubeService.getChannelWithoutSavingSnapshot(channelId);

        YouTubeDailySnapshot snapshot = YouTubeDailySnapshot.builder()
                .channelId(channelId)
                .channelName((String) channel.get("title"))
                .subscribers(((Number) channel.get("subscribers")).longValue())
                .views(((Number) channel.get("views")).longValue())
                .videos(((Number) channel.get("videos")).longValue())
                .estimatedEarnings(
                        ((Number) channel.get("estimatedMonthlyEarnings")).longValue()
                )
                .snapshotDate(today)
                .build();

        repository.save(snapshot);
    }

    public List<YouTubeDailySnapshot> getSnapshots(String channelId) {
        return repository.findByChannelIdOrderBySnapshotDateAsc(channelId);
    }
}
