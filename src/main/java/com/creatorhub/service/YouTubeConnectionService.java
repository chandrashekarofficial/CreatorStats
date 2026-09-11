package com.creatorhub.service;

import com.creatorhub.entity.ConnectedChannel;
import com.creatorhub.entity.User;
import com.creatorhub.repository.ConnectedChannelRepository;
import com.creatorhub.repository.UserRepository;
import com.creatorhub.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class YouTubeConnectionService {

    private static final String AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String CHANNEL_ENDPOINT = "https://www.googleapis.com/youtube/v3/channels";
    private static final String SCOPE = "https://www.googleapis.com/auth/youtube.readonly";

    private final RestClient.Builder restClientBuilder;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ConnectedChannelRepository connectedChannelRepository;

    @Value("${youtube.oauth.client-id:}")
    private String clientId;

    @Value("${youtube.oauth.client-secret:}")
    private String clientSecret;

    @Value("${youtube.oauth.redirect-uri:http://localhost:8080/api/youtube/callback}")
    private String redirectUri;

    public String buildAuthorizationUrl(String email) {
        requireConfigured();

        String state = jwtService.generateOAuthState(email);

        return UriComponentsBuilder.fromUriString(AUTH_ENDPOINT)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", SCOPE)
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .queryParam("state", state)
                .build()
                .encode()
                .toUriString();
    }

    @Transactional
    public void completeConnection(String code, String state) {
        String email = jwtService.validateOAuthState(state);
        requireConfigured();

        Map<String, Object> tokenResponse = exchangeCode(code);
        String accessToken = String.valueOf(tokenResponse.get("access_token"));

        Map<String, Object> channelResponse = restClientBuilder.build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(CHANNEL_ENDPOINT)
                        .queryParam("part", "snippet,statistics")
                        .queryParam("mine", "true")
                        .build())
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(Map.class);

        if (channelResponse == null) {
            throw new IllegalStateException("YouTube returned no channel data.");
        }

        Object itemsValue = channelResponse.get("items");
        if (!(itemsValue instanceof java.util.List<?> items) || items.isEmpty()) {
            throw new IllegalStateException("No YouTube channel was found for the connected Google account.");
        }

        Object first = items.get(0);
        if (!(first instanceof Map<?, ?> channel)) {
            throw new IllegalStateException("Invalid YouTube channel response.");
        }

        String channelId = String.valueOf(channel.get("id"));
        Object snippetValue = channel.get("snippet");
        if (!(snippetValue instanceof Map<?, ?> snippet)) {
            throw new IllegalStateException("YouTube channel details are unavailable.");
        }

        String channelName = String.valueOf(snippet.getOrDefault("title", "YouTube Channel"));
        String handle = snippet.get("customUrl") == null ? null : String.valueOf(snippet.get("customUrl"));
        if (handle != null && !handle.startsWith("@")) {
            handle = "@" + handle;
        }

        String thumbnailUrl = null;
        Object thumbnailsValue = snippet.get("thumbnails");
        if (thumbnailsValue instanceof Map<?, ?> thumbnails) {
            Object highValue = thumbnails.get("high");
            if (highValue instanceof Map<?, ?> high && high.get("url") != null) {
                thumbnailUrl = String.valueOf(high.get("url"));
            } else {
                Object mediumValue = thumbnails.get("medium");
                if (mediumValue instanceof Map<?, ?> medium && medium.get("url") != null) {
                    thumbnailUrl = String.valueOf(medium.get("url"));
                }
            }
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("CreatorStats account not found."));

        ConnectedChannel connected = connectedChannelRepository.findByUserUserId(user.getUserId())
                .orElseGet(() -> ConnectedChannel.builder().user(user).build());

        connected.setYoutubeChannelId(channelId);
        connected.setChannelName(channelName);
        connected.setChannelHandle(handle);
        connected.setThumbnailUrl(thumbnailUrl);
        connectedChannelRepository.save(connected);
    }

    private Map<String, Object> exchangeCode(String code) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("code", code);
        form.put("client_id", clientId);
        form.put("client_secret", clientSecret);
        form.put("redirect_uri", redirectUri);
        form.put("grant_type", "authorization_code");

        Map<String, Object> response = restClientBuilder.build()
                .post()
                .uri(TOKEN_ENDPOINT)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        if (response == null || response.get("access_token") == null) {
            throw new IllegalStateException("Google authorization could not be completed.");
        }

        return response;
    }

    private void requireConfigured() {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException("YouTube OAuth is not configured. Set YOUTUBE_OAUTH_CLIENT_ID and YOUTUBE_OAUTH_CLIENT_SECRET.");
        }
    }
}
