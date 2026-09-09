package com.creatorhub.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class PublicYouTubeService {

    @Value("${youtube.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> getChannel(String query) throws Exception {

        String channelId = resolveChannelId(query);

        String url = UriComponentsBuilder
                .fromHttpUrl("https://www.googleapis.com/youtube/v3/channels")
                .queryParam("part", "snippet,statistics")
                .queryParam("id", channelId)
                .queryParam("key", apiKey)
                .toUriString();

        byte[] responseBytes =
                restTemplate.getForObject(
                        url,
                        byte[].class
                );

        String response =
                new String(
                        responseBytes,
                        StandardCharsets.UTF_8
                );

        JsonNode root = objectMapper.readTree(response);

        if (!root.has("items") || root.get("items").isEmpty()) {
            throw new RuntimeException("YouTube channel not found");
        }

        JsonNode channel = root.get("items").get(0);
        JsonNode snippet = channel.get("snippet");
        JsonNode statistics = channel.get("statistics");

        long subscribers = statistics.has("subscriberCount")
                ? statistics.get("subscriberCount").asLong()
                : 0;

        long views = statistics.has("viewCount")
                ? statistics.get("viewCount").asLong()
                : 0;

        long videos = statistics.has("videoCount")
                ? statistics.get("videoCount").asLong()
                : 0;

        Map<String, Object> result = new LinkedHashMap<>();

        result.put("channelId", channelId);
        result.put("title", snippet.path("title").asText(""));
        result.put("description", snippet.path("description").asText(""));
        result.put("thumbnail",
                snippet.path("thumbnails")
                        .path("high")
                        .path("url")
                        .asText("")
        );

        result.put("publishedAt",
                snippet.path("publishedAt").asText("")
        );

        result.put("country",
                snippet.path("country").asText("")
        );

        result.put("subscribers", subscribers);
        result.put("views", views);
        result.put("videos", videos);

        result.put(
                "averageViews",
                videos > 0 ? views / videos : 0
        );

        result.put(
                "grade",
                calculateGrade(subscribers, views, videos)
        );

        result.put(
                "estimatedMonthlyEarnings",
                estimateMonthlyEarnings(views)
        );

        result.put(
                "estimatedYearlyEarnings",
                estimateYearlyEarnings(views)
        );

        result.put(
                "youtubeUrl",
                "https://www.youtube.com/channel/" + channelId
        );

        result.put(
                "recentVideos",
                getVideos(channelId).get("videos")
        );

        return result;
    }

    private String resolveChannelId(String query) throws Exception {

        query = query.trim();

        // Direct channel ID
        if (query.matches("UC[a-zA-Z0-9_-]{20,}")) {
            return query;
        }

        // YouTube URL containing channel ID
        if (query.contains("/channel/")) {

            String id = query.substring(
                    query.indexOf("/channel/") + 9
            );

            if (id.contains("?")) {
                id = id.substring(0, id.indexOf("?"));
            }

            if (id.contains("/")) {
                id = id.substring(0, id.indexOf("/"));
            }

            return id;
        }

        // YouTube handle
        if (query.startsWith("@")) {
            query = query.substring(1);
        }

        // Search by handle/name
        String searchUrl =
                "https://www.googleapis.com/youtube/v3/search"
                + "?part=snippet"
                + "&type=channel"
                + "&maxResults=1"
                + "&q="
                + URLEncoder.encode(
                        query,
                        StandardCharsets.UTF_8
                )
                + "&key="
                + apiKey;

        String response =
                restTemplate.getForObject(
                        searchUrl,
                        String.class
                );

        JsonNode root =
                objectMapper.readTree(response);

        if (!root.has("items") ||
                root.get("items").isEmpty()) {

            throw new RuntimeException(
                    "YouTube channel not found"
            );
        }

        return root.get("items")
                .get(0)
                .path("snippet")
                .path("channelId")
                .asText();
    }

    public Map<String, Object> getVideos(String channelId)
        throws Exception {

    String searchUrl =
            UriComponentsBuilder
                    .fromHttpUrl(
                            "https://www.googleapis.com/youtube/v3/search"
                    )
                    .queryParam("part", "snippet")
                    .queryParam("channelId", channelId)
                    .queryParam("order", "date")
                    .queryParam("type", "video")
                    .queryParam("maxResults", 20)
                    .queryParam("key", apiKey)
                    .toUriString();

    byte[] responseBytes =
            restTemplate.getForObject(
                    searchUrl,
                    byte[].class
            );

    String response =
            new String(
                    responseBytes,
                    StandardCharsets.UTF_8
            );

    JsonNode root =
            objectMapper.readTree(response);

    List<Map<String, Object>> videos =
            new ArrayList<>();

    for (JsonNode item : root.path("items")) {

        JsonNode snippet = item.path("snippet");

        String videoId =
                item.path("id")
                        .path("videoId")
                        .asText("");

        if (videoId.isBlank()) {
            continue;
        }

        Map<String, Object> video =
                new LinkedHashMap<>();

        video.put(
                "id",
                videoId
        );

        video.put(
                "title",
                snippet.path("title").asText("")
        );

        video.put(
                "thumbnail",
                snippet.path("thumbnails")
                        .path("medium")
                        .path("url")
                        .asText("")
        );

        video.put(
                "publishedAt",
                snippet.path("publishedAt").asText("")
        );

        /*
         * Get duration from YouTube videos endpoint.
         * ISO 8601 duration is returned, for example:
         * PT45S = 45 seconds
         * PT1M30S = 90 seconds
         */
        String detailsUrl =
                UriComponentsBuilder
                        .fromHttpUrl(
                                "https://www.googleapis.com/youtube/v3/videos"
                        )
                        .queryParam("part", "contentDetails,statistics")
                        .queryParam("id", videoId)
                        .queryParam("key", apiKey)
                        .toUriString();

        try {

            String detailsResponse =
                    restTemplate.getForObject(
                            detailsUrl,
                            String.class
                    );

            JsonNode detailsRoot =
                    objectMapper.readTree(detailsResponse);

            JsonNode detailItem =
                    detailsRoot.path("items").path(0);

            String duration =
                    detailItem.path("contentDetails")
                            .path("duration")
                            .asText("");

            video.put(
                    "duration",
                    duration
            );

            long durationSeconds =
                    parseYouTubeDuration(duration);

            video.put(
                    "durationSeconds",
                    durationSeconds
            );

            /*
             * YouTube Shorts are normally short-form vertical videos.
             * Duration alone cannot perfectly identify every Short,
             * but <= 60 seconds is a useful first classification.
             */
            video.put(
                    "isShort",
                    durationSeconds > 0 && durationSeconds <= 60
            );

            JsonNode statistics =
                    detailItem.path("statistics");

            video.put(
                    "views",
                    statistics.path("viewCount").asLong(0)
            );

            video.put(
                    "likes",
                    statistics.path("likeCount").asLong(0)
            );

        } catch (Exception ignored) {

            video.put("duration", "");
            video.put("durationSeconds", 0);
            video.put("isShort", false);
            video.put("views", 0);
            video.put("likes", 0);
        }

        videos.add(video);
    }

    Map<String, Object> result =
            new LinkedHashMap<>();

    result.put(
            "videos",
            videos
    );

    return result;
}

private long parseYouTubeDuration(String duration) {

    if (duration == null || duration.isBlank()) {
        return 0;
    }

    try {

        java.time.Duration parsed =
                java.time.Duration.parse(duration);

        return parsed.getSeconds();

    } catch (Exception e) {

        return 0;
    }
}

    private String calculateGrade(
            long subscribers,
            long views,
            long videos) {

        if (subscribers >= 1_000_000 &&
                views >= 100_000_000) {
            return "A+";
        }

        if (subscribers >= 500_000 &&
                views >= 50_000_000) {
            return "A";
        }

        if (subscribers >= 100_000 &&
                views >= 10_000_000) {
            return "B+";
        }

        if (subscribers >= 50_000 &&
                views >= 5_000_000) {
            return "B";
        }

        if (subscribers >= 10_000 &&
                views >= 1_000_000) {
            return "C+";
        }

        if (subscribers >= 1_000 &&
                views >= 100_000) {
            return "C";
        }

        return "D";
    }

    private long estimateMonthlyEarnings(long totalViews) {

        // Rough public-data estimate only.
        // Actual YouTube revenue varies heavily.

        return Math.round(
                (totalViews / 1000.0) * 1.5
        );
    }

    private long estimateYearlyEarnings(long totalViews) {

        return estimateMonthlyEarnings(totalViews) * 12;
    }
    public Map<String, Object> getChannelWithoutSavingSnapshot(String query) throws Exception {
        return getChannel(query);
    }
}
