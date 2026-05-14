package com.animalplatform.animal.application;

public class OpenRouterApiException extends RuntimeException {

    private final String endpoint;
    private final int statusCode;

    public OpenRouterApiException(String endpoint, int statusCode, String message) {
        super(message);
        this.endpoint = endpoint;
        this.statusCode = statusCode;
    }

    public OpenRouterApiException(String endpoint, int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.endpoint = endpoint;
        this.statusCode = statusCode;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
