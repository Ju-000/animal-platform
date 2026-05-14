package com.animalplatform.animal.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.openrouter")
public class OpenRouterProperties {

    private String apiKey = "";
    private String baseUrl = "https://openrouter.ai";
    private String model = "meta-llama/llama-3.3-70b-instruct:free";
    private String fallbackModels = "nvidia/nemotron-3-nano-30b-a3b:free,nvidia/nemotron-nano-9b-v2:free,openai/gpt-oss-120b:free,qwen/qwen3-14b:free";
    private String referer = "http://localhost:5173";
    private String title = "포동포동";

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getFallbackModels() {
        return fallbackModels;
    }

    public void setFallbackModels(String fallbackModels) {
        this.fallbackModels = fallbackModels;
    }

    public String getReferer() {
        return referer;
    }

    public void setReferer(String referer) {
        this.referer = referer;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
