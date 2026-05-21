package com.mchub;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.Arrays;
import java.util.List;

public class MigrationTool {

    public static void main(String[] args) {
        String uri = "mongodb+srv://trungle:Pitngu%401234@maindatabase.2tirj0y.mongodb.net/mchub?retryWrites=true&w=majority&appName=MainDatabase&tlsInsecure=true";
        String sourceDbName = "mchub";
        String targetDbName = "voice-tranning";

        System.out.println("🚀 Starting standalone migration...");

        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase sourceDb = mongoClient.getDatabase(sourceDbName);
            MongoDatabase targetDb = mongoClient.getDatabase(targetDbName);

            List<String> collections = Arrays.asList(
                    "users", "mcprofiles", "scripts", "conversations", 
                    "messages", "notifications", "auditlogs", "refreshtokens", "certificates"
            );

            for (String colName : collections) {
                System.out.println("📦 Migrating: " + colName);
                MongoCollection<Document> sourceCol = sourceDb.getCollection(colName);
                MongoCollection<Document> targetCol = targetDb.getCollection(colName);

                targetCol.drop();
                
                long count = 0;
                for (Document doc : sourceCol.find()) {
                    targetCol.insertOne(doc);
                    count++;
                }
                System.out.println("✅ Finished " + colName + " (" + count + " docs)");
            }
            System.out.println("🏁 Migration successful!");
        } catch (Exception e) {
            System.err.println("❌ Migration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
