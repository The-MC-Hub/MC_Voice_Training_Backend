package com.mchub.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.Objects;


@Configuration
public class DotenvConfig {

    @PostConstruct
    public void init() {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory("./")
                    .ignoreIfMalformed()
                    .ignoreIfMissing()
                    .load();

            dotenv.entries().forEach(entry -> {
                if (System.getProperty(entry.getKey()) == null) {
                    System.setProperty(entry.getKey(), entry.getValue());
                }
            });
            System.out.println("✅ Environment variables loaded successfully from .env");
        } catch (Exception e) {
            System.out.println("⚠️ Could not find .env file or error loading: " + Objects.requireNonNull(e).getMessage());
        }
    }
}
