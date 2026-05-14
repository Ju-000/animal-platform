package com.animalplatform.common.api;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleResponseStatusException(ResponseStatusException exception) {
        String message = exception.getReason() == null ? "Request failed." : exception.getReason();
        return ResponseEntity
                .status(exception.getStatusCode())
                .body(new ApiResponse<>(
                        false,
                        Map.of("status", exception.getStatusCode().value()),
                        message
                ));
    }
}
