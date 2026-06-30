package com.opendb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opendb.config.AiProperties;
import com.opendb.config.OpenDbProperties;
import com.opendb.dto.AiConfigRequest;
import com.opendb.dto.AiConfigResponse;
import com.opendb.dto.AiProviderPresetResponse;
import com.opendb.exception.OpenDbException;
import com.opendb.model.AiConfig;
import com.opendb.model.AiProviderType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiConfigService {

    private final AiProperties aiProperties;
    private final OpenDbProperties openDbProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Path storagePath;
    private AiConfig config = new AiConfig();

    @PostConstruct
    void init() {
        storagePath = openDbProperties.getDataDir().resolve("ai-config.json");
        try {
            Files.createDirectories(openDbProperties.getDataDir());
        } catch (IOException e) {
            log.warn("Could not create data directory {}: {}", openDbProperties.getDataDir(), e.getMessage());
        }
        loadFromDisk();
    }

    public AiConfig getEffectiveConfig() {
        return cloneConfig(config);
    }

    public AiConfigResponse getPublicConfig() {
        return toResponse(config);
    }

    public AiConfigResponse update(AiConfigRequest request) {
        AiProviderType provider = AiProviderType.fromId(request.getProvider());
        config.setEnabled(request.isEnabled());
        config.setProvider(provider.name());
        config.setApiUrl(request.getApiUrl().trim());
        if (request.getApiKey() != null && !request.getApiKey().isBlank()) {
            config.setApiKey(request.getApiKey().trim());
        }
        config.setModel(request.getModel().trim());
        config.setApiVersion(request.getApiVersion() == null || request.getApiVersion().isBlank()
                ? "2024-02-15-preview"
                : request.getApiVersion().trim());
        config.setTimeoutSeconds(request.getTimeoutSeconds());
        config.setTemperature(request.getTemperature());
        config.setMaxTokens(request.getMaxTokens());
        validate(config);
        persist();
        return toResponse(config);
    }

    public List<AiProviderPresetResponse> listPresets() {
        return Arrays.stream(AiProviderType.values())
                .map(this::toPreset)
                .toList();
    }

    public boolean isConfigured() {
        AiProviderType provider = AiProviderType.fromId(config.getProvider());
        if (!provider.isApiKeyRequired()) {
            return config.getApiUrl() != null && !config.getApiUrl().isBlank();
        }
        return config.getApiKey() != null && !config.getApiKey().isBlank();
    }

    private AiProviderPresetResponse toPreset(AiProviderType type) {
        List<String> models = switch (type) {
            case OPENAI -> List.of("gpt-4o-mini", "gpt-4o", "gpt-4.1-mini", "gpt-4.1");
            case AZURE_OPENAI -> List.of("gpt-4o-mini", "gpt-4o", "gpt-35-turbo");
            case ANTHROPIC -> List.of("claude-3-5-sonnet-20241022", "claude-3-5-haiku-20241022", "claude-3-opus-20240229");
            case DEEPSEEK -> List.of("deepseek-chat", "deepseek-reasoner");
            case OLLAMA -> List.of("llama3.2", "qwen2.5", "deepseek-r1");
            case QWEN -> List.of("qwen-plus", "qwen-max", "qwen-turbo");
            case ZHIPU -> List.of("glm-4-flash", "glm-4-plus", "glm-4-air");
            case MOONSHOT -> List.of("moonshot-v1-8k", "moonshot-v1-32k", "moonshot-v1-128k");
            case CUSTOM -> List.of("gpt-4o-mini", "custom-model");
        };
        String description = switch (type) {
            case OPENAI -> "OpenAI 官方 API";
            case AZURE_OPENAI -> "填写 Azure 部署完整 URL，使用 api-key 请求头";
            case ANTHROPIC -> "Claude Messages API";
            case DEEPSEEK -> "DeepSeek OpenAI 兼容接口";
            case OLLAMA -> "本地 Ollama，默认无需 API Key";
            case QWEN -> "阿里云 DashScope 兼容模式";
            case ZHIPU -> "智谱 BigModel OpenAI 兼容接口";
            case MOONSHOT -> "Moonshot OpenAI 兼容接口";
            case CUSTOM -> "任意 OpenAI Chat Completions 兼容服务";
        };
        return AiProviderPresetResponse.builder()
                .id(type.name())
                .label(type.getLabel())
                .defaultApiUrl(type.getDefaultApiUrl())
                .defaultModel(type.getDefaultModel())
                .suggestedModels(models)
                .apiKeyRequired(type.isApiKeyRequired())
                .description(description)
                .build();
    }

    private void validate(AiConfig config) {
        if (config.getApiUrl() == null || config.getApiUrl().isBlank()) {
            throw new OpenDbException("API 地址不能为空");
        }
        AiProviderType provider = AiProviderType.fromId(config.getProvider());
        if (provider.isApiKeyRequired() && (config.getApiKey() == null || config.getApiKey().isBlank())) {
            throw new OpenDbException("当前 AI 提供商需要配置 API Key");
        }
    }

    private AiConfigResponse toResponse(AiConfig source) {
        AiProviderType provider = AiProviderType.fromId(source.getProvider());
        return AiConfigResponse.builder()
                .enabled(source.isEnabled())
                .provider(provider.name())
                .providerLabel(provider.getLabel())
                .apiUrl(source.getApiUrl())
                .model(source.getModel())
                .apiVersion(source.getApiVersion())
                .timeoutSeconds(source.getTimeoutSeconds())
                .temperature(source.getTemperature())
                .maxTokens(source.getMaxTokens())
                .hasApiKey(source.getApiKey() != null && !source.getApiKey().isBlank())
                .apiKeyMasked(maskApiKey(source.getApiKey()))
                .build();
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return "";
        }
        if (apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }

    private void loadFromDisk() {
        if (!Files.exists(storagePath)) {
            config = fromProperties(aiProperties);
            return;
        }
        try {
            config = objectMapper.readValue(storagePath.toFile(), AiConfig.class);
        } catch (IOException e) {
            log.warn("Failed to load AI config, using defaults: {}", e.getMessage());
            config = fromProperties(aiProperties);
        }
    }

    private AiConfig fromProperties(AiProperties properties) {
        AiConfig defaults = new AiConfig();
        defaults.setEnabled(properties.isEnabled());
        defaults.setApiUrl(properties.getApiUrl());
        defaults.setApiKey(properties.getApiKey() == null ? "" : properties.getApiKey());
        defaults.setModel(properties.getModel());
        defaults.setTimeoutSeconds(properties.getTimeoutSeconds());
        return defaults;
    }

    private void persist() {
        try {
            Files.createDirectories(storagePath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storagePath.toFile(), config);
        } catch (IOException e) {
            throw new OpenDbException("Failed to save AI config: " + e.getMessage(), e);
        }
    }

    private AiConfig cloneConfig(AiConfig source) {
        AiConfig copy = new AiConfig();
        copy.setEnabled(source.isEnabled());
        copy.setProvider(source.getProvider());
        copy.setApiUrl(source.getApiUrl());
        copy.setApiKey(source.getApiKey());
        copy.setModel(source.getModel());
        copy.setApiVersion(source.getApiVersion());
        copy.setTimeoutSeconds(source.getTimeoutSeconds());
        copy.setTemperature(source.getTemperature());
        copy.setMaxTokens(source.getMaxTokens());
        return copy;
    }
}
