package com.pku.libupgrade;

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
}
