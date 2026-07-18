package com.mchub.services.impl;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseMigrationService {

    private final MongoClient mongoClient;

    @Value("${mchub.migration.source-db:}")
    private String configuredSourceDbName;

    @Value("${mchub.migration.target-db:}")
    private String configuredTargetDbName;

    @Value("${mchub.migration.enabled:false}")
    private boolean migrationEnabled;

    @Value("${spring.data.mongodb.database}")
    private String activeDbName;

    public void migrateFromMcHub() {
        if (!migrationEnabled) {
            throw new IllegalStateException(
                    "Database migration is disabled. Set mchub.migration.enabled=true and "
                            + "mchub.migration.source-db/target-db explicitly to run it.");
        }
        if (configuredSourceDbName == null || configuredSourceDbName.isBlank()
                || configuredTargetDbName == null || configuredTargetDbName.isBlank()) {
            throw new IllegalStateException(
                    "mchub.migration.source-db and mchub.migration.target-db must be set explicitly — "
                            + "no hardcoded default is used to avoid touching the wrong database.");
        }

        String sourceDbName = configuredSourceDbName;
        String targetDbName = configuredTargetDbName;

        if (targetDbName.equals(activeDbName)) {
            throw new IllegalStateException(
                    "Refusing to migrate: target database '" + targetDbName
                            + "' is the same as the database this application instance is currently connected to ("
                            + activeDbName + "). This would drop and overwrite live data.");
        }

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
