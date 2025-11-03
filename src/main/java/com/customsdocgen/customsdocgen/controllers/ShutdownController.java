package com.customsdocgen.customsdocgen.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ShutdownController {

    private final ConfigurableApplicationContext context;


    @PostMapping("/shutdown")
    public ResponseEntity<String> shutdown() {

        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            context.close();
            System.exit(0);
        }, 500, TimeUnit.MILLISECONDS);
        
        return ResponseEntity.ok("Сервер зупиняється...");
    }

    @GetMapping("/shutdown")
    public ResponseEntity<String> shutdownGet() {
        return shutdown();
    }
}

