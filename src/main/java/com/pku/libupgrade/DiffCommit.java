package com.pku.libupgrade;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;

import java.io.*;
import java.util.Arrays;
import java.util.List;

public class DiffCommit {
    public String commit;
    public String newCommit;
    public String clientName;
    public String pomName;
    public String libName;
    public Boolean isNew;
    public String oldVersion;
    public String newVersion;
    DiffCommit(String commit_, String newCommit_, String client_name_, String pomName_, String lib_name_, Boolean is_new_,
               String old_version_, String new_version_){
        commit = commit_;
        newCommit = newCommit_;
        clientName = client_name_;
        pomName = pomName_;
        libName = lib_name_;
        isNew = is_new_;
        oldVersion = old_version_;
        newVersion = new_version_;
    }
    public DiffCommit(String text){
        //clientName+","+newCommit+","+commit+","+pomName+","+libName+","+isNew.toString()+","+newVersion+","+ oldVersion;
        String[] temp = text.split(",");
        commit = temp[2];
        newCommit = temp[1];
        clientName = temp[0].split("\"")[1];
        pomName = temp[3];
        libName = temp[4];
        isNew = temp[5].equals("true");
        if (temp[7].equals("\""))oldVersion="";
        else oldVersion = temp[7].split("\"")[0];
        newVersion = temp[6];
    }
    public void print(){
        System.out.println("clientName: "+clientName);
        System.out.println("newCommit: "+ newCommit);
        System.out.println("oldCommit: "+commit);
        System.out.println("pomName: "+pomName);
        System.out.println("libName: "+libName);
        System.out.println("isNew: "+isNew);
        System.out.println("oldVersion: "+oldVersion);
        System.out.println("newVersion: "+newVersion);
        System.out.println("");
    }
    @Override
    public String toString(){
        return clientName+","+newCommit+","+commit+","+pomName+","+libName+","+isNew.toString()+","+newVersion+","+ oldVersion;
    }
    public void saveCSV(String filePath) throws IOException {
        // verify is existed？
        String insertContent = clientName+","+newCommit+","+commit+","+pomName+","+libName+","+isNew.toString()+","+newVersion+","+ oldVersion;
        if(isExist(insertContent,filePath)){ return ;}

        // insert
        File outFile  = new File(filePath);
        try{
            BufferedWriter writer = new BufferedWriter(new FileWriter(outFile,true));
            CsvWriter csvWriter = new CsvWriter(writer,',');

            csvWriter.write(insertContent);
            csvWriter.endRecord();
            csvWriter.flush();
            csvWriter.close();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isExist(String insertContent,String filePath) throws IOException {
        String inString;
        Boolean isExist = false;
        File inFile  = new File(filePath);
        BufferedReader reader = new BufferedReader(new FileReader(inFile));
        CsvReader csvReader = new CsvReader(reader,'，');
        while(csvReader.readRecord()) {
            inString = csvReader.getRawRecord();//读取一行数据
            inString = inString.split("\"")[1];
            if(inString.equals(insertContent)){
                isExist = true;
                break;
            }
        }
        csvReader.close();
        return isExist;
    }

    public static boolean isExistOldCommit(String oldCommit,String filePath) throws IOException {
        String inString;
        Boolean isExist = false;
        File inFile  = new File(filePath);
        BufferedReader reader = new BufferedReader(new FileReader(inFile));
        CsvReader csvReader = new CsvReader(reader,'，');
        while(csvReader.readRecord()) {
            inString = csvReader.getRawRecord();//读取一行数据
            inString = inString.split("\"")[1];
            if(inString.split(",")[2].equals(oldCommit)){
                isExist = true;
                break;
            }
        }
        csvReader.close();
        return isExist;
    }

    public static void cleanCSV(String sourceFilePath, String outputFilePath) throws IOException {
        String inString;
        File inFile  = new File(sourceFilePath);
        File outFile  = new File(outputFilePath);
        BufferedReader reader = new BufferedReader(new FileReader(inFile));
        CsvReader csvReader = new CsvReader(reader,'，');
        BufferedWriter writer = new BufferedWriter(new FileWriter(outFile,false));
        CsvWriter csvWriter = new CsvWriter(writer,',');
        while(csvReader.readRecord()) {
            inString = csvReader.getRawRecord();//读取一行数据
//            Object test = inString.split("\"");
            inString = inString.split("\"")[1];
            String version1 = inString.split(",")[6];
            List<String> tempString = Arrays.asList(inString.split(","));
            String version2;
            if (tempString.size()==7){
                version2 = "";
                continue;
            }
            else{
                version2 = inString.split(",")[7];
            }
            if(version2.contains("SNAPSHOT") || version1.contains("SNAPSHOT")){ continue; }
            if(isExistOldCommit(inString.split(",")[2],outputFilePath)){ continue; }
            if(!isExist(inString,outputFilePath)){
                csvWriter.write(inString);
                csvWriter.endRecord();
                csvWriter.flush();

            }
        }
        csvReader.close();
        csvWriter.close();
    }
}
