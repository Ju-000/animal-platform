package com.animalplatform.animal.application;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import org.springframework.web.server.ResponseStatusException;

public class AnimalNotFoundException extends ResponseStatusException {

    public AnimalNotFoundException(String lookupToken) {
        super(NOT_FOUND, "동물 정보를 찾을 수 없습니다. lookupToken=" + lookupToken);
    }
}
