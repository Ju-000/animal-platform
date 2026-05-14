package com.animalplatform.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public class SocialLoginProperties {

    private Provider google = new Provider();
    private Provider kakao = new Provider();
    private Provider naver = new Provider();

    public Provider getGoogle() {
        return google;
    }

    public void setGoogle(Provider google) {
        this.google = google;
    }

    public Provider getKakao() {
        return kakao;
    }

    public void setKakao(Provider kakao) {
        this.kakao = kakao;
    }

    public Provider getNaver() {
        return naver;
    }

    public void setNaver(Provider naver) {
        this.naver = naver;
    }

    public static class Provider {
        private String clientId;
        private String clientSecret;

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }
    }
}
