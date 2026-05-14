package com.animalplatform.animal.config;

import com.animalplatform.animal.application.OpenRouterProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OpenRouterProperties.class)
public class AnimalAiConfig {
}
