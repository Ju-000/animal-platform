package com.animalplatform.animal.application;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component("openRouter")
public class OpenRouterHealthIndicator implements HealthIndicator {

    private final OpenRouterProperties openRouterProperties;

    public OpenRouterHealthIndicator(OpenRouterProperties openRouterProperties) {
        this.openRouterProperties = openRouterProperties;
    }

    @Override
    public Health health() {
        Health.Builder builder = StringUtils.hasText(openRouterProperties.getApiKey())
                ? Health.up()
                : Health.down();
        return builder
                .withDetail("configured", StringUtils.hasText(openRouterProperties.getApiKey()))
                .withDetail("provider", "openrouter")
                .withDetail("model", openRouterProperties.getModel())
                .withDetail("freeModel", isFreeModel(openRouterProperties.getModel()))
                .build();
    }

    private boolean isFreeModel(String model) {
        return StringUtils.hasText(model)
                && ("openrouter/free".equals(model) || model.endsWith(":free"));
    }
}
