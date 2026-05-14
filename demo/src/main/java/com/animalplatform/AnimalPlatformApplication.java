package com.animalplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AnimalPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnimalPlatformApplication.class, args);
    }
}
