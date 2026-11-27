package com.blinkstay.seed;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// runs after the listing seed (@Order(10)), so the listings exist first
@Component
@RequiredArgsConstructor
@Order(20)
public class ReviewSeedInitializer implements CommandLineRunner {

    private final ReviewSeedService reviewSeedService;

    @Override
    public void run(String... args) {
        reviewSeedService.seed();
    }
}