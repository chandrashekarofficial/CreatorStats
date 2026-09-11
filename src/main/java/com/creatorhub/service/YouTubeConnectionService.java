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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
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
                .uri(UriComponentsBuilder.fromUriString(CHANNEL_ENDPOINT)
                        .queryParam("part", "snippet,statistics")
                        .queryParam("mine", "true")
                        .build()
                        .toUri())
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(Map.class);

        if (channelResponse == null) {
            throw new IllegalStateException("YouTube returned no channel data.");
        }

        Object itemsValue = channelResponse.get("items");
        if (!(itemsValue instanceof List<?> items) || items.isEmpty()) {
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

        Object titleValue = snippet.get("title");
        String channelName = titleValue == null ? "YouTube Channel" : String.valueOf(titleValue);
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

        connectedChannelRepository.findByYoutubeChannelId(channelId)
                .filter(existing -> !existing.getUser().getUserId().equals(user.getUserId()))
                .ifPresent(existing -> {
                    throw new IllegalStateException("That YouTube channel is already connected to another CreatorStats account.");
                });

        ConnectedChannel connected = connectedChannelRepository.findByUserUserId(user.getUserId())
                .orElseGet(() -> ConnectedChannel.builder().user(user).build());

        connected.setYoutubeChannelId(channelId);
        connected.setChannelName(channelName);
        connected.setChannelHandle(handle);
        connected.setThumbnailUrl(thumbnailUrl);
        connectedChannelRepository.save(connected);
    }

    public ConnectedChannel getConnectedChannel(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("CreatorStats account not found."));
        return connectedChannelRepository.findByUserUserId(user.getUserId()).orElse(null);
    }

    @Transactional
    public void disconnect(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("CreatorStats account not found."));
        connectedChannelRepository.deleteByUserUserId(user.getUserId());
    }

    private Map<String, Object> exchangeCode(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", redirectUri);
        form.add("grant_type", "authorization_code");

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
