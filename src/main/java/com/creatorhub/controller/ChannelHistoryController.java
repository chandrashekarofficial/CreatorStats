package com.creatorhub.controller;

import com.creatorhub.entity.ChannelSnapshot;
import com.creatorhub.service.ChannelSnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
            @RequestParam(defaultValue = "365") int days,
            @RequestParam(defaultValue = "1095") int projectionDays) {

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
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", snapshot.getSnapshotDate().toString());
            point.put("subscribers", snapshot.getSubscribers());
            point.put("views", snapshot.getViews());
            point.put("videos", snapshot.getVideos());
            history.add(point);
        }

        Map<String, Object> result = new LinkedHashMap<>();

        result.put("channelId", channelId);
        result.put("history", history);
        result.put("projection",
                buildProjection(filtered, Math.min(Math.max(projectionDays, 30), 1095)));
        result.put("projectionAvailable", filtered.size() >= 2);
        result.put(
                "projectionNote",
                "CreatorStats forecast based on stored public historical data and statistical trend analysis."
        );

        result.put("growthAnalysis", buildGrowthAnalysis(filtered));

        return result;
    }

    private List<Map<String, Object>> buildProjection(
            List<ChannelSnapshot> snapshots,
            int projectionDays) {

        List<Map<String, Object>> projection = new ArrayList<>();

        if (snapshots.size() < 2) {
            return projection;
        }

        ChannelSnapshot first = snapshots.get(0);
        ChannelSnapshot last = snapshots.get(snapshots.size() - 1);

        LocalDate firstDate = first.getSnapshotDate();
        LocalDate lastDate = last.getSnapshotDate();

        double subscriberSlope =
                regressionSlope(snapshots, firstDate, true);

        double viewSlope =
                regressionSlope(snapshots, firstDate, false);

        double subscriberVariation =
                slopeVariation(snapshots, firstDate, true);

        double viewVariation =
                slopeVariation(snapshots, firstDate, false);

        double subscriberSpread =
                Math.max(Math.abs(subscriberSlope) * 0.35, subscriberVariation * 0.35);

        double viewSpread =
                Math.max(Math.abs(viewSlope) * 0.35, viewVariation * 0.35);

        double conservativeSubscriberSlope =
                subscriberSlope - subscriberSpread;

        double optimisticSubscriberSlope =
                subscriberSlope + subscriberSpread;

        double conservativeViewSlope =
                viewSlope - viewSpread;

        double optimisticViewSlope =
                viewSlope + viewSpread;

        for (int day = 7; day <= projectionDays; day += 7) {

            LocalDate date = lastDate.plusDays(day);

            Map<String, Object> point = new LinkedHashMap<>();

            point.put("date", date.toString());
            point.put("daysFromNow", day);

            point.put("subscribers",
                    forecastValue(last.getSubscribers(),
                            subscriberSlope, day));

            point.put("views",
                    forecastValue(last.getViews(),
                            viewSlope, day));

            point.put("conservativeSubscribers",
                    forecastValue(last.getSubscribers(),
                            conservativeSubscriberSlope, day));

            point.put("expectedSubscribers",
                    forecastValue(last.getSubscribers(),
                            subscriberSlope, day));

            point.put("optimisticSubscribers",
                    forecastValue(last.getSubscribers(),
                            optimisticSubscriberSlope, day));

            point.put("conservativeViews",
                    forecastValue(last.getViews(),
                            conservativeViewSlope, day));

            point.put("expectedViews",
                    forecastValue(last.getViews(),
                            viewSlope, day));

            point.put("optimisticViews",
                    forecastValue(last.getViews(),
                            optimisticViewSlope, day));

            projection.add(point);
        }

        if (projection.isEmpty() ||
                projection.get(projection.size() - 1)
                        .get("daysFromNow") instanceof Integer lastPoint &&
                lastPoint < projectionDays) {

            Map<String, Object> point = new LinkedHashMap<>();

            point.put("date", lastDate.plusDays(projectionDays).toString());
            point.put("daysFromNow", projectionDays);

            point.put("subscribers",
                    forecastValue(last.getSubscribers(), subscriberSlope, projectionDays));
            point.put("views",
                    forecastValue(last.getViews(), viewSlope, projectionDays));

            point.put("conservativeSubscribers",
                    forecastValue(last.getSubscribers(), conservativeSubscriberSlope, projectionDays));
            point.put("expectedSubscribers",
                    forecastValue(last.getSubscribers(), subscriberSlope, projectionDays));
            point.put("optimisticSubscribers",
                    forecastValue(last.getSubscribers(), optimisticSubscriberSlope, projectionDays));

            point.put("conservativeViews",
                    forecastValue(last.getViews(), conservativeViewSlope, projectionDays));
            point.put("expectedViews",
                    forecastValue(last.getViews(), viewSlope, projectionDays));
            point.put("optimisticViews",
                    forecastValue(last.getViews(), optimisticViewSlope, projectionDays));

            projection.add(point);
        }

        return projection;
    }

    private Map<String, Object> buildGrowthAnalysis(
            List<ChannelSnapshot> snapshots) {

        Map<String, Object> analysis = new LinkedHashMap<>();

        if (snapshots.size() < 2) {
            analysis.put("available", false);
            analysis.put("confidence", "Low");
            analysis.put("confidenceScore", 0);
            analysis.put("trend", "Not enough historical data");
            return analysis;
        }

        ChannelSnapshot first = snapshots.get(0);
        ChannelSnapshot last = snapshots.get(snapshots.size() - 1);

        long elapsedDays =
                Math.max(
                        1,
                        ChronoUnit.DAYS.between(
                                first.getSnapshotDate(),
                                last.getSnapshotDate()
                        )
                );

        double subscriberSlope =
                regressionSlope(snapshots, first.getSnapshotDate(), true);

        double viewSlope =
                regressionSlope(snapshots, first.getSnapshotDate(), false);

        double subscriberMonthly = subscriberSlope * 30.4375;
        double viewMonthly = viewSlope * 30.4375;

        double subscriberPercent =
                first.getSubscribers() > 0
                        ? (subscriberSlope * 30.4375 / first.getSubscribers()) * 100.0
                        : 0.0;

        double viewPercent =
                first.getViews() > 0
                        ? (viewSlope * 30.4375 / first.getViews()) * 100.0
                        : 0.0;

        double r2Subscribers =
                regressionR2(snapshots, first.getSnapshotDate(), true);

        double r2Views =
                regressionR2(snapshots, first.getSnapshotDate(), false);

        double r2 = (r2Subscribers + r2Views) / 2.0;

        int confidenceScore =
                Math.min(
                        100,
                        (int) Math.round(
                                Math.min(70, snapshots.size() * 2.0)
                                        + Math.min(30, elapsedDays / 3.0)
                        )
                );

        String confidence;

        if (confidenceScore >= 75 && r2 >= 0.45) {
            confidence = "High";
        } else if (confidenceScore >= 45 && r2 >= 0.20) {
            confidence = "Medium";
        } else {
            confidence = "Low";
        }

        String trend;

        if (subscriberSlope > 0 && viewSlope > 0) {
            trend = "Growing";
        } else if (subscriberSlope < 0 && viewSlope < 0) {
            trend = "Declining";
        } else {
            trend = "Mixed";
        }

        analysis.put("available", true);
        analysis.put("snapshotCount", snapshots.size());
        analysis.put("historicalDays", elapsedDays);

        analysis.put("currentSubscribers", last.getSubscribers());
        analysis.put("currentViews", last.getViews());

        analysis.put("dailySubscriberGrowth",
                round(subscriberSlope));
        analysis.put("dailyViewGrowth",
                round(viewSlope));

        analysis.put("monthlySubscriberGrowth",
                round(subscriberMonthly));
        analysis.put("monthlyViewGrowth",
                round(viewMonthly));

        analysis.put("monthlySubscriberGrowthPercent",
                round(subscriberPercent));
        analysis.put("monthlyViewGrowthPercent",
                round(viewPercent));

        analysis.put("subscriberR2", round(r2Subscribers));
        analysis.put("viewR2", round(r2Views));

        analysis.put("confidenceScore", confidenceScore);
        analysis.put("confidence", confidence);
        analysis.put("trend", trend);

        analysis.put(
                "estimatedSubscribers1Year",
                forecastValue(last.getSubscribers(), subscriberSlope, 365)
        );

        analysis.put(
                "estimatedViews1Year",
                forecastValue(last.getViews(), viewSlope, 365)
        );

        analysis.put(
                "estimatedSubscribers3Years",
                forecastValue(last.getSubscribers(), subscriberSlope, 1095)
        );

        analysis.put(
                "estimatedViews3Years",
                forecastValue(last.getViews(), viewSlope, 1095)
        );

        return analysis;
    }

    private double regressionSlope(
            List<ChannelSnapshot> snapshots,
            LocalDate baseDate,
            boolean subscribers) {

        double sumX = 0;
        double sumY = 0;
        double sumXY = 0;
        double sumXX = 0;

        for (ChannelSnapshot snapshot : snapshots) {

            double x =
                    ChronoUnit.DAYS.between(
                            baseDate,
                            snapshot.getSnapshotDate()
                    );

            double y =
                    subscribers
                            ? snapshot.getSubscribers()
                            : snapshot.getViews();

            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXX += x * x;
        }

        double n = snapshots.size();

        double denominator =
                n * sumXX - sumX * sumX;

        if (Math.abs(denominator) < 0.000001) {
            return 0;
        }

        return (n * sumXY - sumX * sumY) / denominator;
    }

    private double slopeVariation(
            List<ChannelSnapshot> snapshots,
            LocalDate baseDate,
            boolean subscribers) {

        if (snapshots.size() < 3) {
            return 0;
        }

        List<Double> dailyRates = new ArrayList<>();

        for (int i = 1; i < snapshots.size(); i++) {

            ChannelSnapshot previous = snapshots.get(i - 1);
            ChannelSnapshot current = snapshots.get(i);

            long days =
                    Math.max(
                            1,
                            ChronoUnit.DAYS.between(
                                    previous.getSnapshotDate(),
                                    current.getSnapshotDate()
                            )
                    );

            double previousValue =
                    subscribers
                            ? previous.getSubscribers()
                            : previous.getViews();

            double currentValue =
                    subscribers
                            ? current.getSubscribers()
                            : current.getViews();

            dailyRates.add(
                    (currentValue - previousValue) / days
            );
        }

        double mean =
                dailyRates.stream()
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0);

        double variance =
                dailyRates.stream()
                        .mapToDouble(rate ->
                                Math.pow(rate - mean, 2))
                        .average()
                        .orElse(0);

        return Math.sqrt(variance);
    }

    private double regressionR2(
            List<ChannelSnapshot> snapshots,
            LocalDate baseDate,
            boolean subscribers) {

        if (snapshots.size() < 3) {
            return 0;
        }

        double slope =
                regressionSlope(
                        snapshots,
                        baseDate,
                        subscribers
                );

        double sumX = 0;
        double sumY = 0;

        for (ChannelSnapshot snapshot : snapshots) {
            double x =
                    ChronoUnit.DAYS.between(
                            baseDate,
                            snapshot.getSnapshotDate()
                    );

            double y =
                    subscribers
                            ? snapshot.getSubscribers()
                            : snapshot.getViews();

            sumX += x;
            sumY += y;
        }

        double meanY = sumY / snapshots.size();

        double ssTotal = 0;
        double ssResidual = 0;

        double intercept =
                meanY -
                        slope *
                                (sumX / snapshots.size());

        for (ChannelSnapshot snapshot : snapshots) {

            double x =
                    ChronoUnit.DAYS.between(
                            baseDate,
                            snapshot.getSnapshotDate()
                    );

            double actual =
                    subscribers
                            ? snapshot.getSubscribers()
                            : snapshot.getViews();

            double predicted =
                    intercept + slope * x;

            ssTotal += Math.pow(actual - meanY, 2);
            ssResidual += Math.pow(actual - predicted, 2);
        }

        if (ssTotal == 0) {
            return 1;
        }

        return Math.max(
                0,
                Math.min(
                        1,
                        1 - (ssResidual / ssTotal)
                )
        );
    }

    private long forecastValue(
            long current,
            double dailyGrowth,
            int days) {

        double value =
                current + dailyGrowth * days;

        return Math.max(
                0,
                Math.round(value)
        );
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
