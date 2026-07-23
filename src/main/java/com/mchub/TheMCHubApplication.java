package com.mchub;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableMongoAuditing
@EnableScheduling
public class TheMCHubApplication {

    public static void main(String[] args) {
        
        
        System.setProperty("https.protocols", "TLSv1.2,TLSv1.3");
        System.setProperty("jdk.tls.client.protocols", "TLSv1.2,TLSv1.3");

        try {
            // Try multiple candidate directories so .env is found regardless of CWD
            String[] candidates = {
                "./",
                System.getProperty("user.dir"),
                System.getProperty("user.dir") + "/MC_Voice_Training_Backend",
            };
            Dotenv dotenv = null;
            for (String dir : candidates) {
                try {
                    dotenv = Dotenv.configure()
                            .directory(dir)
                            .ignoreIfMalformed()
                            .load();
                    System.out.println("✅ .env loaded from: " + dir);
                    break;
                } catch (Exception ignored) {}
            }
            if (dotenv != null) {
                final Dotenv d = dotenv;
                d.entries().forEach(entry -> {
                    if (System.getProperty(entry.getKey()) == null) {
                        System.setProperty(entry.getKey(), entry.getValue());
                    }
                });
            } else {
                System.out.println("⚠️ .env not found in any candidate directory — using defaults");
            }
        } catch (Exception e) {
            System.out.println("⚠️ .env file not found or corrupted: " + e.getMessage());
        }

        
        SpringApplication.run(TheMCHubApplication.class, args);
        System.out.println("🚀 The MC Hub Backend (Java Edition) is running with Virtual Threads support!");
    }
}
