package com.opendb.service;

import com.opendb.config.OpenDbProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opendb.dto.ConnectionProfileRequest;
import com.opendb.dto.ConnectionProfileResponse;
import com.opendb.exception.OpenDbException;
import com.opendb.model.ConnectionProfile;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConnectionProfileService {

    private final OpenDbProperties openDbProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, ConnectionProfile> profiles = new ConcurrentHashMap<>();
    private Path storagePath;

    @PostConstruct
    void init() {
        storagePath = openDbProperties.getDataDir().resolve("connection-profiles.json");
        try {
            Files.createDirectories(openDbProperties.getDataDir());
        } catch (IOException e) {
            log.warn("Could not create data directory {}: {}", openDbProperties.getDataDir(), e.getMessage());
        }
        loadFromDisk();
    }

    public List<ConnectionProfileResponse> list() {
        return profiles.values().stream()
                .sorted((a, b) -> Long.compare(b.getUpdatedAt(), a.getUpdatedAt()))
                .map(this::toResponse)
                .toList();
    }

    public ConnectionProfileResponse get(String id) {
        ConnectionProfile profile = getProfile(id);
        return toResponse(profile);
    }

    public ConnectionProfile getProfile(String id) {
        ConnectionProfile profile = profiles.get(id);
        if (profile == null) {
            throw new OpenDbException("Connection profile not found: " + id);
        }
        return profile;
    }

    public ConnectionProfileResponse upsert(ConnectionProfileRequest request) {
        ConnectionProfile existing = findMatchingProfile(request).orElse(null);
        if (existing != null) {
            if (request.getPassword() == null || request.getPassword().isBlank()) {
                request.setPassword(existing.getConfig().getPassword());
            }
            existing.setConfig(request);
            existing.setUpdatedAt(System.currentTimeMillis());
            persist();
            return toResponse(existing);
        }
        return create(request);
    }

    public ConnectionProfileRequest fromConnectionRequest(com.opendb.dto.ConnectionRequest request) {
        ConnectionProfileRequest profileRequest = new ConnectionProfileRequest();
        profileRequest.setType(request.getType());
        profileRequest.setName(request.getName());
        profileRequest.setHost(request.getHost());
        profileRequest.setPort(request.getPort());
        profileRequest.setUsername(request.getUsername());
        profileRequest.setPassword(request.getPassword());
        profileRequest.setDatabase(request.getDatabase());
        return profileRequest;
    }

    private java.util.Optional<ConnectionProfile> findMatchingProfile(ConnectionProfileRequest request) {
        return profiles.values().stream()
                .filter(profile -> matches(profile.getConfig(), request))
                .findFirst();
    }

    private boolean matches(ConnectionProfileRequest left, ConnectionProfileRequest right) {
        return left.getHost().equalsIgnoreCase(right.getHost())
                && left.getPort() == right.getPort()
                && left.getUsername().equals(right.getUsername())
                && left.getName().equals(right.getName());
    }

    public ConnectionProfileResponse create(ConnectionProfileRequest request) {
        ConnectionProfile profile = ConnectionProfile.fromRequest(request);
        profiles.put(profile.getId(), profile);
        persist();
        return toResponse(profile);
    }

    public ConnectionProfileResponse update(String id, ConnectionProfileRequest request) {
        ConnectionProfile profile = getProfile(id);
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            request.setPassword(profile.getConfig().getPassword());
        }
        profile.setConfig(request);
        profile.setUpdatedAt(System.currentTimeMillis());
        persist();
        return toResponse(profile);
    }

    public void delete(String id) {
        if (profiles.remove(id) == null) {
            throw new OpenDbException("Connection profile not found: " + id);
        }
        persist();
    }

    public ConnectionProfileRequest toConnectionRequest(String id) {
        ConnectionProfile profile = getProfile(id);
        ConnectionProfileRequest request = new ConnectionProfileRequest();
        request.setType(profile.getConfig().getType());
        request.setName(profile.getConfig().getName());
        request.setHost(profile.getConfig().getHost());
        request.setPort(profile.getConfig().getPort());
        request.setUsername(profile.getConfig().getUsername());
        request.setPassword(profile.getConfig().getPassword());
        request.setDatabase(profile.getConfig().getDatabase());
        return request;
    }

    private ConnectionProfileResponse toResponse(ConnectionProfile profile) {
        ConnectionProfileRequest config = profile.getConfig();
        return ConnectionProfileResponse.builder()
                .id(profile.getId())
                .name(config.getName())
                .type(config.getType())
                .host(config.getHost())
                .port(config.getPort())
                .username(config.getUsername())
                .database(config.getDatabase())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private void loadFromDisk() {
        try {
            if (!Files.exists(storagePath)) {
                return;
            }
            List<ConnectionProfile> loaded = objectMapper.readValue(
                    storagePath.toFile(),
                    new TypeReference<List<ConnectionProfile>>() {
                    });
            profiles.clear();
            for (ConnectionProfile profile : loaded) {
                profiles.put(profile.getId(), profile);
            }
        } catch (IOException e) {
            log.warn("Failed to load connection profiles: {}", e.getMessage());
        }
    }

    private void persist() {
        try {
            Files.createDirectories(storagePath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(storagePath.toFile(), new ArrayList<>(profiles.values()));
        } catch (IOException e) {
            throw new OpenDbException("Failed to save connection profiles: " + e.getMessage(), e);
        }
    }
}
