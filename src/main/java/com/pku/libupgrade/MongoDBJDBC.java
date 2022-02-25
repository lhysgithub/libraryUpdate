package com.pku.libupgrade;

import java.util.ArrayList;
import java.util.List;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class MongoDBJDBC{
    public static void insertCommitDiff ( DiffCommit commitDiff ){
        try{
            // 连接到 mongodb 服务
            MongoClient mongoClient = MongoClients.create("mongodb://localhost");

            // 连接到数据库
            MongoDatabase mongoDatabase = mongoClient.getDatabase("libUpdate");
            MongoCollection<Document> collection = mongoDatabase.getCollection("commitDiff");

            Document document = new Document("clientName", commitDiff.clientName).
                    append("newCommit", commitDiff.newCommit).
                    append("oldCommit", commitDiff.commit).
                    append("pomName", commitDiff.pomName).
                    append("libName", commitDiff.libName).
                    append("isNew", commitDiff.isNew).
                    append("newVersion", commitDiff.newVersion).
                    append("oldVersion", commitDiff.oldVersion);
            List<Document> documents = new ArrayList<Document>();
            documents.add(document);
            collection.insertMany(documents);

        }catch(Exception e){
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
        }
    }
}