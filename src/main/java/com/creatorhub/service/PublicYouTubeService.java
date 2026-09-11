package com.creatorhub.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
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

    private static final List<String> POPULAR_HANDLES = List.of(
        "@MrBeast",
        "@TSeries",
        "@CoComelon",
        "@SETIndia",
        "@VladandNiki",
        "@StokesTwins",
        "@KidsDianaShow",
        "@LikeNastyaOfficial",
        "@ZeeMusicCompany",
        "@AlejoIgoa"
    );

    private volatile long popularCacheAt = 0L;
    private volatile List<Map<String,Object>> popularCache = Collections.emptyList();

    public List<Map<String,Object>> getPopularChannels() throws Exception {
        long now = System.currentTimeMillis();

        if (!popularCache.isEmpty()
                && now - popularCacheAt < 6 * 60 * 60 * 1000L) {
            return popularCache;
        }

        List<Map<String,Object>> channels = new ArrayList<>();

        for (String handle : POPULAR_HANDLES) {
            try {
                String cleanHandle = handle.startsWith("@")
                        ? handle.substring(1)
                        : handle;

                String url = UriComponentsBuilder
                        .fromHttpUrl("https://www.googleapis.com/youtube/v3/channels")
                        .queryParam("part", "snippet,statistics")
                        .queryParam("forHandle", cleanHandle)
                        .queryParam("key", apiKey)
                        .toUriString();

                byte[] responseBytes = restTemplate.getForObject(
                        url,
                        byte[].class
                );

                JsonNode root = objectMapper.readTree(
                        new String(responseBytes, StandardCharsets.UTF_8)
                );

                JsonNode items = root.path("items");

                if (!items.isArray() || items.isEmpty()) {
                    continue;
                }

                JsonNode channel = items.get(0);
                JsonNode snippet = channel.path("snippet");
                JsonNode statistics = channel.path("statistics");

                Map<String,Object> result = new LinkedHashMap<>();

                result.put("channelId", channel.path("id").asText(""));
                result.put("title", snippet.path("title").asText(""));
                result.put("handle", snippet.path("customUrl").asText(handle));
                result.put("thumbnail", snippet.path("thumbnails")
                        .path("high").path("url").asText(""));
                result.put("subscribers",
                        statistics.path("subscriberCount").asLong(0));
                result.put("views",
                        statistics.path("viewCount").asLong(0));
                result.put("videos",
                        statistics.path("videoCount").asLong(0));
                result.put("youtubeUrl",
                        "https://www.youtube.com/channel/"
                                + channel.path("id").asText(""));

                channels.add(result);

            } catch (HttpClientErrorException.TooManyRequests e) {
                break;
            } catch (Exception ignored) {
            }
        }

        channels.sort((a, b) -> Long.compare(
            ((Number) b.getOrDefault("subscribers", 0L)).longValue(),
            ((Number) a.getOrDefault("subscribers", 0L)).longValue()
    ));

        if (!channels.isEmpty()) {
            popularCache = Collections.unmodifiableList(
                    new ArrayList<>(channels)
            );
            popularCacheAt = now;
        }

        return popularCache;
    }
    public Map<String, Object> getChannel(String query) throws Exception {
        String channelId = resolveChannelId(query);
        String url = UriComponentsBuilder.fromHttpUrl("https://www.googleapis.com/youtube/v3/channels")
                .queryParam("part", "snippet,statistics,brandingSettings")
                .queryParam("id", channelId).queryParam("key", apiKey).toUriString();
        byte[] responseBytes;
        try { responseBytes = restTemplate.getForObject(url, byte[].class); }
        catch (HttpClientErrorException.TooManyRequests e) {
            throw quotaExceeded("YouTube channel data is temporarily unavailable because the API quota has been exhausted.");
        }
        JsonNode root = objectMapper.readTree(new String(responseBytes, StandardCharsets.UTF_8));
        if (!root.has("items") || root.get("items").isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "YouTube channel not found.");
        JsonNode channel = root.get("items").get(0), snippet = channel.get("snippet"), statistics = channel.get("statistics");
        long subscribers = statistics.has("subscriberCount") ? statistics.get("subscriberCount").asLong() : 0;
        long views = statistics.has("viewCount") ? statistics.get("viewCount").asLong() : 0;
        long videos = statistics.has("videoCount") ? statistics.get("videoCount").asLong() : 0;
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("channelId", channelId); result.put("title", snippet.path("title").asText(""));
        result.put("description", snippet.path("description").asText("")); result.put("thumbnail", snippet.path("thumbnails").path("high").path("url").asText(""));
        result.put("publishedAt", snippet.path("publishedAt").asText("")); result.put("country", snippet.path("country").asText(""));
        result.put("handle", snippet.path("customUrl").asText("")); result.put("banner", channel.path("brandingSettings").path("image").path("bannerExternalUrl").asText(""));
        result.put("subscribers", subscribers); result.put("views", views); result.put("videos", videos);
        result.put("averageViews", videos > 0 ? views / videos : 0); result.put("grade", calculateGrade(subscribers, views, videos));
        result.put("estimatedMonthlyEarnings", estimateMonthlyEarnings(views)); result.put("estimatedYearlyEarnings", estimateYearlyEarnings(views));
        result.put("youtubeUrl", "https://www.youtube.com/channel/" + channelId);
        Map<String,Object> videoResult = getVideos(channelId);
        result.put("recentVideos", videoResult.get("videos"));
        if (Boolean.TRUE.equals(videoResult.get("unavailable"))) {
            result.put("recentVideosUnavailable", true);
            result.put("recentVideosNote", videoResult.get("note"));
        }
        return result;
    }

    private String resolveChannelId(String query) throws Exception {
        query = query.trim();
        if (query.matches("UC[a-zA-Z0-9_-]{20,}")) return query;
        if (query.contains("/channel/")) {
            String id = query.substring(query.indexOf("/channel/") + 9);
            if (id.contains("?")) id = id.substring(0, id.indexOf("?"));
            if (id.contains("/")) id = id.substring(0, id.indexOf("/"));
            return id;
        }
        boolean explicitHandle = query.startsWith("@"); if (explicitHandle) query = query.substring(1); String handleUrl = UriComponentsBuilder.fromHttpUrl("https://www.googleapis.com/youtube/v3/channels").queryParam("part","id").queryParam("forHandle",query).queryParam("key",apiKey).toUriString(); try { JsonNode handleRoot = objectMapper.readTree(restTemplate.getForObject(handleUrl,String.class)); if (handleRoot.has("items") && !handleRoot.get("items").isEmpty()) { String channelId = handleRoot.get("items").get(0).path("id").asText(""); if (!channelId.isBlank()) return channelId; } } catch (HttpClientErrorException.TooManyRequests e) { throw quotaExceeded("YouTube channel lookup is temporarily unavailable. Please try again later."); } if (explicitHandle) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"YouTube channel not found."); String searchUrl = "https://www.googleapis.com/youtube/v3/search?part=snippet&type=channel&maxResults=1&q=" + URLEncoder.encode(query,StandardCharsets.UTF_8) + "&key=" + apiKey; String response; try { response=restTemplate.getForObject(searchUrl,String.class); } catch (HttpClientErrorException.TooManyRequests e) { throw quotaExceeded("YouTube channel search is temporarily unavailable. Try the channel @handle or channel URL instead."); } JsonNode root=objectMapper.readTree(response); if (!root.has("items") || root.get("items").isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"YouTube channel not found."); String channelId=root.get("items").get(0).path("snippet").path("channelId").asText(); if (channelId.isBlank()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"YouTube channel not found."); return channelId;
    }

    public Map<String,Object> getVideos(String channelId) throws Exception {
        String searchUrl = UriComponentsBuilder.fromHttpUrl("https://www.googleapis.com/youtube/v3/search")
                .queryParam("part", "snippet").queryParam("channelId", channelId).queryParam("order", "date")
                .queryParam("type", "video").queryParam("maxResults", 50).queryParam("key", apiKey).toUriString();
        byte[] responseBytes;
        try { responseBytes = restTemplate.getForObject(searchUrl, byte[].class); }
        catch (HttpClientErrorException.TooManyRequests e) {
            Map<String,Object> result = new LinkedHashMap<>(); result.put("videos", Collections.emptyList()); result.put("unavailable", true);
            result.put("note", "Recent videos are temporarily unavailable because the YouTube API quota has been exhausted."); return result;
        }
        JsonNode root = objectMapper.readTree(new String(responseBytes, StandardCharsets.UTF_8));
        List<Map<String,Object>> videos = new ArrayList<>();
        for (JsonNode item : root.path("items")) {
            JsonNode snippet = item.path("snippet"); String videoId = item.path("id").path("videoId").asText(""); if (videoId.isBlank()) continue;
            Map<String,Object> video = new LinkedHashMap<>(); video.put("id", videoId); video.put("title", snippet.path("title").asText(""));
            video.put("thumbnail", snippet.path("thumbnails").path("medium").path("url").asText("")); video.put("publishedAt", snippet.path("publishedAt").asText(""));
            String detailsUrl = UriComponentsBuilder.fromHttpUrl("https://www.googleapis.com/youtube/v3/videos")
                    .queryParam("part", "contentDetails,statistics").queryParam("id", videoId).queryParam("key", apiKey).toUriString();
            try {
                JsonNode detailItem = objectMapper.readTree(restTemplate.getForObject(detailsUrl, String.class)).path("items").path(0);
                String duration = detailItem.path("contentDetails").path("duration").asText(""); long durationSeconds = parseYouTubeDuration(duration);
                JsonNode statistics = detailItem.path("statistics"); video.put("duration", duration); video.put("durationSeconds", durationSeconds);
                video.put("isShort", durationSeconds > 0 && durationSeconds <= 60); video.put("views", statistics.path("viewCount").asLong(0));
                video.put("likes", statistics.path("likeCount").asLong(0)); video.put("comments", statistics.path("commentCount").asLong(0));
            } catch (Exception ignored) {
                video.put("duration", ""); video.put("durationSeconds", 0); video.put("isShort", false); video.put("views", 0); video.put("likes", 0); video.put("comments", 0);
            }
            videos.add(video);
        }
        Map<String,Object> result = new LinkedHashMap<>(); result.put("videos", videos); return result;
    }

    private ResponseStatusException quotaExceeded(String message) { return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, message); }
    private long parseYouTubeDuration(String duration) { if (duration == null || duration.isBlank()) return 0; try { return java.time.Duration.parse(duration).getSeconds(); } catch (Exception e) { return 0; } }
    private String calculateGrade(long subscribers,long views,long videos) {
        if (subscribers >= 1_000_000 && views >= 100_000_000) return "A+"; if (subscribers >= 500_000 && views >= 50_000_000) return "A";
        if (subscribers >= 100_000 && views >= 10_000_000) return "B+"; if (subscribers >= 50_000 && views >= 5_000_000) return "B";
        if (subscribers >= 10_000 && views >= 1_000_000) return "C+"; if (subscribers >= 1_000 && views >= 100_000) return "C"; return "D";
    }
    private long estimateMonthlyEarnings(long totalViews) { return Math.round((totalViews / 1000.0) * 1.5); }
    private long estimateYearlyEarnings(long totalViews) { return estimateMonthlyEarnings(totalViews) * 12; }
}

