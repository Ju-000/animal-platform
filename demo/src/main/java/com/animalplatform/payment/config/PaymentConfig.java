package com.animalplatform.payment.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PortOneProperties.class)
public class PaymentConfig {
}
