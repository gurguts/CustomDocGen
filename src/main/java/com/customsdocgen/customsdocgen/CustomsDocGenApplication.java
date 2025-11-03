package com.customsdocgen.customsdocgen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;

@SpringBootApplication
public class CustomsDocGenApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomsDocGenApplication.class, args);
    }


    @EventListener(ApplicationReadyEvent.class)
    public void openBrowser() {
        String url = "http://localhost:8080";
        
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            try {
                desktop.browse(URI.create(url));
            } catch (IOException e) {
            }
        }
    }

}
