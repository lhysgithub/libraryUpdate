package com.pku.libupgrade;

import java.util.*;

import com.mongodb.client.*;
import com.mongodb.client.model.FindOneAndUpdateOptions;
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
            Document updateDoc = new Document("$set",document);
//            collection.insertOne(document);
            FindOneAndUpdateOptions findops = new FindOneAndUpdateOptions();
            findops.upsert(true);
            collection.findOneAndUpdate(document, updateDoc, findops);
//            List<Document> documents = new ArrayList<Document>();
//            documents.add(document);
//            collection.insertMany(documents);


        }catch(Exception e){
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
        }
    }
    public static List<String> findPopularLib(){
        MongoClient mongoClient = MongoClients.create("mongodb://localhost");

        // 连接到数据库
        MongoDatabase mongoDatabase = mongoClient.getDatabase("libUpdate");
        MongoCollection<Document> collection = mongoDatabase.getCollection("commitDiff");

        List<String> result = new ArrayList<>();
        FindIterable<Document> docs = collection.find(); //SELECT * FROM sample;
        Map<String, Integer> counter = new HashMap<>();
        Map<String, Integer> groupIDCounter = new HashMap<>();
        String libName = "" ;
        String groupID = "" ;
        Integer tempValue = 0;
        for (Document doc : docs) {
            libName = doc.getString("libName");
            if(!counter.containsKey(libName)){
                counter.put(libName,1);
            }
            else {
                tempValue = counter.get(libName);
                tempValue++;
                counter.put(libName,tempValue);
            }

            groupID = libName.split("\t")[0];
            if(!groupIDCounter.containsKey(groupID)){
                groupIDCounter.put(groupID,1);
            }
            else {
                tempValue = groupIDCounter.get(groupID);
                tempValue++;
                groupIDCounter.put(groupID,tempValue);
            }
        }
        counter = sortByValue2(counter);
        groupIDCounter = sortByValue2(groupIDCounter);
        System.out.println("GroupID + ArtifactID: "+counter);
        System.out.println("GroupID: "+groupIDCounter);
        return result;
    }
    public static <T> LinkedHashMap<T, Integer> sortByValue2(Map<T, Integer> hm) {
        // HashMap的entry放到List中
        List<Map.Entry<T, Integer>> list =
                new LinkedList<Map.Entry<T, Integer>>(hm.entrySet());

        //  对List按entry的value排序
        Collections.sort(list, new Comparator<Map.Entry<T, Integer>>() {
            public int compare(Map.Entry<T, Integer> o1,
                               Map.Entry<T, Integer> o2) {
                return -((o1.getValue()).compareTo(o2.getValue()));
            }
        });

        LinkedHashMap<T, Integer> res = new LinkedHashMap<>();
        for (Map.Entry<T, Integer> aa : list) {
            res.put(aa.getKey(), aa.getValue());
        }
        return res;
    }
}