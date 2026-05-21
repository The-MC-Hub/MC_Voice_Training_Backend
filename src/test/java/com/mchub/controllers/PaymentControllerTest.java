package com.mchub.controllers;

import com.mchub.models.User;
import com.mchub.repositories.UserRepository;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=de.flapdoodle.embed.mongo.spring.autoconfigure.EmbeddedMongoAutoConfiguration"
})
public class PaymentControllerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentController paymentController;

    @BeforeAll
    public static void setupEnv() {
        System.setProperty("MONGODB_URI", "mongodb://localhost:27017/voice-tranning");
        System.setProperty("MONGODB_DATABASE", "voice-tranning");
    }

    @Test
    public void testCreateOrder() {
        System.out.println("🔍 --- RUNNING PAYMENT CONTROLLER TEST ---");
        try {
            List<User> users = userRepository.findAll();
            System.out.println("👥 Found " + users.size() + " users in the database:");
            for (User u : users) {
                System.out.println("   - ID: " + u.getId() + ", Email: " + u.getEmail() + ", isPremium: " + u.isPremium());
            }

            // Test 'undefined' user id
            String undefinedId = "undefined";
            System.out.println("🧪 Testing createPremiumOrder for 'undefined' user ID: " + undefinedId);
            try {
                paymentController.createPremiumOrder(undefinedId);
                System.out.println("❌ Expected exception was not thrown!");
            } catch (Exception e) {
                System.out.println("ℹ️ Caught expected exception for 'undefined' user ID:");
                e.printStackTrace();
            }

            if (users.isEmpty()) {
                System.out.println("❌ No users found in database to test payment controller!");
                return;
            }

            User testUser = users.get(0);
            System.out.println("🧪 Testing createPremiumOrder for existing user ID: " + testUser.getId());
            var response = paymentController.createPremiumOrder(testUser.getId());
            System.out.println("✅ Response: " + response.getStatusCode() + " - " + response.getBody());
        } catch (Exception e) {
            System.out.println("💥 Exception in createPremiumOrder test:");
            e.printStackTrace();
        }
    }
}
