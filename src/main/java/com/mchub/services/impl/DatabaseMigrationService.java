package com.mchub.services.impl;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseMigrationService {

    private final MongoClient mongoClient;

    public void migrateFromMcHub() {
        String sourceDbName = "mchub";
        String targetDbName = "voice-tranning";

        log.info("🚀 Starting database migration from {} to {}", sourceDbName, targetDbName);

        MongoDatabase sourceDb = mongoClient.getDatabase(sourceDbName);
        MongoDatabase targetDb = mongoClient.getDatabase(targetDbName);

        List<String> collectionsToMigrate = Arrays.asList(
                "users", "mcprofiles", "scripts", "conversations", 
                "messages", "notifications", "auditlogs", "refreshtokens", "certificates"
        );

        for (String colName : collectionsToMigrate) {
            try {
                log.info("📦 Migrating collection: {}", colName);
                MongoCollection<Document> sourceCol = sourceDb.getCollection(colName);
                MongoCollection<Document> targetCol = targetDb.getCollection(colName);

                // Clear target collection first
                targetCol.drop();

                long count = 0;
                for (Document doc : sourceCol.find()) {
                    targetCol.insertOne(doc);
                    count++;
                }
                log.info("✅ Finished {} - Migrated {} documents", colName, count);
            } catch (Exception e) {
                log.error("❌ Failed to migrate {}: {}", colName, e.getMessage());
            }
        }

        log.info("🏁 Database migration completed!");
    }
}
