package com.creatorhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CreatorHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(CreatorHubApplication.class, args);
    }
}
