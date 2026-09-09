package com.creatorhub.controller;

import com.creatorhub.entity.ChannelSnapshot;
import com.creatorhub.service.ChannelSnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/youtube")
public class ChannelHistoryController {

    private final ChannelSnapshotService snapshotService;

    @GetMapping("/history")
    public Map<String, Object> getHistory(
            @RequestParam String channelId,
            @RequestParam(defaultValue = "90") int days,
            @RequestParam(defaultValue = "365") int projectionDays) {

        List<ChannelSnapshot> snapshots =
                snapshotService.getSnapshots(channelId);

        LocalDate cutoff =
                LocalDate.now().minusDays(Math.max(days, 1) - 1L);

        List<ChannelSnapshot> filtered =
                snapshots.stream()
                        .filter(s -> s.getSnapshotDate() != null)
                        .filter(s -> !s.getSnapshotDate().isBefore(cutoff))
                        .toList();

        List<Map<String, Object>> history = new ArrayList<>();

        for (ChannelSnapshot snapshot : filtered) {

            Map<String, Object> point =
                    new LinkedHashMap<>();

            point.put("date", snapshot.getSnapshotDate().toString());
            point.put("subscribers", snapshot.getSubscribers());
            point.put("views", snapshot.getViews());
            point.put("videos", snapshot.getVideos());

            history.add(point);
        }

        Map<String, Object> result =
                new LinkedHashMap<>();

        result.put("channelId", channelId);
        result.put("history", history);
        result.put("projection",
                buildProjection(filtered, projectionDays));
        result.put("projectionAvailable", filtered.size() >= 2);
        result.put(
                "projectionNote",
                "CreatorStats estimate based on stored public historical data."
        );

        return result;
    }

    private List<Map<String, Object>> buildProjection(
            List<ChannelSnapshot> snapshots,
            int projectionDays) {

        List<Map<String, Object>> projection =
                new ArrayList<>();

        if (snapshots.size() < 2) {
            return projection;
        }

        ChannelSnapshot first = snapshots.get(0);
        ChannelSnapshot last =
                snapshots.get(snapshots.size() - 1);

        long elapsedDays =
                Math.max(
                        1,
                        java.time.temporal.ChronoUnit.DAYS.between(
                                first.getSnapshotDate(),
                                last.getSnapshotDate()
                        )
                );

        double subscriberGrowth =
                (last.getSubscribers() - first.getSubscribers())
                        / (double) elapsedDays;

        double viewGrowth =
                (last.getViews() - first.getViews())
                        / (double) elapsedDays;

        double videoGrowth =
                (last.getVideos() - first.getVideos())
                        / (double) elapsedDays;

        int[] checkpoints = {
                30,
                60,
                90,
                180,
                365
        };

        for (int day : checkpoints) {

            if (day > projectionDays) {
                continue;
            }

            Map<String, Object> point =
                    new LinkedHashMap<>();

            point.put(
                    "date",
                    last.getSnapshotDate()
                            .plusDays(day)
                            .toString()
            );

            point.put(
                    "daysFromNow",
                    day
            );

            point.put(
                    "subscribers",
                    Math.max(
                            0,
                            Math.round(
                                    last.getSubscribers()
                                            + subscriberGrowth * day
                            )
                    )
            );

            point.put(
                    "views",
                    Math.max(
                            0,
                            Math.round(
                                    last.getViews()
                                            + viewGrowth * day
                            )
                    )
            );

            point.put(
                    "videos",
                    Math.max(
                            0,
                            Math.round(
                                    last.getVideos()
                                            + videoGrowth * day
                            )
                    )
            );

            projection.add(point);
        }

        return projection;
    }
}