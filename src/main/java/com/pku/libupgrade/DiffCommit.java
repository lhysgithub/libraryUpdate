package com.pku.libupgrade;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;

import java.io.*;

public class DiffCommit {
    String commit;
    String newCommit;
    String clientName;
    String pomName;
    String libName;
    Boolean isNew;
    String oldVersion;
    String newVersion;
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
    public void saveCSV(){
        File outFile  = new File("commitDiff.csv");
        try{
            BufferedWriter writer = new BufferedWriter(new FileWriter(outFile,true));
            CsvWriter csvWriter = new CsvWriter(writer,',');

            csvWriter.write(clientName+","+newCommit+","+commit+","+pomName+","+libName+","+isNew.toString()+","+newVersion+","+ oldVersion);
            csvWriter.endRecord();
            csvWriter.close();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
}
