package com.opendb.service;

import com.opendb.config.JdbcDriverLoader;
import com.opendb.dto.ConnectionProfileResponse;
import com.opendb.dto.ConnectionRequest;
import com.opendb.dto.ConnectionResponse;
import com.opendb.dialect.DialectRegistry;
import com.opendb.exception.OpenDbException;
import com.opendb.model.ManagedConnection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class ConnectionService {

    private final ConnectionProfileService profileService;
    private final DialectRegistry dialectRegistry;
    private final JdbcDriverLoader driverLoader;
    private final Map<String, ManagedConnection> connections = new ConcurrentHashMap<>();

    public ConnectionResponse create(ConnectionRequest request) {
        ConnectionProfileResponse profile = profileService.upsert(profileService.fromConnectionRequest(request));
        return connectOrCreate(request, profile.getId());
    }

    public ConnectionResponse connectProfile(String profileId) {
        profileService.getProfile(profileId);
        ConnectionRequest request = toConnectionRequest(profileService.toConnectionRequest(profileId));
        return connectOrCreate(request, profileId);
    }

    public List<ConnectionResponse> list() {
        List<ConnectionResponse> result = new ArrayList<>();
        for (ManagedConnection managed : connections.values()) {
            result.add(toResponse(managed));
        }
        return result;
    }

    public ManagedConnection get(String id) {
        ManagedConnection managed = connections.get(id);
        if (managed == null) {
            throw new OpenDbException("Connection not found: " + id);
        }
        return managed;
    }

    public ConnectionResponse test(ConnectionRequest request) {
        ManagedConnection managed = ManagedConnection.create("test", request, null, dialectRegistry, driverLoader);
        try {
            managed.connect();
            return ConnectionResponse.builder()
                    .name(request.getName())
                    .type(request.getType())
                    .host(request.getHost())
                    .port(request.getPort())
                    .username(request.getUsername())
                    .database(request.getDatabase())
                    .connected(true)
                    .build();
        } finally {
            managed.disconnect();
        }
    }

    public void disconnect(String id) {
        ManagedConnection managed = get(id);
        managed.disconnect();
        connections.remove(id);
    }

    private ConnectionResponse connectOrCreate(ConnectionRequest request, String profileId) {
        Optional<ManagedConnection> existing = findByProfileId(profileId);
        if (existing.isPresent()) {
            ManagedConnection managed = existing.get();
            removeDuplicateProfileConnections(profileId, managed.getId());
            managed.connect();
            return toResponse(managed);
        }

        String id = UUID.randomUUID().toString();
        ManagedConnection managed = ManagedConnection.create(id, request, profileId, dialectRegistry, driverLoader);
        managed.connect();
        connections.put(id, managed);
        return toResponse(managed);
    }

    private Optional<ManagedConnection> findByProfileId(String profileId) {
        return connections.values().stream()
                .filter(connection -> profileId.equals(connection.getProfileId()))
                .findFirst();
    }

    private void removeDuplicateProfileConnections(String profileId, String keepId) {
        connections.entrySet().stream()
                .filter(entry -> profileId.equals(entry.getValue().getProfileId()))
                .filter(entry -> !entry.getKey().equals(keepId))
                .map(Map.Entry::getKey)
                .toList()
                .forEach(this::disconnect);
    }

    private ConnectionRequest toConnectionRequest(com.opendb.dto.ConnectionProfileRequest profileRequest) {
        ConnectionRequest request = new ConnectionRequest();
        request.setType(profileRequest.getType());
        request.setName(profileRequest.getName());
        request.setHost(profileRequest.getHost());
        request.setPort(profileRequest.getPort());
        request.setUsername(profileRequest.getUsername());
        request.setPassword(profileRequest.getPassword());
        request.setDatabase(profileRequest.getDatabase());
        return request;
    }

    private ConnectionResponse toResponse(ManagedConnection managed) {
        ConnectionRequest config = managed.getConfig();
        return ConnectionResponse.builder()
                .id(managed.getId())
                .profileId(managed.getProfileId())
                .name(config.getName())
                .type(config.getType())
                .host(config.getHost())
                .port(config.getPort())
                .username(config.getUsername())
                .database(config.getDatabase())
                .connected(managed.isConnected())
                .build();
    }
}
