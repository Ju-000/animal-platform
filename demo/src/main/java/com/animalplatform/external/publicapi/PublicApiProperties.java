package com.animalplatform.external.publicapi;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.public-api")
public class PublicApiProperties {

    private String provider;
    private String serviceKey;
    private String abandonmentBaseUrl;
    private String shelterBaseUrl;
    private String statsBaseUrl;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getServiceKey() {
        return serviceKey;
    }

    public void setServiceKey(String serviceKey) {
        this.serviceKey = serviceKey;
    }

    public String getAbandonmentBaseUrl() {
        return abandonmentBaseUrl;
    }

    public void setAbandonmentBaseUrl(String abandonmentBaseUrl) {
        this.abandonmentBaseUrl = abandonmentBaseUrl;
    }

    public String getShelterBaseUrl() {
        return shelterBaseUrl;
    }

    public void setShelterBaseUrl(String shelterBaseUrl) {
        this.shelterBaseUrl = shelterBaseUrl;
    }

    public String getStatsBaseUrl() {
        return statsBaseUrl;
    }

    public void setStatsBaseUrl(String statsBaseUrl) {
        this.statsBaseUrl = statsBaseUrl;
    }
}
